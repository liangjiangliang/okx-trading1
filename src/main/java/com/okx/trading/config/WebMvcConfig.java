package com.okx.trading.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Web MVC配置类
 * 用于配置HTTP消息转换器，确保JSON中文不乱码
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置HTTP消息转换器
     * 使用FastJson作为JSON序列化工具，确保中文不乱码
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建FastJson消息转换器
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        
        // 创建FastJson配置
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                // 保留空的属性
                SerializerFeature.WriteMapNullValue,
                // 将日期格式化为ISO8601标准格式
                SerializerFeature.WriteDateUseDateFormat,
                // 禁用循环引用检测
                SerializerFeature.DisableCircularReferenceDetect
        );
        // 设置编码为UTF-8
        config.setCharset(StandardCharsets.UTF_8);
        converter.setFastJsonConfig(config);
        
        // 设置支持的媒体类型
        List<MediaType> mediaTypeList = new ArrayList<>();
        mediaTypeList.add(MediaType.APPLICATION_JSON);
        // 不使用过时的APPLICATION_JSON_UTF8，而是用APPLICATION_JSON并设置charset
        mediaTypeList.add(new MediaType(
                MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(),
                StandardCharsets.UTF_8));
        mediaTypeList.add(MediaType.TEXT_HTML);
        converter.setSupportedMediaTypes(mediaTypeList);
        
        // 添加到转换器列表
        converters.add(0, converter);
    }
} 