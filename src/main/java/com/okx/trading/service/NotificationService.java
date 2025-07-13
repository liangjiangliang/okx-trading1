package com.okx.trading.service;

import com.okx.trading.event.WebSocketReconnectEvent;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.trade.Order;

import java.math.BigDecimal;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 监听WebSocket重连事件并发送告警邮件
     *
     * @param event WebSocket重连事件
     */
    void onWebSocketReconnect(WebSocketReconnectEvent event);

    /**
     * 发送交易通知
     *
     * @param strategy    策略信息
     * @param order       订单信息
     * @param side        交易方向 (BUY/SELL)
     * @param signalPrice 信号价格
     * @return 是否发送成功
     */
    boolean sendTradeNotification(RealTimeStrategyEntity strategy, Order order, String side, String signalPrice);

    /**
     * 发送策略错误通知
     *
     * @param strategy     策略信息
     * @param errorMessage 错误信息
     * @return 是否发送成功
     */
    boolean sendStrategyErrorNotification(RealTimeStrategyEntity strategy, String errorMessage);

    public void updateLatestPrice(String symbol, BigDecimal price);

    public void monitorPrice();
}
