package com.okx.trading.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 * 用于验证热部署功能
 */
@RestController
public class TestController {

    /**
     * 测试接口
     * @return 测试消息
     */
    @GetMapping("/test")
    public String test() {
        return "111wa ！！！Trump is the president of the United States! Hello, this is a test message! Hot deployment is working! Good China";
    }
}
