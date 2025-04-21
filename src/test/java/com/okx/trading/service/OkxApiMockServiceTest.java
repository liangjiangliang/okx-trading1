package com.okx.trading.service;

import com.okx.trading.model.account.AccountBalance;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.impl.OkxApiMockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OKX API模拟服务测试类
 */
public class OkxApiMockServiceTest {

    private OkxApiService okxApiService;

    @BeforeEach
    public void setUp() {
        okxApiService = new OkxApiMockServiceImpl();
    }

    /**
     * 测试获取K线数据
     */
    @Test
    public void testGetKlineData() {
        // 执行
        List<Candlestick> candlesticks = okxApiService.getKlineData("BTC-USDT", "1m", 10);

        // 验证
        assertNotNull(candlesticks);
        assertEquals(10, candlesticks.size());

        for (Candlestick candlestick : candlesticks) {
            assertEquals("BTC-USDT", candlestick.getSymbol());
            assertEquals("1m", candlestick.getIntervalVal());
            assertNotNull(candlestick.getOpenTime());
            assertNotNull(candlestick.getOpen());
            assertNotNull(candlestick.getHigh());
            assertNotNull(candlestick.getLow());
            assertNotNull(candlestick.getClose());
            assertNotNull(candlestick.getVolume());

            // 价格验证：高价应该大于等于开盘价和收盘价，低价应该小于等于开盘价和收盘价
            assertTrue(candlestick.getHigh().compareTo(candlestick.getOpen()) >= 0);
            assertTrue(candlestick.getHigh().compareTo(candlestick.getClose()) >= 0);
            assertTrue(candlestick.getLow().compareTo(candlestick.getOpen()) <= 0);
            assertTrue(candlestick.getLow().compareTo(candlestick.getClose()) <= 0);
        }
    }

    /**
     * 测试获取行情数据
     */
    @Test
    public void testGetTicker() {
        // 执行
        Ticker ticker = okxApiService.getTicker("BTC-USDT");

        // 验证
        assertNotNull(ticker);
        assertEquals("BTC-USDT", ticker.getSymbol());
        assertNotNull(ticker.getLastPrice());
        assertNotNull(ticker.getHighPrice());
        assertNotNull(ticker.getLowPrice());
        assertNotNull(ticker.getVolume());
        assertNotNull(ticker.getTimestamp());

        // 验证价格关系
        assertTrue(ticker.getHighPrice().compareTo(ticker.getLowPrice()) >= 0);
    }

    /**
     * 测试获取账户余额
     */
    @Test
    public void testGetAccountBalance() {
        // 执行
        AccountBalance balance = okxApiService.getAccountBalance();

        // 验证
        assertNotNull(balance);
        assertEquals(0, balance.getAccountType());
        assertNotNull(balance.getTotalEquity());
        assertNotNull(balance.getAvailableBalance());
        assertNotNull(balance.getFrozenBalance());
        assertNotNull(balance.getAssetBalances());
        assertFalse(balance.getAssetBalances().isEmpty());

        // 验证余额计算是否正确
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalFrozen = BigDecimal.ZERO;

        for (AccountBalance.AssetBalance assetBalance : balance.getAssetBalances()) {
            assertNotNull(assetBalance.getAsset());
            assertNotNull(assetBalance.getAvailable());
            assertNotNull(assetBalance.getFrozen());
            assertNotNull(assetBalance.getTotal());
            assertNotNull(assetBalance.getUsdValue());

            // 验证总余额计算
            assertEquals(assetBalance.getAvailable().add(assetBalance.getFrozen()),
                    assetBalance.getTotal());

            // 累加USD价值
            totalAvailable = totalAvailable.add(assetBalance.getAvailable());
            totalFrozen = totalFrozen.add(assetBalance.getFrozen());
        }
    }

    /**
     * 测试创建和获取订单
     */
    @Test
    public void testCreateAndGetOrder() {
        // 创建订单请求
        OrderRequest orderRequest = OrderRequest.builder()
                .symbol("BTC-USDT")
                .type("LIMIT")
                .side("BUY")
                .price(new BigDecimal("50000"))
                .quantity(new BigDecimal("0.1"))
                .timeInForce("GTC")
                .build();

        // 执行创建订单
        Order order = okxApiService.createSpotOrder(orderRequest);

        // 验证创建的订单
        assertNotNull(order);
        assertEquals("BTC-USDT", order.getSymbol());
        assertEquals("LIMIT", order.getType());
        assertEquals("BUY", order.getSide());
        assertEquals(new BigDecimal("50000"), order.getPrice());
        assertEquals(new BigDecimal("0.1"), order.getOrigQty());
        assertEquals("NEW", order.getStatus());
        assertNotNull(order.getOrderId());

        // 获取订单列表
        List<Order> orders = okxApiService.getOrders("BTC-USDT", null, 10);

        // 验证订单列表
        assertNotNull(orders);
        assertFalse(orders.isEmpty());

        // 验证是否可以找到刚创建的订单
        boolean found = false;
        for (Order listedOrder : orders) {
            if (listedOrder.getOrderId().equals(order.getOrderId())) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        // 测试取消订单
        boolean cancelResult = okxApiService.cancelOrder("BTC-USDT", order.getOrderId());
        assertTrue(cancelResult);

        // 获取订单并验证状态
        List<Order> canceledOrders = okxApiService.getOrders("BTC-USDT", "CANCELED", 10);
        assertNotNull(canceledOrders);

        // 验证是否可以找到刚取消的订单
        found = false;
        for (Order canceledOrder : canceledOrders) {
            if (canceledOrder.getOrderId().equals(order.getOrderId())) {
                found = true;
                assertEquals("CANCELED", canceledOrder.getStatus());
                break;
            }
        }
        assertTrue(found);
    }
}
