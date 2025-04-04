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
     * 连接模式: REST或WebSocket
     * REST: 使用HTTP REST API
     * WEBSOCKET: 使用WebSocket连接
     */
    private String connectionMode = "WEBSOCKET";
    
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
    
    /**
     * 获取当前连接模式是否为WebSocket
     * 
     * @return 如果当前连接模式为WebSocket则返回true，否则返回false
     */
    public boolean isWebSocketMode() {
        return "WEBSOCKET".equalsIgnoreCase(connectionMode);
    }
    
    /**
     * 获取当前连接模式是否为REST
     * 
     * @return 如果当前连接模式为REST则返回true，否则返回false
     */
    public boolean isRestMode() {
        return "REST".equalsIgnoreCase(connectionMode);
    }
} 