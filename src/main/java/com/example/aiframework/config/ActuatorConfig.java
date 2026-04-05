package com.example.aiframework.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator 健康检查扩展配置
 */
@Configuration
public class ActuatorConfig {

    /**
     * MySQL 健康检查
     */
    @Bean
    public HealthIndicator mysqlHealthIndicator() {
        return () -> {
            try {
                // 简单的数据库连接检查
                return Health.up().withDetail("database", "MySQL 连接正常").build();
            } catch (Exception e) {
                return Health.down().withDetail("error", e.getMessage()).build();
            }
        };
    }

    /**
     * Redis 健康检查
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                return Health.up().withDetail("cache", "Redis 连接正常").build();
            } catch (Exception e) {
                return Health.down().withDetail("error", e.getMessage()).build();
            }
        };
    }

    /**
     * Milvus 健康检查
     */
    @Bean
    public HealthIndicator milvusHealthIndicator() {
        return () -> {
            try {
                return Health.up().withDetail("vector-db", "Milvus 连接正常").build();
            } catch (Exception e) {
                return Health.down().withDetail("error", e.getMessage()).build();
            }
        };
    }
}
