package com.okx.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OKX API配置类
 * 用于从配置文件中加载OKX API相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "okx.api")
public class OkxApiConfig {
    
    /**
     * API基础URL
     */
    private String baseUrl;
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * Secret Key
     */
    private String secretKey;
    
    /**
     * API密码短语
     */
    private String passphrase;
    
    /**
     * 是否使用模拟数据
     */
    private boolean useMockData;
    
    /**
     * 请求超时时间(秒)
     */
    private int timeout;
    
    /**
     * WebSocket配置
     */
    private WebSocketConfig ws = new WebSocketConfig();
    
    /**
     * WebSocket配置类
     */
    @Data
    public static class WebSocketConfig {
        /**
         * 公共频道WebSocket地址
         */
        private String publicChannel;
        
        /**
         * 私有频道WebSocket地址
         */
        private String privateChannel;
    }
} 