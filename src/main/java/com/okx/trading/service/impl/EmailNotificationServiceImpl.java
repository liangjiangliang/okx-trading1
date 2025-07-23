package com.okx.trading.service.impl;

import com.okx.trading.config.NotificationConfig;
import com.okx.trading.event.WebSocketReconnectEvent;
import com.okx.trading.event.WebSocketReconnectEvent.ReconnectType;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.trade.Order;
import com.okx.trading.service.NotificationService;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.strategy.RealTimeStrategyManager;
import com.okx.trading.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


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

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Lazy
    private OkxApiService okxApiService;

    @Autowired
    private RealTimeStrategyManager realTimeStrategyManager;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private static final String SYMBOL = "BTC-USDT";
    private static final int MAX_UNCHANGED_COUNT = 3;
    private final Queue<String> priceQueue = new LinkedList<>();
    private String lastPrice = null;
    private int unchangedCount = 0;
    private Set<String[]> subscribeSymbols = new HashSet<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 用于跟踪每个符号最后接收价格的时间和价格
    private final Map<String, LocalDateTime> lastPriceUpdateTimeMap = new ConcurrentHashMap<>();
    private final Map<String, String> latestPriceMap = new ConcurrentHashMap<>();

    // 用于记录WebSocket频道重启次数
    private final Map<ReconnectType, Integer> channelRestartCountMap = new EnumMap<>(ReconnectType.class);

    // 记录自动重新订阅的次数
    private final Map<String, Integer> symbolResubscribeCountMap = new ConcurrentHashMap<>();

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private WebSocketUtil webSocketUtil;


    public EmailNotificationServiceImpl(RealTimeStrategyManager realTimeStrategyManager) {
        this.realTimeStrategyManager = realTimeStrategyManager;
    }

    public EmailNotificationServiceImpl() {
        // 初始化各频道重启次数
        for (ReconnectType type : ReconnectType.values()) {
            channelRestartCountMap.put(type, 0);
        }
    }

    /**
     * 监听WebSocket重连事件并发送告警邮件
     *
     * @param event WebSocket重连事件
     */
    @EventListener
    @Async
    public void onWebSocketReconnect(WebSocketReconnectEvent event) {
        ReconnectType type = event.getType();

        // 增加重启次数
        int count = channelRestartCountMap.getOrDefault(type, 0) + 1;
        channelRestartCountMap.put(type, count);
        if (count == 1) {
            log.info("初次订阅{}频道不进行邮件发送通知", type.name());
        }

        // 发送重启告警邮件
        sendWebSocketRestartAlert(type, count);

        log.info("WebSocket频道 {} 重启，当前重启次数: {}", type, count);
    }

    /**
     * 发送WebSocket重启告警邮件
     *
     * @param type  重启的频道类型
     * @param count 当前重启次数
     */
    private void sendWebSocketRestartAlert(ReconnectType type, int count) {
        try {
            if (!notificationConfig.isEmailNotificationEnabled()) {
                log.warn("邮件通知未启用，无法发送WebSocket重启告警");
                return;
            }

            String subject = "【WebSocket告警】" + type + "频道已重启";
            String time = LocalDateTime.now().format(formatter);

            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>");
            content.append("<h2 style='color: #cc0000;'>WebSocket重启告警</h2>");
            content.append("<div style='background-color: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

            // 基本信息
            content.append("<h3 style='color: #cc0000;'>重启信息</h3>");
            content.append("<p><strong>频道类型：</strong>").append(getChannelTypeName(type)).append("</p>");
            content.append("<p><strong>重启时间：</strong>").append(time).append("</p>");
            content.append("<p><strong>当前重启次数：</strong><span style='color: #cc0000; font-weight: bold;'>")
                    .append(count).append("</span></p>");

            // 其他频道重启情况
            content.append("<h3 style='color: #0066cc;'>其他频道重启情况</h3>");
            content.append("<ul style='list-style-type: none; padding-left: 10px;'>");

            for (Map.Entry<ReconnectType, Integer> entry : channelRestartCountMap.entrySet()) {
                if (entry.getKey() != type) {
                    content.append("<li><strong>").append(getChannelTypeName(entry.getKey()))
                            .append("：</strong> ").append(entry.getValue()).append("次</li>");
                }
            }

            content.append("</ul>");
            content.append("</div>");
            content.append("<p style='font-size: 12px; color: #666; margin-top: 20px;'>此邮件由系统自动发送，请勿回复。</p>");
            content.append("</div>");

            sendEmail(notificationConfig.getEmailRecipient(), subject, content.toString());
            log.info("已发送WebSocket重启告警邮件，频道: {}, 重启次数: {}", type, count);
        } catch (Exception e) {
            log.error("发送WebSocket重启告警邮件失败", e);
        }
    }

    /**
     * 获取频道类型的中文名称
     *
     * @param type 频道类型
     * @return 中文名称
     */
    private String getChannelTypeName(ReconnectType type) {
        switch (type) {
            case PUBLIC:
                return "公共频道";
            case PRIVATE:
                return "私有频道";
            case BUSINESS:
                return "业务频道";
            default:
                return type.name();
        }
    }

    /**
     * 获取指定频道的重启次数
     *
     * @param type 频道类型
     * @return 重启次数
     */
    public int getChannelRestartCount(ReconnectType type) {
        return channelRestartCountMap.getOrDefault(type, 0);
    }

    /**
     * 重置指定频道的重启次数
     *
     * @param type 频道类型
     */
    public void resetChannelRestartCount(ReconnectType type) {
        channelRestartCountMap.put(type, 0);
        log.info("已重置{}频道的重启计数", type);
    }

    /**
     * 更新最新价格
     * 由WebSocket服务在收到K线数据时调用
     *
     * @param symbol 交易对
     * @param price  最新价格
     */
    @Override
    public void updateLatestPrice(String symbol, BigDecimal price) {
        if (price == null || symbol == null) {
            return;
        }

        String priceStr = price.toString();
        latestPriceMap.put(symbol, priceStr);
        lastPriceUpdateTimeMap.put(symbol, LocalDateTime.now());

        log.debug("从WebSocket更新{}的最新价格: {}", symbol, priceStr);
    }

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

    /**
     * 每30秒执行一次，检查价格变化
     */
    @Override
    @Scheduled(fixedRate = 30000)
    public void monitorPrice() {
        try {
            // 从本地缓存获取价格
            String currentPrice = latestPriceMap.get(SYMBOL);

            if (currentPrice == null) {
                log.warn("无法获取{}的最新价格", SYMBOL);
                return;
            }

            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();

            // 记录上次更新时间
            LocalDateTime lastUpdateTime = lastPriceUpdateTimeMap.getOrDefault(SYMBOL, now);

            // 计算上次价格更新到现在的时间间隔（秒）
            long secondsSinceLastUpdate = java.time.Duration.between(lastUpdateTime, now).getSeconds();

            // 记录到队列中
            priceQueue.add(currentPrice);
            if (priceQueue.size() > MAX_UNCHANGED_COUNT) {
                priceQueue.poll(); // 保持队列长度
            }

            log.debug("当前{}价格: {}，上次更新时间: {}, 距离上次更新: {}秒",
                    SYMBOL, currentPrice, lastUpdateTime.format(formatter), secondsSinceLastUpdate);

            // 检查价格是否变化
            if (lastPrice != null && lastPrice.equals(currentPrice)) {
                unchangedCount++;
                log.debug("{}价格连续{}次未变化, 已经{}秒未更新", SYMBOL, unchangedCount, secondsSinceLastUpdate);

                // 如果超过30秒没有收到新价格，并且连续3次价格相同，发送告警并重新订阅
                if (unchangedCount >= MAX_UNCHANGED_COUNT && secondsSinceLastUpdate >= 30) {
                    // 检查队列中的所有价格是否都相同
                    boolean allSame = priceQueue.stream().allMatch(price -> price.equals(currentPrice));

                    if (allSame) {
                        // 发送价格告警邮件
                        sendPriceAlertEmail(currentPrice, secondsSinceLastUpdate);

                        // 重新订阅相关频道
                        resubscribeChannels();

                        // 重置计数器，避免连续告警
                        unchangedCount = 0;
                    }
                }
            } else {
                // 价格发生变化，重置计数器
                unchangedCount = 0;
            }

            // 更新上次价格
            lastPrice = currentPrice;

        } catch (Exception e) {
            log.error("监控价格时发生错误", e);
        }
    }

    /**
     * 重新订阅所有监控的交易对频道
     */
    private void resubscribeChannels() {
        log.info("检测到价格未更新，开始重新订阅频道");

        // 1. 触发WebSocket重连事件
        applicationEventPublisher.publishEvent(new WebSocketReconnectEvent(this, ReconnectType.PUBLIC));

        // 2. 针对每个监控的交易对和时间间隔重新订阅
        Set<String> subscribeSymbols = realTimeStrategyManager.getRunningStrategies().values().stream()
                .map(x -> x.getSymbol() + ":" + x.getInterval()).distinct().collect(Collectors.toSet());
        for (String symbolInterval : subscribeSymbols) {
            // 更新重新订阅计数
            int count = symbolResubscribeCountMap.getOrDefault(symbolInterval, 0) + 1;
            symbolResubscribeCountMap.put(symbolInterval, count);
        }

        // 发送重新订阅通知邮件
        sendResubscribeNotification();
    }

    /**
     * 发送重新订阅通知邮件
     */
    private void sendResubscribeNotification() {
        try {
            String subject = "【频道重新订阅通知】价格监控已触发自动重新订阅";

            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>");
            content.append("<h2 style='color: #ff9900;'>频道自动重新订阅通知</h2>");
            content.append("<div style='background-color: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

            content.append("<p>系统检测到价格数据连续").append(MAX_UNCHANGED_COUNT).append("次未更新，已自动重新订阅相关频道。</p>");

            content.append("<h3 style='color: #0066cc;'>重新订阅详情</h3>");
            content.append("<table style='border-collapse: collapse; width: 100%; margin-top: 10px;'>");
            content.append("<tr style='background-color: #f2f2f2;'>");
            content.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>交易对</th>");
            content.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>时间间隔</th>");
            content.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>重新订阅次数</th>");
            content.append("</tr>");
            realTimeStrategyManager.getRunningStrategies()
                    .values().stream()
                    .map(x -> x.getSymbol() + ":" + x.getInterval())
                    .distinct()
                    .forEach(strategy -> {
                        String[] split = strategy.split(":");
                        String symbol = split[0];
                        String interval = split[1];
                        String key = symbol + ":" + interval;
                        int count = symbolResubscribeCountMap.getOrDefault(key, 0);

                        content.append("<tr>");
                        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(symbol).append("</td>");
                        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(interval).append("</td>");
                        content.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(count).append("</td>");
                        content.append("</tr>");
                    });

            content.append("</table>");
            content.append("<p style='margin-top: 15px;'>重新订阅时间：").append(LocalDateTime.now().format(formatter)).append("</p>");

            content.append("</div>");
            content.append("<p style='font-size: 12px; color: #666; margin-top: 20px;'>此邮件由系统自动发送，请勿回复。</p>");
            content.append("</div>");

            sendEmail(notificationConfig.getEmailRecipient(), subject, content.toString());
            log.info("已发送频道重新订阅通知邮件");

        } catch (Exception e) {
            log.error("发送重新订阅通知邮件失败", e);
        }
    }

    /**
     * 发送价格告警邮件
     *
     * @param price                  当前价格
     * @param secondsSinceLastUpdate 上次更新到现在的秒数
     */
    private void sendPriceAlertEmail(String price, long secondsSinceLastUpdate) {
        try {
            String subject = "【价格监控告警】" + SYMBOL + "价格连续" + MAX_UNCHANGED_COUNT + "次未变化";
            String time = LocalDateTime.now().format(formatter);

            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>");
            content.append("<h2 style='color: #cc0000;'>价格监控告警</h2>");
            content.append("<div style='background-color: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

            // 告警信息
            content.append("<h3 style='color: #cc0000;'>告警详情</h3>");
            content.append("<p><strong>币种：</strong>").append(SYMBOL).append("</p>");
            content.append("<p><strong>当前价格：</strong>").append(price).append("</p>");
            content.append("<p><strong>告警时间：</strong>").append(time).append("</p>");
            content.append("<p><strong>告警原因：</strong>连续")
                    .append(MAX_UNCHANGED_COUNT)
                    .append("次（<span style='color: #cc0000; font-weight: bold;'>")
                    .append(secondsSinceLastUpdate)
                    .append("</span>秒）价格未发生变化</p>");

            content.append("<div style='background-color: #fff8e1; padding: 10px; border-left: 4px solid #ffca28; margin: 15px 0;'>");
            content.append("<p style='margin: 0;'><strong>注意：</strong>系统将自动重新订阅相关频道。</p>");
            content.append("</div>");

            content.append("</div>");
            content.append("<p style='font-size: 12px; color: #666; margin-top: 20px;'>此邮件由系统自动发送，请勿回复。</p>");
            content.append("</div>");

            sendEmail(notificationConfig.getEmailRecipient(), subject, content.toString());
            log.info("已发送{}价格告警邮件", SYMBOL);
        } catch (Exception e) {
            log.error("发送告警邮件失败", e);
        }
    }
}
