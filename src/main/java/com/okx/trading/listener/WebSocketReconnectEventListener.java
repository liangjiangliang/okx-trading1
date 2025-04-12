package com.okx.trading.listener;

import com.okx.trading.event.WebSocketReconnectEvent;
import com.okx.trading.service.PriceUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * WebSocket重连事件监听器
 * 监听WebSocket重连事件，自动处理重连后的数据恢复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketReconnectEventListener {

    private final PriceUpdateService priceUpdateService;

    /**
     * 处理WebSocket重连事件
     * 异步处理，不阻塞主线程
     *
     * @param event WebSocket重连事件
     */
    @Async
    @EventListener
    public void handleWebSocketReconnectEvent(WebSocketReconnectEvent event) {
        try {
            if (event.getType() == WebSocketReconnectEvent.ReconnectType.PUBLIC) {
                log.info("检测到WebSocket公共频道重连，开始恢复价格数据");
                
                // 强制更新所有订阅币种的价格
                priceUpdateService.forceUpdateAllPrices();
            }
        } catch (Exception e) {
            log.error("处理WebSocket重连事件失败", e);
        }
    }
} 