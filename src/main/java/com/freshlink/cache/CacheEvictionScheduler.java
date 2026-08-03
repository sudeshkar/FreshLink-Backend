package com.freshlink.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Sweeps the in-process analytics caches, which have no expiry of their own.
 * Redis caches carry real TTLs, so this is not created when they are in use.
 *
 * Account status is not swept here: it expires its own entries, since that
 * expiry is what bounds how long a revoked account keeps working and is too
 * important to depend on a scheduler having run.
 */
@Component
@ConditionalOnProperty(name = "app.cache.backend", havingValue = "simple", matchIfMissing = true)
@RequiredArgsConstructor
public class CacheEvictionScheduler {

	private static final Logger log = LoggerFactory.getLogger(CacheEvictionScheduler.class);

	private final CacheManager cacheManager;

	@Scheduled(fixedRate = 10 * 60 * 1000)
	public void clearAnalyticsCache() {
		cacheManager.getCache("admin-dashboard").clear();
		log.debug("Analytics cache cleared");
	}
}
