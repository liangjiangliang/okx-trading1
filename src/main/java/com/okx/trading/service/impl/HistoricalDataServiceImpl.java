package com.okx.trading.service.impl;

import com.okx.trading.model.TimeSlice;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 历史K线数据服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataServiceImpl implements HistoricalDataService{

    private final OkxApiService okxApiService;
    private final CandlestickRepository candlestickRepository;

    @Value("${okx.historical-data.batch-size:100}")
    private int batchSize = 100;

    @Value("${okx.historical-data.max-threads:10}")
    private int maxThreads = 10;

    // 用于执行多线程查询的线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 时间分片类
     */
    @Data
    @AllArgsConstructor
    private static class TimeSlice{
        private LocalDateTime start;
        private LocalDateTime end;

        @Override
        public String toString(){
            return String.format("[%s - %s]", start, end);
        }
    }

    @Override
    public CompletableFuture<Integer> fetchAndSaveHistoricalData(String symbol, String interval,
                                                                 LocalDateTime startTime, LocalDateTime endTime){
        log.info("开始获取历史K线数据: symbol={}, interval={}, startTime={}, endTime={}",
            symbol, interval, startTime, endTime);

        // 按天检查数据完整性，找出需要获取的天数
        List<TimeSlice> daysToFetch = getIncompleteDays(symbol, interval, startTime, endTime);

        if(daysToFetch.isEmpty()){
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
                    int requiredBatches = (int)Math.ceil((double)expectedDayTotal / batchSize);
                    log.info("将分为{}个批次获取, 每批次{}条数据", requiredBatches, batchSize);

                    // 创建当天的时间分片
                    List<TimeSlice> timeSlices = createTimeSlices(interval, dayStart, dayEnd, requiredBatches);

                    // 创建多线程任务列表
                    List<CompletableFuture<List<CandlestickEntity>>> batchFutures = new ArrayList<>();

                    for(TimeSlice slice: timeSlices){
                        CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                            try{
                                log.debug("获取时间片段数据: {}", slice);
                                List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(
                                    symbol, interval, toEpochMilli(slice.getStart()), toEpochMilli(slice.getEnd()), batchSize);

                                // 转换为实体类
                                List<CandlestickEntity> entities = convertToEntities(candlesticks, symbol, interval);
                                log.debug("时间片段{}获取到{}条数据", slice, entities.size());

                                // 保存数据
                                return saveBatch(entities);
                            }catch(Exception e){
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
                        List<LocalDateTime> missingTimes = checkDataIntegrity(symbol, interval, dayStart, dayEnd);

                        if (!missingTimes.isEmpty()) {
                            log.info("日期 {} 有 {} 个缺失的数据点，尝试单点填充", dayStart.toLocalDate(), missingTimes.size());
                            int filledCount = fillMissingData(symbol, interval, missingTimes).get();
                            totalSaved += filledCount;
                        }
                    }

                    return totalSaved;
                } catch (Exception e) {
                    log.error("获取日期 {} 的数据失败: {}", daySlice.getStart().toLocalDate(), e.getMessage(), e);
                    return 0;
                }
            }, executorService);

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
    private List<TimeSlice> getIncompleteDays(String symbol, String interval, LocalDateTime startTime, LocalDateTime endTime){
        List<TimeSlice> incompleteDays = new ArrayList<>();

        // 获取时间范围内的所有天数
        LocalDateTime currentDay = startTime.toLocalDate().atStartOfDay();
        LocalDateTime lastDay = endTime.toLocalDate().atStartOfDay().minusSeconds(1);

        while(! currentDay.isAfter(lastDay)){
            // 计算当天结束时间（次日0点）-1 秒
            LocalDateTime nextDay = currentDay.plusDays(1);
            if(nextDay.isAfter(endTime)){
                nextDay = endTime;
            }

            // 检查当天数据是否完整
            if(! isDayDataComplete(symbol, interval, currentDay, nextDay)){
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
    private boolean isDayDataComplete(String symbol, String interval, LocalDateTime dayStart, LocalDateTime dayEnd){
        // 获取预期的所有时间点
        List<LocalDateTime> expectedTimes = generateExpectedTimePoints(interval, dayStart, dayEnd);

        // 获取数据库中已存在的时间点
        List<LocalDateTime> existingTimes = candlestickRepository
            .findExistingOpenTimesBySymbolAndIntervalBetween(symbol, interval, dayStart, dayEnd);

        // 如果预期时间点数量与已存在时间点数量相同，则认为数据完整
        Set<String> expectedStr = expectedTimes.stream().map(LocalDateTime :: toString).collect(Collectors.toSet());
        Set<String> existingStr = existingTimes.stream().map(LocalDateTime :: toString).collect(Collectors.toSet());
        expectedStr.removeAll(existingStr);

        if(expectedStr.isEmpty()){
            // 进一步检查是否所有预期时间点都存在
            Set<LocalDateTime> existingTimeSet = new HashSet<>(existingTimes);
            boolean isComplete = expectedTimes.stream().allMatch(existingTimeSet :: contains);

            if(isComplete){
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
                                                     LocalDateTime startTime, LocalDateTime endTime){
        return candlestickRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            symbol, interval, startTime, endTime);
    }

    @Override
    public List<LocalDateTime> checkDataIntegrity(String symbol, String interval,
                                                  LocalDateTime startTime, LocalDateTime endTime){
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
            .filter(time -> ! existingTimeSet.contains(time))
            .collect(Collectors.toList());

        log.info("缺失的数据点数量: {}", missingTimes.size());
        return missingTimes;
    }

    @Override
    public CompletableFuture<Integer> fillMissingData(String symbol, String interval, List<LocalDateTime> missingTimes){
        if(missingTimes.isEmpty()){
            return CompletableFuture.completedFuture(0);
        }

        log.info("开始补充缺失数据: symbol={}, interval={}, 缺失点数量={}",
            symbol, interval, missingTimes.size());

        // 将缺失的时间点按批次分组
        List<List<LocalDateTime>> batches = new ArrayList<>();
        for(int i = 0;i < missingTimes.size();i += batchSize){
            batches.add(missingTimes.subList(i, Math.min(i + batchSize, missingTimes.size())));
        }

        log.info("缺失数据分为{}个批次获取", batches.size());

        // 为每个批次创建一个异步任务
        List<CompletableFuture<List<CandlestickEntity>>> futures = new ArrayList<>();

        for(List<LocalDateTime> batch: batches){
            // 为每个批次找到时间范围
            LocalDateTime batchStart = batch.stream().min(LocalDateTime :: compareTo).orElse(null);
            LocalDateTime batchEnd = batch.stream().max(LocalDateTime :: compareTo).orElse(null);

            if(batchStart == null || batchEnd == null){
                continue;
            }

            // 获取时间范围内的所有数据
            CompletableFuture<List<CandlestickEntity>> future = CompletableFuture.supplyAsync(() -> {
                try{
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
                }catch(Exception e){
                    log.error("补充缺失数据失败: {}", e.getMessage(), e);
                    return Collections.emptyList();
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有异步任务完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture :: join)
                .mapToInt(List :: size)
                .sum());
    }

    /**
     * 计算时间间隔内预期的数据点数量
     */
    private int calculateExpectedDataPoints(String interval, LocalDateTime startTime, LocalDateTime endTime){
        long minutes = getIntervalMinutes(interval);
        return (int)(ChronoUnit.MINUTES.between(startTime, endTime.minusSeconds(1)) / minutes) + 1;
    }

    /**
     * 创建时间分片
     */
    private List<TimeSlice> createTimeSlices(String interval, LocalDateTime startTime, LocalDateTime endTime, int batchCount){
        List<TimeSlice> slices = new ArrayList<>();

        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        long minutesPerBatch = totalMinutes / batchCount;
        long intervalMinutes = getIntervalMinutes(interval);

        // 确保分片大小是间隔的整数倍
        minutesPerBatch = ((minutesPerBatch / intervalMinutes) + 1) * intervalMinutes;

        LocalDateTime current = startTime;
        while(current.isBefore(endTime)){
            LocalDateTime sliceEnd = current.plusMinutes(minutesPerBatch);
            if(sliceEnd.isAfter(endTime)){
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
    private long getIntervalMinutes(String interval){
        String unit = interval.substring(interval.length() - 1);
        int amount = Integer.parseInt(interval.substring(0, interval.length() - 1));

        switch(unit){
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
    private List<LocalDateTime> generateExpectedTimePoints(String interval, LocalDateTime startTime, LocalDateTime endTime){
        List<LocalDateTime> timePoints = new ArrayList<>();
        endTime = endTime.minusSeconds(1);

        long intervalMinutes = getIntervalMinutes(interval);

        LocalDateTime current = startTime;
        while(! current.isAfter(endTime)){
            timePoints.add(current);
            current = current.plusMinutes(intervalMinutes);
        }

        return timePoints;
    }

    /**
     * 将LocalDateTime转换为毫秒时间戳
     */
    private Long toEpochMilli(LocalDateTime time){
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 将Candlestick转换为CandlestickEntity
     */
    private List<CandlestickEntity> convertToEntities(List<Candlestick> candlesticks, String symbol, String intervalVal){
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
     * 如果数据已存在则跳过，不删除已有数据
     */
    @Transactional
    public List<CandlestickEntity> saveBatch(List<CandlestickEntity> entities){
        if(entities.isEmpty()){
            return Collections.emptyList();
        }

        try{
            // 获取第一个和最后一个实体的时间范围
            String symbol = entities.get(0).getSymbol();
            String interval = entities.get(0).getIntervalVal();

            LocalDateTime minTime = entities.stream()
                .map(CandlestickEntity :: getOpenTime)
                .min(LocalDateTime :: compareTo)
                .orElse(null);

            LocalDateTime maxTime = entities.stream()
                .map(CandlestickEntity :: getOpenTime)
                .max(LocalDateTime :: compareTo)
                .orElse(null);

            if(minTime != null && maxTime != null){
                // 查询已存在的数据
                List<CandlestickEntity> existingEntities = candlestickRepository
                    .findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(symbol, interval, minTime, maxTime);

                // 创建已存在数据的ID集合，用于过滤
                Set<String> existingIds = existingEntities.stream()
                    .map(CandlestickEntity :: getId)
                    .collect(Collectors.toSet());

                // 过滤出不存在的数据
                List<CandlestickEntity> newEntities = entities.stream()
                    .filter(entity -> ! existingIds.contains(entity.getId()))
                    .collect(Collectors.toList());

                log.info("时间范围 {} ~ {} 内已有 {} 条数据，新增 {} 条数据",
                    minTime, maxTime, existingEntities.size(), newEntities.size());

                // 只保存新数据
                if(! newEntities.isEmpty()){
                    return candlestickRepository.saveAll(newEntities);
                }else{
                    return Collections.emptyList();
                }
            }

            // 如果没有时间范围信息，直接保存所有数据
            return candlestickRepository.saveAll(entities);
        }catch(Exception e){
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
    public List<CandlestickEntity> getLatestHistoricalData(String symbol, String interval, int limit){
        return candlestickRepository.findLatestBySymbolAndInterval(
            symbol, interval, PageRequest.of(0, limit));
    }

    /**
     * 将缺失的时间点按天分组
     *
     * @param missingTimes 缺失的时间点列表
     * @return 按天分组的缺失时间点Map，key为每天的0点时间
     */
    private Map<LocalDateTime,List<LocalDateTime>> groupMissingTimesByDay(List<LocalDateTime> missingTimes){
        Map<LocalDateTime,List<LocalDateTime>> result = new HashMap<>();

        for(LocalDateTime time: missingTimes){
            // 获取当天的0点时间作为key
            LocalDateTime dayStart = time.toLocalDate().atStartOfDay();

            if(! result.containsKey(dayStart)){
                result.put(dayStart, new ArrayList<>());
            }

            result.get(dayStart).add(time);
        }

        return result;
    }
}

