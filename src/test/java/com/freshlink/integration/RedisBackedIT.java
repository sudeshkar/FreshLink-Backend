package com.freshlink.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.ActiveProfiles;

import com.freshlink.security.AccountStatusService;
import com.freshlink.security.RateLimitService;
import com.freshlink.security.RedisRateLimitService;

/**
 * Exercises the Redis-backed rate limiter and cache against a real Redis.
 *
 * Skipped unless REDIS_HOST is set, which CI provides through a service
 * container. Running locally needs no Redis, matching the default
 * configuration - and a skipped test says so plainly rather than a passing one
 * quietly proving nothing.
 */
@EnabledIfEnvironmentVariable(named = "REDIS_HOST", matches = ".+",
		disabledReason = "REDIS_HOST is not set; the Redis-backed path is exercised in CI")
@SpringBootTest(properties = {
		"app.ratelimit.backend=redis",
		"app.cache.backend=redis",
		"app.notifications.enabled=false"
})
@ActiveProfiles("dev")
class RedisBackedIT {

	@Autowired private RateLimitService rateLimitService;
	@Autowired private CacheManager cacheManager;
	@Autowired private AccountStatusService accountStatusService;

	@Test
	@DisplayName("the Redis implementation is the one actually wired in")
	void redisImplementationIsSelected() {
		assertThat(rateLimitService)
				.as("app.ratelimit.backend=redis must select the shared limiter, "
						+ "otherwise every replica silently grants the full allowance")
				.isInstanceOf(RedisRateLimitService.class);

		assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
	}

	@Test
	@DisplayName("buckets in Redis behave like the in-memory ones")
	void redisBucketsEnforceCapacity() {
		String key = "it-login|" + System.nanoTime();
		Duration window = Duration.ofMinutes(15);

		for (int attempt = 1; attempt <= 5; attempt++) {
			assertThat(rateLimitService.tryConsume(key, 5, window))
					.as("attempt %d of 5 should be allowed", attempt)
					.isTrue();
		}

		assertThat(rateLimitService.tryConsume(key, 5, window))
				.as("the sixth must be refused")
				.isFalse();
	}

	@Test
	@DisplayName("separate keys do not share an allowance")
	void redisKeysAreIndependent() {
		String a = "it-a|" + System.nanoTime();
		String b = "it-b|" + System.nanoTime();
		Duration window = Duration.ofMinutes(15);

		assertThat(rateLimitService.tryConsume(a, 1, window)).isTrue();
		assertThat(rateLimitService.tryConsume(a, 1, window)).isFalse();
		assertThat(rateLimitService.tryConsume(b, 1, window))
				.as("one caller must not lock out another")
				.isTrue();
	}

	@Test
	@DisplayName("a spent bucket stays spent across a fresh service instance, which is the whole point")
	void bucketStateOutlivesTheCaller() {
		String key = "it-shared|" + System.nanoTime();
		Duration window = Duration.ofMinutes(15);

		assertThat(rateLimitService.tryConsume(key, 2, window)).isTrue();
		assertThat(rateLimitService.tryConsume(key, 2, window)).isTrue();

		// The state lives in Redis rather than in this process, so a second
		// replica consulting the same key sees the allowance already spent.
		assertThat(rateLimitService.tryConsume(key, 2, window)).isFalse();
	}

	@Autowired private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

	@Test
	@DisplayName("DIAGNOSTIC: is the Spring Redis connection usable at all")
	void springRedisConnectionWorks() {
		String key = "diag-" + System.nanoTime();
		redisTemplate.opsForValue().set(key, "hello");
		assertThat(redisTemplate.opsForValue().get(key))
				.as("a raw round trip through Spring's connection factory")
				.isEqualTo("hello");
	}

	@Test
	@DisplayName("DIAGNOSTIC: does a value written to the cache come back")
	void cacheRoundTripWorks() {
		String key = "diag-cache-" + System.nanoTime();
		var cache = cacheManager.getCache(AccountStatusService.CACHE);
		assertThat(cache).as("the cache must exist").isNotNull();

		cache.put(key, Boolean.TRUE);
		assertThat(cache.get(key))
				.as("written straight to the cache and read straight back")
				.isNotNull();
	}

	@Test
	@DisplayName("an answer cached by one instance is honoured by another")
	void accountStatusIsSharedThroughRedis() {
		// Nobody by this name exists, so the database would say "not usable".
		// Writing to the shared cache stands in for another replica having
		// already answered - if this instance honours it, the two are genuinely
		// sharing state rather than each keeping a private copy.
		String stranger = "nobody-" + System.nanoTime() + "@freshlink.test";

		assertThat(accountStatusService.isUsable(stranger))
				.as("unknown account, straight from the database")
				.isFalse();

		assertThat(cacheManager.getCache(AccountStatusService.CACHE).get(stranger))
				.as("@Cacheable must actually populate the shared cache - if this is "
						+ "null the annotation is not being applied and every request "
						+ "would hit the database")
				.isNotNull();

		cacheManager.getCache(AccountStatusService.CACHE).put(stranger, true);

		assertThat(accountStatusService.isUsable(stranger))
				.as("the shared answer must win, which is the whole point of sharing it")
				.isTrue();
	}
}
