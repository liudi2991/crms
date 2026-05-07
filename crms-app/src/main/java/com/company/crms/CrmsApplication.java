package com.company.crms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@MapperScan("com.company.crms.**.mapper")
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class CrmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrmsApplication.class, args);
    }
}
