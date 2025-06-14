package com.okx.trading.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.okx.trading.model.entity.BacktestSummaryEntity;
import reactor.util.function.Tuple2;

/**
 * 回测汇总信息存储库接口
 */
@Repository
public interface BacktestSummaryRepository extends JpaRepository<BacktestSummaryEntity, Long> {

    /**
     * 根据回测ID查询回测汇总信息
     *
     * @param backtestId 回测ID
     * @return 回测汇总信息
     */
    Optional<BacktestSummaryEntity> findByBacktestId(String backtestId);

    /**
     * 根据批量回测ID查询回测汇总信息列表
     *
     * @param batchBacktestId 批量回测ID
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> findByBatchBacktestIdOrderByTotalReturnDesc(String batchBacktestId);

    /**
     * 根据策略名称查询回测汇总信息列表
     *
     * @param strategyName 策略名称
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> findByStrategyNameOrderByCreateTimeDesc(String strategyName);

    /**
     * 根据交易对查询回测汇总信息列表
     *
     * @param symbol 交易对
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> findBySymbolOrderByCreateTimeDesc(String symbol);

    /**
     * 查询每个策略代码的最高收益回测
     *
     * @return 每个策略代码的最高收益回测信息
     */
    @Query("SELECT b FROM BacktestSummaryEntity b WHERE b.strategyCode IS NOT NULL AND b.strategyCode <> '' AND b.totalReturn = (SELECT MAX(b2.totalReturn) FROM BacktestSummaryEntity b2 WHERE b2.strategyCode = b.strategyCode)")
    List<BacktestSummaryEntity> findBestPerformingBacktests();

    /**
     * 删除指定回测ID的汇总信息
     *
     * @param backtestId 回测ID
     */
    void deleteByBacktestId(String backtestId);
}
