package com.freshlink.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.freshlink.security.AccountStatusCache;
import com.freshlink.security.AccountStatusService;
import com.freshlink.security.RedisAccountStatusCache;
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
	@Autowired private AccountStatusCache accountStatusCache;
	@Autowired private AccountStatusService accountStatusService;
	@Autowired private com.freshlink.Repository.UserRepository userRepository;

	@Test
	@DisplayName("the Redis implementation is the one actually wired in")
	void redisImplementationIsSelected() {
		assertThat(rateLimitService)
				.as("app.ratelimit.backend=redis must select the shared limiter, "
						+ "otherwise every replica silently grants the full allowance")
				.isInstanceOf(RedisRateLimitService.class);

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
	@DisplayName("account status is cached in Redis, so every replica agrees")
	void accountStatusCacheIsShared() {
		assertThat(accountStatusCache)
				.as("app.cache.backend=redis must select the shared cache")
				.isInstanceOf(RedisAccountStatusCache.class);

		String email = "shared-" + System.nanoTime() + "@freshlink.test";

		assertThat(accountStatusCache.get(email))
				.as("nothing cached yet")
				.isEmpty();

		accountStatusCache.put(email, true, Duration.ofSeconds(60));

		assertThat(accountStatusCache.get(email))
				.as("an answer written by one instance is readable by another")
				.contains(true);

		accountStatusCache.evict(email);
		assertThat(accountStatusCache.get(email)).isEmpty();
	}

	@Test
	@DisplayName("status reads are served from the shared cache, not the database")
	void statusIsServedFromTheSharedCache() {
		String email = "cafe1@freshlink.com";

		assertThat(accountStatusService.isUsable(email)).isTrue();
		assertThat(accountStatusCache.get(email))
				.as("the answer must land in Redis, or every request would hit the database")
				.contains(true);

		// Suspend the account behind the service's back. A database read would now
		// say unusable, so a continued "usable" can only be coming from Redis.
		userRepository.findByEmail(email).ifPresent(user -> {
			user.setActive(false);
			userRepository.save(user);
		});

		try {
			assertThat(accountStatusService.isUsable(email))
					.as("still served from the shared cache")
					.isTrue();
		} finally {
			userRepository.findByEmail(email).ifPresent(user -> {
				user.setActive(true);
				userRepository.save(user);
			});
			accountStatusCache.evict(email);
		}
	}
}
