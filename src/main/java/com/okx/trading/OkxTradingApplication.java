package com.okx.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OKX交易API应用程序主类
 * 提供与OKX交易所API交互的功能
 */
@SpringBootApplication
@EnableScheduling
public class OkxTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OkxTradingApplication.class, args);
    }
} 