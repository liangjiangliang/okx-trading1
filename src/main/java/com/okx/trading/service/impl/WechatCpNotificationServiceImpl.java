package com.okx.trading.service.impl;

import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.trade.Order;
import com.okx.trading.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 企业微信通知服务实现类
 * 仅在选择企业微信通知方式时生效
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "notification.type", havingValue = "wechat_cp")
public class WechatCpNotificationServiceImpl implements NotificationService {

    private final WxCpService wxCpService;
    private final List<String> userIds;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${notification.trade.enabled:false}")
    private boolean tradeNotificationEnabled;

    @Value("${notification.error.enabled:false}")
    private boolean errorNotificationEnabled;

    @Autowired
    public WechatCpNotificationServiceImpl(WxCpService wxCpService,
                                           @Value("${wechat.cp.userIds:}") String userIdsStr) {
        this.wxCpService = wxCpService;
        this.userIds = Arrays.asList(userIdsStr.split(","));
    }

    @Override
    public boolean sendTradeNotification(RealTimeStrategyEntity strategy, Order order, String side, String signalPrice) {
        if (!tradeNotificationEnabled) {
            return false;
        }

        try {
            // 构建通知内容
            String title = String.format("【交易提醒】%s %s 信号", strategy.getStrategyName(), side);

            StringBuilder content = new StringBuilder();
            content.append("策略名称: ").append(strategy.getStrategyName()).append("\n");
            content.append("交易对: ").append(strategy.getSymbol()).append("\n");
            content.append("K线周期: ").append(strategy.getInterval()).append("\n");
            content.append("交易类型: ").append(side).append("\n");
            content.append("信号价格: ").append(signalPrice).append("\n");
            content.append("成交价格: ").append(order.getPrice()).append("\n");

            if ("BUY".equals(side)) {
                content.append("买入金额: ").append(order.getCummulativeQuoteQty()).append("\n");
                content.append("买入数量: ").append(order.getExecutedQty()).append("\n");
            } else {
                content.append("卖出数量: ").append(order.getExecutedQty()).append("\n");
                content.append("卖出金额: ").append(order.getCummulativeQuoteQty()).append("\n");

                // 计算利润
                if (strategy.getLastTradeAmount() != null) {
                    BigDecimal profit = order.getCummulativeQuoteQty().subtract(BigDecimal.valueOf(strategy.getLastTradeAmount()));
                    BigDecimal profitRate = profit.divide(BigDecimal.valueOf(strategy.getLastTradeAmount()), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    content.append("本次利润: ").append(profit).append(" (").append(profitRate).append("%)\n");
                }
            }

            content.append("手续费: ").append(order.getFee()).append(" ").append(order.getFeeCurrency()).append("\n");
            content.append("成交时间: ").append(FORMATTER.format(order.getCreateTime())).append("\n");

            // 发送消息
            return sendMessage(title, content.toString());
        } catch (Exception e) {
            log.error("发送交易通知失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendStrategyErrorNotification(RealTimeStrategyEntity strategy, String errorMessage) {
        if (!errorNotificationEnabled) {
            return false;
        }

        try {
            String title = String.format("【策略错误】%s - %s", strategy.getStrategyName(), strategy.getSymbol());

            StringBuilder content = new StringBuilder();
            content.append("策略名称: ").append(strategy.getStrategyName()).append("\n");
            content.append("策略代码: ").append(strategy.getStrategyCode()).append("\n");
            content.append("交易对: ").append(strategy.getSymbol()).append("\n");
            content.append("K线周期: ").append(strategy.getInterval()).append("\n");
            content.append("错误时间: ").append(FORMATTER.format(LocalDateTime.now())).append("\n");
            content.append("错误信息: ").append(strategy.getMessage()).append("\n");

            return sendMessage(title, content.toString());
        } catch (Exception e) {
            log.error("发送策略错误通知失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送企业微信消息
     *
     * @param title   消息标题
     * @param content 消息内容
     * @return 是否发送成功
     */
    private boolean sendMessage(String title, String content) {
        try {
            // 构建文本消息
            WxCpMessage message = WxCpMessage.TEXT()
                    .toUser(String.join("|", userIds))
                    .content(title + "\n\n" + content)
                    .build();

            // 发送消息
            wxCpService.getMessageService().send(message);
            log.info("企业微信通知发送成功: {}", title);
            return true;
        } catch (WxErrorException e) {
            log.error("企业微信通知发送失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
