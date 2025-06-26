//package com.okx.trading.component;
//
//import com.okx.trading.service.KlineCacheService;
//import com.okx.trading.service.KlineUpdateService;
//import com.okx.trading.service.OkxApiService;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//import java.util.List;
//
///**
// * K线数据初始化组件
// * 在应用启动时自动订阅和加载K线数据
// */
//@Component
//public class KlineDataInitializer implements CommandLineRunner {
//
//    private static final Logger log = LoggerFactory.getLogger(KlineDataInitializer.class);
//
//    private final KlineCacheService klineCacheService;
//    private final KlineUpdateService klineUpdateService;
//    private final OkxApiService okxApiService;
//
//    @Autowired
//    public KlineDataInitializer(KlineCacheService klineCacheService,
//                               @Lazy KlineUpdateService klineUpdateService,
//                               @Lazy OkxApiService okxApiService) {
//        this.klineCacheService = klineCacheService;
//        this.klineUpdateService = klineUpdateService;
//        this.okxApiService = okxApiService;
//    }
//
//    @Override
//    public void run(String... args) {
//        try {
//            log.info("K线数据初始化开始...");
//
//            // 延迟5秒等待WebSocket连接建立
//            Thread.sleep(5000);
//
//            // 恢复之前的K线订阅
//            restorePreviousKlineSubscriptions();
//
//            // 启动K线更新线程
////            klineUpdateService.startUpdateThread();
//
//            log.info("K线数据初始化完成");
//        } catch (Exception e) {
//            log.error("K线数据初始化过程中发生错误: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 恢复之前的K线订阅
//     */
//    private void restorePreviousKlineSubscriptions() {
//        try {
//            // 从缓存中获取所有之前订阅的K线数据
//            Map<String, List<String>> allSubscribedKlines = klineCacheService.getAllSubscribedKlines();
//
//            if (allSubscribedKlines.isEmpty()) {
//                log.info("没有发现之前的K线订阅记录");
//                return;
//            }
//
//            log.info("发现 {} 个交易对的K线订阅记录，开始恢复订阅...", allSubscribedKlines.size());
//
//            int totalRestored = 0;
//            int successCount = 0;
//
//            // 遍历所有订阅的交易对和时间间隔
//            for (Map.Entry<String, List<String>> entry : allSubscribedKlines.entrySet()) {
//                String symbol = entry.getKey();
//                List<String> intervals = entry.getValue();
//
//                for (String interval : intervals) {
//                    totalRestored++;
//                    try {
//                        // 重新订阅K线数据
//                        boolean success = okxApiService.subscribeKlineData(symbol, interval);
//                        if (success) {
//                            successCount++;
//                            log.debug("恢复K线订阅成功: {} {}", symbol, interval);
//                        } else {
//                            log.warn("恢复K线订阅失败: {} {}", symbol, interval);
//                        }
//
//                        // 添加小延迟避免频率过高
//                        Thread.sleep(100);
//                    } catch (Exception e) {
//                        log.error("恢复K线订阅异常: {} {}, 错误: {}", symbol, interval, e.getMessage());
//                    }
//                }
//            }
//
//            log.info("K线订阅恢复完成: 总计 {} 个，成功 {} 个，失败 {} 个",
//                    totalRestored, successCount, totalRestored - successCount);
//
//        } catch (Exception e) {
//            log.error("恢复K线订阅失败: {}", e.getMessage(), e);
//        }
//    }
//}
