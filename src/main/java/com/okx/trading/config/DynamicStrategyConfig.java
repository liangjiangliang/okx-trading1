package com.okx.trading.config;

import com.okx.trading.service.impl.DynamicStrategyService;
import com.okx.trading.service.impl.JavaCompilerDynamicStrategyService;
import com.okx.trading.service.impl.SmartDynamicStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 动态策略配置类
 * 在应用启动时自动加载数据库中的动态策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicStrategyConfig implements ApplicationRunner {

    private final DynamicStrategyService dynamicStrategyService;
    private final JavaCompilerDynamicStrategyService javaCompilerDynamicStrategyService;
    private final SmartDynamicStrategyService smartDynamicStrategyService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("应用启动，开始加载动态策略...");
        try {
            smartDynamicStrategyService.loadAllDynamicStrategies();
            log.info("使用智能编译服务加载动态策略完成");
        } catch (Exception e) {
            log.error("加载动态策略失败: {}", e.getMessage(), e);
        }
    }
}
