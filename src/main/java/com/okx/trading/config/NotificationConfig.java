package com.okx.trading.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 通知配置类
 */
@Slf4j
@Data
@Configuration
public class NotificationConfig {

    @Value("${notification.type:email}")
    private String notificationType;

    @Value("${notification.trade.enabled:false}")
    private boolean tradeNotificationEnabled;

    @Value("${notification.error.enabled:false}")
    private boolean errorNotificationEnabled;

    // 邮件通知配置
    @Value("${notification.email.enabled:false}")
    private boolean emailNotificationEnabled;

    @Value("${notification.email.recipient:}")
    private String emailRecipient;

    // 策略状态邮件配置
    @Value("${strategy.state.email.enabled:false}")
    private boolean strategyStateEmailEnabled;

    @Value("${strategy.state.email.hours:9,12,15,18,21,0}")
    private String strategyStateEmailHours;

    @Value("${strategy.state.email.show-details:false}")
    private boolean showStrategyDetails;

    @Value("${strategy.state.email.alert.interval:600000}")
    private long webSocketAlertTimeInterval;

    @Value("${strategy.state.email.alert.frequently:false}")
    private boolean enableDetailNotification;

    /**
     * 获取策略状态邮件发送的时间点列表
     */
    public List<Integer> getStrategyStateEmailHoursList() {
        return Arrays.stream(strategyStateEmailHours.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }

    @PostConstruct
    public void init() {
        log.info("通知配置初始化完成:");
        log.info("- 通知方式: {}", notificationType);
        log.info("- 交易通知: {}", tradeNotificationEnabled ? "已启用" : "已禁用");
        log.info("- 错误通知: {}", errorNotificationEnabled ? "已启用" : "已禁用");
        log.info("- 邮件通知: {}", emailNotificationEnabled ? "已启用" : "已禁用");
        log.info("- 策略状态邮件: {}", strategyStateEmailEnabled ? "已启用" : "已禁用");
        log.info("- 邮件发送时间点: {}", strategyStateEmailHours);

        if ("server_chan".equals(notificationType)) {
            log.info("使用 Server酱 发送通知");
        } else if ("wechat_cp".equals(notificationType)) {
            log.info("使用 企业微信 发送通知");
        } else if ("email".equals(notificationType)) {
            log.info("使用 邮件 发送通知");
        } else {
            log.warn("未知的通知方式: {}, 将使用默认的 Server酱 通知", notificationType);
        }
    }
}
