package com.okx.trading.util;

import com.alibaba.fastjson.JSON;
import com.okx.trading.exception.OkxApiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类
 * 用于发送HTTP请求
 */
@Slf4j
public class HttpUtil {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    /**
     * 发送GET请求
     *
     * @param client    OkHttpClient实例
     * @param url       请求URL
     * @param headers   请求头
     * @return 响应内容字符串
     */
    public static String get(OkHttpClient client, String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url).get();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
        
        return execute(client, builder.build());
    }
    
    /**
     * 发送POST请求
     *
     * @param client    OkHttpClient实例
     * @param url       请求URL
     * @param headers   请求头
     * @param body      请求体（JSON字符串）
     * @return 响应内容字符串
     */
    public static String post(OkHttpClient client, String url, Map<String, String> headers, String body) {
        RequestBody requestBody = RequestBody.create(body, JSON_TYPE);
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
        
        return execute(client, builder.build());
    }
    
    /**
     * 执行HTTP请求
     *
     * @param client  OkHttpClient实例
     * @param request 请求对象
     * @return 响应内容字符串
     */
    private static String execute(OkHttpClient client, Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("HTTP请求失败: {} {}", response.code(), response.message());
                throw new OkxApiException(response.code(), "HTTP请求失败: " + response.message());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new OkxApiException("HTTP响应体为空");
            }
            
            String responseStr = responseBody.string();
            log.debug("HTTP响应: {}", responseStr);
            return responseStr;
        } catch (IOException e) {
            log.error("HTTP请求异常", e);
            throw new OkxApiException("HTTP请求异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将对象转换为JSON字符串
     *
     * @param obj 要转换的对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }
    
    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
} 