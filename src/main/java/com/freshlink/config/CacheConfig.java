package com.freshlink.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Caching: in-process by default, shared through Redis when asked for.
 *
 * Selected by {@code app.cache.backend}. The CacheManager is built here rather
 * than left to {@code spring.cache.type} so both options sit together and the
 * Redis one can carry real per-cache TTLs.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	private static final Duration ANALYTICS_TTL = Duration.ofMinutes(10);

	/**
	 * In-process caches. Each instance keeps its own copy, so a suspended account
	 * can stay usable on one replica after another has already noticed. They have
	 * no TTL of their own and rely on CacheEvictionScheduler sweeping them.
	 */
	@Bean
	@ConditionalOnProperty(name = "app.cache.backend", havingValue = "simple", matchIfMissing = true)
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager(
				"admin-dashboard",
				"supplier-analytics",
				"cafe-analytics");
	}

	/**
	 * Shared caches, so every instance sees the same answer. Entries carry a real
	 * TTL here, which is both simpler and more accurate than a periodic sweep.
	 */
	// Named "cacheManager" like the in-process one: the two are mutually
	// exclusive by condition, and the caching interceptor resolves the manager by
	// that conventional name.
	@Bean("cacheManager")
	@ConditionalOnProperty(name = "app.cache.backend", havingValue = "redis")
	public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration analytics =
				RedisCacheConfiguration.defaultCacheConfig().entryTtl(ANALYTICS_TTL);

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(analytics)
				.withInitialCacheConfigurations(Map.of(
						"admin-dashboard", analytics,
						"supplier-analytics", analytics,
						"cafe-analytics", analytics))
				.build();
	}
}
