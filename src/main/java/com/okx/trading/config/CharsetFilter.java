package com.okx.trading.config;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 字符编码过滤器
 * 确保所有的HTTP请求和响应都使用UTF-8编码
 */
@Component
public class CharsetFilter extends OncePerRequestFilter {

    /**
     * 过滤每个请求，设置请求和响应的字符编码为UTF-8
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 设置请求字符编码
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        
        // 设置响应字符编码
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 设置响应内容类型，确保JSON内容也使用UTF-8
        if (response.getContentType() == null) {
            response.setContentType("application/json;charset=UTF-8");
        } else if (!response.getContentType().contains("charset=")) {
            response.setContentType(response.getContentType() + ";charset=UTF-8");
        }
        
        // 添加额外的编码相关的响应头
        response.setHeader("Content-Type", response.getContentType());
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
} 