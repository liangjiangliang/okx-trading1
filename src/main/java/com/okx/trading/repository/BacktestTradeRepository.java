package com.okx.trading.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.okx.trading.model.entity.BacktestTradeEntity;

/**
 * 回测交易记录存储库接口
 */
@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTradeEntity, Long> {
    
    /**
     * 根据回测ID查询交易记录
     * 
     * @param backtestId 回测ID
     * @return 交易记录列表
     */
    List<BacktestTradeEntity> findByBacktestIdOrderByIndexAsc(String backtestId);
    
    /**
     * 根据回测ID查询交易记录总数
     * 
     * @param backtestId 回测ID
     * @return 交易记录总数
     */
    long countByBacktestId(String backtestId);
    
    /**
     * 删除指定回测ID的交易记录
     * 
     * @param backtestId 回测ID
     */
    void deleteByBacktestId(String backtestId);
    
    /**
     * 查询指定回测ID的最大回撤
     * 
     * @param backtestId 回测ID
     * @return 最大回撤百分比
     */
    @Query("SELECT MAX(b.maxDrawdown) FROM BacktestTradeEntity b WHERE b.backtestId = :backtestId")
    BigDecimal findMaxDrawdownByBacktestId(@Param("backtestId") String backtestId);
} 