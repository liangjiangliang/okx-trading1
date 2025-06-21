package com.okx.trading.controller;

import com.alibaba.fastjson.JSONArray;
import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.RedisCacheService;
import com.okx.trading.util.TechnicalIndicatorUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static com.okx.trading.util.BacktestDataGenerator.parseIntervalToMinutes;

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
public class MarketController {

    private final OkxApiService okxApiService;
    private final HistoricalDataService historicalDataService;
    private final RedisCacheService redisCacheService;

    // 注入历史数据查询线程池
    @Qualifier("historicalDataExecutorService")
    private final ExecutorService historicalDataExecutorService;

    // 判断是否为开发环境，用于控制日志详细程度
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 判断是否为开发环境
     */
    private boolean isDevelopmentEnvironment() {
        return "dev".equals(activeProfile) || "development".equals(activeProfile);
    }

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
            @RequestParam(required = false) @Min(value = 1, message = "数据条数必须大于0") Integer limit) {

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
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {

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
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval) {

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
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {

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
            @RequestParam(required = false, defaultValue = "100") @Min(value = 1, message = "数据条数必须大于0") Integer limit) {

        log.info("获取最新K线数据, symbol: {}, interval: {}, limit: {}", symbol, interval, limit);

        List<CandlestickEntity> candlesticks = historicalDataService.getLatestHistoricalData(symbol, interval, limit);

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
            @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr) {

        log.info("查询已保存的历史K线数据, symbol: {}, interval: {}, startTime: {}, endTime: {}",
                symbol, interval, startTimeStr, endTimeStr);

        try {
            // 将字符串时间转换为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 查询数据
            List<CandlestickEntity> data = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);

            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("查询历史K线数据失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "查询历史K线数据失败: " + e.getMessage());
        }
    }


    /**
     * 获取历史K线数据并保存，智能计算需要获取的数据量
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @return 操作结果，包含获取的K线数据
     */
    @ApiOperation(value = "智能获取历史K线数据", notes = "根据入参计算需要获取的K线数量，扣除已有数据，按需获取并保存")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2018-01-01 00:00:00", paramType = "query"),
            @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2025-04-01 00:00:00", paramType = "query")
    })
    @GetMapping("/fetch_history_with_integrity_check")
    public ApiResponse<List<CandlestickEntity>> fetchAndSaveHistoryWithIntegrityCheck(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
            @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr) {

        // 将字符串时间转换为LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

        // 🔍 检查并调整时间范围，避免获取未完成的时间周期
        LocalDateTime adjustedEndTime = adjustEndTimeToAvoidIncompleteData(endTime, interval);
        if (!adjustedEndTime.equals(endTime)) {
            log.info("⚠️ 检测到查询时间包含未完成的周期，已调整结束时间: {} → {}", endTime, adjustedEndTime);
            endTime = adjustedEndTime;
        }

        // 计算需要获取的K线数量（基于时间范围和间隔）
        long intervalMinutes = historicalDataService.getIntervalMinutes(interval);
        long totalExpectedCount = ChronoUnit.MINUTES.between(startTime, endTime) / intervalMinutes + 1;
        log.info("📊 根据时间范围计算，预期需要获取的K线数量: {}", totalExpectedCount);
        List<CandlestickEntity> cachedData = new ArrayList<>();

        // 先检查Redis Sorted Set缓存
        try {
            long startTimestamp = startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTimestamp = endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            Set<String> cachedJsonSet = redisCacheService.getKlineFromSortedSet(symbol, interval, startTimestamp, endTimestamp);

            if (!cachedJsonSet.isEmpty()) {
                for (String jsonStr : cachedJsonSet) {
                    try {
                        CandlestickEntity entity = CandlestickEntity.fromJSONObject(jsonStr);
                        cachedData.add(entity);
                    } catch (Exception e) {
                        log.warn("⚠️ 解析缓存K线数据失败: {}", e.getMessage());
                    }
                }

                if (!cachedData.isEmpty() && totalExpectedCount == cachedData.size()) {
                    // 按时间排序
                    cachedData.sort(CandlestickEntity::compareTo);
                    log.info("📦 从Redis Sorted Set获取历史K线数据, symbol: {}, interval: {}, 数量: {}, 时间范围: {} ~ {}",
                            symbol, interval, cachedData.size(), startTimeStr, endTime.toString());
                    return ApiResponse.success(cachedData);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 从Redis Sorted Set获取缓存失败，继续执行原逻辑: {}", e.getMessage());
        }

        log.info("🚀 智能获取历史K线数据开始, symbol: {}, interval: {}, startTime: {}, endTime: {}", symbol, interval, startTimeStr, endTimeStr);

        try {

            // 2. 从MySQL获取已经有的K线数量
            List<CandlestickEntity> existingData = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);
            long existingCount = existingData.size();
            log.info("💾 MySQL中已存在的K线数量: {}", existingCount);

            // 3. 计算需要新获取的数量
            long neededCount = totalExpectedCount - existingCount;
            log.info("🔢 需要新获取的K线数量: {}", neededCount);

            // 如果MySQL的数据已经足够，直接返回
            if (neededCount <= 0) {
                log.info("✅ 数据已完整，无需获取新数据，直接返回MySQL中的 {} 条数据", existingCount);
                // 将结果存入Redis Sorted Set（24小时过期）
                try {
                    List<CandlestickEntity> cacheData = existingData.stream().filter(entity -> !cachedData.contains(entity)).collect(Collectors.toList());
                    redisCacheService.batchAddKlineToSortedSet(symbol, interval, cacheData, 24 * 60);
                    log.info("💾 历史K线数据已存入Redis Sorted Set，key: coin_nrt_kline:{}{}, 条数: {}, 过期时间: 24小时",
                            symbol, interval, existingData.size());
                } catch (Exception e) {
                    log.warn("⚠️ 存储历史K线数据到Redis Sorted Set失败: {}", e.getMessage());
                }

                return ApiResponse.success(existingData);
            }

            // 4. 检查数据完整性，找出缺失的时间范围
            if (existingData.isEmpty()) {
                CandlestickEntity candlestick = new CandlestickEntity();
                candlestick.setSymbol(symbol);
                candlestick.setIntervalVal(interval);
                existingData.add(candlestick);
            }
            List<LocalDateTime> missingTimePoints = historicalDataService.checkDataIntegrity(existingData, startTime, endTime);
            log.info("🔍 发现 {} 个缺失的时间点需要获取", missingTimePoints.size());

            if (missingTimePoints.isEmpty()) {
                log.info("✅ 数据完整性检查通过，直接返回MySQL中的 {} 条数据", existingCount);
                return ApiResponse.success(existingData);
            }

            // 5. 使用多线程并行获取缺失数据
            List<CandlestickEntity> newlyFetchedData = Collections.synchronizedList(new ArrayList<>());
            int batchSize = 300;

            // 将缺失时间点按连续范围分组，便于批量处理
            List<List<LocalDateTime>> timeRanges = groupConsecutiveTimePoints(missingTimePoints, intervalMinutes);
            log.info("📦 缺失数据被分为 {} 个连续时间范围", timeRanges.size());

            // 创建并行任务列表
            List<CompletableFuture<List<CandlestickEntity>>> futures = new ArrayList<>();

            for (int i = 0; i < timeRanges.size(); i++) {
                List<LocalDateTime> range = timeRanges.get(i);
                if (range.isEmpty()) continue;

                LocalDateTime rangeStart = range.get(0);
                LocalDateTime rangeEnd = range.get(range.size() - 1);
                final int rangeIndex = i + 1;

                log.info("🔄 准备并行处理第 {} 个时间范围: {} 到 {} ({} 个数据点)",
                        rangeIndex, rangeStart, rangeEnd, range.size());

                // 创建异步任务
                CompletableFuture<List<CandlestickEntity>> future = CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                if (isDevelopmentEnvironment()) {
                                    log.info("🧵 线程 {} 开始处理第 {} 个时间范围", Thread.currentThread().getName(), rangeIndex);
                                }

                                // 按批次获取这个范围的数据
                                List<CandlestickEntity> rangeData = fetchRangeDataInBatches(
                                        symbol, interval, rangeStart, rangeEnd, batchSize, intervalMinutes);

                                if (isDevelopmentEnvironment()) {
                                    log.info("✅ 线程 {} 完成第 {} 个时间范围，获取了 {} 条数据",
                                            Thread.currentThread().getName(), rangeIndex, rangeData.size());
                                }

                                return rangeData;
                            } catch (Exception e) {
                                log.error("❌ 线程 {} 处理第 {} 个时间范围失败: {}",
                                        Thread.currentThread().getName(), rangeIndex, e.getMessage(), e);
                                return new ArrayList<>();
                            }
                        }, historicalDataExecutorService);

                futures.add(future);
            }

            // 等待所有任务完成并收集结果
            log.info("⏳ 等待 {} 个并行任务完成...", futures.size());
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            try {
                allTasks.join(); // 等待所有任务完成

                // 收集所有结果
                for (CompletableFuture<List<CandlestickEntity>> future : futures) {
                    List<CandlestickEntity> rangeData = future.get();
                    newlyFetchedData.addAll(rangeData);
                }

                log.info("🎉 所有并行任务完成，总共新获取了 {} 条K线数据", newlyFetchedData.size());

            } catch (Exception e) {
                log.error("❌ 等待并行任务完成时发生错误: {}", e.getMessage(), e);
                throw new RuntimeException("并行获取数据失败", e);
            }

            // 6. 合并所有数据并按时间排序
            List<CandlestickEntity> allData = new ArrayList<>(existingData);
            allData.addAll(newlyFetchedData);

            // 过滤掉时间字段为null的数据，避免排序时出现NullPointerException
            allData = allData.stream()
                    .filter(Objects::nonNull)  // 过滤null对象
                    .filter(entity -> entity.getOpenTime() != null)  // 过滤开盘时间字段为null的对象
                    .distinct()
                    .collect(Collectors.toList());

            // 安全地进行排序
            try {
                Collections.sort(allData);
                log.info("📊 数据排序完成，最终有效数据: {} 条", allData.size());
            } catch (Exception e) {
                log.error("❌ 数据排序失败: {}", e.getMessage(), e);
                // 如果排序失败，至少返回未排序的数据
                log.warn("⚠️ 使用未排序的数据返回");
            }

            log.info("✨ 智能获取历史K线数据完成，最终返回 {} 条数据 (原有: {}, 新获取: {})，预期返回{} 条数据，还差{}条",
                    allData.size(), existingCount, newlyFetchedData.size(), totalExpectedCount, totalExpectedCount - allData.size());

            // 创建成功响应
            ApiResponse<List<CandlestickEntity>> successResponse = ApiResponse.success(allData);

            // 将结果存入Codis的Sorted Set（24小时过期）
            try {
                redisCacheService.batchAddKlineToSortedSet(symbol, interval, allData, 24 * 60); // 24小时 = 1440分钟
                log.info("💾 历史K线数据已存入Redis Sorted Set，key: coin_nrt_kline:{}{}, 条数: {}, 过期时间: 24小时",
                        symbol, interval, allData.size());
            } catch (Exception e) {
                log.warn("⚠️ 存储历史K线数据到Redis Sorted Set失败: {}", e.getMessage());
            }

            return successResponse;

        } catch (Exception e) {
            log.error("❌ 智能获取历史K线数据失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取历史K线数据失败: " + e.getMessage());
        }
    }

    /**
     * 将缺失时间点按连续范围分组
     */
    private List<List<LocalDateTime>> groupConsecutiveTimePoints(List<LocalDateTime> timePoints, long intervalMinutes) {
        List<List<LocalDateTime>> groups = new ArrayList<>();
        if (timePoints.isEmpty()) {
            return groups;
        }

        List<LocalDateTime> currentGroup = new ArrayList<>();
        currentGroup.add(timePoints.get(0));

        for (int i = 1; i < timePoints.size(); i++) {
            LocalDateTime current = timePoints.get(i);
            LocalDateTime previous = timePoints.get(i - 1);

            // 如果当前时间点与前一个时间点相差正好一个间隔，则属于同一组
            if (ChronoUnit.MINUTES.between(previous, current) == intervalMinutes) {
                currentGroup.add(current);
            } else {
                // 否则开始新的一组
                groups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
                currentGroup.add(current);
            }
        }

        // 添加最后一组
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }

    /**
     * 按每批100条分批获取指定时间范围的数据（使用多线程并行处理）
     */
    private List<CandlestickEntity> fetchRangeDataInBatches(String symbol, String interval,
                                                            LocalDateTime startTime, LocalDateTime endTime, int batchSize, long intervalMinutes) {
        List<CandlestickEntity> result = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<List<CandlestickEntity>>> batchFutures = new ArrayList<>();

        LocalDateTime currentStart = startTime;
        int batchCount = 0;

        // 准备所有批次的任务
        while (currentStart.isBefore(endTime)) {
            batchCount++;
            final int currentBatchNumber = batchCount;

            // 计算当前批次的结束时间
            LocalDateTime currentEnd = currentStart.plusMinutes(intervalMinutes * batchSize);
            if (currentEnd.isAfter(endTime)) {
                currentEnd = endTime;
            }

            final LocalDateTime finalCurrentStart = currentStart;
            final LocalDateTime finalCurrentEnd = currentEnd;

            // 计算实际需要获取的条数
            long expectedCount = ChronoUnit.MINUTES.between(finalCurrentStart, finalCurrentEnd) / intervalMinutes;

            // 创建异步任务
            CompletableFuture<List<CandlestickEntity>> batchFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            if (log.isDebugEnabled()) {
                                log.debug("  📥 线程 {} 获取第 {} 批数据: {} 到 {} (预期 {} 条)",
                                        Thread.currentThread().getName(), currentBatchNumber,
                                        finalCurrentStart, finalCurrentEnd, expectedCount);
                            }

                            // 调用API获取数据 (将LocalDateTime转换为时间戳)
                            long startTimestamp = finalCurrentStart.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                            long endTimestamp = finalCurrentEnd.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

                            // 添加随机延迟避免API限制（50-150ms）
                            Thread.sleep(50 + (int) (Math.random() * 100));

                            List<Candlestick> apiData = okxApiService.getHistoryKlineData(symbol, interval, startTimestamp, endTimestamp, batchSize);

                            if (apiData != null && !apiData.isEmpty()) {
                                // 转换并保存数据到MySQL
                                List<CandlestickEntity> entities = convertAndSaveCandlesticks(apiData, symbol, interval);

                                if (log.isDebugEnabled()) {
                                    log.debug("  ✅ 线程 {} 第 {} 批数据获取成功，实际获得 {} 条数据",
                                            Thread.currentThread().getName(), currentBatchNumber, entities.size());
                                }

                                return entities;
                            } else {
                                log.warn("  ⚠️ 线程 {} 第 {} 批数据获取结果为空: {} 到 {}",
                                        Thread.currentThread().getName(), currentBatchNumber, finalCurrentStart, finalCurrentEnd);
                                return new ArrayList<>();
                            }

                        } catch (Exception e) {
                            log.error("  ❌ 线程 {} 第 {} 批数据获取失败: {} 到 {}, 错误: {}",
                                    Thread.currentThread().getName(), currentBatchNumber, finalCurrentStart, finalCurrentEnd, e.getMessage());
                            return new ArrayList<>();
                        }
                    }, historicalDataExecutorService);

            batchFutures.add(batchFuture);
            currentStart = finalCurrentEnd;
        }

        // 等待所有批次任务完成
        log.info("  ⏳ 准备并行执行 {} 个批次任务...", batchFutures.size());
        CompletableFuture<Void> allBatches = CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));

        try {
            allBatches.join(); // 等待所有批次完成

            // 收集所有批次结果
            for (CompletableFuture<List<CandlestickEntity>> future : batchFutures) {
                List<CandlestickEntity> batchData = future.get();
                result.addAll(batchData);
            }

            log.info("  🏁 范围数据获取完成，共处理 {} 批，获得 {} 条数据", batchCount, result.size());

        } catch (Exception e) {
            log.error("  ❌ 等待批次任务完成时发生错误: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 调整结束时间以避免获取未完成的数据
     * 针对包含最新时间周期的查询进行时间边界调整
     */
    private LocalDateTime adjustEndTimeToAvoidIncompleteData(LocalDateTime endTime, String interval) {
        LocalDateTime now = LocalDateTime.now();

        // 如果结束时间在过去，无需调整
        if (endTime.isBefore(now.minusHours(1))) {
            return endTime;
        }

        LocalDateTime adjustedEndTime;

        switch (interval.toUpperCase()) {
            case "1W":
                // 周线: 排除当前周 (周一为一周开始)
                adjustedEndTime = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
            case "1D":
                // 日线: 排除当前日
                adjustedEndTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
            case "12H":
                // 12小时线: 排除当前12小时周期 (0点或12点开始)
                int currentHour = now.getHour();
                int alignedHour = (currentHour >= 12) ? 12 : 0;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0);
                break;
            case "6H":
                // 6小时线: 排除当前6小时周期 (0,6,12,18点开始)
                currentHour = now.getHour();
                alignedHour = (currentHour / 6) * 6;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0);
                break;
            case "4H":
                // 4小时线: 排除当前4小时周期 (0,4,8,12,16,20点开始)
                currentHour = now.getHour();
                alignedHour = (currentHour / 4) * 4;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0);
                break;
            case "2H":
                // 2小时线: 排除当前2小时周期
                currentHour = now.getHour();
                alignedHour = (currentHour / 2) * 2;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0);
                break;
            case "1H":
                // 1小时线: 排除当前小时
                adjustedEndTime = now.withMinute(0).withSecond(0).withNano(0);
                break;
            case "30M":
                // 30分钟线: 排除当前30分钟周期 (0或30分开始)
                int currentMinute = now.getMinute();
                int alignedMinute = (currentMinute >= 30) ? 30 : 0;
                adjustedEndTime = now.withMinute(alignedMinute).withSecond(0).withNano(0);
                break;
            case "15M":
                // 15分钟线: 排除当前15分钟周期 (0,15,30,45分开始)
                currentMinute = now.getMinute();
                alignedMinute = (currentMinute / 15) * 15;
                adjustedEndTime = now.withMinute(alignedMinute).withSecond(0).withNano(0);
                break;
            case "5M":
                // 5分钟线: 排除当前5分钟周期
                currentMinute = now.getMinute();
                alignedMinute = (currentMinute / 5) * 5;
                adjustedEndTime = now.withMinute(alignedMinute).withSecond(0).withNano(0);
                break;
            case "1M":
                // 包含两种情况: 月线和1分钟线，通过上下文判断
                if (endTime.isAfter(now.minusDays(40))) {
                    // 如果结束时间是近期，可能是月线，排除当前月
                    LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                    if (endTime.isAfter(monthStart)) {
                        adjustedEndTime = monthStart;
                    } else {
                        // 1分钟线: 排除当前分钟
                        adjustedEndTime = now.withSecond(0).withNano(0);
                    }
                } else {
                    // 1分钟线: 排除当前分钟
                    adjustedEndTime = now.withSecond(0).withNano(0);
                }
                break;
            default:
                // 未知间隔，保守起见排除当前小时
                adjustedEndTime = now.withMinute(0).withSecond(0).withNano(0);
                break;
        }

        // 返回调整后的时间与原始结束时间的较小值
        return endTime.isBefore(adjustedEndTime) ? endTime : adjustedEndTime;
    }

    /**
     * 转换并保存K线数据到MySQL数据库
     */
    private List<CandlestickEntity> convertAndSaveCandlesticks(List<Candlestick> candlesticks, String symbol, String interval) {
        List<CandlestickEntity> entities = new ArrayList<>();

        for (Candlestick candlestick : candlesticks) {
            try {
                // 验证必要字段不为空
                if (candlestick == null || candlestick.getOpenTime() == null) {
                    log.warn("⚠️ 跳过无效的K线数据: openTime为null");
                    continue;
                }

                CandlestickEntity entity = new CandlestickEntity();
                entity.setSymbol(symbol);
                entity.setIntervalVal(interval);
                entity.setOpenTime(candlestick.getOpenTime());
                entity.setCloseTime(candlestick.getCloseTime());
                entity.setOpen(candlestick.getOpen());
                entity.setHigh(candlestick.getHigh());
                entity.setLow(candlestick.getLow());
                entity.setClose(candlestick.getClose());
                entity.setVolume(candlestick.getVolume());
                entity.setQuoteVolume(candlestick.getQuoteVolume());
                entity.setTrades(candlestick.getTrades());
                entity.setFetchTime(LocalDateTime.now());
                entities.add(entity);
            } catch (Exception e) {
                log.error("❌ 转换K线数据失败: {}", e.getMessage(), e);
            }
        }

        try {
            // 保存数据到MySQL数据库
            historicalDataService.saveHistoricalData(entities);
            log.info("    💾 已将 {} 条K线数据保存到MySQL", entities.size());
        } catch (Exception e) {
            log.error("    ❌ 保存K线数据到MySQL失败: {}", e.getMessage());
            // 即使保存失败也返回数据，避免影响接口响应
        }

        return entities;
    }
}
