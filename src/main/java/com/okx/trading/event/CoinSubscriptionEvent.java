package com.okx.trading.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 币种订阅变更事件
 * 当币种订阅列表发生变化时触发
 */
@Getter
public class CoinSubscriptionEvent extends ApplicationEvent {

    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 添加订阅
         */
        SUBSCRIBE,
        
        /**
         * 取消订阅
         */
        UNSUBSCRIBE
    }
    
    /**
     * 交易对符号
     */
    private final String symbol;
    
    /**
     * 事件类型
     */
    private final EventType type;

    /**
     * 创建币种订阅变更事件
     *
     * @param source 事件源
     * @param symbol 交易对符号
     * @param type 事件类型
     */
    public CoinSubscriptionEvent(Object source, String symbol, EventType type) {
        super(source);
        this.symbol = symbol;
        this.type = type;
    }
} 