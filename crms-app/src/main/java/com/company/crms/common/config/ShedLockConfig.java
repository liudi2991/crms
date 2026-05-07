package com.company.crms.common.config;

import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 分布式锁：避免多实例下定时任务重复执行（DSS §8.3）。
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    public RedisLockProvider redisLockProvider(RedisConnectionFactory factory) {
        return new RedisLockProvider(factory, "crms-shedlock");
    }
}
