package com.okx.trading.service.impl;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.model.account.AccountBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OkxApiMockServiceImplTest {

    private OkxApiMockServiceImpl okxApiService;

    @BeforeEach
    void setUp() {
        okxApiService = new OkxApiMockServiceImpl();
    }

    @Test
    void getKlineData() {
        String symbol = "BTC-USDT";
        String interval = "1h";
        int limit = 10;
        
        List<Candlestick> candles = okxApiService.getKlineData(symbol, interval, limit);
        
        assertNotNull(candles);
        assertEquals(limit, candles.size());
        for (Candlestick candle : candles) {
            assertNotNull(candle.getOpenTime());
            assertNotNull(candle.getOpen());
            assertNotNull(candle.getHigh());
            assertNotNull(candle.getLow());
            assertNotNull(candle.getClose());
            assertNotNull(candle.getVolume());
        }
    }

    @Test
    void getTicker() {
        String symbol = "BTC-USDT";
        
        Ticker ticker = okxApiService.getTicker(symbol);
        
        assertNotNull(ticker);
        assertEquals(symbol, ticker.getSymbol());
        assertNotNull(ticker.getLastPrice());
        assertNotNull(ticker.getBidPrice());
        assertNotNull(ticker.getAskPrice());
        assertNotNull(ticker.getVolume());
    }

    @Test
    void getAccountBalance() {
        AccountBalance balance = okxApiService.getAccountBalance();
        
        assertNotNull(balance);
        assertNotNull(balance.getTotalEquity());
        assertNotNull(balance.getAvailableBalance());
        assertNotNull(balance.getFrozenBalance());
        assertNotNull(balance.getAssetBalances());
        
        assertFalse(balance.getAssetBalances().isEmpty());
        
        for (AccountBalance.AssetBalance assetBalance : balance.getAssetBalances()) {
            assertNotNull(assetBalance.getAsset());
            assertNotNull(assetBalance.getAvailable());
            assertNotNull(assetBalance.getFrozen());
            assertNotNull(assetBalance.getTotal());
        }
    }

    @Test
    void createSpotOrderWithQuantity() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setQuantity(new BigDecimal("0.1"));
        
        Order order = okxApiService.createSpotOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        assertEquals(orderRequest.getQuantity(), order.getOrigQty());
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createSpotOrderWithAmount() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setAmount(new BigDecimal("5000")); // 金额5000USDT
        
        Order order = okxApiService.createSpotOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        // 验证计算的数量是否正确 5000 / 50000 = 0.1
        assertEquals(new BigDecimal("0.1").setScale(8), order.getOrigQty().setScale(8));
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createSpotOrderWithAmountMarketBuy() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("MARKET");
        orderRequest.setSide("BUY");
        orderRequest.setAmount(new BigDecimal("5000")); // 金额5000USDT
        
        Order order = okxApiService.createSpotOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertNotNull(order.getPrice()); // 验证市场价格已设置
        assertNotNull(order.getOrigQty()); // 验证数量已计算
        assertTrue(order.getOrigQty().compareTo(BigDecimal.ZERO) > 0); // 验证数量大于0
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createSpotOrderWithAmountMarketSell() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("MARKET");
        orderRequest.setSide("SELL");
        orderRequest.setAmount(new BigDecimal("0.1")); // 卖出0.1个BTC
        
        Order order = okxApiService.createSpotOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertNotNull(order.getPrice()); // 验证市场价格已设置
        assertEquals(new BigDecimal("0.1").setScale(8), order.getOrigQty().setScale(8)); // 验证数量与输入金额相同
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createFuturesOrderWithAmount() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT-SWAP");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setAmount(new BigDecimal("5000")); // 金额5000USDT
        
        Order order = okxApiService.createFuturesOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        // 验证计算的数量是否正确 5000 / 50000 = 0.1
        assertEquals(new BigDecimal("0.1").setScale(8), order.getOrigQty().setScale(8));
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createOrderFailsWithoutQuantityOrAmount() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        // 不设置任何数量相关参数
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            okxApiService.createSpotOrder(orderRequest);
        });
        
        assertTrue(exception.getMessage().contains("必须指定数量、金额或比例"));
    }

    @Test
    void cancelOrder() {
        // 先创建一个订单
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setQuantity(new BigDecimal("0.1"));
        
        Order createdOrder = okxApiService.createSpotOrder(orderRequest);
        
        // 取消订单
        String symbol = createdOrder.getSymbol();
        String orderId = createdOrder.getOrderId();
        boolean cancelResult = okxApiService.cancelOrder(symbol, orderId);
        
        assertTrue(cancelResult);
        
        // 验证订单状态
        List<Order> orders = okxApiService.getOrders(symbol, "CANCELED", null);
        Optional<Order> canceledOrder = orders.stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst();
        
        assertTrue(canceledOrder.isPresent());
        assertEquals("CANCELED", canceledOrder.get().getStatus());
    }

    @Test
    void getOrders() {
        // 创建几个订单
        for (int i = 0; i < 3; i++) {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setSymbol("BTC-USDT");
            orderRequest.setType("LIMIT");
            orderRequest.setSide("BUY");
            orderRequest.setPrice(new BigDecimal("50000").add(new BigDecimal(i * 100)));
            orderRequest.setQuantity(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)));
            
            okxApiService.createSpotOrder(orderRequest);
        }
        
        List<Order> orders = okxApiService.getOrders("BTC-USDT", null, null);
        
        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        assertTrue(orders.size() >= 3);
        
        for (Order order : orders) {
            assertEquals("BTC-USDT", order.getSymbol());
        }
    }

    @Test
    void createSpotOrderWithBuyRatio() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setBuyRatio(new BigDecimal("0.5")); // 使用50%的USDT余额购买BTC
        
        Order order = okxApiService.createSpotOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        
        // 验证数量不为零
        assertTrue(order.getOrigQty().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("NEW", order.getStatus());
        
        // 验证手续费
        assertNotNull(order.getFee());
        assertEquals("USDT", order.getFeeCurrency());
    }
    
    @Test
    void createSpotOrderWithSellRatio() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("SELL");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setSellRatio(new BigDecimal("0.5")); // 卖出50%的BTC持仓
        
        Order order = okxApiService.createSpotOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        
        // 验证数量不为零
        assertTrue(order.getOrigQty().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("NEW", order.getStatus());
        
        // 验证手续费
        assertNotNull(order.getFee());
        assertEquals("USDT", order.getFeeCurrency());
    }
    
    @Test
    void createFuturesOrderWithBuyRatio() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT-SWAP");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setBuyRatio(new BigDecimal("0.3")); // 使用30%的USDT余额购买BTC合约
        orderRequest.setLeverage(5); // 5倍杠杆
        
        Order order = okxApiService.createFuturesOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        
        // 验证数量不为零
        assertTrue(order.getOrigQty().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createFuturesOrderWithSellRatio() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT-SWAP");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("SELL");
        orderRequest.setPrice(new BigDecimal("50000"));
        orderRequest.setSellRatio(new BigDecimal("0.8")); // 卖出80%的BTC合约持仓
        orderRequest.setLeverage(5); // 5倍杠杆
        
        Order order = okxApiService.createFuturesOrder(orderRequest);
        
        assertNotNull(order);
        assertEquals(orderRequest.getSymbol(), order.getSymbol());
        assertEquals(orderRequest.getType(), order.getType());
        assertEquals(orderRequest.getSide(), order.getSide());
        assertEquals(orderRequest.getPrice(), order.getPrice());
        
        // 验证数量不为零
        assertTrue(order.getOrigQty().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("NEW", order.getStatus());
    }
    
    @Test
    void createOrderFailsWithInvalidParameters() {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC-USDT");
        orderRequest.setType("LIMIT");
        orderRequest.setSide("BUY");
        orderRequest.setPrice(new BigDecimal("50000"));
        // 不设置任何数量相关参数
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            okxApiService.createSpotOrder(orderRequest);
        });
        
        assertTrue(exception.getMessage().contains("必须指定数量、金额或比例"));
    }
} 