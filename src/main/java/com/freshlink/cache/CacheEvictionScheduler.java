package com.freshlink.cache;

import java.time.LocalDateTime;

import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {
	
	private final CacheManager cacheManager;

    
	 @Scheduled(fixedRate = 10 * 60 * 1000)
    public void clearAnalyticsCache() {
        cacheManager.getCache("admin-dashboard").clear();
        System.out.println("Cache cleared at " + LocalDateTime.now());
    }
}
