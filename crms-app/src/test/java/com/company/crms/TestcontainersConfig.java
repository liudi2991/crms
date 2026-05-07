package com.company.crms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基础设施：MySQL + Redis 容器，由 Spring Boot 测试自动注入连接信息。
 *
 * <p>用法：
 * <pre>
 *   SpringApplication.from(CrmsApplication::main)
 *       .with(TestcontainersConfig.class)
 *       .run(args);
 * </pre>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("crms")
                .withUsername("root")
                .withPassword("root")
                .withCommand("--character-set-server=utf8mb4",
                             "--collation-server=utf8mb4_0900_ai_ci",
                             "--default-authentication-plugin=mysql_native_password");
    }

    @Bean
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(6379);
    }

    public static void main(String[] args) {
        SpringApplication.from(CrmsApplication::main)
                .with(TestcontainersConfig.class)
                .run(args);
    }
}
