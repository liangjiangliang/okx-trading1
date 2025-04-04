package com.okx.trading.service.impl;

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
import com.okx.trading.service.RedisCacheService;
import com.okx.trading.util.WebSocketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class OkxApiWebSocketServiceImpl implements OkxApiService{

    private final OkxApiConfig okxApiConfig;
    private final WebSocketUtil webSocketUtil;
    private final RedisCacheService redisCacheService;

    // 缓存和回调
    private final Map<String,CompletableFuture<Ticker>> tickerFutures = new ConcurrentHashMap<>();
    private final Map<String,CompletableFuture<List<Candlestick>>> klineFutures = new ConcurrentHashMap<>();
    private final Map<String,CompletableFuture<AccountBalance>> balanceFutures = new ConcurrentHashMap<>();
    private final Map<String,CompletableFuture<List<Order>>> ordersFutures = new ConcurrentHashMap<>();
    private final Map<String,CompletableFuture<Order>> orderFutures = new ConcurrentHashMap<>();
    private final Map<String,CompletableFuture<Boolean>> cancelOrderFutures = new ConcurrentHashMap<>();

    // 跟踪当前已订阅的币种
    private final Set<String> subscribedSymbols = Collections.synchronizedSet(new HashSet<>());

    // 消息ID生成
    private final AtomicLong messageIdGenerator = new AtomicLong(1);

    @PostConstruct
    public void init(){
        // 注册消息处理器
        webSocketUtil.registerHandler("tickers", this :: handleTickerMessage);

        // 注册标准K线处理器
        webSocketUtil.registerHandler("mark-price-candle1m", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle5m", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle15m", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle30m", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle1H", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle2H", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle4H", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle6H", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle12H", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle1D", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle1W", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle1M", this :: handleKlineMessage);
        webSocketUtil.registerHandler("mark-price-candle3M", this :: handleKlineMessage);

        // 注册标记价格K线处理器
        webSocketUtil.registerHandler("mark-price", this :: handleTickerMessage);

        webSocketUtil.registerHandler("account", this :: handleAccountMessage);
        webSocketUtil.registerHandler("orders", this :: handleOrdersMessage);
        webSocketUtil.registerHandler("order", this :: handleOrderMessage);
    }

    /**
     * 处理Ticker消息
     */
    private void handleTickerMessage(JSONObject message){
        try{
            String channel = message.getJSONObject("arg").getString("channel");
            String symbol = message.getJSONObject("arg").getString("instId");
            JSONArray data = message.getJSONArray("data");
            if(data != null && ! data.isEmpty()){
                JSONObject tickerData = data.getJSONObject(0);
                Ticker ticker = parseTicker(tickerData, symbol, channel);

                log.debug("获取实时指数行情信息: {}", ticker);

                // 将最新价格写入Redis缓存
                BigDecimal lastPrice = ticker.getLastPrice();
                if(lastPrice != null){
                    redisCacheService.updateCoinPrice(symbol, lastPrice);
                }

                CompletableFuture<Ticker> future = tickerFutures.get(channel + "_" + symbol);
                if(future != null && ! future.isDone()){
                    future.complete(ticker);
                }
            }
        }catch(Exception e){
            log.error("处理Ticker消息失败", e);
        }
    }

    /**
     * 处理K线消息,实时行情消息,都是标记价格
     */
    private void handleKlineMessage(JSONObject message){
        try{
            if(! message.containsKey("arg") || ! message.containsKey("data")){
                log.debug("忽略不包含必要字段的K线消息: {}", message);
                return;
            }

            JSONObject arg = message.getJSONObject("arg");
            String symbol = arg.getString("instId");
            String channel = arg.getString("channel");

            // 对于标记价格K线，从bar参数获取interval
            String interval = channel.replaceAll("mark-price-candle", "");
            // 构建缓存键 - 确保与getKlineData和unsubscribeKlineData方法使用相同的键格式
            String key = channel + "_" + symbol + "_" + interval;

            // 获取数据并解析
            List<Candlestick> candlesticks = new ArrayList<>();

            // 处理data字段 - 不同类型
            Object dataObj = message.get("data");

            // 数组格式 - 可能是数组的数组或对象的数组
            JSONArray dataArray = (JSONArray)dataObj;
            for(int i = 0;i < dataArray.size();i++){
                Object item = dataArray.get(i);
                Candlestick candlestick = null;

                if(item instanceof JSONArray){
                    // 标准K线格式：数组的数组
                    JSONArray candleData = (JSONArray)item;
                    candlestick = parseCandlestick(candleData, symbol, channel);
                }else if(item instanceof JSONObject){
                    // 某些API返回对象数组
                    JSONObject candleObj = (JSONObject)item;
                    candlestick = parseCandlestickFromObject(candleObj, symbol, channel);
                }

                if(candlestick != null){
                    candlestick.setInterval(interval);
                    log.info("获取实时标记价格k线数据: {}", candlestick);
                    candlesticks.add(candlestick);
                }
            }
            // 如果解析到了数据，完成等待中的Future
            if(! candlesticks.isEmpty()){
                CompletableFuture<List<Candlestick>> future = klineFutures.get(key);
                if(future != null && ! future.isDone()){
                    log.debug("完成K线数据Future，符号: {}, 间隔: {}, 数据量: {}", symbol, interval, candlesticks.size());
                    future.complete(candlesticks);
                }
            }
        }catch(Exception e){
            log.error("处理K线消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从JSONObject解析K线数据
     * 用于处理非标准格式的K线数据
     */
    private Candlestick parseCandlestickFromObject(JSONObject candleObj, String symbol, String channel){
        try{
            Candlestick candlestick = new Candlestick();
            candlestick.setSymbol(symbol);
            candlestick.setChannel(channel);

            // 解析时间戳
            if(candleObj.containsKey("ts")){
                long timestamp = candleObj.getLongValue("ts");
                LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                candlestick.setOpenTime(time);
            }

            // 解析标记价格K线特有字段
            if(candleObj.containsKey("markPx")){
                BigDecimal markPrice = new BigDecimal(candleObj.getString("markPx"));
                candlestick.setOpen(markPrice);
                candlestick.setHigh(markPrice);
                candlestick.setLow(markPrice);
                candlestick.setClose(markPrice);
                // 标记价格K线可能没有交易量
                candlestick.setVolume(BigDecimal.ZERO);
                return candlestick;
            }

            // 解析标准K线字段
            if(candleObj.containsKey("o")){
                candlestick.setOpen(new BigDecimal(candleObj.getString("o")));
            }
            if(candleObj.containsKey("h")){
                candlestick.setHigh(new BigDecimal(candleObj.getString("h")));
            }
            if(candleObj.containsKey("l")){
                candlestick.setLow(new BigDecimal(candleObj.getString("l")));
            }
            if(candleObj.containsKey("c")){
                candlestick.setClose(new BigDecimal(candleObj.getString("c")));
            }
            if(candleObj.containsKey("vol")){
                candlestick.setVolume(new BigDecimal(candleObj.getString("vol")));
            }

            return candlestick;
        }catch(Exception e){
            log.error("解析K线对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 处理账户消息
     */
    private void handleAccountMessage(JSONObject message){
        try{
            JSONArray data = message.getJSONArray("data");
            if(data != null && ! data.isEmpty()){
                JSONObject balanceData = data.getJSONObject(0);
                AccountBalance accountBalance = parseAccountBalance(balanceData);

                String key = message.getJSONObject("arg").containsKey("simulated")?"simulated":"real";
                CompletableFuture<AccountBalance> future = balanceFutures.get(key);
                if(future != null && ! future.isDone()){
                    future.complete(accountBalance);
                }
            }
        }catch(Exception e){
            log.error("处理账户消息失败", e);
        }
    }

    /**
     * 处理订单列表消息
     */
    private void handleOrdersMessage(JSONObject message){
        try{
            String symbol = message.getJSONObject("arg").getString("instId");
            JSONArray data = message.getJSONArray("data");
            if(data != null){
                List<Order> orders = new ArrayList<>();
                for(int i = 0;i < data.size();i++){
                    JSONObject orderData = data.getJSONObject(i);
                    Order order = parseOrder(orderData);
                    orders.add(order);
                }

                String key = symbol + "_orders";
                CompletableFuture<List<Order>> future = ordersFutures.get(key);
                if(future != null && ! future.isDone()){
                    future.complete(orders);
                }
            }
        }catch(Exception e){
            log.error("处理订单列表消息失败", e);
        }
    }

    /**
     * 处理订单消息
     */
    private void handleOrderMessage(JSONObject message){
        try{
            JSONArray data = message.getJSONArray("data");
            if(data != null && ! data.isEmpty()){
                JSONObject orderData = data.getJSONObject(0);
                Order order = parseOrder(orderData);
                
                // 使用clOrdId查找对应的future，而不是ordId
                String clientOrderId = orderData.getString("clOrdId");
                log.info("收到订单消息: orderId={}, clientOrderId={}, status={}", 
                    orderData.getString("ordId"), clientOrderId, orderData.getString("state"));
                
                CompletableFuture<Order> future = orderFutures.get(clientOrderId);
                if(future != null && ! future.isDone()){
                    future.complete(order);
                }

                // 处理取消订单的响应
                if("canceled".equals(orderData.getString("state"))){
                    String orderId = orderData.getString("ordId");
                    CompletableFuture<Boolean> cancelFuture = cancelOrderFutures.get(orderId);
                    if(cancelFuture != null && ! cancelFuture.isDone()){
                        cancelFuture.complete(true);
                    }
                }
            }
        }catch(Exception e){
            log.error("处理订单消息失败", e);
        }
    }

    @Override
    public List<Candlestick> getKlineData(String symbol, String interval, Integer limit){
        try{
            // 构建标记价格K线的正确频道名和参数
            String channel = "mark-price-candle" + interval;
            // 确保键格式统一
            String key = channel + "_" + symbol + "_" + interval;

            CompletableFuture<List<Candlestick>> future = new CompletableFuture<>();
            klineFutures.put(key, future);

            // 创建完整的WebSocket参数对象
            JSONObject arg = new JSONObject();
            arg.put("channel", channel);
            arg.put("instId", symbol);

            log.debug("订阅标记价格K线数据，符号: {}, 间隔: {}", symbol, interval);
            webSocketUtil.subscribePublicTopicWithArgs(arg, symbol);

            // 设置超时时间更长一些
            List<Candlestick> candlesticks = future.get(15, TimeUnit.SECONDS);
            klineFutures.remove(key);

            // 添加间隔信息
            candlesticks.forEach(c -> c.setInterval(interval));

            // 限制返回数量
            int size = limit != null && limit > 0?Math.min(limit, candlesticks.size()):candlesticks.size();
            return candlesticks.subList(0, size);
        }catch(TimeoutException e){
            log.error("获取K线数据超时，符号: {}, 间隔: {}", symbol, interval);
            throw new OkxApiException("获取K线数据超时，请稍后重试", e);
        }catch(Exception e){
            log.error("获取K线数据失败: {}", e.getMessage(), e);
            throw new OkxApiException("获取K线数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Ticker getTicker(String symbol){
        try{
            // 检查是否已订阅，避免重复订阅
            if(subscribedSymbols.contains(symbol)){
                log.debug("币种 {} 已经订阅，跳过重复订阅", symbol);
                // 从Redis获取最新价格
                BigDecimal price = redisCacheService.getCoinPrice(symbol);
                if(price != null){
                    Ticker ticker = new Ticker();
                    ticker.setSymbol(symbol);
                    ticker.setLastPrice(price);
                    ticker.setTimestamp(LocalDateTime.now());
                    return ticker;
                }
                // 如果Redis中没有价格，继续订阅以获取最新数据
            }

            String channel = "tickers";
            String key = channel + "_" + symbol;

            CompletableFuture<Ticker> future = new CompletableFuture<>();
            tickerFutures.put(key, future);

            log.info("订阅币种 {} 行情数据", symbol);
            webSocketUtil.subscribePublicTopic(channel, symbol);

            // 标记为已订阅
            subscribedSymbols.add(symbol);

            Ticker ticker = future.get(10, TimeUnit.SECONDS);
            tickerFutures.remove(key);

            return ticker;
        }catch(Exception e){
            log.error("获取行情数据失败", e);
            throw new OkxApiException("获取行情数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AccountBalance getAccountBalance(){
        try{
            CompletableFuture<AccountBalance> future = new CompletableFuture<>();
            balanceFutures.put("real", future);

            webSocketUtil.subscribePrivateTopic("account");

            AccountBalance accountBalance = future.get(10, TimeUnit.SECONDS);
            balanceFutures.remove("real");

            return accountBalance;
        }catch(Exception e){
            log.error("获取账户余额失败", e);
            throw new OkxApiException("获取账户余额失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AccountBalance getSimulatedAccountBalance(){
        try{
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
        }catch(Exception e){
            log.error("获取模拟账户余额失败", e);
            throw new OkxApiException("获取模拟账户余额失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> getOrders(String symbol, String status, Integer limit){
        try{
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
            if(status != null && ! status.isEmpty()){
                arg.put("state", mapToOkxOrderStatus(status));
            }
            if(limit != null && limit > 0){
                arg.put("limit", limit.toString());
            }

            JSONObject[] args = new JSONObject[]{arg};
            requestMessage.put("args", args);

            // 发送请求
            webSocketUtil.sendPrivateRequest(requestMessage.toJSONString());

            List<Order> orders = future.get(10, TimeUnit.SECONDS);
            ordersFutures.remove(key);

            return orders;
        }catch(Exception e){
            log.error("获取订单列表失败", e);
            throw new OkxApiException("获取订单列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Order createSpotOrder(OrderRequest orderRequest){
        return createOrder(orderRequest, "SPOT", orderRequest.getSimulated() != null && orderRequest.getSimulated());
    }

    @Override
    public Order createFuturesOrder(OrderRequest orderRequest){
        return createOrder(orderRequest, "SWAP", orderRequest.getSimulated() != null && orderRequest.getSimulated());
    }

    /**
     * 创建订单
     */
    private Order createOrder(OrderRequest orderRequest, String instType, boolean isSimulated){
        try{
            // 生成唯一订单ID，保证每次请求的ID都是不同的
            String orderId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            String clientOrderId = orderRequest.getClientOrderId() != null ? 
                orderRequest.getClientOrderId() : "okx_" + System.currentTimeMillis() + "_" + orderId.substring(0, 8);
            
            CompletableFuture<Order> future = new CompletableFuture<>();
            
            // 将future与clientOrderId关联，而不是orderId
            orderFutures.put(clientOrderId, future);
            
            log.info("准备创建订单, symbol: {}, type: {}, side: {}, clientOrderId: {}", 
                orderRequest.getSymbol(), orderRequest.getType(), orderRequest.getSide(), clientOrderId);

            // 构建订单请求
            JSONObject requestMessage = new JSONObject();
            requestMessage.put("id", messageIdGenerator.getAndIncrement());
            requestMessage.put("op", "order");

            JSONObject arg = new JSONObject();
            arg.put("instId", orderRequest.getSymbol());
            arg.put("tdMode", "cash"); // 资金模式，cash为现钞
            arg.put("side", orderRequest.getSide().toLowerCase());
            arg.put("ordType", mapToOkxOrderType(orderRequest.getType())); // MARKET LIMIT
            
            // 处理市价单和限价单逻辑
            if("market".equals(mapToOkxOrderType(orderRequest.getType()))){
                // 市价单
                if(orderRequest.getAmount() != null){
                    // 按金额下单
                    BigDecimal coinPrice = redisCacheService.getCoinPrice(orderRequest.getSymbol());
                    if(coinPrice == null || coinPrice.compareTo(BigDecimal.ZERO) <= 0){
                        log.error("无法获取币种价格: {}", orderRequest.getSymbol());
                        throw new OkxApiException("无法获取币种价格，请稍后重试");
                    }
                    
                    // 为市价单计算数量，金额除以价格
                    BigDecimal quantity = orderRequest.getAmount().divide(coinPrice, 8, RoundingMode.HALF_UP);
                    log.info("市价单按金额下单计算: 金额={}, 价格={}, 数量={}", 
                        orderRequest.getAmount(), coinPrice, quantity);
                    arg.put("sz", quantity.toString());
                    
                    // 市价单买入时使用tgtCcy=base标记按数量购买，卖出时不需要标记
                    if("buy".equalsIgnoreCase(orderRequest.getSide().toLowerCase())){
                        arg.put("tgtCcy", "base");
                    }
                } else if(orderRequest.getQuantity() != null){
                    // 按数量下单
                    arg.put("sz", orderRequest.getQuantity().toString());
                    
                    // 买入市价单总是按数量购买基础货币
                    if("buy".equalsIgnoreCase(orderRequest.getSide().toLowerCase())){
                        arg.put("tgtCcy", "base");
                    }
                } else {
                    throw new OkxApiException("市价单必须指定金额或数量");
                }
            } else {
                // 限价单
                if(orderRequest.getPrice() == null){
                    throw new OkxApiException("限价单必须指定价格");
                }
                arg.put("px", orderRequest.getPrice().toString());
                
                if(orderRequest.getQuantity() == null){
                    throw new OkxApiException("限价单必须指定数量");
                }
                arg.put("sz", orderRequest.getQuantity().toString());
            }

            // 设置客户端订单ID
            arg.put("clOrdId", clientOrderId);

            // 设置杠杆倍数（合约交易）
            if("SWAP".equals(instType) && orderRequest.getLeverage() != null){
                arg.put("lever", orderRequest.getLeverage().toString());
            }
            
            // 设置被动委托
            if(orderRequest.getPostOnly() != null && orderRequest.getPostOnly()){
                arg.put("postOnly", "1");
            }
            
            // 设置模拟交易
            if(isSimulated){
                arg.put("simulated", "1");
            }

            JSONObject[] args = new JSONObject[]{arg};
            requestMessage.put("args", args);

            // 记录发送的订单请求
            log.info("发送订单请求: {}", requestMessage.toJSONString());
            
            // 检查WebSocket连接状态
            if (!webSocketUtil.isPrivateSocketConnected()) {
                log.error("私有WebSocket未连接，无法发送订单请求");
                throw new OkxApiException("WebSocket连接已断开，请重新连接后再尝试");
            }
            
            // 发送请求
            webSocketUtil.sendPrivateRequest(requestMessage.toJSONString());

            // 增加订单超时时间到45秒
            Order order;
            try {
                order = future.get(45, TimeUnit.SECONDS);
                log.info("成功收到订单响应, clientOrderId: {}, orderId: {}, status: {}", 
                    clientOrderId, order.getOrderId(), order.getStatus());
            } catch (TimeoutException e) {
                log.error("订单请求超时(45秒), symbol: {}, type: {}, side: {}, clientOrderId: {}, 请求内容: {}", 
                    orderRequest.getSymbol(), orderRequest.getType(), orderRequest.getSide(), 
                    clientOrderId, requestMessage.toJSONString());
                throw new OkxApiException("订单请求超时(45秒)，请检查网络连接或OKX服务器状态，可能订单已发送但未收到响应，请使用查询订单接口确认订单状态");
            } catch (Exception e) {
                log.error("订单请求异常, symbol: {}, type: {}, side: {}, clientOrderId: {}, 错误: {}", 
                    orderRequest.getSymbol(), orderRequest.getType(), orderRequest.getSide(), 
                    clientOrderId, e.getMessage(), e);
                throw new OkxApiException("订单请求异常: " + e.getMessage(), e);
            } finally {
                // 清理资源
                orderFutures.remove(clientOrderId);
            }

            return order;
        } catch(OkxApiException e) {
            throw e;
        } catch(Exception e){
            log.error("创建订单失败: {}", e.getMessage(), e);
            throw new OkxApiException("创建订单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelOrder(String symbol, String orderId){
        try{
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
        }catch(Exception e){
            log.error("取消订单失败", e);
            throw new OkxApiException("取消订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Ticker数据
     */
    private Ticker parseTicker(JSONObject tickerData, String symbol, String channel){
        Ticker ticker = new Ticker();
        ticker.setSymbol(symbol);
        ticker.setChannel(channel);
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
        if(tickerData.containsKey("open24h") && tickerData.containsKey("last")){
            BigDecimal open = new BigDecimal(tickerData.getString("open24h"));
            BigDecimal last = new BigDecimal(tickerData.getString("last"));
            if(open.compareTo(BigDecimal.ZERO) > 0){
                ticker.setPriceChange(last.subtract(open));
                ticker.setPriceChangePercent(last.subtract(open).divide(open, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")));
            }
        }

        return ticker;
    }

    /**
     * 解析K线数据
     */
    private Candlestick parseCandlestick(JSONArray candleData, String symbol, String channel){
        Candlestick candlestick = new Candlestick();
        candlestick.setSymbol(symbol);
        candlestick.setChannel(channel);

        // 解析时间戳
        long timestamp = Long.parseLong(candleData.getString(0));
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        candlestick.setOpenTime(time);

        candlestick.setOpen(new BigDecimal(candleData.getString(1)));
        candlestick.setHigh(new BigDecimal(candleData.getString(2)));
        candlestick.setLow(new BigDecimal(candleData.getString(3)));
        candlestick.setClose(new BigDecimal(candleData.getString(4)));
        candlestick.setState(Integer.parseInt(candleData.getString(5)));

        return candlestick;
    }

    /**
     * 解析账户余额数据
     */
    private AccountBalance parseAccountBalance(JSONObject balanceData){
        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setTotalEquity(new BigDecimal(balanceData.getString("totalEq")));

        List<AssetBalance> assetBalances = new ArrayList<>();
        JSONArray detailsArray = balanceData.getJSONArray("details");

        for(int i = 0;i < detailsArray.size();i++){
            JSONObject detail = detailsArray.getJSONObject(i);
            AssetBalance assetBalance = new AssetBalance();
            assetBalance.setAsset(detail.getString("ccy"));
            assetBalance.setAvailable(new BigDecimal(detail.getString("availEq")));
            assetBalance.setFrozen(new BigDecimal(detail.getString("frozenBal")));
            assetBalance.setTotal(new BigDecimal(detail.getString("eq")));
            assetBalance.setUsdValue(new BigDecimal(detail.getString("eqUsd")));
            assetBalances.add(assetBalance);
        }

        accountBalance.setAvailableBalance(assetBalances.stream().map(x -> x.getAvailable().divide(x.getTotal(), 8, RoundingMode.HALF_UP).multiply(x.getUsdValue())).reduce(BigDecimal :: add).orElseGet(() -> BigDecimal.ZERO));
        accountBalance.setFrozenBalance(accountBalance.getTotalEquity().subtract(accountBalance.getAvailableBalance()));
        accountBalance.setAssetBalances(assetBalances);
        return accountBalance;
    }

    /**
     * 解析订单数据
     */
    private Order parseOrder(JSONObject orderData){
        Order order = new Order();
        order.setOrderId(orderData.getString("ordId"));
        order.setClientOrderId(orderData.getString("clOrdId"));
        order.setSymbol(orderData.getString("instId"));

        if(orderData.containsKey("px") && ! orderData.getString("px").isEmpty()){
            order.setPrice(new BigDecimal(orderData.getString("px")));
        }

        order.setOrigQty(new BigDecimal(orderData.getString("sz")));

        if(orderData.containsKey("accFillSz")){
            order.setExecutedQty(new BigDecimal(orderData.getString("accFillSz")));
        }

        if(orderData.containsKey("fillPx") && ! orderData.getString("fillPx").isEmpty()){
            BigDecimal fillPrice = new BigDecimal(orderData.getString("fillPx"));
            BigDecimal fillSize = new BigDecimal(orderData.getString("accFillSz"));
            order.setCummulativeQuoteQty(fillPrice.multiply(fillSize));
        }

        order.setStatus(mapOrderStatus(orderData.getString("state")));
        order.setType(mapOrderType(orderData.getString("ordType")));
        order.setSide(orderData.getString("side").toUpperCase());

        if(orderData.containsKey("fee") && ! orderData.getString("fee").isEmpty()){
            order.setFee(new BigDecimal(orderData.getString("fee")));
        }

        if(orderData.containsKey("feeCcy")){
            order.setFeeCurrency(orderData.getString("feeCcy"));
        }

        // 解析时间戳
        if(orderData.containsKey("cTime")){
            long createTime = Long.parseLong(orderData.getString("cTime"));
            order.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(createTime), ZoneId.systemDefault()));
        }

        if(orderData.containsKey("uTime")){
            long updateTime = Long.parseLong(orderData.getString("uTime"));
            order.setUpdateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(updateTime), ZoneId.systemDefault()));
        }

        return order;
    }

    /**
     * 格式化K线间隔
     */
    private String formatInterval(String interval){
        return interval;
    }

    /**
     * 映射OKX订单状态到标准状态
     */
    private String mapOrderStatus(String okxStatus){
        switch(okxStatus){
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
    private String mapToOkxOrderStatus(String standardStatus){
        switch(standardStatus.toUpperCase()){
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
    private String mapOrderType(String okxType){
        switch(okxType){
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
    private String mapToOkxOrderType(String standardType){
        switch(standardType.toUpperCase()){
            case "LIMIT":
                return "limit";
            case "MARKET":
                return "market";
            default:
                return standardType.toLowerCase();
        }
    }

    @Override
    public boolean unsubscribeTicker(String symbol){
        try{
            // 检查是否已订阅
            if(! subscribedSymbols.contains(symbol)){
                log.debug("币种 {} 未订阅，无需取消", symbol);
                return true;
            }

            String channel = "tickers";
            String key = channel + "_" + symbol;

            log.info("取消订阅行情数据，交易对: {}", symbol);
            webSocketUtil.unsubscribePublicTopic(channel, symbol);

            // 移除订阅标记
            subscribedSymbols.remove(symbol);

            // 清理相关Future
            tickerFutures.remove(key);
            return true;
        }catch(Exception e){
            log.error("取消订阅行情数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean unsubscribeKlineData(String symbol, String interval){
        try{
            // 构建标记价格K线的正确频道名
            String channel = "mark-price-candle" + interval;
            // 使用与getKlineData方法相同的键格式
            String key = channel + "_" + symbol + "_" + interval;

            log.info("取消订阅K线数据，交易对: {}, 间隔: {}", symbol, interval);

            // 创建取消订阅参数
            JSONObject arg = new JSONObject();
            arg.put("channel", channel);
            arg.put("instId", symbol);

            webSocketUtil.unsubscribePublicTopicWithArgs(arg, symbol);

            // 清理相关Future - 使用正确的键格式
            klineFutures.remove(key);
            return true;
        }catch(Exception e){
            log.error("取消订阅K线数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查币种是否已订阅
     *
     * @param symbol 交易对符号
     * @return 是否已订阅
     */
    public boolean isSymbolSubscribed(String symbol){
        return subscribedSymbols.contains(symbol);
    }

    /**
     * 获取所有已订阅的币种
     *
     * @return 已订阅币种集合
     */
    public Set<String> getSubscribedSymbols(){
        return new HashSet<>(subscribedSymbols);
    }
}
