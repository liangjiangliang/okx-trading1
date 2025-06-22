package com.okx.trading.repository;

import com.okx.trading.model.entity.RealTimeOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时交易订单Repository
 */
@Repository
public interface RealTimeOrderRepository extends JpaRepository<RealTimeOrderEntity, Long> {

    /**
     * 根据策略代码查询订单
     */
    List<RealTimeOrderEntity> findByStrategyCodeOrderByCreateTimeDesc(String strategyCode);

    /**
     * 根据交易对查询订单
     */
    List<RealTimeOrderEntity> findBySymbolOrderByCreateTimeDesc(String symbol);

    /**
     * 根据策略代码和交易对查询订单
     */
    List<RealTimeOrderEntity> findByStrategyCodeAndSymbolOrderByCreateTimeDesc(String strategyCode, String symbol);

    /**
     * 根据订单ID查询订单
     */
    RealTimeOrderEntity findByOrderId(String orderId);

    /**
     * 根据客户端订单ID查询订单
     */
    RealTimeOrderEntity findByClientOrderId(String clientOrderId);

    /**
     * 根据时间范围查询订单
     */
    List<RealTimeOrderEntity> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据策略代码和时间范围查询订单
     */
    List<RealTimeOrderEntity> findByStrategyCodeAndCreateTimeBetweenOrderByCreateTimeDesc(
            String strategyCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定策略的最新订单
     */
    @Query("SELECT o FROM RealTimeOrderEntity o WHERE o.strategyCode = :strategyCode ORDER BY o.createTime DESC")
    List<RealTimeOrderEntity> findLatestOrdersByStrategy(@Param("strategyCode") String strategyCode);

    /**
     * 统计策略的订单数量
     */
    @Query("SELECT COUNT(o) FROM RealTimeOrderEntity o WHERE o.strategyCode = :strategyCode")
    Long countByStrategyCode(@Param("strategyCode") String strategyCode);

    /**
     * 查询指定状态的订单
     */
    List<RealTimeOrderEntity> findByStatusOrderByCreateTimeDesc(String status);

}
