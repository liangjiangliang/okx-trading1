package com.okx.trading.service.impl;

import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.PriceUpdateService;
import com.okx.trading.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 价格更新服务实现类
 * 在独立线程中更新缓存价格，不会因为主线程调试而暂停
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceUpdateServiceImpl implements PriceUpdateService {

    private final OkxApiService okxApiService;
    private final RedisCacheService redisCacheService;
    
    /**
     * 价格更新线程
     */
    private Thread priceUpdateThread;
    
    /**
     * 控制线程运行的标志
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 线程池，用于执行价格更新任务
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * 价格更新间隔（毫秒）
     */
    private static final long UPDATE_INTERVAL = 5000; // 5秒更新一次
    
    /**
     * 应用启动时自动启动价格更新线程
     */
    @PostConstruct
    @Override
    public void startPriceUpdateThread() {
        if (running.get()) {
            log.info("价格更新线程已在运行中");
            return;
        }
        
        running.set(true);
        priceUpdateThread = new Thread(this::runPriceUpdateLoop, "price-update-thread");
        priceUpdateThread.setDaemon(true); // 设置为守护线程，随主线程退出而退出
        priceUpdateThread.start();
        
        log.info("价格更新线程已启动");
    }
    
    /**
     * 应用关闭时停止价格更新线程
     */
    @PreDestroy
    @Override
    public void stopPriceUpdateThread() {
        if (!running.get()) {
            log.info("价格更新线程未运行");
            return;
        }
        
        running.set(false);
        if (priceUpdateThread != null && priceUpdateThread.isAlive()) {
            try {
                priceUpdateThread.join(5000); // 等待线程结束，最多等待5秒
            } catch (InterruptedException e) {
                log.warn("等待价格更新线程结束时被中断", e);
                Thread.currentThread().interrupt();
            }
        }
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("等待线程池关闭时被中断", e);
            Thread.currentThread().interrupt();
        }
        
        log.info("价格更新线程已停止");
    }
    
    /**
     * 价格更新循环
     * 定期获取所有订阅币种的最新价格并更新到Redis缓存
     */
    private void runPriceUpdateLoop() {
        log.info("价格更新线程开始运行");
        
        while (running.get()) {
            try {
                // 获取所有订阅的币种
                Set<String> subscribedCoins = redisCacheService.getSubscribedCoins();
                
                if (subscribedCoins.isEmpty()) {
                    log.debug("没有订阅的币种，等待下次更新");
                    Thread.sleep(UPDATE_INTERVAL);
                    continue;
                }
                
                log.debug("开始更新 {} 个币种的价格", subscribedCoins.size());
                
                // 为每个币种创建一个更新任务
                for (String symbol : subscribedCoins) {
                    final String finalSymbol = symbol;
                    executorService.submit(() -> {
                        try {
                            // 获取行情数据（会自动写入Redis缓存）
                            okxApiService.getTicker(finalSymbol);
                            log.debug("已更新币种 {} 的价格", finalSymbol);
                        } catch (Exception e) {
                            log.error("更新币种 {} 价格失败: {}", finalSymbol, e.getMessage());
                        }
                    });
                }
                
                // 等待一段时间后再次更新
                Thread.sleep(UPDATE_INTERVAL);
            } catch (InterruptedException e) {
                log.warn("价格更新线程被中断", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("价格更新过程中发生错误: {}", e.getMessage(), e);
                try {
                    // 发生错误时等待一段时间再继续
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException ie) {
                    log.warn("价格更新线程被中断", ie);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("价格更新线程已退出");
    }
    
    @Override
    public Set<String> getSubscribedCoins() {
        return redisCacheService.getSubscribedCoins();
    }
    
    @Override
    public boolean addSubscribedCoin(String symbol) {
        return redisCacheService.addSubscribedCoin(symbol);
    }
    
    @Override
    public boolean removeSubscribedCoin(String symbol) {
        return redisCacheService.removeSubscribedCoin(symbol);
    }
} 