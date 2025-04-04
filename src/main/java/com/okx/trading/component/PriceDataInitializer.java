package com.okx.trading.component;

import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.RedisCacheService;
import com.okx.trading.service.impl.OkxApiWebSocketServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 价格数据初始化组件
 * 在应用启动时自动订阅主要币种的行情数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceDataInitializer implements CommandLineRunner {

    private final OkxApiService okxApiService;
    private final RedisCacheService redisCacheService;

    @Override
    public void run(String... args) {
        log.info("开始初始化币种价格数据...");

        try {
            // 初始化默认订阅币种
            redisCacheService.initDefaultSubscribedCoins();

            // 从Redis获取订阅币种列表
            Set<String> subscribedCoins = redisCacheService.getSubscribedCoins();
            log.info("从Redis获取订阅币种列表，共 {} 个币种", subscribedCoins.size());

            // 检查是否是WebSocket实现
            OkxApiWebSocketServiceImpl webSocketService = null;
            if (okxApiService instanceof OkxApiWebSocketServiceImpl) {
                webSocketService = (OkxApiWebSocketServiceImpl) okxApiService;
            }

            // 遍历订阅币种列表，订阅行情数据
            for (String symbol : subscribedCoins) {
                try {
                    // 检查是否已订阅
                    if (webSocketService != null && webSocketService.isSymbolSubscribed(symbol)) {
                        log.info("币种 {} 已被订阅，跳过重复订阅", symbol);
                        continue;
                    }

                    // 获取行情数据（会自动写入Redis缓存）
                    okxApiService.getTicker(symbol);
                    log.info("已订阅币种 {} 的行情数据", symbol);
                    // 稍微暂停一下，避免请求过于频繁
                    Thread.sleep(300);
                } catch (Exception e) {
                    log.error("订阅币种 {} 行情数据失败: {}", symbol, e.getMessage());
                }
            }

            log.info("币种价格数据初始化完成");
        } catch (Exception e) {
            log.error("币种价格数据初始化过程中发生错误: {}", e.getMessage(), e);
        }
    }
}
