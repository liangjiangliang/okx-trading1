package com.okx.trading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 日志目录配置类
 * 负责在应用启动时创建必要的日志目录结构
 */
@Configuration
public class LogDirConfig {
    private static final Logger log = LoggerFactory.getLogger(LogDirConfig.class);
    private static final String LOG_DIR = "logs";

    /**
     * 在应用启动时创建日志目录结构
     * @return CommandLineRunner实例
     */
    @Bean
    public CommandLineRunner initLogDirectories() {
        return args -> {
            log.info("正在初始化日志目录结构...");
            
            // 创建主日志目录
            createDirectoryIfNotExists(LOG_DIR);
            
            // 创建子目录
            createDirectoryIfNotExists(LOG_DIR + "/all");
            createDirectoryIfNotExists(LOG_DIR + "/error");
            createDirectoryIfNotExists(LOG_DIR + "/api");
            
            log.info("日志目录结构初始化完成");
        };
    }
    
    /**
     * 如果目录不存在则创建
     * @param dirPath 目录路径
     */
    private void createDirectoryIfNotExists(String dirPath) {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("创建日志目录: {}", path.toAbsolutePath());
            } catch (IOException e) {
                log.error("无法创建日志目录: {}, 错误: {}", path.toAbsolutePath(), e.getMessage());
            }
        } else {
            log.info("日志目录已存在: {}", path.toAbsolutePath());
        }
    }
} 