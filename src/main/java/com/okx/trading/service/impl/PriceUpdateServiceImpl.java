//package com.okx.trading.service.impl;
//
//import com.okx.trading.service.OkxApiService;
//import com.okx.trading.service.PriceUpdateService;
//import com.okx.trading.service.RedisCacheService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import java.math.BigDecimal;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.CountDownLatch;
//
///**
// * 价格更新服务实现类
// * 在独立线程中更新缓存价格，不会因为主线程调试而暂停
// */
//@Slf4j
//@Service
//public class PriceUpdateServiceImpl implements PriceUpdateService {
//
//    private final OkxApiService okxApiService;
//    private final RedisCacheService redisCacheService;
//    private final ExecutorService executorService;
//
//    /**
//     * 价格更新线程
//     */
//    private Thread priceUpdateThread;
//
//    /**
//     * 控制线程运行的标志
//     */
//    private final AtomicBoolean running = new AtomicBoolean(false);
//
//    /**
//     * 价格更新间隔（毫秒）
//     */
//    private static final long UPDATE_INTERVAL = 5000; // 5秒更新一次
//
//    /**
//     * 构造函数，注入依赖
//     */
//    @Autowired
//    public PriceUpdateServiceImpl(OkxApiService okxApiService,
//                                 RedisCacheService redisCacheService,
//                                 @Qualifier("priceUpdateExecutorService") ExecutorService executorService) {
//        this.okxApiService = okxApiService;
//        this.redisCacheService = redisCacheService;
//        this.executorService = executorService;
//    }
//
//    /**
//     * 应用启动时自动启动价格更新线程
//     */
//    @PostConstruct
//    @Override
//    public void startPriceUpdateThread() {
////        if (running.get()) {
////            log.info("价格更新线程已在运行中");
////            return;
////        }
////
////        running.set(true);
////        priceUpdateThread = new Thread(this::runPriceUpdateLoop, "price-update-thread");
////        priceUpdateThread.setDaemon(true); // 设置为守护线程，随主线程退出而退出
////        priceUpdateThread.start();
////
////        log.info("价格更新线程已启动");
//    }
//
//    /**
//     * 应用关闭时停止价格更新线程
//     */
//    @PreDestroy
//    @Override
//    public void stopPriceUpdateThread() {
//        if (!running.get()) {
//            log.info("价格更新线程未运行");
//            return;
//        }
//
//        running.set(false);
//        if (priceUpdateThread != null && priceUpdateThread.isAlive()) {
//            try {
//                priceUpdateThread.join(5000); // 等待线程结束，最多等待5秒
//            } catch (InterruptedException e) {
//                log.warn("等待价格更新线程结束时被中断", e);
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        // 关闭线程池
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            log.warn("等待线程池关闭时被中断", e);
//            Thread.currentThread().interrupt();
//        }
//
//        log.info("价格更新线程已停止");
//    }
//
//    /**
//     * 价格更新循环
//     * 定期获取所有订阅币种的最新价格并更新到Redis缓存
//     */
//    private void runPriceUpdateLoop() {
//        log.info("价格更新线程开始运行");
//
//        // 记录上次价格更新时间，用于检测价格更新中断
//        Map<String, Long> lastPriceUpdateTime = new ConcurrentHashMap<>();
//
//        while (running.get()) {
//            try {
//                // 获取所有订阅的币种
//                Set<String> subscribedCoins = redisCacheService.getSubscribedCoins();
//
//                if (subscribedCoins.isEmpty()) {
//                    log.debug("没有订阅的币种，等待下次更新");
//                    Thread.sleep(UPDATE_INTERVAL);
//                    continue;
//                }
//
//                log.debug("开始更新 {} 个币种的价格", subscribedCoins.size());
//
//                // 获取当前时间戳，用于检测价格更新
//                final long currentTime = System.currentTimeMillis();
//
//                // 为每个币种创建一个更新任务
//                for (String symbol : subscribedCoins) {
//                    final String finalSymbol = symbol;
//                    executorService.submit(() -> {
//                        try {
//                            // 获取当前Redis中的价格
//                            BigDecimal currentPrice = redisCacheService.getCoinPrice(finalSymbol);
//
//                            // 检查是否需要强制更新价格 - 如果长时间未更新或Redis中没有价格数据
//                            boolean forceUpdate = false;
//                            if (!lastPriceUpdateTime.containsKey(finalSymbol)) {
//                                // 第一次更新，记录时间
//                                forceUpdate = true;
//                            } else if (currentTime - lastPriceUpdateTime.get(finalSymbol) > UPDATE_INTERVAL * 3) {
//                                // 超过正常更新间隔的3倍，可能出现问题
//                                log.warn("币种 {} 价格长时间未更新，强制更新", finalSymbol);
//                                forceUpdate = true;
//                            }
//
//                            // Redis中没有价格，强制更新
//                            if (currentPrice == null) {
//                                log.warn("Redis中没有币种 {} 的价格数据，强制更新", finalSymbol);
//                                forceUpdate = true;
//                            }
//
//                            // 获取行情数据（会自动写入Redis缓存）
//                            if (forceUpdate) {
//                                // 强制触发WebSocket重新订阅更新
//                                okxApiService.unsubscribeTicker(finalSymbol);
//                                Thread.sleep(100); // 短暂等待取消订阅完成
//                                okxApiService.getTicker(finalSymbol);
//                            } else {
//                                // 正常更新
//                                okxApiService.getTicker(finalSymbol);
//                            }
//
//                            // 记录更新时间
//                            lastPriceUpdateTime.put(finalSymbol, currentTime);
//                            log.debug("已更新币种 {} 的价格", finalSymbol);
//                        } catch (Exception e) {
//                            log.error("更新币种 {} 价格失败: {}", finalSymbol, e.getMessage());
//                        }
//                    });
//                }
//
//                // 等待一段时间后再次更新
//                Thread.sleep(UPDATE_INTERVAL);
//            } catch (InterruptedException e) {
//                log.warn("价格更新线程被中断", e);
//                Thread.currentThread().interrupt();
//                break;
//            } catch (Exception e) {
//                log.error("价格更新过程中发生错误: {}", e.getMessage(), e);
//                try {
//                    // 发生错误时等待一段时间再继续
//                    Thread.sleep(UPDATE_INTERVAL);
//                } catch (InterruptedException ie) {
//                    log.warn("价格更新线程被中断", ie);
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//
//        log.info("价格更新线程已退出");
//    }
//
//    /**
//     * 强制更新所有订阅币种的价格
//     * 用于WebSocket连接重建后的价格恢复
//     */
//    @Override
//    public void forceUpdateAllPrices() {
//        try {
//            // 获取所有订阅的币种
//            Set<String> subscribedCoins = redisCacheService.getSubscribedCoins();
//
//            if (subscribedCoins.isEmpty()) {
//                log.info("没有订阅的币种，无需强制更新价格");
//                return;
//            }
//
//            log.info("开始强制更新 {} 个币种的价格", subscribedCoins.size());
//
//            // 创建计数器和闭锁，以便等待所有更新完成
//            CountDownLatch latch = new CountDownLatch(subscribedCoins.size());
//            AtomicInteger successCount = new AtomicInteger(0);
//
//            // 为每个币种创建一个更新任务
//            for (String symbol : subscribedCoins) {
//                final String finalSymbol = symbol;
//                executorService.submit(() -> {
//                    try {
//                        // 先取消订阅，再重新订阅以获取最新数据
//                        okxApiService.unsubscribeTicker(finalSymbol);
//                        Thread.sleep(100); // 短暂等待取消订阅完成
//                        okxApiService.getTicker(finalSymbol);
//
//                        successCount.incrementAndGet();
//                        log.debug("强制更新币种 {} 价格成功", finalSymbol);
//                    } catch (Exception e) {
//                        log.error("强制更新币种 {} 价格失败: {}", finalSymbol, e.getMessage());
//                    } finally {
//                        latch.countDown();
//                    }
//                });
//            }
//
//            // 等待所有更新完成，最多等待30秒
//            boolean completed = latch.await(30, TimeUnit.SECONDS);
//
//            if (completed) {
////                log.info("所有币种价格强制更新完成，成功: {}/{}", successCount.get(), subscribedCoins.size());
//            } else {
////                log.warn("币种价格强制更新超时，已完成: {}/{}", latch.getCount(), subscribedCoins.size());
//            }
//        } catch (Exception e) {
//            log.error("强制更新价格过程中发生错误: {}", e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public Set<String> getSubscribedCoins() {
//        return redisCacheService.getSubscribedCoins();
//    }
//
//    @Override
//    public boolean addSubscribedCoin(String symbol) {
//        return redisCacheService.addSubscribedCoin(symbol);
//    }
//
//    @Override
//    public boolean removeSubscribedCoin(String symbol) {
//        return redisCacheService.removeSubscribedCoin(symbol);
//    }
//}
