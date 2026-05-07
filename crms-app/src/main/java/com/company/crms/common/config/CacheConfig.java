package com.company.crms.common.config;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 简易内存缓存（看板/报表/系统参数）。
 * 多实例下若需共享，可替换为 RedisCacheManager。
 */
@Configuration
public class CacheConfig {

    @Bean
    public org.springframework.cache.CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "system-params",
                "report-dashboard",
                "report-trend",
                "report-top-customers"
        );
    }

    @Bean
    public CacheManagerCustomizer<ConcurrentMapCacheManager> customizer() {
        return cm -> cm.setAllowNullValues(false);
    }
}
