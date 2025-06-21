//package com.okx.trading.component;
//
//import com.okx.trading.service.PriceUpdateService;
//import com.okx.trading.service.RedisCacheService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.Set;
//
///**
// * 价格数据初始化组件
// * 在应用启动时自动订阅主要币种的行情数据
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PriceDataInitializer implements CommandLineRunner {
//
//    private final PriceUpdateService priceUpdateService;
//    private final RedisCacheService redisCacheService;
//
//    @Override
//    public void run(String... args) {
//        log.info("开始初始化币种价格数据...");
//
//        try {
//            // 初始化默认订阅币种
////            redisCacheService.initDefaultSubscribedCoins();
//
//            // 从Redis获取订阅币种列表
//            Set<String> subscribedCoins = redisCacheService.getSubscribedCoins();
//            log.info("从Redis获取订阅币种列表，共 {} 个币种", subscribedCoins.size());
//
//            // 启动价格更新线程
//            priceUpdateService.startPriceUpdateThread();
//
//            log.info("币种价格数据初始化完成，价格更新线程已启动");
//        } catch (Exception e) {
//            log.error("币种价格数据初始化过程中发生错误: {}", e.getMessage(), e);
//        }
//    }
//}
