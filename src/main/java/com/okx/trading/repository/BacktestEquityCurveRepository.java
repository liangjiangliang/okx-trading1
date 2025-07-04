package com.okx.trading.repository;

import com.okx.trading.model.entity.BacktestEquityCurveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 回测资金曲线数据访问接口
 */
@Repository
public interface BacktestEquityCurveRepository extends JpaRepository<BacktestEquityCurveEntity, Long> {

    /**
     * 根据回测ID查询资金曲线数据
     *
     * @param backtestId 回测ID
     * @return 资金曲线数据列表，按索引位置排序
     */
    List<BacktestEquityCurveEntity> findByBacktestIdOrderByIndexPositionAsc(String backtestId);

    /**
     * 根据回测ID查询资金曲线数据
     *
     * @param backtestId 回测ID
     * @return 资金曲线数据列表，按时间戳排序
     */
    List<BacktestEquityCurveEntity> findByBacktestIdOrderByTimestampAsc(String backtestId);

    /**
     * 根据回测ID删除资金曲线数据
     *
     * @param backtestId 回测ID
     */
    void deleteByBacktestId(String backtestId);
} 