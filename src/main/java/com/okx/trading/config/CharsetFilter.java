package com.okx.trading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * 字符集过滤器
 * 确保请求和响应的字符集为UTF-8
 */
@Slf4j
@Component
public class CharsetFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("CharsetFilter初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 设置请求的字符编码
        if (httpRequest.getCharacterEncoding() == null) {
            httpRequest.setCharacterEncoding("UTF-8");
        }

        // 设置响应的字符编码
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/json;charset=UTF-8");

        // 继续处理请求
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("CharsetFilter销毁");
    }
} 