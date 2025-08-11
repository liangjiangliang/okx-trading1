package com.okx.trading.service.impl;

import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.model.trade.Order;
import com.okx.trading.repository.RealTimeOrderRepository;
import com.okx.trading.service.RealTimeOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 实时交易订单服务实现类
 */
@Slf4j
@Service
public class RealTimeOrderServiceImpl implements RealTimeOrderService {

    @Autowired
    private RealTimeOrderRepository realTimeOrderRepository;

    @Override
    public RealTimeOrderEntity saveOrder(RealTimeOrderEntity orderEntity) {
        try {
            return realTimeOrderRepository.save(orderEntity);
        } catch (Exception e) {
            log.error("保存订单失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存订单失败", e);
        }
    }

    @Override
    public RealTimeOrderEntity createOrderRecord(String strategyCode, String symbol, Order order,
                                                 String signalType, String side, String signalPrice, Boolean simulated, BigDecimal preAmount, BigDecimal preQuantity,LocalDateTime singalTime) {
        try {
            RealTimeOrderEntity orderEntity = RealTimeOrderEntity.builder()
                    .clientOrderId(order.getClientOrderId())
                    .preAmount(preAmount)
                    .preQuantity(preQuantity)
                    .executedAmount(order.getCummulativeQuoteQty())
                    .executedQty(order.getExecutedQty())
                    .price(order.getPrice())
                    .fee(order.getFee())
                    .feeCurrency(order.getFeeCurrency())
                    .orderId(order.getOrderId())
                    .orderType(order.getType())
                    .side(order.getSide())
                    .signalPrice(signalPrice != null ? new BigDecimal(signalPrice) : null)
                    .signalType(signalType)
                    .strategyCode(strategyCode)
                    .symbol(symbol)
                    .status(order.getStatus())
                    .createTime(order.getCreateTime())
                    .singalTime(singalTime)
                    .build();

            return orderEntity;
        } catch (Exception e) {
            log.error("创建订单记录失败: strategyCode={}, symbol={}, orderId={}, error={}",
                    strategyCode, symbol, order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("创建订单记录失败", e);
        }
    }

    @Override
    public RealTimeOrderEntity updateOrderStatus(String orderId, String status, String executedQty, String executedAmount) {
        try {
            RealTimeOrderEntity orderEntity = realTimeOrderRepository.findByOrderId(orderId);
            if (orderEntity != null) {
                orderEntity.setStatus(status);
                if (executedQty != null) {
                    orderEntity.setExecutedQty(new BigDecimal(executedQty));
                }
                if (executedAmount != null) {
                    orderEntity.setExecutedAmount(new BigDecimal(executedAmount));
                }
                orderEntity.setUpdateTime(LocalDateTime.now());
                return saveOrder(orderEntity);
            }
            return null;
        } catch (Exception e) {
            log.error("更新订单状态失败: orderId={}, status={}, error={}", orderId, status, e.getMessage(), e);
            throw new RuntimeException("更新订单状态失败", e);
        }
    }

    @Override
    public List<RealTimeOrderEntity> getOrdersByStrategy(String strategyCode) {
        return realTimeOrderRepository.findByStrategyCodeOrderByCreateTimeDesc(strategyCode);
    }

    @Override
    public List<RealTimeOrderEntity> getOrdersBySymbol(String symbol) {
        return realTimeOrderRepository.findBySymbolOrderByCreateTimeDesc(symbol);
    }

    @Override
    public List<RealTimeOrderEntity> getOrdersByStrategyId(Long strategyId) {
        return realTimeOrderRepository.findByStrategyIdOrderByCreateTimeDesc(strategyId);
    }

    @Override
    public List<RealTimeOrderEntity> getOrdersByStrategyAndSymbol(String strategyCode, String symbol) {
        return realTimeOrderRepository.findByStrategyCodeAndSymbolOrderByCreateTimeDesc(strategyCode, symbol);
    }

    @Override
    public List<RealTimeOrderEntity> getOrdersByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return realTimeOrderRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(startTime, endTime);
    }

    @Override
    public List<RealTimeOrderEntity> getOrdersByStrategyAndTimeRange(String strategyCode, LocalDateTime startTime, LocalDateTime endTime) {
        return realTimeOrderRepository.findByStrategyCodeAndCreateTimeBetweenOrderByCreateTimeDesc(strategyCode, startTime, endTime);
    }

    @Override
    public List<RealTimeOrderEntity> getLatestOrdersByStrategy(String strategyCode) {
        return realTimeOrderRepository.findLatestOrdersByStrategy(strategyCode);
    }

    @Override
    public List<RealTimeOrderEntity> getLatestOrdersByStrategyId(Long strategyId) {
        return realTimeOrderRepository.findLatestOrdersByStrategyId(strategyId);
    }

    @Override
    public Long countOrdersByStrategy(String strategyCode) {
        return realTimeOrderRepository.countByStrategyCode(strategyCode);
    }

    @Override
    public RealTimeOrderEntity getOrderById(String orderId) {
        return realTimeOrderRepository.findByOrderId(orderId);
    }

    @Override
    public List<RealTimeOrderEntity> getAllOrders() {
        return realTimeOrderRepository.findAll();
    }
}
