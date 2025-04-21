package com.okx.trading.component;

import com.okx.trading.service.KlineCacheService;
import com.okx.trading.service.KlineUpdateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * K线数据初始化组件
 * 在应用启动时自动订阅和加载K线数据
 */
@Component
public class KlineDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KlineDataInitializer.class);

    private final KlineCacheService klineCacheService;
    private final KlineUpdateService klineUpdateService;

    @Autowired
    public KlineDataInitializer(KlineCacheService klineCacheService, 
                               @Lazy KlineUpdateService klineUpdateService) {
        this.klineCacheService = klineCacheService;
        this.klineUpdateService = klineUpdateService;
    }

    @Override
    public void run(String... args) {
        try {
            // 启动K线更新线程
            klineUpdateService.startUpdateThread();
        } catch (Exception e) {
            log.error("K线数据初始化过程中发生错误: {}", e.getMessage(), e);
        }
    }
}
