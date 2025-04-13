package com.okx.trading.config;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring DevTools配置类
 * 用于禁用自动重启功能，配合JRebel使用
 */
@Configuration
public class DevToolsConfig {

    private final Environment environment;

    public DevToolsConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 初始化时设置系统属性，禁用Spring DevTools的自动重启
     */
    @PostConstruct
    public void disableDevToolsRestart() {
        System.setProperty("spring.devtools.restart.enabled", "false");
    }
} 