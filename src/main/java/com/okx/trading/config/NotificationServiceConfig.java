package com.okx.trading.config;

import com.okx.trading.service.NotificationService;
import com.okx.trading.service.impl.EmailNotificationServiceImpl;
import com.okx.trading.service.impl.ServerChanNotificationServiceImpl;
import com.okx.trading.service.impl.WechatCpNotificationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 通知服务配置类
 * 根据配置选择使用哪种通知服务
 */
@Slf4j
@Configuration
public class NotificationServiceConfig {

    @Value("${notification.type:server_chan}")
    private String notificationType;

    @Autowired(required = false)
    private ServerChanNotificationServiceImpl serverChanNotificationService;

    @Autowired(required = false)
    private WechatCpNotificationServiceImpl wechatCpNotificationService;
    
    @Autowired(required = false)
    private EmailNotificationServiceImpl emailNotificationService;

    @Bean
    @Primary
    public NotificationService notificationService() {
        switch (notificationType) {
            case "server_chan":
                if (serverChanNotificationService != null) {
                    log.info("使用Server酱通知服务");
                    return serverChanNotificationService;
                }
                break;
            case "wechat_cp":
                if (wechatCpNotificationService != null) {
                    log.info("使用企业微信通知服务");
                    return wechatCpNotificationService;
                }
                break;
            case "email":
                if (emailNotificationService != null) {
                    log.info("使用邮件通知服务");
                    return emailNotificationService;
                }
                break;
            default:
                log.warn("未知的通知类型: {}, 将使用默认的Server酱通知服务", notificationType);
                break;
        }
        
        // 默认使用Server酱通知服务
        if (serverChanNotificationService != null) {
            return serverChanNotificationService;
        } else if (wechatCpNotificationService != null) {
            return wechatCpNotificationService;
        } else if (emailNotificationService != null) {
            return emailNotificationService;
        } else {
            log.warn("没有可用的通知服务，将使用空实现");
            // 返回一个空实现，避免NPE
            return new NotificationService() {
                @Override
                public boolean sendTradeNotification(com.okx.trading.model.entity.RealTimeStrategyEntity strategy, com.okx.trading.model.trade.Order order, String side, String signalPrice) {
                    log.warn("没有配置通知服务，交易通知未发送");
                    return false;
                }

                @Override
                public boolean sendStrategyErrorNotification(com.okx.trading.model.entity.RealTimeStrategyEntity strategy, String errorMessage) {
                    log.warn("没有配置通知服务，错误通知未发送");
                    return false;
                }
            };
        }
    }
} 