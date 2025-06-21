//package com.okx.trading.service;
//
//import com.okx.trading.event.WebSocketReconnectEvent;
//import com.okx.trading.service.impl.PriceUpdateServiceImpl;
//import com.okx.trading.util.WebSocketUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.ApplicationEventPublisher;
//
//import java.math.BigDecimal;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * WebSocket重连测试类
// * 测试WebSocket重连后价格更新机制
// */
//@Slf4j
//@SpringBootTest
//public class WebSocketReconnectTest {
//
//    @Autowired
//    private PriceUpdateService priceUpdateService;
//
//    @Autowired
//    private RedisCacheService redisCacheService;
//
//    @Autowired
//    private OkxApiService okxApiService;
//
//    @Autowired
//    private ApplicationEventPublisher eventPublisher;
//
//    @Autowired
//    private WebSocketUtil webSocketUtil;
//
//    /**
//     * 测试正常情况下的价格更新
//     * 1. 订阅一些币种
//     * 2. 等待价格更新
//     * 3. 验证Redis中是否有价格数据
//     */
//    @Test
//    public void testNormalPriceUpdate() throws Exception {
//        // 确保有一些订阅的币种
//        priceUpdateService.addSubscribedCoin("BTC-USDT");
//        priceUpdateService.addSubscribedCoin("ETH-USDT");
//
//        // 等待价格更新（5秒应该足够）
//        TimeUnit.SECONDS.sleep(5);
//
//        // 获取Redis中的价格数据
//        BigDecimal btcPrice = redisCacheService.getCoinPrice("BTC-USDT");
//        BigDecimal ethPrice = redisCacheService.getCoinPrice("ETH-USDT");
//
//        // 验证价格数据
//        assertNotNull(btcPrice, "BTC价格不应为空");
//        assertNotNull(ethPrice, "ETH价格不应为空");
//
//        log.info("BTC价格: {}", btcPrice);
//        log.info("ETH价格: {}", ethPrice);
//    }
//
//    /**
//     * 测试手动强制更新价格
//     * 1. 清空Redis中的价格数据
//     * 2. 调用forceUpdateAllPrices方法
//     * 3. 验证Redis中是否有价格数据
//     */
//    @Test
//    public void testForceUpdatePrices() throws Exception {
//        // 确保有一些订阅的币种
//        priceUpdateService.addSubscribedCoin("BTC-USDT");
//        priceUpdateService.addSubscribedCoin("ETH-USDT");
//
//        // 模拟Redis中价格数据被清空的情况
//        // 实际项目中不要直接操作Redis，这里只是模拟
//        redisCacheService.updateCoinPrice("BTC-USDT", null);
//        redisCacheService.updateCoinPrice("ETH-USDT", null);
//
//        // 强制更新价格
//        priceUpdateService.forceUpdateAllPrices();
//
//        // 等待价格更新（需要一些时间处理异步请求）
//        TimeUnit.SECONDS.sleep(5);
//
//        // 获取Redis中的价格数据
//        BigDecimal btcPrice = redisCacheService.getCoinPrice("BTC-USDT");
//        BigDecimal ethPrice = redisCacheService.getCoinPrice("ETH-USDT");
//
//        // 验证价格数据
//        assertNotNull(btcPrice, "强制更新后BTC价格不应为空");
//        assertNotNull(ethPrice, "强制更新后ETH价格不应为空");
//
//        log.info("强制更新后BTC价格: {}", btcPrice);
//        log.info("强制更新后ETH价格: {}", ethPrice);
//    }
//
//    /**
//     * 测试WebSocket重连事件
//     * 1. 手动发布WebSocket重连事件
//     * 2. 验证价格是否被更新
//     */
//    @Test
//    public void testWebSocketReconnectEvent() throws Exception {
//        // 确保有一些订阅的币种
//        priceUpdateService.addSubscribedCoin("BTC-USDT");
//        priceUpdateService.addSubscribedCoin("ETH-USDT");
//
//        // 记录当前价格
//        BigDecimal btcPriceBefore = redisCacheService.getCoinPrice("BTC-USDT");
//        BigDecimal ethPriceBefore = redisCacheService.getCoinPrice("ETH-USDT");
//
//        log.info("重连前BTC价格: {}", btcPriceBefore);
//        log.info("重连前ETH价格: {}", ethPriceBefore);
//
//        // 模拟WebSocket重连事件
//        eventPublisher.publishEvent(new WebSocketReconnectEvent(this, WebSocketReconnectEvent.ReconnectType.PUBLIC));
//
//        // 等待价格更新（需要一些时间处理异步请求）
//        TimeUnit.SECONDS.sleep(5);
//
//        // 获取更新后的价格
//        BigDecimal btcPriceAfter = redisCacheService.getCoinPrice("BTC-USDT");
//        BigDecimal ethPriceAfter = redisCacheService.getCoinPrice("ETH-USDT");
//
//        // 验证价格数据
//        assertNotNull(btcPriceAfter, "重连后BTC价格不应为空");
//        assertNotNull(ethPriceAfter, "重连后ETH价格不应为空");
//
//        log.info("重连后BTC价格: {}", btcPriceAfter);
//        log.info("重连后ETH价格: {}", ethPriceAfter);
//    }
//
//    /**
//     * 测试订阅币种列表恢复
//     */
//    @Test
//    public void testSubscribedCoinsRestore() {
//        // 获取当前订阅的币种
//        Set<String> subscribedCoins = priceUpdateService.getSubscribedCoins();
//
//        // 确保有一些默认订阅的币种
//        assertFalse(subscribedCoins.isEmpty(), "订阅币种列表不应为空");
//
//        log.info("当前订阅的币种: {}", subscribedCoins);
//
//        // 添加一个新的币种
//        String newCoin = "XRP-USDT";
//        priceUpdateService.addSubscribedCoin(newCoin);
//
//        // 验证新币种是否添加成功
//        Set<String> updatedCoins = priceUpdateService.getSubscribedCoins();
//        assertTrue(updatedCoins.contains(newCoin), "新币种应该已添加到订阅列表");
//
//        log.info("添加新币种后的订阅列表: {}", updatedCoins);
//    }
//
//    /**
//     * 测试所有币种的价格数据
//     */
//    @Test
//    public void testAllCoinPrices() throws Exception {
//        // 确保价格更新服务运行
//        priceUpdateService.startPriceUpdateThread();
//
//        // 等待价格更新（5秒应该足够）
//        TimeUnit.SECONDS.sleep(5);
//
//        // 获取所有币种价格
//        Map<String, BigDecimal> allPrices = redisCacheService.getAllCoinPrices();
//
//        // 验证价格数据
//        assertFalse(allPrices.isEmpty(), "币种价格列表不应为空");
//
//        // 输出所有币种价格
//        for (Map.Entry<String, BigDecimal> entry : allPrices.entrySet()) {
//            log.info("币种: {}, 价格: {}", entry.getKey(), entry.getValue());
//        }
//    }
//}
