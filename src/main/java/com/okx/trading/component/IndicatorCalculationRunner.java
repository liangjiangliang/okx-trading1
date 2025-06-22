//package com.okx.trading.component;
//
//import com.okx.trading.service.IndicatorCalculationService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
///**
// * 指标计算初始化运行器
// * 在应用启动时执行，负责扫描Redis中的K线数据并计算指标
// * 使用CommandLineRunner接口实现，在应用启动后执行
// * Order注解指定执行顺序
// */
//@Component
//@Order(3)
//public class IndicatorCalculationRunner implements CommandLineRunner {
//
//    private static final Logger logger = LoggerFactory.getLogger(IndicatorCalculationRunner.class);
//
//    /**
//     * 指标计算服务
//     */
//    private final IndicatorCalculationService indicatorCalculationService;
//
//    /**
//     * 构造函数，通过Spring注入服务
//     *
//     * @param indicatorCalculationService 指标计算服务
//     */
//    @Autowired
//    public IndicatorCalculationRunner(IndicatorCalculationService indicatorCalculationService) {
//        this.indicatorCalculationService = indicatorCalculationService;
//    }
//
//    /**
//     * 在应用启动后执行的方法
//     *
//     * @param args 命令行参数
//     */
//    @Override
//    public void run(String... args) {
//        try {
////            logger.info("开始启动指标计算服务...");
////
////            // 启动指标计算服务
////            indicatorCalculationService.startService();
////
////            logger.info("指标计算服务启动完成");
//        } catch (Exception e) {
//            logger.error("启动指标计算服务失败", e);
//        }
//    }
//}
