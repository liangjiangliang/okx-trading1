package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.util.TechnicalIndicatorUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

/**
 * 市场数据控制器
 * 提供K线数据获取和技术指标计算的接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
@Api(tags = "市场数据接口", description = "提供K线数据获取和技术指标计算的接口")
public class MarketController{

    private final OkxApiService okxApiService;
    private final HistoricalDataService historicalDataService;

    /**
     * 获取K线数据
     *
     * @param symbol   交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit    获取数据条数，最大为1000
     * @return K线数据列表
     */
    @ApiOperation(value = "订阅实时标记价格K线数据,订阅完成后自动推送最新行情信息", notes = "获取指定交易对的K线数据，支持多种时间间隔")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
        @ApiImplicitParam(name = "limit", value = "获取数据条数，最大为1000，不传默认返回500条数据",
            required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/subscribe_klines")
    public ApiResponse<List<Candlestick>> subscribeKlineData(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
        @RequestParam(required = false) @Min(value = 1, message = "数据条数必须大于0") Integer limit){

        log.info("获取K线数据, symbol: {}, interval: {}, limit: {}", symbol, interval, limit);

        List<Candlestick> candlesticks = okxApiService.getKlineData(symbol, interval, limit);

        return ApiResponse.success(candlesticks);
    }

    /**
     * 获取最新行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 行情数据
     */
    @ApiOperation(value = "获取最新行情", notes = "获取指定交易对的最新价格、24小时涨跌幅等行情数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对 (格式为 基础资产-计价资产，如BTC-USDT、ETH-USDT等)",
            required = true, dataType = "String", example = "BTC-USDT", paramType = "query")
    })
    @GetMapping("/ticker")
    public ApiResponse<Ticker> getTicker(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol){

        log.info("获取最新行情, symbol: {}", symbol);

        Ticker ticker = okxApiService.getTicker(symbol);

        return ApiResponse.success(ticker);
    }

    /**
     * 取消订阅K线数据
     *
     * @param symbol   交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 操作结果
     */
    @ApiOperation(value = "取消订阅K线数据", notes = "取消订阅指定交易对的K线数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M")
    })
    @GetMapping("/unsubscribe_klines")
    public ApiResponse<Boolean> unsubscribeKlineData(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval){

        log.info("取消订阅K线数据, symbol: {}, interval: {}", symbol, interval);

        boolean result = okxApiService.unsubscribeKlineData(symbol, interval);

        return ApiResponse.success(result);
    }

    /**
     * 取消订阅行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 操作结果
     */
    @ApiOperation(value = "取消订阅行情数据", notes = "取消订阅指定交易对的实时行情数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对 (格式为 基础资产-计价资产，如BTC-USDT、ETH-USDT等)",
            required = true, dataType = "String", example = "BTC-USDT", paramType = "query")
    })
    @GetMapping("/unsubscribe_ticker")
    public ApiResponse<Boolean> unsubscribeTicker(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol){

        log.info("取消订阅行情数据, symbol: {}", symbol);

        boolean result = okxApiService.unsubscribeTicker(symbol);

        return ApiResponse.success(result);
    }

    /**
     * 获取最新的K线数据
     *
     * @param symbol   交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit    获取数据条数，默认100
     * @return 最新的K线数据列表
     */
    @ApiOperation(value = "获取最新K线数据", notes = "从数据库获取最新的K线数据，按时间降序排列")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
        @ApiImplicitParam(name = "limit", value = "获取数据条数，默认100",
            required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/latest_klines")
    public ApiResponse<List<CandlestickEntity>> getLatestKlineData(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
        @RequestParam(required = false, defaultValue = "100") @Min(value = 1, message = "数据条数必须大于0") Integer limit){

        log.info("获取最新K线数据, symbol: {}, interval: {}, limit: {}", symbol, interval, limit);

        List<CandlestickEntity> candlesticks = historicalDataService.getLatestHistoricalData(symbol, interval, limit);

        return ApiResponse.success(candlesticks);
    }

    /**
     * 获取历史K线数据
     *
     * @param symbol    交易对，如BTC-USDT
     * @param interval  K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime 开始时间戳（毫秒）
     * @param endTime   结束时间戳（毫秒）
     * @param limit     获取数据条数，最大为1000
     * @return K线数据列表
     */
    @ApiOperation(value = "获取历史K线数据", notes = "获取指定交易对的历史K线数据，支持时间范围查询")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
        @ApiImplicitParam(name = "startTime", value = "开始时间戳（毫秒）", required = false, dataType = "Long", example = "1656086400000", paramType = "query"),
        @ApiImplicitParam(name = "endTime", value = "结束时间戳（毫秒）", required = false, dataType = "Long", example = "1656172800000", paramType = "query"),
        @ApiImplicitParam(name = "limit", value = "获取数据条数，最大为1000，不传默认返回500条数据",
            required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/history_klines")
    public ApiResponse<List<Candlestick>> getHistoryKlineData(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
        @RequestParam(required = false) Long startTime,
        @RequestParam(required = false) Long endTime,
        @RequestParam(required = false) @Min(value = 1, message = "数据条数必须大于0") Integer limit){

        log.info("获取历史K线数据, symbol: {}, interval: {}, startTime: {}, endTime: {}, limit: {}",
            symbol, interval, startTime, endTime, limit);

        List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(symbol, interval, startTime, endTime, limit);

        return ApiResponse.success(candlesticks);
    }

    /**
     * 查询数据库中已保存的历史K线数据
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @return 历史K线数据列表
     */
    @ApiOperation(value = "查询已保存的历史K线数据", notes = "查询数据库中已保存的历史K线数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
        @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2023-01-01 00:00:00", paramType = "query"),
        @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2023-01-02 00:00:00", paramType = "query")
    })
    @GetMapping("/query_saved_history")
    public ApiResponse<List<CandlestickEntity>> querySavedHistoricalData(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
        @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
        @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr){

        log.info("查询已保存的历史K线数据, symbol: {}, interval: {}, startTime: {}, endTime: {}",
            symbol, interval, startTimeStr, endTimeStr);

        try{
            // 将字符串时间转换为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 查询数据
            List<CandlestickEntity> data = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);

            return ApiResponse.success(data);
        }catch(Exception e){
            log.error("查询历史K线数据失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "查询历史K线数据失败: " + e.getMessage());
        }
    }


    /**
     * 获取历史K线数据并保存，自动检查并补充缺失数据
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @param maxRetries   最大重试次数
     * @param maxExecutions 最大执行次数，每次执行包含maxRetries次重试。默认为10
     * @return 操作结果
     */
    @ApiOperation(value = "获取历史K线数据并保存，自动检查并补充缺失数据", notes = "获取并保存指定交易对的历史K线数据，检查数据完整性并自动按天分组递归获取缺失数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
        @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2018-01-01 00:00:00", paramType = "query"),
        @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2025-04-01 00:00:00", paramType = "query"),
        @ApiImplicitParam(name = "maxRetries", value = "每次执行中的最大重试次数", required = false, dataType = "Integer", example = "3", paramType = "query"),
        @ApiImplicitParam(name = "maxExecutions", value = "最大执行次数，直到数据完整或达到此次数", required = false, dataType = "Integer", example = "10", paramType = "query")
    })
    @GetMapping("/fetch_history_with_integrity_check")
    public ApiResponse<String> fetchAndSaveHistoryWithIntegrityCheck(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
        @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
        @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr,
        @RequestParam(required = false, defaultValue = "3") Integer maxRetries,
        @RequestParam(required = false, defaultValue = "10") Integer maxExecutions){

        log.info("开始获取并保存历史K线数据(带完整性检查), symbol: {}, interval: {}, startTime: {}, endTime: {}, 每次执行最大重试次数: {}, 最大执行次数: {}",
            symbol, interval, startTimeStr, endTimeStr, maxRetries, maxExecutions);

        try{
            // 将字符串时间转换为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 计算预期的数据点总数（用于统计）
            long intervalMinutes = historicalDataService.getIntervalMinutes(interval);
            long expectedDataPoints = ChronoUnit.MINUTES.between(startTime, endTime) / intervalMinutes + 1;
            log.info("预期获取的数据点总数: {}", expectedDataPoints);

            // 记录初始请求开始时间
            long startTimestamp = System.currentTimeMillis();

            // 异步执行数据获取任务
            CompletableFuture.runAsync(() -> {
                try {
                    // 多次执行，直到数据完整或达到最大执行次数
                    int executionCount = 0;
                    List<LocalDateTime> missingPoints = new ArrayList<>();
                    int totalDataCount = 0;
                    
                    do {
                        executionCount++;
                        log.info("第 {} 次执行数据获取流程", executionCount);
                        
                        // 创建一个ConcurrentHashMap来记录失败的请求
                        ConcurrentMap<String, Integer> failedRequests = new ConcurrentHashMap<>();
                        
                        // 执行初始数据获取
                        CompletableFuture<Integer> future;
                        if (executionCount == 1 || missingPoints.isEmpty()) {
                            // 首次执行或没有特定缺失点时，获取整个时间范围
                            future = historicalDataService.fetchAndSaveHistoricalDataWithFailureRecord(
                                symbol, interval, startTime, endTime, failedRequests);
                        } else {
                            // 有缺失点时，只获取缺失的数据
                            log.info("针对性获取 {} 个缺失数据点", missingPoints.size());
                            future = historicalDataService.fillMissingData(symbol, interval, missingPoints, failedRequests);
                        }
                        
                        // 等待初始请求完成
                        int dataCount = future.get();
                        log.info("第 {} 次执行初始数据获取完成，获取 {} 条数据", executionCount, dataCount);
                        totalDataCount += dataCount;
                        
                        // 重试机制 - 处理失败的请求
                        int retryCount = 0;
                        while (!failedRequests.isEmpty() && retryCount < maxRetries) {
                            retryCount++;
                            log.info("执行 {} - 第 {} 次重试，处理 {} 个失败的请求", executionCount, retryCount, failedRequests.size());
                            
                            // 复制当前失败请求列表进行重试
                            Map<String, Integer> currentFailures = new HashMap<>(failedRequests);
                            failedRequests.clear(); // 清空列表，准备记录本次重试中的失败
                            
                            // 为每个失败的请求创建重试任务
                            List<CompletableFuture<Integer>> retryFutures = new ArrayList<>();
                            for (Map.Entry<String, Integer> entry : currentFailures.entrySet()) {
                                String[] parts = entry.getKey().split(":");
                                if (parts.length == 2) {
                                    LocalDateTime sliceStart = LocalDateTime.parse(parts[0]);
                                    LocalDateTime sliceEnd = LocalDateTime.parse(parts[1]);
                                    
                                    CompletableFuture<Integer> retryFuture = historicalDataService.fetchAndSaveTimeSliceWithFailureRecord(
                                        symbol, interval, sliceStart, sliceEnd, failedRequests);
                                    retryFutures.add(retryFuture);
                                }
                            }
                            
                            // 等待所有重试任务完成
                            CompletableFuture.allOf(retryFutures.toArray(new CompletableFuture[0])).join();
                            
                            // 计算重试获取的数据量
                            int retryDataCount = retryFutures.stream()
                                .map(f -> {
                                    try {
                                        return f.get();
                                    } catch (Exception e) {
                                        log.error("获取重试任务结果失败", e);
                                        return 0;
                                    }
                                })
                                .mapToInt(Integer::intValue)
                                .sum();
                            
                            totalDataCount += retryDataCount;
                            log.info("执行 {} - 第 {} 次重试完成，此次获取 {} 条数据，累计 {} 条数据，剩余 {} 个失败请求", 
                                executionCount, retryCount, retryDataCount, totalDataCount, failedRequests.size());
                        }
                        
                        // 检查整体数据完整性
                        missingPoints = historicalDataService.checkDataIntegrity(symbol, interval, startTime, endTime);
                        log.info("执行 {} 完成后，仍有 {} 个数据点缺失", executionCount, missingPoints.size());
                        
                        // 如果已经达到数据完整（没有缺失点）或者达到最大执行次数，则跳出循环
                    } while (!missingPoints.isEmpty() && executionCount < maxExecutions);
                    
                    // 完成后检查整体数据完整性
                    List<LocalDateTime> finalMissingPoints = historicalDataService.checkDataIntegrity(
                        symbol, interval, startTime, endTime);
                    
                    // 获取实际存储的数据量
                    List<CandlestickEntity> storedData = historicalDataService.getHistoricalData(
                        symbol, interval, startTime, endTime);
                    int actualDataCount = storedData.size();
                    
                    // 计算总耗时
                    long totalTimeMillis = System.currentTimeMillis() - startTimestamp;
                    
                    // 打印最终统计信息
                    log.info("=== 历史数据获取任务最终统计 ===");
                    log.info("交易对: {}, 时间间隔: {}", symbol, interval);
                    log.info("时间范围: {} 至 {}", startTimeStr, endTimeStr);
                    log.info("预期数据点: {}", expectedDataPoints);
                    log.info("实际存储数量: {}", actualDataCount);
                    log.info("成功率: {}%", String.format("%.2f", (actualDataCount * 100.0 / expectedDataPoints)));
                    log.info("仍然缺失: {}", finalMissingPoints.size());
                    log.info("总执行次数: {}/{}", executionCount, maxExecutions);
                    log.info("总耗时: {}秒", totalTimeMillis / 1000);
                    
                    if (!finalMissingPoints.isEmpty()) {
                        log.info("最终缺失点列表 (前20个):");
                        finalMissingPoints.stream()
                            .limit(20)
                            .forEach(time -> log.info("  {}", time));
                    }
                    
                    String completionStatus = finalMissingPoints.isEmpty() ? 
                        "数据已完整获取" : 
                        String.format("任务完成但仍有 %d 个数据点缺失", finalMissingPoints.size());
                    log.info(completionStatus);
                    log.info("===========================");
                    
                } catch (Exception e) {
                    log.error("历史数据获取任务异常", e);
                }
            });

            return ApiResponse.success("历史数据获取任务已启动，包含完整性检查和失败请求重试机制，最多执行 " + maxExecutions + " 次，请稍后查询数据");
        } catch (Exception e) {
            log.error("获取历史K线数据失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取历史K线数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取布林带指标数据
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param period       布林带周期，默认为20
     * @param multiplier   标准差倍数，默认为2
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @return 布林带数据
     */
    @ApiOperation(value = "获取布林带指标数据", notes = "根据历史K线数据计算布林带指标")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
        @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
            required = true, dataType = "String", example = "1m", paramType = "query",
            allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
        @ApiImplicitParam(name = "period", value = "布林带周期", required = false, dataType = "Integer", example = "20", paramType = "query"),
        @ApiImplicitParam(name = "multiplier", value = "标准差倍数", required = false, dataType = "Double", example = "2.0", paramType = "query"),
        @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2023-01-01 00:00:00", paramType = "query"),
        @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2023-01-02 00:00:00", paramType = "query")
    })
    @GetMapping("/bollinger_bands")
    public ApiResponse<Map<String, Object>> getBollingerBands(
        @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
        @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
        @RequestParam(required = false, defaultValue = "20") Integer period,
        @RequestParam(required = false, defaultValue = "2.0") Double multiplier,
        @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
        @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr) {

        log.info("获取布林带指标数据, symbol: {}, interval: {}, period: {}, multiplier: {}, startTime: {}, endTime: {}",
            symbol, interval, period, multiplier, startTimeStr, endTimeStr);

        try {
            // 将字符串时间转换为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 获取历史K线数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(
                symbol, interval, startTime, endTime);

            if (candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定时间范围内的K线数据");
            }

            // 提取收盘价
            List<BigDecimal> closePrices = candlesticks.stream()
                .map(CandlestickEntity::getClose)
                .collect(Collectors.toList());

            // 计算布林带
            TechnicalIndicatorUtil.BollingerBands bands =
                TechnicalIndicatorUtil.calculateBollingerBands(closePrices, period, multiplier, 8);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("interval", interval);
            result.put("period", period);
            result.put("multiplier", multiplier);
            result.put("startTime", startTimeStr);
            result.put("endTime", endTimeStr);
            result.put("dataPoints", candlesticks.size());

            // 收集时间和价格等数据
            List<Map<String, Object>> dataPoints = new ArrayList<>();
            for (int i = 0; i < candlesticks.size(); i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("time", candlesticks.get(i).getOpenTime().format(formatter));
                point.put("price", candlesticks.get(i).getClose());
                point.put("middle", bands.getMiddle().get(i));
                point.put("upper", bands.getUpper().get(i));
                point.put("lower", bands.getLower().get(i));
                dataPoints.add(point);
            }
            result.put("data", dataPoints);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("计算布林带指标失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "计算布林带指标失败: " + e.getMessage());
        }
    }
}
