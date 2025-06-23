package com.okx.trading.service.impl;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.repository.CandlestickRepository;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.RedisCacheService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 历史数据服务实现类
 * 提供获取历史K线数据并保存到数据库的功能
 */
@Slf4j
@Service
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private final OkxApiService okxApiService;
    private final CandlestickRepository candlestickRepository;
    private final ExecutorService executorService;
    private final ExecutorService batchExecutorService;
    // 注入历史数据查询线程池
    private final ExecutorService historicalDataExecutorService;

    @Value("${okx.historical-data.batch-size:100}")
    private int batchSize = 100;

    @Value("${okx.historical-data.max-threads:10}")
    private int maxThreads = 10;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisCacheService redisCacheService;


    private DateTimeFormatter dateFormatPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 时间分片类
     */
    @Data
    @AllArgsConstructor
    private static class TimeSlice {
        private LocalDateTime start;
        private LocalDateTime end;

        @Override
        public String toString() {
            return String.format("[%s - %s]", start, end);
        }
    }

    @Autowired
    public HistoricalDataServiceImpl(@Lazy OkxApiService okxApiService,
                                     CandlestickRepository candlestickRepository,
                                     @Qualifier("historicalDataExecutorService") ExecutorService executorService,
                                     @Qualifier("batchHistoricalDataExecutorService") ExecutorService batchExecutorService,
                                     @Qualifier("historicalDataExecutorService")
                                     ExecutorService historicalDataExecutorService) {
        this.okxApiService = okxApiService;
        this.candlestickRepository = candlestickRepository;
        this.executorService = executorService;
        this.batchExecutorService = batchExecutorService;
        this.historicalDataExecutorService = historicalDataExecutorService;
    }

    @Override
    public CompletableFuture<Integer> fetchAndSaveHistoricalData(String symbol, String interval,
                                                                 LocalDateTime startTime, LocalDateTime endTime) {
        log.info("开始获取历史K线数据: symbol={}, interval={}, startTime={}, endTime={}",
                symbol, interval, startTime, endTime);

        // 按天检查数据完整性，找出需要获取的天数
        List<TimeSlice> daysToFetch = getIncompleteDays(symbol, interval, startTime, endTime);

        if (daysToFetch.isEmpty()) {
            log.info("指定时间范围内的所有天数数据都已完整，无需获取");
            return CompletableFuture.completedFuture(0);
        }

        log.info("需要获取的天数: {}", daysToFetch.size());

        // 按不完整的天数创建任务列表
        List<CompletableFuture<Integer>> dayFutures = new ArrayList<>();

        for (TimeSlice daySlice : daysToFetch) {
            CompletableFuture<Integer> dayFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    LocalDateTime dayStart = daySlice.getStart();
                    LocalDateTime dayEnd = daySlice.getEnd();

                    log.info("开始获取日期 {} 的数据", dayStart.toLocalDate());

                    // 计算当天预期的数据点数量
                    int expectedDayTotal = calculateExpectedDataPoints(interval, dayStart, dayEnd);
                    log.info("预计需要获取的数据点数量: {}", expectedDayTotal);

                    // 计算需要分成的批次数
                    int requiredBatches = (int) Math.ceil((double) expectedDayTotal / batchSize);
                    log.info("将分为{}个批次获取, 每批次{}条数据", requiredBatches, batchSize);

                    // 创建当天的时间分片
                    List<TimeSlice> timeSlices = createTimeSlices(interval, dayStart, dayEnd, requiredBatches);

                    // 创建多线程任务列表
                    List<CompletableFuture<List<CandlestickEntity>>> batchFutures = new ArrayList<>();

                    for (TimeSlice slice : timeSlices) {
                        CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                            try {
                                log.debug("获取时间片段数据,左右都不包括,故意开始时间减一秒,结束时间也减一秒: {}", slice);
                                List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                                        symbol, interval, toEpochMilli(slice.getStart().minusSeconds(1)), toEpochMilli(slice.getEnd().minusSeconds(1)), batchSize);

                                // 转换为实体类
                                List<CandlestickEntity> entities = convertToEntities(candlesticks, symbol, interval);
                                log.debug("时间片段{}获取到{}条数据", slice, entities.size());

                                // 保存数据
                                return saveBatch(entities);
                            } catch (Exception e) {
                                log.error("获取时间片段{}数据失败: {}", slice, e.getMessage(), e);
                                return Collections.emptyList();
                            }
                        }, executorService);

                        batchFutures.add(future);
                    }

                    // 合并当天所有批次的结果
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

                    int totalSaved = batchFutures.stream()
                            .map(CompletableFuture::join)
                            .mapToInt(List::size)
                            .sum();

                    log.info("完成日期 {} 的数据获取, 共保存{}条数据", dayStart.toLocalDate(), totalSaved);

                    // 再次检查当天数据完整性
                    boolean isComplete = isDayDataComplete(symbol, interval, dayStart, dayEnd);

                    if (!isComplete) {
                        log.info("日期 {} 的数据仍不完整，尝试填充缺失数据点", dayStart.toLocalDate());
                        CandlestickEntity candlestickEntity = new CandlestickEntity();
                        candlestickEntity.setSymbol(symbol);
                        candlestickEntity.setIntervalVal(interval);
                        List<LocalDateTime> missingTimes = checkDataIntegrity(Arrays.asList(candlestickEntity), dayStart, dayEnd);

                        if (!missingTimes.isEmpty()) {
                            log.info("日期 {} 有 {} 个缺失的数据点，尝试单点填充", dayStart.toLocalDate(), missingTimes.size());
                            int filledCount = fillMissingData(symbol, interval, missingTimes, new ConcurrentHashMap<>()).get();
                            totalSaved += filledCount;
                        }
                    }

                    return totalSaved;
                } catch (Exception e) {
                    log.error("获取日期 {} 的数据失败: {}", daySlice.getStart().toLocalDate(), e.getMessage(), e);
                    return 0;
                }
            }, batchExecutorService);

            dayFutures.add(dayFuture);
        }

        // 合并所有天数的任务结果
        return CompletableFuture.allOf(dayFutures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> {
                    int totalSaved = dayFutures.stream()
                            .map(future -> {
                                try {
                                    return future.get();
                                } catch (Exception e) {
                                    log.error("获取任务结果失败: {}", e.getMessage(), e);
                                    return 0;
                                }
                            })
                            .mapToInt(Integer::intValue)
                            .sum();

                    log.info("完成所有不完整天数的历史数据获取, 共保存{}条数据", totalSaved);
                    return totalSaved;
                }, executorService);
    }

    /**
     * 获取不完整的天数列表
     *
     * @param symbol    交易对
     * @param interval  时间间隔
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 不完整的天数列表
     */
    private List<TimeSlice> getIncompleteDays(String symbol, String interval, LocalDateTime startTime, LocalDateTime endTime) {
        List<TimeSlice> incompleteDays = new ArrayList<>();


        // 获取时间范围内的所有天数
        LocalDateTime currentDay = startTime;
        LocalDateTime lastDay = endTime.minusSeconds(1);
        if (startTime.getDayOfYear() == endTime.getDayOfYear()) {
            // 检查当天数据是否完整
            if (!isDayDataComplete(symbol, interval, currentDay, lastDay)) {
                incompleteDays.add(new TimeSlice(currentDay, lastDay));
                return incompleteDays;
            }
        }

        while (!currentDay.isAfter(lastDay)) {
            // 计算当天结束时间（次日0点）-1 秒
            LocalDateTime nextDay = currentDay.plusDays(1);
            if (nextDay.isAfter(endTime)) {
                nextDay = endTime;
            }

            // 检查当天数据是否完整
            if (!isDayDataComplete(symbol, interval, currentDay, nextDay)) {
                incompleteDays.add(new TimeSlice(currentDay, nextDay));
            }
//            else{
//                log.info("{} 的数据已完整，跳过获取", currentDay.toLocalDate());
//            }

            currentDay = nextDay;
        }

        return incompleteDays;
    }

    /**
     * 检查某一天的数据是否完整
     *
     * @param symbol   交易对
     * @param interval 时间间隔
     * @param dayStart 当天开始时间
     * @param dayEnd   当天结束时间
     * @return 数据是否完整
     */
    private boolean isDayDataComplete(String symbol, String interval, LocalDateTime dayStart, LocalDateTime dayEnd) {
        // 获取预期的所有时间点
        List<LocalDateTime> expectedTimes = generateExpectedTimePoints(interval, dayStart, dayEnd);

        // 获取数据库中已存在的时间点
        List<LocalDateTime> existingTimes = candlestickRepository
                .findExistingOpenTimesBySymbolAndIntervalBetween(symbol, interval, dayStart, dayEnd);

        // 如果预期时间点数量与已存在时间点数量相同，则认为数据完整
        Set<String> expectedStr = expectedTimes.stream().map(LocalDateTime::toString).collect(Collectors.toSet());
        Set<String> existingStr = existingTimes.stream().map(LocalDateTime::toString).collect(Collectors.toSet());
        expectedStr.removeAll(existingStr);

        if (expectedStr.isEmpty()) {
            // 进一步检查是否所有预期时间点都存在
            Set<LocalDateTime> existingTimeSet = new HashSet<>(existingTimes);
            boolean isComplete = expectedTimes.stream().allMatch(existingTimeSet::contains);

            if (isComplete) {
                log.debug("{} 的数据已完整，共{}个数据点", dayStart.toLocalDate(), expectedTimes.size());
                return true;
            }
        }

        log.info("{} 的数据不完整，预期{}个数据点，实际{}个数据点",
                dayStart.toLocalDate(), expectedTimes.size(), existingTimes.size());
        return false;
    }

    @Override
    public List<CandlestickEntity> getHistoricalData(String symbol, String interval,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        return candlestickRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                symbol, interval, startTime, endTime);
    }

    @Override
    public List<CandlestickEntity> fetchAndSaveHistoryWithIntegrityCheck(String symbol, String interval, String startTimeStr, String endTimeStr) {
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
        long intervalMinutes = getIntervalMinutes(interval);
        long totalExpectedCount = ChronoUnit.MINUTES.between(startTime, endTime) / intervalMinutes;
        log.info("📊 根据时间范围计算，预期需要获取的K线数量: {}", totalExpectedCount);
        TreeSet<CandlestickEntity> cachedData = new TreeSet<>();

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
                    log.info("📦 从Redis Sorted Set获取历史K线数据, symbol: {}, interval: {}, 数量: {}, 时间范围: {} ~ {}",
                            symbol, interval, cachedData.size(), startTimeStr, endTime.toString());
                    return cachedData.stream().collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 从Redis Sorted Set获取缓存失败，继续执行原逻辑: {}", e.getMessage());
        }

