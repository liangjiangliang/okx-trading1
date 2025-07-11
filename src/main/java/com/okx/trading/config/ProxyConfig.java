package com.okx.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP代理配置类
 * 用于从配置文件中加载代理相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "okx.proxy")
public class ProxyConfig {

    private boolean httpsEnable;

    /**
     * 是否启用代理
     */
    private boolean enabled;

    /**
     * 代理主机地址
     */
    private String host;

    /**
     * 代理端口
     */
    private int port;
}
