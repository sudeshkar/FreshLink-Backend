package com.freshlink.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
	@Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "admin-dashboard",
            "supplier-analytics",
            "cafe-analytics",
            // Swept every minute by CacheEvictionScheduler, which is what bounds
            // how long a suspended account keeps working.
            "account-status"
        );
    }
}
