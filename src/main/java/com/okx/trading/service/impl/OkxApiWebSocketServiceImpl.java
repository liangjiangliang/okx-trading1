package com.okx.trading.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okx.trading.config.OkxApiConfig;
import com.okx.trading.exception.OkxApiException;
import com.okx.trading.model.account.AccountBalance;
import com.okx.trading.model.account.AccountBalance.AssetBalance;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.util.SignatureUtil;
import com.okx.trading.util.WebSocketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OKX API WebSocket服务实现类
 * 通过WebSocket连接实现与OKX交易所的交互
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "okx.api.connection-mode", 
    havingValue = "WEBSOCKET",
    matchIfMissing = true
)
public class OkxApiWebSocketServiceImpl implements OkxApiService {

    private final OkxApiConfig okxApiConfig;
    private final WebSocketUtil webSocketUtil;
    
    // 缓存和回调
    private final Map<String, CompletableFuture<Ticker>> tickerFutures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<List<Candlestick>>> klineFutures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<AccountBalance>> balanceFutures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<List<Order>>> ordersFutures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Order>> orderFutures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Boolean>> cancelOrderFutures = new ConcurrentHashMap<>();
    
    // 消息ID生成
    private final AtomicLong messageIdGenerator = new AtomicLong(1);
    
    @PostConstruct
    public void init() {
        // 注册消息处理器
        webSocketUtil.registerHandler("tickers", this::handleTickerMessage);
        webSocketUtil.registerHandler("candle1m", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle5m", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle15m", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle30m", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle1H", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle2H", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle4H", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle6H", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle12H", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle1D", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle1W", this::handleKlineMessage);
        webSocketUtil.registerHandler("candle1M", this::handleKlineMessage);
        webSocketUtil.registerHandler("account", this::handleAccountMessage);
        webSocketUtil.registerHandler("orders", this::handleOrdersMessage);
        webSocketUtil.registerHandler("order", this::handleOrderMessage);
    }
    
    /**
     * 处理Ticker消息
     */
    private void handleTickerMessage(JSONObject message) {
        try {
            String symbol = message.getJSONObject("arg").getString("instId");
            JSONArray data = message.getJSONArray("data");
            if (data != null && !data.isEmpty()) {
                JSONObject tickerData = data.getJSONObject(0);
                Ticker ticker = parseTicker(tickerData, symbol);
                
                CompletableFuture<Ticker> future = tickerFutures.get(symbol);
                if (future != null && !future.isDone()) {
                    future.complete(ticker);
                }
            }
        } catch (Exception e) {
            log.error("处理Ticker消息失败", e);
        }
    }
    
