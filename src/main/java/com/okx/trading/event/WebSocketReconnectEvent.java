package com.okx.trading.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * WebSocket重连事件
 * 当WebSocket连接断开并重新连接后触发
 */
@Getter
public class WebSocketReconnectEvent extends ApplicationEvent {

    /**
     * 重连类型
     */
    public enum ReconnectType {
        /**
         * 公共频道重连
         */
        PUBLIC,
        
        /**
         * 私有频道重连
         */
        PRIVATE
    }
    
    /**
     * 重连类型
     */
    private final ReconnectType type;

    /**
     * 创建WebSocket重连事件
     *
     * @param source 事件源
     * @param type 重连类型
     */
    public WebSocketReconnectEvent(Object source, ReconnectType type) {
        super(source);
        this.type = type;
    }
} 