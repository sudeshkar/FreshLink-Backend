package com.freshlink.cache;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.freshlink.security.AccountStatusService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {
	
	private static final Logger log = LoggerFactory.getLogger(CacheEvictionScheduler.class);

	private final CacheManager cacheManager;

    
	@Scheduled(fixedRate = 10 * 60 * 1000)
    public void clearAnalyticsCache() {
        cacheManager.getCache("admin-dashboard").clear();
        log.debug("Analytics cache cleared at {}", LocalDateTime.now());
    }

    /**
     * Swept far more often than the analytics: this cache is what lets a
     * suspended or removed account keep making requests, so the sweep interval
     * is the upper bound on how long that lasts.
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void clearAccountStatusCache() {
        cacheManager.getCache(AccountStatusService.CACHE).clear();
    }
}