    /**
     * 处理K线消息
     */
    private void handleKlineMessage(JSONObject message) {
        try {
            String symbol = message.getJSONObject("arg").getString("instId");
            String interval = message.getJSONObject("arg").getString("channel").replace("candle", "");
            String key = symbol + "_" + interval;
            
            JSONArray data = message.getJSONArray("data");
            if (data != null && !data.isEmpty()) {
                List<Candlestick> candlesticks = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JSONArray candleData = data.getJSONArray(i);
                    Candlestick candlestick = parseCandlestick(candleData, symbol);
                    candlesticks.add(candlestick);
                }
                
                CompletableFuture<List<Candlestick>> future = klineFutures.get(key);
                if (future != null && !future.isDone()) {
                    future.complete(candlesticks);
                }
            }
        } catch (Exception e) {
            log.error("处理K线消息失败", e);
        }
    }
    
    /**
     * 处理账户消息
     */
    private void handleAccountMessage(JSONObject message) {
        try {
            JSONArray data = message.getJSONArray("data");
            if (data != null && !data.isEmpty()) {
                JSONObject balanceData = data.getJSONObject(0);
                AccountBalance accountBalance = parseAccountBalance(balanceData);
                
                String key = message.getJSONObject("arg").containsKey("simulated") ? "simulated" : "real";
                CompletableFuture<AccountBalance> future = balanceFutures.get(key);
                if (future != null && !future.isDone()) {
                    future.complete(accountBalance);
                }
            }
        } catch (Exception e) {
            log.error("处理账户消息失败", e);
        }
    }
    
    /**
     * 处理订单列表消息
     */
    private void handleOrdersMessage(JSONObject message) {
        try {
            String symbol = message.getJSONObject("arg").getString("instId");
            JSONArray data = message.getJSONArray("data");
            if (data != null) {
                List<Order> orders = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JSONObject orderData = data.getJSONObject(i);
                    Order order = parseOrder(orderData);
                    orders.add(order);
                }
                
                String key = symbol + "_orders";
                CompletableFuture<List<Order>> future = ordersFutures.get(key);
                if (future != null && !future.isDone()) {
                    future.complete(orders);
                }
            }
        } catch (Exception e) {
            log.error("处理订单列表消息失败", e);
        }
    }
    
    /**
     * 处理订单消息
     */
    private void handleOrderMessage(JSONObject message) {
        try {
            JSONArray data = message.getJSONArray("data");
            if (data != null && !data.isEmpty()) {
                JSONObject orderData = data.getJSONObject(0);
                Order order = parseOrder(orderData);
                
                String orderId = orderData.getString("ordId");
                CompletableFuture<Order> future = orderFutures.get(orderId);
                if (future != null && !future.isDone()) {
                    future.complete(order);
                }
                
                // 处理取消订单的响应
                if ("canceled".equals(orderData.getString("state"))) {
                    CompletableFuture<Boolean> cancelFuture = cancelOrderFutures.get(orderId);
                    if (cancelFuture != null && !cancelFuture.isDone()) {
                        cancelFuture.complete(true);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理订单消息失败", e);
        }
    }
    
    @Override
    public List<Candlestick> getKlineData(String symbol, String interval, Integer limit) {
        try {
            String formattedInterval = formatInterval(interval);
            String channel = "candle" + formattedInterval;
            String key = symbol + "_" + formattedInterval;
            
            CompletableFuture<List<Candlestick>> future = new CompletableFuture<>();
            klineFutures.put(key, future);
            
            webSocketUtil.subscribePublicTopic(channel, symbol);
            
            List<Candlestick> candlesticks = future.get(10, TimeUnit.SECONDS);
            klineFutures.remove(key);
            
            // 限制返回数量
            int size = limit != null && limit > 0 ? Math.min(limit, candlesticks.size()) : candlesticks.size();
            return candlesticks.subList(0, size);
        } catch (Exception e) {
            log.error("获取K线数据失败", e);
            throw new OkxApiException("获取K线数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Ticker getTicker(String symbol) {
        try {
            CompletableFuture<Ticker> future = new CompletableFuture<>();
            tickerFutures.put(symbol, future);
            
            webSocketUtil.subscribePublicTopic("tickers", symbol);
            
            Ticker ticker = future.get(10, TimeUnit.SECONDS);
            tickerFutures.remove(symbol);
            
            return ticker;
        } catch (Exception e) {
            log.error("获取行情数据失败", e);
            throw new OkxApiException("获取行情数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AccountBalance getAccountBalance() {
        try {
            CompletableFuture<AccountBalance> future = new CompletableFuture<>();
            balanceFutures.put("real", future);
            
            webSocketUtil.subscribePrivateTopic("account");
            
            AccountBalance accountBalance = future.get(10, TimeUnit.SECONDS);
            balanceFutures.remove("real");
            
            return accountBalance;
        } catch (Exception e) {
            log.error("获取账户余额失败", e);
            throw new OkxApiException("获取账户余额失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AccountBalance getSimulatedAccountBalance() {
        try {
            CompletableFuture<AccountBalance> future = new CompletableFuture<>();
            balanceFutures.put("simulated", future);
            
            // 向服务器发送请求获取模拟账户信息
            JSONObject requestMessage = new JSONObject();
            requestMessage.put("id", messageIdGenerator.getAndIncrement());
            requestMessage.put("op", "request");
            
            JSONObject arg = new JSONObject();
            arg.put("channel", "account");
            arg.put("simulated", "1");
            
            JSONObject[] args = new JSONObject[]{arg};
            requestMessage.put("args", args);
            
            // 发送请求
            webSocketUtil.sendPrivateRequest(requestMessage.toJSONString());
            
            AccountBalance accountBalance = future.get(10, TimeUnit.SECONDS);
            balanceFutures.remove("simulated");
            
            return accountBalance;
        } catch (Exception e) {
            log.error("获取模拟账户余额失败", e);
            throw new OkxApiException("获取模拟账户余额失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Order> getOrders(String symbol, String status, Integer limit) {
        try {
            String key = symbol + "_orders";
            CompletableFuture<List<Order>> future = new CompletableFuture<>();
            ordersFutures.put(key, future);
            
            // 向服务器发送请求获取订单列表
            JSONObject requestMessage = new JSONObject();
            requestMessage.put("id", messageIdGenerator.getAndIncrement());
            requestMessage.put("op", "request");
            
            JSONObject arg = new JSONObject();
            arg.put("channel", "orders");
            arg.put("instId", symbol);
            if (status != null && !status.isEmpty()) {
                arg.put("state", mapToOkxOrderStatus(status));
            }
            if (limit != null && limit > 0) {
                arg.put("limit", limit.toString());
            }
            
            JSONObject[] args = new JSONObject[]{arg};
            requestMessage.put("args", args);
            
            // 发送请求
            webSocketUtil.sendPrivateRequest(requestMessage.toJSONString());
            
            List<Order> orders = future.get(10, TimeUnit.SECONDS);
            ordersFutures.remove(key);
            
            return orders;
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            throw new OkxApiException("获取订单列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Order createSpotOrder(OrderRequest orderRequest) {
        return createOrder(orderRequest, "SPOT", orderRequest.getSimulated() != null && orderRequest.getSimulated());
    }
    
    @Override
    public Order createFuturesOrder(OrderRequest orderRequest) {
        return createOrder(orderRequest, "SWAP", orderRequest.getSimulated() != null && orderRequest.getSimulated());
    }
    
    /**
     * 创建订单
     */
    private Order createOrder(OrderRequest orderRequest, String instType, boolean isSimulated) {
        try {
            String orderId = UUID.randomUUID().toString().replace("-", "");
            CompletableFuture<Order> future = new CompletableFuture<>();
            orderFutures.put(orderId, future);
            
            // 构建订单请求
            JSONObject requestMessage = new JSONObject();
            requestMessage.put("id", messageIdGenerator.getAndIncrement());
            requestMessage.put("op", "order");
            
            JSONObject arg = new JSONObject();
            arg.put("instId", orderRequest.getSymbol());
            arg.put("tdMode", "cash"); // 资金模式，cash为现钞
            arg.put("side", orderRequest.getSide().toLowerCase());
            arg.put("ordType", mapToOkxOrderType(orderRequest.getType()));
            arg.put("sz", orderRequest.getQuantity().toString());
            
            if ("limit".equals(mapToOkxOrderType(orderRequest.getType()))) {
                arg.put("px", orderRequest.getPrice().toString());
            }
            
            if (orderRequest.getClientOrderId() != null) {
                arg.put("clOrdId", orderRequest.getClientOrderId());
            } else {
                arg.put("clOrdId", orderId);
            }
            
            // 设置杠杆倍数（合约交易）
            if ("SWAP".equals(instType) && orderRequest.getLeverage() != null) {
                arg.put("lever", orderRequest.getLeverage().toString());
            }
            
            // 设置被动委托
            if (orderRequest.getPostOnly() != null && orderRequest.getPostOnly()) {
                arg.put("postOnly", "1");
            }
            
            // 设置模拟交易
            if (isSimulated) {
                arg.put("simulated", "1");
            }
            
            JSONObject[] args = new JSONObject[]{arg};
            requestMessage.put("args", args);
            
            // 发送请求
            webSocketUtil.sendPrivateRequest(requestMessage.toJSONString());
            
            Order order = future.get(10, TimeUnit.SECONDS);
            orderFutures.remove(orderId);
            
            return order;
        } catch (Exception e) {
            log.error("创建订单失败", e);
            throw new OkxApiException("创建订单失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean cancelOrder(String symbol, String orderId) {
        try {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            cancelOrderFutures.put(orderId, future);
            
            // 构建取消订单请求
            JSONObject requestMessage = new JSONObject();
            requestMessage.put("id", messageIdGenerator.getAndIncrement());
            requestMessage.put("op", "cancel-order");
            
            JSONObject arg = new JSONObject();
            arg.put("instId", symbol);
            arg.put("ordId", orderId);
            
            JSONObject[] args = new JSONObject[]{arg};
            requestMessage.put("args", args);
            
            // 发送请求
            webSocketUtil.sendPrivateRequest(requestMessage.toJSONString());
            
            boolean success = future.get(10, TimeUnit.SECONDS);
            cancelOrderFutures.remove(orderId);
            
            return success;
        } catch (Exception e) {
            log.error("取消订单失败", e);
            throw new OkxApiException("取消订单失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析Ticker数据
     */
    private Ticker parseTicker(JSONObject tickerData, String symbol) {
        Ticker ticker = new Ticker();
        ticker.setSymbol(symbol);
        ticker.setLastPrice(new BigDecimal(tickerData.getString("last")));
        ticker.setBidPrice(new BigDecimal(tickerData.getString("bidPx")));
        ticker.setAskPrice(new BigDecimal(tickerData.getString("askPx")));
        ticker.setHighPrice(new BigDecimal(tickerData.getString("high24h")));
        ticker.setLowPrice(new BigDecimal(tickerData.getString("low24h")));
        ticker.setVolume(new BigDecimal(tickerData.getString("vol24h")));
        ticker.setQuoteVolume(new BigDecimal(tickerData.getString("volCcy24h")));
        
        // 解析时间戳
        long timestamp = tickerData.getLongValue("ts");
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        ticker.setTimestamp(time);
        
        // 计算24小时涨跌幅
        if (tickerData.containsKey("open24h") && tickerData.containsKey("last")) {
            BigDecimal open = new BigDecimal(tickerData.getString("open24h"));
            BigDecimal last = new BigDecimal(tickerData.getString("last"));
            if (open.compareTo(BigDecimal.ZERO) > 0) {
                ticker.setPriceChange(last.subtract(open));
                ticker.setPriceChangePercent(last.subtract(open).divide(open, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")));
            }
        }
        
        return ticker;
    }
    
    /**
     * 解析K线数据
     */
    private Candlestick parseCandlestick(JSONArray candleData, String symbol) {
        Candlestick candlestick = new Candlestick();
        candlestick.setSymbol(symbol);
        
        // 解析时间戳
        long timestamp = Long.parseLong(candleData.getString(0));
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        candlestick.setOpenTime(time);
        
        candlestick.setOpen(new BigDecimal(candleData.getString(1)));
        candlestick.setHigh(new BigDecimal(candleData.getString(2)));
        candlestick.setLow(new BigDecimal(candleData.getString(3)));
        candlestick.setClose(new BigDecimal(candleData.getString(4)));
        candlestick.setVolume(new BigDecimal(candleData.getString(5)));
        
        return candlestick;
    }
    
    /**
     * 解析账户余额数据
     */
    private AccountBalance parseAccountBalance(JSONObject balanceData) {
        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setTotalEquity(new BigDecimal(balanceData.getString("totalEq")));
        
        List<AssetBalance> assetBalances = new ArrayList<>();
        JSONArray detailsArray = balanceData.getJSONArray("details");
        
        for (int i = 0; i < detailsArray.size(); i++) {
            JSONObject detail = detailsArray.getJSONObject(i);
            AssetBalance assetBalance = new AssetBalance();
            assetBalance.setAsset(detail.getString("ccy"));
            assetBalance.setAvailable(new BigDecimal(detail.getString("availEq")));
            assetBalance.setFrozen(new BigDecimal(detail.getString("frozenBal")));
            assetBalance.setTotal(new BigDecimal(detail.getString("eq")));
            assetBalances.add(assetBalance);
        }
        
        accountBalance.setAssetBalances(assetBalances);
        return accountBalance;
    }
    
    /**
     * 解析订单数据
     */
    private Order parseOrder(JSONObject orderData) {
        Order order = new Order();
        order.setOrderId(orderData.getString("ordId"));
        order.setClientOrderId(orderData.getString("clOrdId"));
        order.setSymbol(orderData.getString("instId"));
        
        if (orderData.containsKey("px") && !orderData.getString("px").isEmpty()) {
            order.setPrice(new BigDecimal(orderData.getString("px")));
        }
        
        order.setOrigQty(new BigDecimal(orderData.getString("sz")));
        
        if (orderData.containsKey("accFillSz")) {
            order.setExecutedQty(new BigDecimal(orderData.getString("accFillSz")));
        }
        
        if (orderData.containsKey("fillPx") && !orderData.getString("fillPx").isEmpty()) {
            BigDecimal fillPrice = new BigDecimal(orderData.getString("fillPx"));
            BigDecimal fillSize = new BigDecimal(orderData.getString("accFillSz"));
            order.setCummulativeQuoteQty(fillPrice.multiply(fillSize));
        }
        
        order.setStatus(mapOrderStatus(orderData.getString("state")));
        order.setType(mapOrderType(orderData.getString("ordType")));
        order.setSide(orderData.getString("side").toUpperCase());
        
        if (orderData.containsKey("fee") && !orderData.getString("fee").isEmpty()) {
            order.setFee(new BigDecimal(orderData.getString("fee")));
        }
        
        if (orderData.containsKey("feeCcy")) {
            order.setFeeCurrency(orderData.getString("feeCcy"));
        }
        
        // 解析时间戳
        if (orderData.containsKey("cTime")) {
            long createTime = Long.parseLong(orderData.getString("cTime"));
            order.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(createTime), ZoneId.systemDefault()));
        }
        
        if (orderData.containsKey("uTime")) {
            long updateTime = Long.parseLong(orderData.getString("uTime"));
            order.setUpdateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(updateTime), ZoneId.systemDefault()));
        }
        
        return order;
    }
    
    /**
     * 格式化K线间隔
     */
    private String formatInterval(String interval) {
        return interval;
    }
    
    /**
     * 映射OKX订单状态到标准状态
     */
    private String mapOrderStatus(String okxStatus) {
        switch (okxStatus) {
            case "live":
                return "NEW";
            case "partially_filled":
                return "PARTIALLY_FILLED";
            case "filled":
                return "FILLED";
            case "canceled":
                return "CANCELED";
            case "canceling":
                return "CANCELING";
            default:
                return okxStatus.toUpperCase();
        }
    }
    
    /**
     * 映射标准订单状态到OKX订单状态
     */
    private String mapToOkxOrderStatus(String standardStatus) {
        switch (standardStatus.toUpperCase()) {
            case "NEW":
                return "live";
            case "PARTIALLY_FILLED":
                return "partially_filled";
            case "FILLED":
                return "filled";
            case "CANCELED":
                return "canceled";
            case "CANCELING":
                return "canceling";
            default:
                return standardStatus.toLowerCase();
        }
    }
    
    /**
     * 映射OKX订单类型到标准类型
     */
    private String mapOrderType(String okxType) {
        switch (okxType) {
            case "limit":
                return "LIMIT";
            case "market":
                return "MARKET";
            default:
                return okxType.toUpperCase();
        }
    }
    
    /**
     * 映射标准订单类型到OKX订单类型
     */
    private String mapToOkxOrderType(String standardType) {
        switch (standardType.toUpperCase()) {
            case "LIMIT":
                return "limit";
            case "MARKET":
                return "market";
            default:
                return standardType.toLowerCase();
        }
    }
} 