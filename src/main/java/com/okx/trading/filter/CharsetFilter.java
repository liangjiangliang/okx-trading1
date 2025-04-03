package com.okx.trading.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 字符编码过滤器
 * 处理所有HTTP请求和响应的字符编码，确保使用UTF-8
 */
@Component
public class CharsetFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 设置请求编码
        request.setCharacterEncoding("UTF-8");
        
        // 设置响应编码
        response.setCharacterEncoding("UTF-8");
        
        // 设置Content-Type
        if (response.getContentType() == null) {
            response.setContentType("text/html;charset=UTF-8");
        } else if (!response.getContentType().contains("charset")) {
            response.setContentType(response.getContentType() + ";charset=UTF-8");
        }
        
        // 添加额外的响应头，确保浏览器正确解析
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        
        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
} 