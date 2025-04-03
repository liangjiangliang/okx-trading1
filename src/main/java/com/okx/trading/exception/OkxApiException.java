package com.okx.trading.exception;

import lombok.Getter;

/**
 * OKX API异常类
 * 封装与OKX API交互过程中可能出现的异常
 */
public class OkxApiException extends RuntimeException {
    
    @Getter
    private final int code;
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public OkxApiException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public OkxApiException(String message) {
        super(message);
        this.code = 500;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原始异常
     */
    public OkxApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
} 