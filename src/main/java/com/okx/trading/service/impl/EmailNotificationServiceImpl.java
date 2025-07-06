package com.okx.trading.service.impl;

import com.okx.trading.config.NotificationConfig;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.trade.Order;
import com.okx.trading.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件通知服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "notification.type", havingValue = "email")
public class EmailNotificationServiceImpl implements NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationConfig notificationConfig;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean sendTradeNotification(RealTimeStrategyEntity strategy, Order order, String side, String signalPrice) {
        if (!notificationConfig.isTradeNotificationEnabled() || !notificationConfig.isEmailNotificationEnabled()) {
            return false;
        }

        String subject = String.format("[交易提醒] %s %s %s", strategy.getSymbol(), "BUY".equals(side) ? "买入" : "卖出", strategy.getStrategyName());

        StringBuilder content = new StringBuilder();
        content.append("<div style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>");
        content.append("<h2 style='color: #333;'>交易执行通知</h2>");
        content.append("<div style='background-color: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

        // 交易基本信息
        content.append("<h3 style='color: #0066cc;'>交易信息</h3>");
        content.append("<p><strong>策略名称：</strong>").append(strategy.getStrategyName()).append("</p>");
        content.append("<p><strong>交易对：</strong>").append(strategy.getSymbol()).append("</p>");
        content.append("<p><strong>交易方向：</strong><span style='color: ").append("BUY".equals(side) ? "#00aa00" : "#cc0000").append(";'>");
        content.append("BUY".equals(side) ? "买入" : "卖出").append("</span></p>");
        content.append("<p><strong>交易时间：</strong>").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("</p>");

        // 订单详情
        if (order != null) {
            content.append("<h3 style='color: #0066cc;'>订单详情</h3>");
            content.append("<p><strong>订单ID：</strong>").append(order.getOrderId()).append("</p>");
            content.append("<p><strong>金额：</strong>").append(order.getCummulativeQuoteQty()).append("</p>");
            content.append("<p><strong>价格：</strong>").append(order.getPrice()).append("</p>");
            if ("SELL".equals(side)) {
                content.append("<p><strong>利润：</strong>").append(BigDecimal.valueOf(strategy.getLastTradeProfit()).setScale(8, BigDecimal.ROUND_HALF_UP)).append("</p>");
            }
            content.append("<p><strong>数量：</strong>").append(order.getExecutedQty()).append("</p>");
            content.append("<p><strong>费用：</strong>").append(order.getFee()).append("</p>");

        }

        // 信号价格
        if (signalPrice != null) {
            content.append("<p><strong>信号价格：</strong>").append(signalPrice).append("</p>");
        }

        content.append("</div>");
        content.append("<p style='font-size: 12px; color: #666; margin-top: 20px;'>此邮件由系统自动发送，请勿回复。</p>");
        content.append("</div>");

        return sendEmail(notificationConfig.getEmailRecipient(), subject, content.toString());
    }

    @Override
    public boolean sendStrategyErrorNotification(RealTimeStrategyEntity strategy, String errorMessage) {
        if (!notificationConfig.isErrorNotificationEnabled() || !notificationConfig.isEmailNotificationEnabled()) {
            return false;
        }

        String subject = String.format("[错误警告] %s 策略异常 - %s", strategy.getSymbol(), strategy.getStrategyCode());

        StringBuilder content = new StringBuilder();
        content.append("<div style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>");
        content.append("<h2 style='color: #cc0000;'>策略执行异常</h2>");
        content.append("<div style='background-color: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

        // 错误信息
        content.append("<h3 style='color: #cc0000;'>错误详情</h3>");
        content.append("<p><strong>策略名称：</strong>").append(strategy.getStrategyName()).append("</p>");
        content.append("<p><strong>交易对：</strong>").append(strategy.getSymbol()).append("</p>");
        content.append("<p><strong>时间间隔：</strong>").append(strategy.getInterval()).append("</p>");
        content.append("<p><strong>错误时间：</strong>").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("</p>");
        content.append("<p><strong>错误信息：</strong></p>");
        content.append("<pre style='background-color: #f8f8f8; padding: 10px; border-radius: 3px; overflow-x: auto;'>").append(strategy.getMessage()).append("</pre>");

        content.append("</div>");
        content.append("<p style='font-size: 12px; color: #666; margin-top: 20px;'>此邮件由系统自动发送，请勿回复。</p>");
        content.append("</div>");

        return sendEmail(notificationConfig.getEmailRecipient(), subject, content.toString());
    }

    /**
     * 发送邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML内容
     * @return 是否发送成功
     */
    private boolean sendEmail(String to, String subject, String content) {
        try {
            if (to == null || to.isEmpty()) {
                log.error("邮件接收人为空，无法发送邮件");
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true表示支持HTML内容

            mailSender.send(message);
            log.info("邮件发送成功: {}", subject);
            return true;
        } catch (MessagingException e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
