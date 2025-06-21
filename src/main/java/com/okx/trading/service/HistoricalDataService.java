package com.okx.trading.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.entity.CandlestickEntity;

/**
 * 历史K线数据服务
 * 负责获取、处理和存储历史K线数据
 */
public interface HistoricalDataService {

    /**
     * 根据时间范围获取并保存历史K线数据
     * 将自动分片、多线程获取并检查数据完整性
     *
     * @param symbol     交易对，如BTC-USDT
     * @param interval   K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 获取的K线数据数量
     */
    CompletableFuture<Integer> fetchAndSaveHistoricalData(String symbol, String interval,
                                                      LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围获取并保存历史K线数据，同时记录失败的请求
     * 将自动分片、多线程获取并检查数据完整性
     *
     * @param symbol     交易对，如BTC-USDT
     * @param interval   K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param failedRequests 用于记录失败请求的并发Map
     * @return 获取的K线数据数量
     */
    CompletableFuture<Integer> fetchAndSaveHistoricalDataWithFailureRecord(String symbol, String interval,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      ConcurrentMap<String, Integer> failedRequests);

    /**
     * 获取并保存指定时间片段的K线数据，同时记录失败的请求
     *
     * @param symbol     交易对，如BTC-USDT
     * @param interval   K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime  时间片段开始时间
     * @param endTime    时间片段结束时间
     * @param failedRequests 用于记录失败请求的并发Map
     * @return 获取的K线数据数量
     */
    CompletableFuture<Integer> fetchAndSaveTimeSliceWithFailureRecord(String symbol, String interval,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      ConcurrentMap<String, Integer> failedRequests);

    /**
     * 查询数据库中存储的历史K线数据
     *
     * @param symbol     交易对，如BTC-USDT
     * @param interval   K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 历史K线数据列表
     */
    List<CandlestickEntity> getHistoricalData(String symbol, String interval,
                                             LocalDateTime startTime, LocalDateTime endTime);


    List<CandlestickEntity> fetchAndSaveHistoryWithIntegrityCheck(String symbol, String interval, String startTimeStr, String endTimeStr);

    /**
     * 查询数据库中存储的历史K线数据
     *
     * @param symbol     交易对，如BTC-USDT
     * @param interval   K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 历史K线数据列表
     */
    List<CandlestickEntity> getHistoricalData(String symbol, String interval);

    /**
     * 检查指定时间范围内的数据完整性
     *
     * @param symbol     交易对，如BTC-USDT
     * @param interval   K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 缺失的时间点列表
     */
    List<LocalDateTime> checkDataIntegrity(List<CandlestickEntity> data,
                                           LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 单独获取缺失的数据点
     *
     * @param symbol      交易对，如BTC-USDT
     * @param interval    K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param missingTimes 缺失的时间点列表
     * @return 填补的数据点数量
     */
    CompletableFuture<Integer> fillMissingData(String symbol, String interval, List<LocalDateTime> missingTimes);

    /**
     * 单独获取缺失的数据点，同时记录失败的请求
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param missingTimes 缺失的时间点列表
     * @param failedRequests 用于记录失败请求的并发Map
     * @return 填补的数据点数量
     */
    CompletableFuture<Integer> fillMissingData(String symbol, String interval, List<LocalDateTime> missingTimes,
                                              ConcurrentMap<String, Integer> failedRequests);

    /**
     * 获取指定交易对和间隔的最新K线数据
     *
     * @param symbol    交易对，如BTC-USDT
     * @param interval  K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit     获取的数据条数限制
     * @return 最新的K线数据列表，按时间降序排列
     */
    List<CandlestickEntity> getLatestHistoricalData(String symbol, String interval, int limit);

    /**
     * 获取间隔对应的分钟数
     *
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 对应的分钟数
     */
    long getIntervalMinutes(String interval);

    /**
     * 保存K线数据到数据库
     *
     * @param entities K线数据实体列表
     */
    void saveHistoricalData(List<CandlestickEntity> entities);
}
