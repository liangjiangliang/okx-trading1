package com.okx.trading.exception;

import com.okx.trading.model.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中抛出的各类异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理OKX API异常
     *
     * @param e OKX API异常
     * @return 统一API响应
     */
    @ExceptionHandler(OkxApiException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleOkxApiException(OkxApiException e) {
        log.error("OKX API异常", e);
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数验证异常
     *
     * @param e 参数验证异常
     * @return 统一API响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数验证异常: {}", errorMsg);
        return ApiResponse.error(400, errorMsg);
    }

    /**
     * 处理绑定异常
     *
     * @param e 绑定异常
     * @return 统一API响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("绑定异常: {}", errorMsg);
        return ApiResponse.error(400, errorMsg);
    }

    /**
     * 处理其他异常
     *
     * @param e 异常
     * @return 统一API响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统异常，请稍后重试");
    }
} 