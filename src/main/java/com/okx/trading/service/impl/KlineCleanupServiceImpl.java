//package com.okx.trading.service.impl;
//
//import com.okx.trading.service.KlineCacheService;
//import com.okx.trading.service.KlineCleanupService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * K线数据清理服务实现类
// * 负责定期清理Redis中的K线数据，防止内存占用过大
// *
// * @author system
// * @since 2023-11-06
// */
//@Service
//@Slf4j
//public class KlineCleanupServiceImpl implements KlineCleanupService {
//
//    /**
//     * Redis中K线数据的key前缀
//     */
//    private static final String KLINE_REDIS_PREFIX = "coin-rt-kline:";
//
//    /**
//     * 清理线程调度器
//     */
//    private ScheduledExecutorService scheduler;
//
//    /**
//     * 清理任务运行状态标志
//     */
//    private final AtomicBoolean running = new AtomicBoolean(false);
//
//    /**
//     * 每个K线键保留的最大数据条数，默认300条
//     */
//    @Value("${okx.trading.kline.max-count:300}")
//    private int maxKlineCount;
//
//    /**
//     * 清理时间间隔（单位：秒），默认60秒
//     */
//    @Value("${okx.trading.kline.cleanup-interval:300}")
//    private int cleanupInterval;
//
//    /**
//     * 最小保留数据条数
//     */
//    private static final int MIN_KLINE_COUNT = 100;
//
//    /**
//     * 最小清理间隔（秒）
//     */
//    private static final int MIN_CLEANUP_INTERVAL = 10;
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private KlineCacheService klineCacheService;
//
//    /**
//     * 初始化清理任务
//     * 该方法在Spring容器启动后自动调用
//     */
//    @PostConstruct
//    @Override
//    public void initCleanupTask() {
//        startCleanupThread();
//    }
//
//    /**
//     * 销毁清理任务
//     * 该方法在Spring容器关闭前自动调用
//     */
//    @PreDestroy
//    @Override
//    public void destroyCleanupTask() {
//        stopCleanupThread();
//    }
//
//    @Override
//    public void startCleanupThread() {
//        if (running.compareAndSet(false, true)) {
//            log.info("启动K线数据清理线程，清理间隔：{}秒，保留最新数据条数：{}", cleanupInterval, maxKlineCount);
//            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
//                Thread thread = new Thread(r, "kline-cleanup-thread");
//                thread.setDaemon(true);
//                return thread;
//            });
//            scheduler.scheduleWithFixedDelay(this::cleanupKlineData, 10, cleanupInterval, TimeUnit.SECONDS);
//        }
//    }
//
//    @Override
//    public void stopCleanupThread() {
//        if (running.compareAndSet(true, false)) {
//            log.info("停止K线数据清理线程");
//            if (scheduler != null) {
//                scheduler.shutdown();
//                try {
//                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
//                        scheduler.shutdownNow();
//                    }
//                } catch (InterruptedException e) {
//                    scheduler.shutdownNow();
//                    Thread.currentThread().interrupt();
//                } finally {
//                    scheduler = null;
//                }
//            }
//        }
//    }
//
//    @Override
//    public void cleanupKlineData() {
//        try {
//            log.info("开始执行K线数据清理任务");
//
//            // 获取所有已订阅的交易对和时间间隔
//            Map<String,List<String>> subscribedSymbols = klineCacheService.getAllSubscribedKlines();
//
//            if (subscribedSymbols.isEmpty()) {
//                log.info("没有已订阅的K线数据，无需清理");
//                return;
//            }
//
//            AtomicInteger totalRemoved = new AtomicInteger(0);
//
//            // 遍历所有订阅项，执行清理
//            subscribedSymbols.forEach((symbol, intervals) -> {
//                intervals.forEach(interval -> {
//                    int removed = cleanupKlineData(symbol, interval);
//                    totalRemoved.addAndGet(removed);
//                });
//            });
//
//            log.info("K线数据清理任务完成，共清理{}条数据", totalRemoved.get());
//        } catch (Exception e) {
//            log.error("K线数据清理任务执行异常", e);
//        }
//    }
//
//    @Override
//    public int cleanupKlineData(String symbol, String interval) {
//        if (symbol == null || interval == null) {
//            log.warn("清理K线数据失败：交易对或时间间隔为空");
//            return 0;
//        }
//
//        String key = KLINE_REDIS_PREFIX + symbol + ":" + interval;
//        int removed = 0;
//
//        try {
//            // 获取该key的所有成员数量
//            Long size = redisTemplate.opsForZSet().size(key);
//
//            if (size == null || size <= maxKlineCount) {
//                return 0;
//            }
//
//            // 计算需要删除的数量
//            long toRemove = size - maxKlineCount;
//
//            // 获取需要删除的元素（按分数排序，保留最新的数据）
//            // 注意：这里假设分数是时间戳，值越大表示数据越新
//            // 我们需要删除分数较小的元素（较旧的数据）
//            Set<Object> elementsToRemove = redisTemplate.opsForZSet().range(key, 0, toRemove - 1);
//
//            if (elementsToRemove != null && !elementsToRemove.isEmpty()) {
//                removed = elementsToRemove.size();
//                redisTemplate.opsForZSet().remove(key, elementsToRemove.toArray());
//                log.debug("已清理K线数据：{} {} 共{}条", symbol, interval, removed);
//            }
//        } catch (Exception e) {
//            log.error("清理K线数据异常：{} {}", symbol, interval, e);
//        }
//
//        return removed;
//    }
//
//    @Override
//    public void setCleanupInterval(int intervalSeconds) {
//        if (intervalSeconds < MIN_CLEANUP_INTERVAL) {
//            log.warn("设置的清理间隔{}秒小于最小值{}秒，将使用最小值", intervalSeconds, MIN_CLEANUP_INTERVAL);
//            intervalSeconds = MIN_CLEANUP_INTERVAL;
//        }
//
//        if (this.cleanupInterval != intervalSeconds) {
//            this.cleanupInterval = intervalSeconds;
//
//            // 如果清理线程正在运行，重启以应用新的间隔
//            if (running.get()) {
//                stopCleanupThread();
//                startCleanupThread();
//            }
//
//            log.info("K线数据清理间隔已更新为{}秒", cleanupInterval);
//        }
//    }
//
//    @Override
//    public void setMaxKlineCount(int maxCount) {
//        if (maxCount < MIN_KLINE_COUNT) {
//            log.warn("设置的最大数据条数{}小于最小值{}，将使用最小值", maxCount, MIN_KLINE_COUNT);
//            maxCount = MIN_KLINE_COUNT;
//        }
//
//        if (this.maxKlineCount != maxCount) {
//            this.maxKlineCount = maxCount;
//            log.info("K线数据保留最大条数已更新为{}", maxKlineCount);
//        }
//    }
//
//    @Override
//    public int getCleanupInterval() {
//        return cleanupInterval;
//    }
//
//    @Override
//    public int getMaxKlineCount() {
//        return maxKlineCount;
//    }
//}
