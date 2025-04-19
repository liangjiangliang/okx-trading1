package com.okx.trading.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * K线数据订阅变更事件
 * 当K线数据订阅列表发生变化时触发
 */
@Getter
public class KlineSubscriptionEvent extends ApplicationEvent {

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
     * K线时间间隔
     */
    private final String interval;
    
    /**
     * 事件类型
     */
    private final EventType type;

    /**
     * 创建K线数据订阅变更事件
     *
     * @param source 事件源
     * @param symbol 交易对符号
     * @param interval K线时间间隔
     * @param type 事件类型
     */
    public KlineSubscriptionEvent(Object source, String symbol, String interval, EventType type) {
        super(source);
        this.symbol = symbol;
        this.interval = interval;
        this.type = type;
    }
} 