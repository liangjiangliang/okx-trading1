package com.okx.trading.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.BacktestEquityCurveEntity;
import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.BacktestTradeEntity;
import reactor.util.function.Tuple2;

/**
 * 回测交易服务接口
 */
public interface BacktestTradeService {

    /**
     * 保存回测交易记录
     *
     * @param backtestResult 回测结果
     * @param strategyParams 策略参数
     * @return 保存的回测ID
     */
    String saveBacktestTrades(String symbol ,BacktestResultDTO backtestResult, String strategyParams);

    /**
     * 保存回测汇总信息
     *
     * @param backtestResult 回测结果
     * @param strategyParams 策略参数
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param backtestId 回测ID (如果已经有ID则使用该ID)
     * @return 保存的回测汇总信息
     */
    BacktestSummaryEntity saveBacktestSummary(BacktestResultDTO backtestResult,
                                              String strategyParams,
                                              String symbol,
                                              String interval,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime,
                                              String backtestId);

    /**
     * 保存回测汇总信息(包含批量回测ID)
     *
     * @param backtestResult 回测结果
     * @param strategyParams 策略参数
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param backtestId 回测ID (如果已经有ID则使用该ID)
     * @param batchBacktestId 批量回测ID
     * @return 保存的回测汇总信息
     */
    BacktestSummaryEntity saveBacktestSummary(BacktestResultDTO backtestResult,
                                              String strategyParams,
                                              String symbol,
                                              String interval,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime,
                                              String backtestId,
                                              String batchBacktestId);

    /**
     * 保存回测资金曲线数据
     *
     * @param backtestId 回测ID
     * @param equityCurveData 资金曲线数据
     * @param timestamps 对应的时间戳列表
     */
    void saveBacktestEquityCurve(String backtestId, List<java.math.BigDecimal> equityCurveData, List<LocalDateTime> timestamps);

    /**
     * 根据回测ID获取资金曲线数据
     *
     * @param backtestId 回测ID
     * @return 资金曲线数据列表
     */
    List<BacktestEquityCurveEntity> getEquityCurveByBacktestId(String backtestId);

    /**
     * 根据回测ID查询交易记录列表
     *
     * @param backtestId 回测ID
     * @return 交易记录列表
     */
    List<BacktestTradeEntity> getTradesByBacktestId(String backtestId);

    /**
     * 获取回测的最大回撤
     *
     * @param backtestId 回测ID
     * @return 最大回撤百分比
     */
    double getMaxDrawdown(String backtestId);

    /**
     * 获取所有回测ID
     *
     * @return 回测ID列表
     */
    List<String> getAllBacktestIds();

    /**
     * 删除指定回测的所有记录
     *
     * @param backtestId 回测ID
     */
    void deleteBacktestRecords(String backtestId);

    /**
     * 获取所有回测汇总信息
     *
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> getAllBacktestSummaries();

    /**
     * 根据回测ID获取回测汇总信息
     *
     * @param backtestId 回测ID
     * @return 回测汇总信息
     */
    Optional<BacktestSummaryEntity> getBacktestSummaryById(String backtestId);

    /**
     * 根据策略名称获取回测汇总信息列表
     *
     * @param strategyName 策略名称
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> getBacktestSummariesByStrategy(String strategyName);

    /**
     * 根据交易对获取回测汇总信息列表
     *
     * @param symbol 交易对
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> getBacktestSummariesBySymbol(String symbol);

    /**
     * 获取每个策略代码的最高收益回测
     * 
     * @return 每个策略代码的最高收益回测信息列表
     */
    List<BacktestSummaryEntity> getBestPerformingBacktests();

    /**
     * 根据批量回测ID获取回测汇总信息列表
     *
     * @param batchBacktestId 批量回测ID
     * @return 回测汇总信息列表
     */
    List<BacktestSummaryEntity> getBacktestSummariesByBatchId(String batchBacktestId);
}
