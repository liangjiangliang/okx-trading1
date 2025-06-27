package com.okx.trading.listener;

import com.okx.trading.event.CoinSubscriptionEvent;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.impl.OkxApiWebSocketServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 币种订阅事件监听器
 * 监听币种订阅变更事件，自动处理WebSocket订阅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinSubscriptionEventListener {

    private final OkxApiService okxApiService;

    /**
     * 处理币种订阅变更事件
     * 异步处理，不阻塞主线程
     *
     * @param event 币种订阅变更事件
     */
    @Async("coinSubscribeScheduler")
    @EventListener
    public void handleCoinSubscriptionEvent(CoinSubscriptionEvent event) {
        String symbol = event.getSymbol();

        try {
            // 获取WebSocket服务实现，用于检查币种订阅状态
            OkxApiWebSocketServiceImpl apiService = null;
            if (okxApiService instanceof OkxApiWebSocketServiceImpl) {
                apiService = (OkxApiWebSocketServiceImpl) okxApiService;
            }

            switch (event.getType()) {
                case SUBSCRIBE:
                    // 检查币种是否已订阅
                    if (apiService != null && apiService.isSymbolSubscribed(symbol)) {
                        log.info("币种 {} 已被订阅，跳过重复订阅", symbol);
                        return;
                    }

                    // 订阅币种行情
                    log.info("收到订阅币种事件，开始订阅币种 {} 的行情数据", symbol);
                    okxApiService.getTicker(symbol);
                    log.info("币种 {} 的行情数据订阅成功", symbol);
                    break;

                case UNSUBSCRIBE:
                    // 检查币种是否未订阅
                    if (apiService != null && !apiService.isSymbolSubscribed(symbol)) {
                        log.info("币种 {} 未被订阅，跳过取消订阅", symbol);
                        return;
                    }

                    // 取消订阅币种行情
                    log.info("收到取消订阅币种事件，开始取消订阅币种 {} 的行情数据", symbol);
                    boolean success = okxApiService.unsubscribeTicker(symbol);
                    if (success) {
                        log.info("币种 {} 的行情数据取消订阅成功", symbol);
                    } else {
                        log.warn("币种 {} 的行情数据取消订阅失败", symbol);
                    }
                    break;

                default:
                    log.warn("未知的币种订阅事件类型: {}", event.getType());
                    break;
            }
        } catch (Exception e) {
            log.error("处理币种 {} 订阅事件失败: {}", symbol, e.getMessage(), e);
        }
    }
}
