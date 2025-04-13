package com.okx.trading.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据存储配置类
 * 明确区分JPA和Redis存储库的扫描范围
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.okx.trading.model.entity")
@EnableJpaRepositories(basePackages = "com.okx.trading.repository")
@EnableRedisRepositories(basePackages = "com.okx.trading.repository.redis")
public class DataStoreConfig {
    
    // 如果有特定的Redis或JPA配置，可以在这里添加
    
} 