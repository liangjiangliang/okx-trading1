package com.okx.trading.service;

import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.model.trade.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时交易订单服务接口
 */
public interface RealTimeOrderService {

    /**
     * 保存订单信息
     */
    RealTimeOrderEntity saveOrder(RealTimeOrderEntity orderEntity);

    /**
     * 根据订单信息创建并保存订单记录
     */
    RealTimeOrderEntity createOrderRecord(String strategyCode, String symbol, Order order,
                                         String signalType, String side,String signalPrice, Boolean simulated, BigDecimal tradeAmount,BigDecimal preQuantity,LocalDateTime singalTime);

    /**
     * 更新订单状态
     */
    RealTimeOrderEntity updateOrderStatus(String orderId, String status, String executedQty, String executedAmount);

    /**
     * 根据策略代码查询订单
     */
    List<RealTimeOrderEntity> getOrdersByStrategy(String strategyCode);

    /**
     * 根据交易对查询订单
     */
    List<RealTimeOrderEntity> getOrdersBySymbol(String symbol);


    List<RealTimeOrderEntity> getOrdersByStrategyId(Long strategyId);

    /**
     * 根据策略代码和交易对查询订单
     */
    List<RealTimeOrderEntity> getOrdersByStrategyAndSymbol(String strategyCode, String symbol);

    /**
     * 根据时间范围查询订单
     */
    List<RealTimeOrderEntity> getOrdersByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据策略代码和时间范围查询订单
     */
    List<RealTimeOrderEntity> getOrdersByStrategyAndTimeRange(String strategyCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取策略的最新订单
     */
    List<RealTimeOrderEntity> getLatestOrdersByStrategy(String strategyCode);


    List<RealTimeOrderEntity> getLatestOrdersByStrategyId(Long strategyId);

    /**
     * 统计策略的订单数量
     */
    Long countOrdersByStrategy(String strategyCode);

    /**
     * 根据订单ID查询订单
     */
    RealTimeOrderEntity getOrderById(String orderId);

    /**
     * 查询所有订单
     */
    List<RealTimeOrderEntity> getAllOrders();
}
