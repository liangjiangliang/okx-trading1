package com.okx.trading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 通知配置类
 */
@Slf4j
@Configuration
public class NotificationConfig {

    @Value("${notification.type:server_chan}")
    private String notificationType;
    
    @Value("${notification.trade.enabled:false}")
    private boolean tradeNotificationEnabled;
    
    @Value("${notification.error.enabled:false}")
    private boolean errorNotificationEnabled;
    
    @PostConstruct
    public void init() {
        log.info("通知配置初始化完成:");
        log.info("- 通知方式: {}", notificationType);
        log.info("- 交易通知: {}", tradeNotificationEnabled ? "已启用" : "已禁用");
        log.info("- 错误通知: {}", errorNotificationEnabled ? "已启用" : "已禁用");
        
        if ("server_chan".equals(notificationType)) {
            log.info("使用 Server酱 发送通知");
        } else if ("wechat_cp".equals(notificationType)) {
            log.info("使用 企业微信 发送通知");
        } else {
            log.warn("未知的通知方式: {}, 将使用默认的 Server酱 通知", notificationType);
        }
    }
} 