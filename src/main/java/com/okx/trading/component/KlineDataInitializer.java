package com.okx.trading.component;

import com.okx.trading.service.KlineCacheService;
import com.okx.trading.service.KlineUpdateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * K线数据初始化组件
 * 在应用启动时自动订阅和加载K线数据
 */
@Component
@RequiredArgsConstructor
public class KlineDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KlineDataInitializer.class);
    
    private final KlineCacheService klineCacheService;
    private final KlineUpdateService klineUpdateService;

    @Override
    public void run(String... args) {
        log.info("开始初始化K线数据...");

        try {
            // 初始化默认K线订阅
            klineCacheService.initDefaultKlineSubscriptions();

            // 从Redis获取所有订阅的K线信息
            Map<String, List<String>> subscribedKlines = klineCacheService.getAllSubscribedKlines();
            
            log.info("从Redis获取订阅K线列表，共 {} 个交易对", subscribedKlines.size());
            
            // 为每个交易对和时间间隔打印详细信息
            subscribedKlines.forEach((symbol, intervals) -> {
                log.info("交易对: {}, 订阅间隔: {}", symbol, intervals);
            });

            // 启动K线更新线程
            klineUpdateService.startUpdateThread();
            
            log.info("K线数据初始化完成，K线更新线程已启动");
        } catch (Exception e) {
            log.error("K线数据初始化过程中发生错误: {}", e.getMessage(), e);
        }
    }
} 