//        log.info("🚀 智能获取历史K线数据开始, symbol: {}, interval: {}, startTime: {}, endTime: {}", symbol, interval, startTimeStr, endTimeStr);


        // 2. 从MySQL获取已经有的K线数量
        List<CandlestickEntity> existingData = getHistoricalData(symbol, interval, startTime, endTime);
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
                if (!cacheData.isEmpty()) {
                    redisCacheService.batchAddKlineToSortedSet(symbol, interval, cacheData, 24 * 60);
                }
                log.info("💾 历史K线数据已存入Redis Sorted Set，key: coin_nrt_kline:{}{}, 条数: {}, 过期时间: 24小时",
                        symbol, interval, existingData.size());
            } catch (Exception e) {
                log.warn("⚠️ 存储历史K线数据到Redis Sorted Set失败: {}", e.getMessage());
            }

            return existingData;
        }

        // 4. 检查数据完整性，找出缺失的时间范围
        if (existingData.isEmpty()) {
            CandlestickEntity candlestick = new CandlestickEntity();
            candlestick.setSymbol(symbol);
            candlestick.setIntervalVal(interval);
            existingData.add(candlestick);
        }
        List<LocalDateTime> missingTimePoints = checkDataIntegrity(existingData, startTime, endTime);
        log.info("🔍 发现 {} 个缺失的时间点需要获取", missingTimePoints.size());

        if (missingTimePoints.isEmpty()) {
            log.info("✅ 数据完整性检查通过，直接返回MySQL中的 {} 条数据", existingCount);
            return existingData;
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

                            log.info("🧵 线程 {} 开始处理第 {} 个时间范围", Thread.currentThread().getName(), rangeIndex);

                            // 按批次获取这个范围的数据
                            List<CandlestickEntity> rangeData = fetchRangeDataInBatches(
                                    symbol, interval, rangeStart, rangeEnd, batchSize, intervalMinutes);

                            log.info("✅ 线程 {} 完成第 {} 个时间范围，获取了 {} 条数据",
                                    Thread.currentThread().getName(), rangeIndex, rangeData.size());

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

        // 将结果存入Codis的Sorted Set（24小时过期）
        try {
            redisCacheService.batchAddKlineToSortedSet(symbol, interval, allData, 24 * 60); // 24小时 = 1440分钟
            log.info("💾 历史K线数据已存入Redis Sorted Set，key: coin_nrt_kline:{}{}, 条数: {}, 过期时间: 24小时",
                    symbol, interval, allData.size());
        } catch (Exception e) {
            log.warn("⚠️ 存储历史K线数据到Redis Sorted Set失败: {}", e.getMessage());
        }

        return allData;


    }

    @Override
    public List<CandlestickEntity> getHistoricalData(String symbol, String interval) {
        return candlestickRepository.findBySymbolAndIntervalAsc(symbol, interval);
    }

    @Override
    public List<LocalDateTime> checkDataIntegrity(List<CandlestickEntity> data, LocalDateTime startTime, LocalDateTime endTime) {
        String symbol = data.get(0).getSymbol();
        String interval = data.get(0).getIntervalVal();
//        log.info("检查数据完整性: symbol={}, interval={}, startTime={}, endTime={}", data.get(0), interval, startTime, endTime);
        // 获取预期的所有时间点
        List<LocalDateTime> expectedTimes = generateExpectedTimePoints(interval, startTime, endTime);
        log.info("预期数据点数量: {}", expectedTimes.size());

        // 获取数据库中已存在的时间点
        List<LocalDateTime> existingTimes = data.stream().filter(candlestick -> candlestick.getOpenTime() != null)
                .map(CandlestickEntity::getOpenTime).collect(Collectors.toList());
        log.info("数据库中已有数据点数量: {}", existingTimes.size());

        // 计算缺失的时间点
        Set<LocalDateTime> existingTimeSet = new HashSet<>(existingTimes);
        List<LocalDateTime> missingTimes = expectedTimes.stream()
                .filter(time -> !existingTimeSet.contains(time))
                .collect(Collectors.toList());

        log.info("缺失的数据点数量: {}", missingTimes.size());
        return missingTimes;
    }

    @Override
    public CompletableFuture<Integer> fillMissingData(String symbol, String interval, List<LocalDateTime> missingTimes) {
        if (missingTimes.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        log.info("开始补充缺失数据: symbol={}, interval={}, 缺失点数量={}",
                symbol, interval, missingTimes.size());

        // 将缺失的时间点按批次分组
        List<List<LocalDateTime>> batches = new ArrayList<>();
        for (int i = 0; i < missingTimes.size(); i += batchSize) {
            batches.add(missingTimes.subList(i, Math.min(i + batchSize, missingTimes.size())));
        }

        log.info("缺失数据分为{}个批次获取", batches.size());

        // 为每个批次创建一个异步任务
        List<CompletableFuture<List<CandlestickEntity>>> futures = new ArrayList<>();

        for (List<LocalDateTime> batch : batches) {
            // 为每个批次找到时间范围
            LocalDateTime batchStart = batch.stream().min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime batchEnd = batch.stream().max(LocalDateTime::compareTo).orElse(null);

            if (batchStart == null || batchEnd == null) {
                continue;
            }

            // 获取时间范围内的所有数据
            CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                            symbol, interval, toEpochMilli(batchStart), toEpochMilli(batchEnd), batchSize);

                    // 过滤出缺失的时间点对应的数据
                    Set<LocalDateTime> batchTimeSet = new HashSet<>(batch);
                    List<Candlestick> filteredCandlesticks = candlesticks.stream()
                            .filter(c -> batchTimeSet.contains(c.getOpenTime()))
                            .collect(Collectors.toList());

                    // 转换并保存
                    List<CandlestickEntity> entities = convertToEntities(filteredCandlesticks, symbol, interval);
                    return saveBatch(entities);
                } catch (Exception e) {
                    log.error("补充缺失数据失败: {}", e.getMessage(), e);
                    return Collections.emptyList();
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有异步任务完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .mapToInt(List::size)
                        .sum());
    }

    /**
     * 单独获取缺失的数据点，同时记录失败的请求
     *
     * @param symbol         交易对，如BTC-USDT
     * @param interval       K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param missingTimes   缺失的时间点列表
     * @param failedRequests 用于记录失败请求的并发Map
     * @return 填补的数据点数量
     */
    @Override
    public CompletableFuture<Integer> fillMissingData(String symbol, String interval, List<LocalDateTime> missingTimes,
                                                      ConcurrentMap<String, Integer> failedRequests) {
        if (missingTimes.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        log.info("开始补充缺失数据(带失败记录): symbol={}, interval={}, 缺失点数量={}",
                symbol, interval, missingTimes.size());

        // 将缺失的时间点按批次分组
        List<List<LocalDateTime>> batches = new ArrayList<>();
        for (int i = 0; i < missingTimes.size(); i += batchSize) {
            batches.add(missingTimes.subList(i, Math.min(i + batchSize, missingTimes.size())));
        }

        log.info("缺失数据分为{}个批次获取", batches.size());

        // 为每个批次创建一个异步任务
        List<CompletableFuture<List<CandlestickEntity>>> futures = new ArrayList<>();

        for (List<LocalDateTime> batch : batches) {
            // 为每个批次找到时间范围
            LocalDateTime batchStart = batch.stream().min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime batchEnd = batch.stream().max(LocalDateTime::compareTo).orElse(null);

            if (batchStart == null || batchEnd == null) {
                continue;
            }

            String batchKey = batchStart.toString() + ":" + batchEnd.toString();

            // 获取时间范围内的所有数据
            CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                            symbol, interval, toEpochMilli(batchStart), toEpochMilli(batchEnd), batchSize);

                    if (candlesticks.isEmpty()) {
                        log.warn("缺失数据批次 {} 未获取到数据", batchKey);
                        // 记录失败请求
                        failedRequests.compute(batchKey, (k, v) -> (v == null) ? 1 : v + 1);
                        return Collections.emptyList();
                    }

                    // 过滤出缺失的时间点对应的数据
                    Set<LocalDateTime> batchTimeSet = new HashSet<>(batch);
                    List<Candlestick> filteredCandlesticks = candlesticks.stream()
                            .filter(c -> batchTimeSet.contains(c.getOpenTime()))
                            .collect(Collectors.toList());

                    // 转换并保存
                    List<CandlestickEntity> entities = convertToEntities(filteredCandlesticks, symbol, interval);
                    List<CandlestickEntity> savedEntities = saveBatch(entities);

                    // 检查是否完整获取所有缺失点
                    Set<LocalDateTime> savedTimeSet = savedEntities.stream()
                            .map(CandlestickEntity::getOpenTime)
                            .collect(Collectors.toSet());

                    batchTimeSet.removeAll(savedTimeSet);
                    if (!batchTimeSet.isEmpty()) {
                        log.warn("批次 {} 仍有 {} 个点未能获取", batchKey, batchTimeSet.size());
                        // 记录未完全成功的批次
                        String missKey = String.format("%s_%s_missing_points_%s", symbol, interval, batchKey);
                        failedRequests.compute(missKey, (k, v) -> (v == null) ? batchTimeSet.size() : v + batchTimeSet.size());
                    }

                    return savedEntities;
                } catch (Exception e) {
                    log.error("补充缺失数据批次 {} 失败: {}", batchKey, e.getMessage(), e);
                    // 记录失败请求
                    failedRequests.compute(batchKey, (k, v) -> (v == null) ? 1 : v + 1);
                    return Collections.emptyList();
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有异步任务完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int totalSaved = futures.stream()
                            .map(CompletableFuture::join)
                            .mapToInt(List::size)
                            .sum();

                    log.info("总共补充了 {} 个缺失数据点", totalSaved);
                    return totalSaved;
                });
    }

    /**
     * 计算时间间隔内预期的数据点数量
     */
    private int calculateExpectedDataPoints(String interval, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = getIntervalMinutes(interval);
        return (int) (ChronoUnit.MINUTES.between(startTime, endTime.minusSeconds(1)) / minutes) + 1;
    }

    /**
     * 创建时间分片
     */
    private List<TimeSlice> createTimeSlices(String interval, LocalDateTime startTime, LocalDateTime endTime, int batchCount) {
        List<TimeSlice> slices = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (endTime.isAfter(now)) {
            endTime = now;
        }
        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        long minutesPerBatch = totalMinutes / batchCount;
        long intervalMinutes = getIntervalMinutes(interval);

        // 确保分片大小是间隔的整数倍
        minutesPerBatch = ((minutesPerBatch / intervalMinutes) + 1) * intervalMinutes;

        LocalDateTime current = startTime;
        while (current.isBefore(endTime)) {
            LocalDateTime sliceEnd = current.plusMinutes(minutesPerBatch);
            if (sliceEnd.isAfter(endTime)) {
                sliceEnd = endTime;
            }

            slices.add(new TimeSlice(current, sliceEnd));
            current = sliceEnd;
        }

        return slices;
    }

    /**
     * 获取间隔对应的分钟数  周期间隔分钟数 月期间隔分钟数  年期间隔分钟数  默认1分钟  Ralph
     */
    @Override
    public long getIntervalMinutes(String interval) {
        String unit = interval.substring(interval.length() - 1);
        int amount = Integer.parseInt(interval.substring(0, interval.length() - 1));

        switch (unit) {
            case "m":
                return amount;
            case "H":
                return amount * 60;
            case "D":
                return amount * 60 * 24;
            case "W":
                return amount * 60 * 24 * 7;
            case "M":
                return amount * 60 * 24 * 30; // 简化处理，按30天/月计算
            default:
                return 1;
        }
    }

    /**
     * 生成预期的时间点列表
     */
    private List<LocalDateTime> generateExpectedTimePoints(String interval, LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDateTime> timePoints = new ArrayList<>();
        endTime = endTime;

        long intervalMinutes = getIntervalMinutes(interval);

        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            timePoints.add(current);
            current = current.plusMinutes(intervalMinutes);
        }

        return timePoints;
    }

    /**
     * 将LocalDateTime转换为毫秒时间戳
     */
    private Long toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 将Candlestick转换为CandlestickEntity
     */
    private List<CandlestickEntity> convertToEntities(List<Candlestick> candlesticks, String symbol, String intervalVal) {
        LocalDateTime now = LocalDateTime.now();

        return candlesticks.stream()
                .map(c -> {
                    CandlestickEntity entity = new CandlestickEntity();
                    entity.setSymbol(symbol);
                    entity.setIntervalVal(intervalVal);
                    entity.setOpenTime(c.getOpenTime());
                    entity.setCloseTime(c.getCloseTime());
                    entity.setOpen(c.getOpen());
                    entity.setHigh(c.getHigh());
                    entity.setLow(c.getLow());
                    entity.setClose(c.getClose());
                    entity.setVolume(c.getVolume());
                    entity.setQuoteVolume(c.getQuoteVolume());
                    entity.setTrades(c.getTrades());
                    entity.setFetchTime(now);
                    return entity;
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量保存实体，避免重复
     * 如果数据已存在则跳过，不删除已有数据
     */
    @Transactional
    public synchronized List<CandlestickEntity> saveBatch(List<CandlestickEntity> entities) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 获取第一个和最后一个实体的时间范围
            String symbol = entities.get(0).getSymbol();
            String interval = entities.get(0).getIntervalVal();

            LocalDateTime minTime = entities.stream()
                    .map(CandlestickEntity::getOpenTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime maxTime = entities.stream()
                    .map(CandlestickEntity::getOpenTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            if (minTime != null && maxTime != null) {
                // 查询已存在的数据
                List<CandlestickEntity> existingEntities = candlestickRepository
                        .findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(symbol, interval, minTime, maxTime);

                // 创建已存在数据的时间点集合，用于过滤
                Set<String> existingTimePoints = existingEntities.stream()
                        .map(CandlestickEntity::getOpenTime).map(time -> time.format(dateFormatPattern))
                        .collect(Collectors.toSet());

                // 过滤出不存在的数据
                List<CandlestickEntity> newEntities = entities.stream()
                        .filter(entity -> !existingTimePoints.contains(entity.getOpenTime().format(dateFormatPattern)))
                        .collect(Collectors.toList());

                log.info("时间范围 {} ~ {} 内已有 {} 条数据, 查询获取 {} 条数据，新增 {} 条数据",
                        minTime.format(dateFormatPattern), maxTime.format(dateFormatPattern), existingEntities.size(), entities.size(), newEntities.size());

                List<Candlestick> candlestickEntities = newEntities.stream().map(x -> {
                    Candlestick candlestick = new Candlestick();
                    BeanUtils.copyProperties(x, candlestick);
                    return candlestick;
                }).collect(Collectors.toList());

                // 只保存新数据
                if (!newEntities.isEmpty()) {
                    return candlestickRepository.saveAll(newEntities);
                } else {
                    return Collections.emptyList();
                }
            }

            // 如果没有时间范围信息，直接保存所有数据
            return candlestickRepository.saveAll(entities);
        } catch (Exception e) {
            log.error("保存批量数据时出错: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据交易对和时间间隔查询最新的K线数据
     *
     * @param symbol   交易对
     * @param interval 时间间隔
     * @param limit    数量限制
     * @return K线数据列表
     */
    public List<CandlestickEntity> getLatestHistoricalData(String symbol, String interval, int limit) {
        return candlestickRepository.findLatestBySymbolAndInterval(
                symbol, interval, PageRequest.of(0, limit));
    }

    /**
     * 将缺失的时间点按天分组
     *
     * @param missingTimes 缺失的时间点列表
     * @return 按天分组的缺失时间点Map，key为每天的0点时间
     */
    private Map<LocalDateTime, List<LocalDateTime>> groupMissingTimesByDay(List<LocalDateTime> missingTimes) {
        Map<LocalDateTime, List<LocalDateTime>> result = new HashMap<>();

        for (LocalDateTime time : missingTimes) {
            // 获取当天的0点时间作为key
            LocalDateTime dayStart = time.toLocalDate().atStartOfDay();

            if (!result.containsKey(dayStart)) {
                result.put(dayStart, new ArrayList<>());
            }

            result.get(dayStart).add(time);
        }

        return result;
    }

    @Override
    public CompletableFuture<Integer> fetchAndSaveHistoricalDataWithFailureRecord(String symbol, String interval,
                                                                                  LocalDateTime startTime, LocalDateTime endTime,
                                                                                  ConcurrentMap<String, Integer> failedRequests) {
        log.info("开始获取历史K线数据(带失败记录): symbol={}, interval={}, startTime={}, endTime={}",
                symbol, interval, startTime, endTime);

        // 按天检查数据完整性，找出需要获取的天数
        List<TimeSlice> daysToFetch = getIncompleteDays(symbol, interval, startTime, endTime);

        if (daysToFetch.isEmpty()) {
            log.info("指定时间范围内的所有天数数据都已完整，无需获取");
            return CompletableFuture.completedFuture(0);
        }

        log.info("需要获取的天数: {}", daysToFetch.size());

        // 按不完整的天数创建任务列表
        List<CompletableFuture<Integer>> dayFutures = new ArrayList<>();

        for (TimeSlice daySlice : daysToFetch) {
            CompletableFuture<Integer> dayFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    LocalDateTime dayStart = daySlice.getStart();
                    LocalDateTime dayEnd = daySlice.getEnd();

                    log.info("开始获取日期 {} 的数据", dayStart.toLocalDate());

                    // 计算当天预期的数据点数量
                    int expectedDayTotal = calculateExpectedDataPoints(interval, dayStart, dayEnd);
                    log.info("预计需要获取的数据点数量: {}", expectedDayTotal);

                    // 计算需要分成的批次数
                    int requiredBatches = (int) Math.ceil((double) expectedDayTotal / batchSize);
                    log.info("将分为{}个批次获取, 每批次{}条数据", requiredBatches, batchSize);

                    // 创建当天的时间分片
                    List<TimeSlice> timeSlices = createTimeSlices(interval, dayStart, dayEnd, requiredBatches);

                    // 创建多线程任务列表
                    List<CompletableFuture<List<CandlestickEntity>>> batchFutures = new ArrayList<>();

                    for (TimeSlice slice : timeSlices) {
                        CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                            try {
                                log.debug("获取时间片段数据,左右都不包括,故意开始时间减一秒,结束时间也减一秒: {}", slice);
                                List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                                        symbol, interval, toEpochMilli(slice.getStart().minusSeconds(1)), toEpochMilli(slice.getEnd().minusSeconds(1)), batchSize);

                                // 转换为实体类
                                List<CandlestickEntity> entities = convertToEntities(candlesticks, symbol, interval);
                                log.debug("时间片段{}获取到{}条数据", slice, entities.size());

                                // 保存数据
                                return saveBatch(entities);
                            } catch (Exception e) {
                                log.error("获取时间片段{}数据失败: {}", slice, e.getMessage(), e);
                                // 记录失败的请求
                                String requestKey = String.format("%s_%s_%s_%s", symbol, interval,
                                        slice.getStart().toString(), slice.getEnd().toString());
                                failedRequests.compute(requestKey, (k, v) -> (v == null) ? 1 : v + 1);
                                return Collections.emptyList();
                            }
                        }, executorService);

                        batchFutures.add(future);
                    }

                    // 合并当天所有批次的结果
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

                    int totalSaved = batchFutures.stream()
                            .map(CompletableFuture::join)
                            .mapToInt(List::size)
                            .sum();

                    log.info("完成日期 {} 的数据获取, 共保存{}条数据", dayStart.toLocalDate(), totalSaved);

                    // 再次检查当天数据完整性
                    boolean isComplete = isDayDataComplete(symbol, interval, dayStart, dayEnd);

                    if (!isComplete) {
                        log.info("日期 {} 的数据仍不完整，尝试填充缺失数据点", dayStart.toLocalDate());
                        CandlestickEntity candlestickEntity = new CandlestickEntity();
                        candlestickEntity.setSymbol(symbol);
                        candlestickEntity.setIntervalVal(interval);
                        List<LocalDateTime> missingTimes = checkDataIntegrity(Arrays.asList(candlestickEntity), dayStart, dayEnd);

                        if (!missingTimes.isEmpty()) {
                            log.info("日期 {} 有 {} 个缺失的数据点，尝试单点填充", dayStart.toLocalDate(), missingTimes.size());
                            try {
                                int filledCount = fillMissingData(symbol, interval, missingTimes, failedRequests).get();
                                totalSaved += filledCount;
                            } catch (Exception e) {
                                log.error("填充缺失数据点失败: {}", e.getMessage(), e);
                                // 记录失败的填充请求
                                String requestKey = String.format("%s_%s_fill_missing_%s", symbol, interval,
                                        dayStart.toLocalDate().toString());
                                failedRequests.compute(requestKey, (k, v) -> (v == null) ? 1 : v + 1);
                            }
                        }
                    }

                    return totalSaved;
                } catch (Exception e) {
                    log.error("获取日期 {} 的数据失败: {}", daySlice.getStart().toLocalDate(), e.getMessage(), e);
                    // 记录整天失败的请求
                    String requestKey = String.format("%s_%s_day_%s", symbol, interval,
                            daySlice.getStart().toLocalDate().toString());
                    failedRequests.compute(requestKey, (k, v) -> (v == null) ? 1 : v + 1);
                    return 0;
                }
            }, batchExecutorService);

            dayFutures.add(dayFuture);
        }

        // 合并所有天数的任务结果
        return CompletableFuture.allOf(dayFutures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> {
                    int totalSaved = dayFutures.stream()
                            .map(future -> {
                                try {
                                    return future.get();
                                } catch (Exception e) {
                                    log.error("获取任务结果失败: {}", e.getMessage(), e);
                                    // 记录获取结果失败
                                    String requestKey = String.format("%s_%s_result_failure_%s", symbol, interval,
                                            UUID.randomUUID().toString());
                                    failedRequests.compute(requestKey, (k, val) -> (val == null) ? 1 : val + 1);
                                    return 0;
                                }
                            })
                            .mapToInt(Integer::intValue)
                            .sum();

                    log.info("完成所有不完整天数的历史数据获取, 共保存{}条数据, 失败请求数量: {}", totalSaved, failedRequests.size());
                    return totalSaved;
                }, executorService);
    }

    @Override
    public CompletableFuture<Integer> fetchAndSaveTimeSliceWithFailureRecord(String symbol, String interval,
                                                                             LocalDateTime startTime, LocalDateTime endTime,
                                                                             ConcurrentMap<String, Integer> failedRequests) {
        String sliceKey = startTime.toString() + ":" + endTime.toString();
        log.info("开始获取时间片段数据（带失败记录）: symbol={}, interval={}, timeSlice={}", symbol, interval, sliceKey);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取原始数据
                List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                        symbol, interval, toEpochMilli(startTime.minusSeconds(1)), toEpochMilli(endTime.minusSeconds(1)), batchSize);

                if (candlesticks.isEmpty()) {
                    log.warn("时间片段 {} 未获取到数据", sliceKey);
                    // 记录失败请求
                    failedRequests.compute(sliceKey, (k, v) -> (v == null) ? 1 : v + 1);
                    return 0;
                }

                // 转换为实体并保存
                List<CandlestickEntity> entities = convertToEntities(candlesticks, symbol, interval);
                List<CandlestickEntity> savedEntities = saveBatch(entities);

                log.info("时间片段 {} 成功保存 {} 条数据", sliceKey, savedEntities.size());

                // 检查是否完整获取
                List<LocalDateTime> expectedTimes = generateExpectedTimePoints(interval, startTime, endTime);
                List<LocalDateTime> existingTimes = savedEntities.stream()
                        .map(CandlestickEntity::getOpenTime)
                        .collect(Collectors.toList());

                Set<LocalDateTime> expectedSet = new HashSet<>(expectedTimes);
                expectedSet.removeAll(existingTimes);

                if (!expectedSet.isEmpty()) {
                    log.warn("时间片段 {} 仍有 {} 个缺失的数据点", sliceKey, expectedSet.size());

                    // 尝试直接填充缺失点
                    List<LocalDateTime> missingTimes = new ArrayList<>(expectedSet);
                    try {
                        int filledCount = fillMissingData(symbol, interval, missingTimes, failedRequests).get();
                        log.info("时间片段 {} 成功填充 {} 个缺失数据点", sliceKey, filledCount);
                        return savedEntities.size() + filledCount;
                    } catch (Exception e) {
                        log.error("填充缺失数据点失败: {}", e.getMessage(), e);
                        // 如果填充失败但原始获取成功，不记录失败
                        return savedEntities.size();
                    }
                }

                return savedEntities.size();
            } catch (Exception e) {
                log.error("获取时间片段 {} 数据失败: {}", sliceKey, e.getMessage(), e);
                // 记录失败请求
                failedRequests.compute(sliceKey, (k, v) -> (v == null) ? 1 : v + 1);
                return 0;
            }
        }, executorService);
    }

    @Override
    public void saveHistoricalData(List<CandlestickEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            log.debug("没有数据需要保存");
            return;
        }

        try {
            // 使用现有的saveBatch方法保存数据
            saveBatch(entities);
            log.debug("成功保存 {} 条K线数据到数据库", entities.size());
        } catch (Exception e) {
            log.error("保存K线数据到数据库失败: {}", e.getMessage(), e);
            throw e;
        }
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
                adjustedEndTime = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0).minusWeeks(1);
                break;
            case "1D":
                // 日线: 排除当前日
                adjustedEndTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(1);
                break;
            case "12H":
                // 12小时线: 排除当前12小时周期 (0点或12点开始)
                int currentHour = now.getHour();
                int alignedHour = (currentHour >= 12) ? 12 : 0;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0).minusHours(12);
                break;
            case "6H":
                // 6小时线: 排除当前6小时周期 (0,6,12,18点开始)
                currentHour = now.getHour();
                alignedHour = (currentHour / 6) * 6;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0).minusHours(6);
                break;
            case "4H":
                // 4小时线: 排除当前4小时周期 (0,4,8,12,16,20点开始)
                currentHour = now.getHour();
                alignedHour = (currentHour / 4) * 4;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0).minusHours(4);
                break;
            case "2H":
                // 2小时线: 排除当前2小时周期
                currentHour = now.getHour();
                alignedHour = (currentHour / 2) * 2;
                adjustedEndTime = now.withHour(alignedHour).withMinute(0).withSecond(0).withNano(0).minusHours(2);
                break;
            case "1H":
                // 1小时线: 排除当前小时
                adjustedEndTime = now.withMinute(0).withSecond(0).withNano(0).minusHours(1);
                break;
            case "30M":
                // 30分钟线: 排除当前30分钟周期 (0或30分开始)
                int currentMinute = now.getMinute();
                int alignedMinute = (currentMinute >= 30) ? 30 : 0;
                adjustedEndTime = now.withMinute(alignedMinute).withSecond(0).withNano(0).minusMinutes(30);
                break;
            case "15M":
                // 15分钟线: 排除当前15分钟周期 (0,15,30,45分开始)
                currentMinute = now.getMinute();
                alignedMinute = (currentMinute / 15) * 15;
                adjustedEndTime = now.withMinute(alignedMinute).withSecond(0).withNano(0).minusMinutes(15);
                break;
            case "5M":
                // 5分钟线: 排除当前5分钟周期
                currentMinute = now.getMinute();
                alignedMinute = (currentMinute / 5) * 5;
                adjustedEndTime = now.withMinute(alignedMinute).withSecond(0).withNano(0).minusMinutes(5);
                break;
            case "1M":
                // 包含两种情况: 月线和1分钟线，通过上下文判断
                if (endTime.isAfter(now.minusDays(40))) {
                    // 如果结束时间是近期，可能是月线，排除当前月
                    LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                    if (endTime.isAfter(monthStart)) {
                        adjustedEndTime = monthStart.minusMonths(1);
                    } else {
                        // 1分钟线: 排除当前分钟
                        adjustedEndTime = now.withSecond(0).withNano(0).minusMinutes(1);
                    }
                } else {
                    // 1分钟线: 排除当前分钟
                    adjustedEndTime = now.withSecond(0).withNano(0).minusMinutes(1);
                }
                break;
            default:
                // 未知间隔，保守起见排除当前小时
                adjustedEndTime = now.withMinute(0).withSecond(0).withNano(0).minusHours(1);
                break;
        }

        // 返回调整后的时间与原始结束时间的较小值
        return endTime.isBefore(adjustedEndTime) ? endTime : adjustedEndTime;
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
        // 调用API获取数据 (将LocalDateTime转换为时间戳)
        ZoneId zoneId = ZoneId.systemDefault();
        long startTimestamp = startTime.atZone(zoneId).toInstant().toEpochMilli();
        long endTimestamp = endTime.atZone(zoneId).toInstant().toEpochMilli();
        if (startTimestamp == endTimestamp) {
            List<Candlestick> apiData = okxApiService.getHistoryKlineData(symbol, interval, startTimestamp, endTimestamp, batchSize);
            // 转换并保存数据到MySQL
            if (apiData != null && !apiData.isEmpty()) {
                // 转换并保存数据到MySQL
                List<CandlestickEntity> entities = convertAndSaveCandlesticks(apiData, symbol, interval);

                result.addAll(entities);
            }
        } else {
            while (currentStart.isBefore(endTime)) {
                try {
                    List<Candlestick> apiData = okxApiService.getHistoryKlineData(symbol, interval,
                            currentStart.atZone(zoneId).toEpochSecond() * 1000,
                            currentStart.plusMinutes(intervalMinutes * batchSize).atZone(zoneId).toEpochSecond() * 1000, batchSize);
                    if (apiData != null && !apiData.isEmpty()) {
                        // 转换并保存数据到MySQL
                        List<CandlestickEntity> entities = convertAndSaveCandlesticks(apiData, symbol, interval);
                        result.addAll(entities);
                    }
                    currentStart = currentStart.plusMinutes(intervalMinutes* batchSize);
                } catch (Exception e) {
                    log.error("  数据获取失败: {}", e.getMessage());
                }
            }
        }

        return result;
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
            saveHistoricalData(entities);
            log.info("    💾 已将 {} 条K线数据保存到MySQL", entities.size());
        } catch (Exception e) {
            log.error("    ❌ 保存K线数据到MySQL失败: {}", e.getMessage());
            // 即使保存失败也返回数据，避免影响接口响应
        }

        return entities;
    }

}

