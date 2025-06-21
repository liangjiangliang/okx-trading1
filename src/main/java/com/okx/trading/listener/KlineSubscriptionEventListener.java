//package com.okx.trading.listener;
//
//import com.okx.trading.event.KlineSubscriptionEvent;
//import com.okx.trading.service.KlineUpdateService;
//import com.okx.trading.service.OkxApiService;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
///**
// * K线订阅事件监听器
// * 监听K线订阅变更事件，自动处理数据更新
// */
//@Component
//@RequiredArgsConstructor
//public class KlineSubscriptionEventListener {
//
//    private static final Logger log = LoggerFactory.getLogger(KlineSubscriptionEventListener.class);
//
//    private final KlineUpdateService klineUpdateService;
//    private final OkxApiService okxApiService;
//
//    /**
//     * 处理K线订阅变更事件
//     * 异步处理，不阻塞主线程
//     *
//     * @param event K线订阅变更事件
//     */
//    @Async
//    @EventListener
//    public void handleKlineSubscriptionEvent(KlineSubscriptionEvent event) {
//        String symbol = event.getSymbol();
//        String interval = event.getInterval();
//        boolean isSubscribe = event.getType() == KlineSubscriptionEvent.EventType.SUBSCRIBE;
//
//        try {
//            log.info("收到K线订阅变更事件：{} {}，类型：{}", symbol, interval, event.getType());
//
//            // 如果是订阅事件，立即从API获取K线数据
//            if (isSubscribe) {
//                log.info("开始订阅K线数据: {} {}", symbol, interval);
//
//                try {
//                    // 调用API获取K线数据
//                    okxApiService.getKlineData(symbol, interval, 10);
//
//                    // 通知K线更新服务处理订阅
//                    klineUpdateService.handleKlineSubscription(symbol, interval, true);
//
//                    log.info("K线数据订阅成功: {} {}", symbol, interval);
//                } catch (Exception e) {
//                    log.error("获取K线数据失败: {} {}，错误: {}", symbol, interval, e.getMessage(), e);
//                }
//            } else {
//                // 如果是取消订阅事件
//                log.info("开始取消订阅K线数据: {} {}", symbol, interval);
//
//                // 通知K线更新服务处理取消订阅
//                klineUpdateService.handleKlineSubscription(symbol, interval, false);
//
//                log.info("K线数据取消订阅成功: {} {}", symbol, interval);
//            }
//        } catch (Exception e) {
//            log.error("处理K线订阅变更事件失败: {} {}，错误: {}", symbol, interval, e.getMessage(), e);
//        }
//    }
//}
