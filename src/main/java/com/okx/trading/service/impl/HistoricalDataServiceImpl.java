package com.okx.trading.service.impl;

import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.repository.CandlestickRepository;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.OkxApiService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 历史K线数据服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private final OkxApiService okxApiService;
    private final CandlestickRepository candlestickRepository;

    @Value("${okx.historical-data.batch-size:100}")
    private int batchSize = 100;

    @Value("${okx.historical-data.max-threads:10}")
    private int maxThreads = 10;

    // 用于执行多线程查询的线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

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

    @Override
    public CompletableFuture<Integer> fetchAndSaveHistoricalData(String symbol, String interval,
                                                         LocalDateTime startTime, LocalDateTime endTime) {
        log.info("开始获取历史K线数据: symbol={}, interval={}, startTime={}, endTime={}",
                symbol, interval, startTime, endTime);

        // 计算预期的数据点总数（根据时间间隔）
        int expectedTotal = calculateExpectedDataPoints(interval, startTime, endTime);
        log.info("预计需要获取的数据点数量: {}", expectedTotal);

        // 计算需要分成的批次数
        int requiredBatches = (int) Math.ceil((double) expectedTotal / batchSize);
        log.info("将分为{}个批次获取, 每批次{}条数据", requiredBatches, batchSize);

        // 创建时间分片
        List<TimeSlice> timeSlices = createTimeSlices(interval, startTime, endTime, requiredBatches);

        // 限制并发线程数
        int threadCount = Math.min(maxThreads, requiredBatches);
        log.info("将使用{}个线程并行获取数据", threadCount);

        // 创建多线程任务列表
        List<CompletableFuture<List<CandlestickEntity>>> futures = new ArrayList<>();

        for (TimeSlice slice : timeSlices) {
            CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("获取时间片段数据: {}", slice);
                    List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                            symbol, interval, toEpochMilli(slice.getStart()), toEpochMilli(slice.getEnd()), batchSize);

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

            futures.add(future);
        }

        // 合并所有异步任务的结果
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> {
                    int totalSaved = futures.stream()
                            .map(CompletableFuture::join)
                            .mapToInt(List::size)
                            .sum();

                    log.info("完成历史数据获取, 共保存{}条数据", totalSaved);

                    // 检查数据完整性
                    List<LocalDateTime> missingTimes = checkDataIntegrity(symbol, interval, startTime, endTime);

                    if (!missingTimes.isEmpty()) {
                        log.info("发现{}个缺失的数据点, 开始补充获取", missingTimes.size());

                        // 补充获取缺失的数据
                        try {
                            int filledCount = fillMissingData(symbol, interval, missingTimes).get();
                            log.info("成功补充{}个缺失的数据点", filledCount);
                            return totalSaved + filledCount;
                        } catch (Exception e) {
                            log.error("补充缺失数据失败: {}", e.getMessage(), e);
                            return totalSaved;
                        }
                    }

                    return totalSaved;
                });
    }

    @Override
    public List<CandlestickEntity> getHistoricalData(String symbol, String interval,
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        return candlestickRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                symbol, interval, startTime, endTime);
    }

    @Override
    public List<LocalDateTime> checkDataIntegrity(String symbol, String interval,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        log.info("检查数据完整性: symbol={}, interval={}, startTime={}, endTime={}",
                symbol, interval, startTime, endTime);

        // 获取预期的所有时间点
        List<LocalDateTime> expectedTimes = generateExpectedTimePoints(interval, startTime, endTime);
        log.info("预期数据点数量: {}", expectedTimes.size());

        // 获取数据库中已存在的时间点
        List<LocalDateTime> existingTimes = candlestickRepository
                .findExistingOpenTimesBySymbolAndIntervalBetween(symbol, interval, startTime, endTime);
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
     * 计算时间间隔内预期的数据点数量
     */
    private int calculateExpectedDataPoints(String interval, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = getIntervalMinutes(interval);
        return (int) (ChronoUnit.MINUTES.between(startTime, endTime) / minutes) + 1;
    }

    /**
     * 创建时间分片
     */
    private List<TimeSlice> createTimeSlices(String interval, LocalDateTime startTime, LocalDateTime endTime, int batchCount) {
        List<TimeSlice> slices = new ArrayList<>();

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
     * 获取间隔对应的分钟数
     */
    private long getIntervalMinutes(String interval) {
        String unit = interval.substring(interval.length() - 1);
        int amount = Integer.parseInt(interval.substring(0, interval.length() - 1));

        switch (unit) {
            case "m": return amount;
            case "H": return amount * 60;
            case "D": return amount * 60 * 24;
            case "W": return amount * 60 * 24 * 7;
            case "M": return amount * 60 * 24 * 30; // 简化处理，按30天/月计算
            default: return 1;
        }
    }

    /**
     * 生成预期的时间点列表
     */
    private List<LocalDateTime> generateExpectedTimePoints(String interval, LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDateTime> timePoints = new ArrayList<>();
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
                    entity.setId(CandlestickEntity.createId(symbol, intervalVal, c.getOpenTime()));
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
     */
    @Transactional
    protected List<CandlestickEntity> saveBatch(List<CandlestickEntity> entities) {
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
                // 删除可能存在的重复数据
                log.info("删除时间范围 {} ~ {} 内的数据，以避免重复", minTime, maxTime);
                candlestickRepository.deleteBySymbolAndIntervalAndOpenTimeBetween(symbol, interval, minTime, maxTime);
            }

            // 保存新数据
            log.info("保存 {} 条新数据", entities.size());
            return candlestickRepository.saveAll(entities);
        } catch (Exception e) {
            log.error("保存批量数据时出错: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据交易对和时间间隔查询最新的K线数据
     *
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param limit 数量限制
     * @return K线数据列表
     */
    public List<CandlestickEntity> getLatestHistoricalData(String symbol, String interval, int limit) {
        return candlestickRepository.findLatestBySymbolAndInterval(
                symbol, interval, PageRequest.of(0, limit));
    }
}
