package com.okx.trading.service.impl;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.service.KlineCacheService;
import com.okx.trading.service.KlineUpdateService;
import com.okx.trading.service.OkxApiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * K线数据更新服务实现类
 * 负责定期从交易所API获取K线数据并更新缓存
 */
@Service
@RequiredArgsConstructor
public class KlineUpdateServiceImpl implements KlineUpdateService {

    private static final Logger log = LoggerFactory.getLogger(KlineUpdateServiceImpl.class);

    private final KlineCacheService klineCacheService;
    private final OkxApiService okxApiService;

    // 所有需要更新的K线订阅
    private final Map<String, Set<String>> klineSubscriptions = new ConcurrentHashMap<>();

    // 更新线程是否在运行
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 调度器
    private @Qualifier("klineUpdateScheduler") ScheduledExecutorService scheduler;
//    private ScheduledExecutorService scheduler;

    // 默认更新频率为30秒
    @Value("${okx.kline.update-interval-seconds:30}")
    private int updateIntervalSeconds;

    /**
     * 初始化方法，加载已有订阅
     */
    @PostConstruct
    public void init() {
        try {
            log.info("初始化K线数据更新服务...");

            // 加载已有的K线订阅
            loadExistingSubscriptions();

            log.info("K线数据更新服务初始化完成");
        } catch (Exception e) {
            log.error("初始化K线数据更新服务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 销毁方法，关闭调度器
     */
    @PreDestroy
    public void destroy() {
        stopUpdateThread();
    }

    /**
     * 加载已有的K线订阅
     */
    private void loadExistingSubscriptions() {
        try {
            Map<String, List<String>> subscribedKlines = klineCacheService.getAllSubscribedKlines();

            // 清空当前订阅
            klineSubscriptions.clear();

            // 添加已有订阅
            subscribedKlines.forEach((symbol, intervals) -> {
                Set<String> intervalSet = klineSubscriptions.computeIfAbsent(symbol, k -> new HashSet<>());
                intervalSet.addAll(intervals);
            });

            log.info("已加载 {} 个交易对的K线订阅", klineSubscriptions.size());
        } catch (Exception e) {
            log.error("加载已有K线订阅失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void startUpdateThread() {
        if (running.compareAndSet(false, true)) {
            log.info("启动K线数据更新线程，更新频率：{}秒", updateIntervalSeconds);

            // 创建调度器
//            scheduler = Executors.newSingleThreadScheduledExecutor();

            // 立即执行一次，然后按照设定的频率定期执行
            scheduler.scheduleAtFixedRate(
                this::updateKlineData,
                0,
                updateIntervalSeconds,
                TimeUnit.SECONDS);
        } else {
            log.info("K线数据更新线程已在运行中");
        }
    }

    @Override
    public void stopUpdateThread() {
        if (running.compareAndSet(true, false)) {
            log.info("停止K线数据更新线程");

            if (scheduler != null && !scheduler.isShutdown()) {
                try {
                    scheduler.shutdown();
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            log.info("K线数据更新线程已停止");
        }
    }

    @Override
    public void updateKlineData() {
        if (klineSubscriptions.isEmpty()) {
            log.debug("没有K线订阅，跳过更新");
            return;
        }

        log.debug("开始更新K线数据，共 {} 个交易对", klineSubscriptions.size());

        // 保存成功更新的计数
        int successCount = 0;
        int totalCount = 0;

        try {
            // 遍历所有订阅
            for (Map.Entry<String, Set<String>> entry : klineSubscriptions.entrySet()) {
                String symbol = entry.getKey();
                Set<String> intervals = entry.getValue();

                for (String interval : intervals) {
                    totalCount++;

                    try {
                        // 从交易所API获取K线数据
                        List<Candlestick> klineData = okxApiService.getKlineData(symbol, interval, 10);

                        // 更新到缓存
                        int cached = klineCacheService.batchCacheKlineData(klineData);

                        if (cached > 0) {
                            successCount++;
                            log.debug("成功更新交易对 {} 的 {} 间隔K线数据，共 {} 条", symbol, interval, cached);
                        }
                    } catch (Exception e) {
                        log.error("更新交易对 {} 的 {} 间隔K线数据失败: {}", symbol, interval, e.getMessage());
                    }
                }
            }

            log.debug("K线数据更新完成，成功：{}/{}，耗时：{}ms",
                     successCount, totalCount, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("K线数据更新过程中发生错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleKlineSubscription(String symbol, String interval, boolean isSubscribe) {
        try {
            if (isSubscribe) {
                // 添加订阅
                Set<String> intervals = klineSubscriptions.computeIfAbsent(symbol, k -> new HashSet<>());
                if (intervals.add(interval)) {
                    log.info("添加K线订阅：{} {}", symbol, interval);

                    // 立即获取一次K线数据
                    fetchAndCacheKlineData(symbol, interval);
                }
            } else {
                // 取消订阅
                Set<String> intervals = klineSubscriptions.get(symbol);
                if (intervals != null && intervals.remove(interval)) {
                    log.info("移除K线订阅：{} {}", symbol, interval);

                    // 如果该交易对没有其他间隔的订阅，则移除整个交易对
                    if (intervals.isEmpty()) {
                        klineSubscriptions.remove(symbol);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理K线订阅变更失败: {} {}, isSubscribe={}, 错误: {}",
                     symbol, interval, isSubscribe, e.getMessage(), e);
        }
    }

    /**
     * 获取并缓存K线数据
     */
    private void fetchAndCacheKlineData(String symbol, String interval) {
        try {
            // 从交易所API获取最新的K线数据
            List<Candlestick> klineData = okxApiService.getKlineData(symbol, interval, 100);

            // 更新到缓存
            int cached = klineCacheService.batchCacheKlineData(klineData);

            log.info("初始加载交易对 {} 的 {} 间隔K线数据，共 {} 条", symbol, interval, cached);
        } catch (Exception e) {
            log.error("初始加载交易对 {} 的 {} 间隔K线数据失败: {}", symbol, interval, e.getMessage(), e);
        }
    }

    @Override
    public int getUpdateInterval() {
        return updateIntervalSeconds;
    }

    @Override
    public void setUpdateInterval(int seconds) {
        if (seconds < 5) {
            log.warn("更新频率不能小于5秒，设置为5秒");
            seconds = 5;
        }

        this.updateIntervalSeconds = seconds;

        // 如果线程正在运行，则重启线程以应用新的频率
        if (running.get()) {
            stopUpdateThread();
            startUpdateThread();
        }

        log.info("K线数据更新频率已设置为 {} 秒", seconds);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
