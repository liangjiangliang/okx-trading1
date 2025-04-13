package com.okx.trading.repository;

import com.okx.trading.model.entity.BacktestTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 回测交易详情存储库接口
 */
@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTradeEntity, Long> {
    
    /**
     * 根据回测ID查询所有交易明细
     * 
     * @param backtestId 回测ID
     * @return 交易明细列表
     */
    List<BacktestTradeEntity> findByBacktestIdOrderByIndexAsc(String backtestId);
    
    /**
     * 根据策略名称查询交易明细
     * 
     * @param strategyName 策略名称
     * @return 交易明细列表
     */
    List<BacktestTradeEntity> findByStrategyNameOrderByBacktestIdAscIndexAsc(String strategyName);
    
    /**
     * 查询某个回测的最大回撤
     * 
     * @param backtestId 回测ID
     * @return 最大回撤值
     */
    @Query("SELECT MAX(b.maxDrawdown) FROM BacktestTradeEntity b WHERE b.backtestId = :backtestId")
    BigDecimal findMaxDrawdownByBacktestId(@Param("backtestId") String backtestId);
    
    /**
     * 查询某个回测的总盈亏
     * 
     * @param backtestId 回测ID
     * @return 总盈亏
     */
    @Query("SELECT SUM(b.profit) FROM BacktestTradeEntity b WHERE b.backtestId = :backtestId")
    BigDecimal findTotalProfitByBacktestId(@Param("backtestId") String backtestId);
    
    /**
     * 删除指定回测ID的所有交易记录
     * 
     * @param backtestId 回测ID
     */
    void deleteByBacktestId(String backtestId);
} 