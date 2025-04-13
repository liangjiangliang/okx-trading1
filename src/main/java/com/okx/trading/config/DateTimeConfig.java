package com.okx.trading.config;

import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

/**
 * 日期时间格式化配置
 * 用于解决Spring MVC中日期时间格式化问题
 */
@Configuration
public class DateTimeConfig {

    /**
     * 配置全局日期时间格式
     * 允许处理不同格式的日期时间字符串
     *
     * @return FormattingConversionService
     */
    @Bean
    public FormattingConversionService conversionService() {
        DefaultFormattingConversionService conversionService = 
            new DefaultFormattingConversionService(false);

        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        registrar.registerFormatters(conversionService);

        return conversionService;
    }
} 