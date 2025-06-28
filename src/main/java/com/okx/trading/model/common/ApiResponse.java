package com.okx.trading.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * API响应封装类
 * 用于统一接口返回格式
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 状态码，200表示成功
     */
    private int code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 创建成功响应
     * @param data 数据
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "SUCCESS", data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, "SUCCESS", null);
    }

    /**
     * 创建成功响应
     * @param message 消息
     * @param data 数据
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> success(T data,String message) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误消息
     * @param data 数据
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }


    public static <T> ApiResponse<T> error( String message) {
         return new ApiResponse<>(-1, message, null);
     }
}
