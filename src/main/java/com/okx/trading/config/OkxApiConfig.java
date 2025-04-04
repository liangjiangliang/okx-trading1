package com.okx.trading.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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
        
        public String getPublicChannel() {
            return publicChannel;
        }
        
        public void setPublicChannel(String publicChannel) {
            this.publicChannel = publicChannel;
        }
        
        public String getPrivateChannel() {
            return privateChannel;
        }
        
        public void setPrivateChannel(String privateChannel) {
            this.privateChannel = privateChannel;
        }
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
    
    // 明确添加getter和setter方法以确保编译时可以正确解析
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getPassphrase() {
        return passphrase;
    }
    
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
    
    public boolean isUseMockData() {
        return useMockData;
    }
    
    public void setUseMockData(boolean useMockData) {
        this.useMockData = useMockData;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public String getConnectionMode() {
        return connectionMode;
    }
    
    public void setConnectionMode(String connectionMode) {
        this.connectionMode = connectionMode;
    }
    
    public WebSocketConfig getWs() {
        return ws;
    }
    
    public void setWs(WebSocketConfig ws) {
        this.ws = ws;
    }
} 