package com.okx.trading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于配置Web相关功能，如字符编码等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 移除characterEncodingFilter方法，使用Spring Boot的自动配置
} 