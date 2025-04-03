package com.okx.trading.exception;

import com.okx.trading.model.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 负责捕获应用中抛出的各种异常，并转换为一致的API响应格式
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleOkxApiException(OkxApiException e) {
        // 确保异常信息使用UTF-8编码
        String message = new String(e.getMessage().getBytes(StandardCharsets.UTF_8));
        log.error("OKX API调用异常: {}", message, e);
        return ApiResponse.error(e.getCode(), message);
    }

    /**
     * 处理参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一API响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("参数校验失败: {}", errorMessage);
        return ApiResponse.error(400, errorMessage);
    }

    /**
     * 处理参数绑定异常
     *
     * @param e 参数绑定异常
     * @return 统一API响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        BindingResult result = e.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("参数绑定失败: {}", errorMessage);
        return ApiResponse.error(400, errorMessage);
    }

    /**
     * 处理HTTP消息不可读异常
     *
     * @param e HTTP消息不可读异常
     * @return 统一API响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String errorMessage = "请求体格式不正确，请检查JSON格式";
        log.error("请求体解析失败: {}", e.getMessage());
        return ApiResponse.error(400, errorMessage);
    }

    /**
     * 处理业务逻辑异常
     *
     * @param e 业务逻辑异常
     * @return 统一API响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        // 确保异常信息使用UTF-8编码
        String message = new String(e.getMessage().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.error("业务逻辑异常: {}", message, e);
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getCode(), message));
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param e 异常
     * @return 统一API响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        // 确保异常信息使用UTF-8编码
        String message = new String(e.getMessage() != null ? e.getMessage().getBytes(StandardCharsets.ISO_8859_1) : "未知错误".getBytes(), StandardCharsets.UTF_8);
        log.error("系统异常: {}", message, e);
        return ApiResponse.error(500, "服务器内部错误: " + message);
    }
}
