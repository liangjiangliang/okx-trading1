package com.okx.trading.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务逻辑异常类
 * 用于表示业务处理过程中的逻辑异常
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final int code;
    
    /**
     * HTTP状态码
     */
    private final HttpStatus httpStatus;
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param httpStatus HTTP状态码
     */
    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
} 