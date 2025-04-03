package com.okx.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.filter.CharacterEncodingFilter;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 应用程序入口类
 */
@SpringBootApplication
@EnableSwagger2
@ComponentScan(basePackages = "com.okx.trading")
public class Application {
    
    /**
     * 应用程序入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置默认字符编码为UTF-8
        System.setProperty("file.encoding", "UTF-8");
        SpringApplication.run(Application.class, args);
    }
} 