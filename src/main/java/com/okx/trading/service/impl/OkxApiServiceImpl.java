package com.okx.trading.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okx.trading.config.OkxApiConfig;
import com.okx.trading.exception.OkxApiException;
import com.okx.trading.model.account.AccountBalance;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.util.HttpUtil;
import com.okx.trading.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * OKX API REST服务实现类
 * 实现与OKX交易所REST API的实际交互
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"okx.api.use-mock-data", "okx.api.connection-mode"}, 
                       havingValue = "false,REST", 
                       matchIfMissing = false)
public class OkxApiServiceImpl implements OkxApiService {

    private final OkHttpClient okHttpClient;
    private final OkxApiConfig okxApiConfig;

    private static final String API_PATH = "/api/v5";
    private static final String MARKET_PATH = API_PATH + "/market";
    private static final String ACCOUNT_PATH = API_PATH + "/account";
    private static final String TRADE_PATH = API_PATH + "/trade";

    /**
     * 获取K线数据
     *
     * @param symbol    交易对，如BTC-USDT
     * @param interval  K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit     获取数据条数，最大为1000
     * @return K线数据列表
     */
    @Override
    public List<Candlestick> getKlineData(String symbol, String interval, Integer limit) {
        try {
            String url = okxApiConfig.getBaseUrl() + MARKET_PATH + "/candles";
            url = url + "?instId=" + symbol + "&bar=" + interval;
            if (limit != null && limit > 0) {
                url = url + "&limit=" + limit;
            }

            String response = HttpUtil.get(okHttpClient, url, null);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (!"0".equals(jsonResponse.getString("code"))) {
                throw new OkxApiException(jsonResponse.getIntValue("code"), jsonResponse.getString("msg"));
            }

            JSONArray dataArray = jsonResponse.getJSONArray("data");
            List<Candlestick> result = new ArrayList<>();

            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray item = dataArray.getJSONArray(i);

                // OKX API返回格式：[时间戳, 开盘价, 最高价, 最低价, 收盘价, 成交量, 成交额]
                Candlestick candlestick = new Candlestick();
                candlestick.setSymbol(symbol);
                candlestick.setInterval(interval);

                // 转换时间戳为LocalDateTime
                long timestamp = item.getLongValue(0);
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestamp),
                        ZoneId.systemDefault());

                candlestick.setOpenTime(dateTime);
                candlestick.setOpen(new BigDecimal(item.getString(1)));
                candlestick.setHigh(new BigDecimal(item.getString(2)));
                candlestick.setLow(new BigDecimal(item.getString(3)));
                candlestick.setClose(new BigDecimal(item.getString(4)));
                candlestick.setVolume(new BigDecimal(item.getString(5)));
                candlestick.setQuoteVolume(new BigDecimal(item.getString(6)));

                // 收盘时间根据interval计算
                candlestick.setCloseTime(dateTime); // 简化处理，实际应根据interval计算

                // 成交笔数，OKX API可能没提供，设为0
                candlestick.setTrades(0L);

                result.add(candlestick);
            }

            return result;
        } catch (OkxApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取K线数据异常", e);
            throw new OkxApiException("获取K线数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取最新行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 行情数据
     */
    @Override
    public Ticker getTicker(String symbol) {
        try {
            String url = okxApiConfig.getBaseUrl() + MARKET_PATH + "/ticker";
            url = url + "?instId=" + symbol;

            String response = HttpUtil.get(okHttpClient, url, null);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (!"0".equals(jsonResponse.getString("code"))) {
                throw new OkxApiException(jsonResponse.getIntValue("code"), jsonResponse.getString("msg"));
            }

            JSONObject data = jsonResponse.getJSONArray("data").getJSONObject(0);

            Ticker ticker = new Ticker();
            ticker.setSymbol(symbol);
            ticker.setLastPrice(new BigDecimal(data.getString("last")));

            // 计算24小时价格变动
            BigDecimal open24h = new BigDecimal(data.getString("open24h"));
            BigDecimal priceChange = ticker.getLastPrice().subtract(open24h);
            ticker.setPriceChange(priceChange);

            // 计算24小时价格变动百分比
            if (open24h.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changePercent = priceChange.multiply(new BigDecimal("100")).divide(open24h, 2, BigDecimal.ROUND_HALF_UP);
                ticker.setPriceChangePercent(changePercent);
            } else {
                ticker.setPriceChangePercent(BigDecimal.ZERO);
            }

            ticker.setHighPrice(new BigDecimal(data.getString("high24h")));
            ticker.setLowPrice(new BigDecimal(data.getString("low24h")));
            ticker.setVolume(new BigDecimal(data.getString("vol24h")));
            ticker.setQuoteVolume(new BigDecimal(data.getString("volCcy24h")));

            ticker.setBidPrice(new BigDecimal(data.getString("bidPx")));
            ticker.setBidQty(new BigDecimal(data.getString("bidSz")));
            ticker.setAskPrice(new BigDecimal(data.getString("askPx")));
            ticker.setAskQty(new BigDecimal(data.getString("askSz")));

            // 转换时间戳为LocalDateTime
            long timestamp = data.getLongValue("ts");
            ticker.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()));

            return ticker;
        } catch (OkxApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取行情数据异常", e);
            throw new OkxApiException("获取行情数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取账户余额
     *
     * @return 账户余额信息
     */
    @Override
    public AccountBalance getAccountBalance() {
        return getBalance(false);
    }

    /**
     * 获取模拟账户余额
     *
     * @return 模拟账户余额信息
     */
    @Override
    public AccountBalance getSimulatedAccountBalance() {
        return getBalance(true);
    }

    /**
     * 获取余额的通用方法
     *
     * @param isSimulated 是否为模拟账户
     * @return 账户余额信息
     */
    private AccountBalance getBalance(boolean isSimulated) {
        try {
            String url = okxApiConfig.getBaseUrl() + ACCOUNT_PATH + "/balance";
            String timestamp = SignatureUtil.getIsoTimestamp();
            String method = "GET";
            String requestPath = ACCOUNT_PATH + "/balance";

            Map<String, String> headers = buildHeaders(timestamp, method, requestPath, null, isSimulated);

            String response = HttpUtil.get(okHttpClient, url, headers);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (!"0".equals(jsonResponse.getString("code"))) {
                throw new OkxApiException(jsonResponse.getIntValue("code"), jsonResponse.getString("msg"));
            }

            JSONObject data = jsonResponse.getJSONArray("data").getJSONObject(0);

            AccountBalance accountBalance = new AccountBalance();
            accountBalance.setTotalEquity(new BigDecimal(data.getString("totalEq")));
            accountBalance.setAccountType(isSimulated ? 1 : 0);
            accountBalance.setAccountId(data.getString("uid"));



            // 处理各币种余额
            JSONArray detailsArray = data.getJSONArray("details");
            List<AccountBalance.AssetBalance> assetBalances = new ArrayList<>();

            for (int i = 0; i < detailsArray.size(); i++) {
                JSONObject detail = detailsArray.getJSONObject(i);

                AccountBalance.AssetBalance assetBalance = new AccountBalance.AssetBalance();
                assetBalance.setAsset(detail.getString("ccy"));
                assetBalance.setAvailable(new BigDecimal(detail.getString("availBal")));
                assetBalance.setFrozen(new BigDecimal(detail.getString("frozenBal")));

                // 计算总余额
                assetBalance.setTotal(assetBalance.getAvailable().add(assetBalance.getFrozen()));

                // 美元价值
                assetBalance.setUsdValue(new BigDecimal(detail.getString("eqUsd")));

                assetBalances.add(assetBalance);
            }


            accountBalance.setAssetBalances(assetBalances);
            // 计算可用和冻结余额，OKX API可能返回方式不同
            accountBalance.setTotalEquity(assetBalances.stream().map(AccountBalance.AssetBalance :: getUsdValue).reduce(BigDecimal::add).orElseGet(()->BigDecimal.ZERO));
            accountBalance.setAvailableBalance(assetBalances.stream().map(bal->bal.getUsdValue().divide(bal.getTotal(),8, BigDecimal.ROUND_HALF_UP).multiply(bal.getAvailable())).reduce(BigDecimal::add).orElseGet(()->BigDecimal.ZERO));
            accountBalance.setFrozenBalance(accountBalance.getTotalEquity().subtract(accountBalance.getAvailableBalance()));

            return accountBalance;
        } catch (OkxApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取账户余额异常", e);
            throw new OkxApiException("获取账户余额失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取订单列表
     *
     * @param symbol    交易对，如BTC-USDT
     * @param status    订单状态：live, partially_filled, filled, cancelled
     * @param limit     获取数据条数，最大为100
     * @return 订单列表
     */
    @Override
    public List<Order> getOrders(String symbol, String status, Integer limit) {
        try {
            String url = okxApiConfig.getBaseUrl() + TRADE_PATH + "/orders-history";
            url = url + "?instId=" + symbol;

            if (status != null && !status.isEmpty()) {
                url = url + "&state=" + status;
            }

            if (limit != null && limit > 0) {
                url = url + "&limit=" + limit;
            }

            String timestamp = SignatureUtil.getIsoTimestamp();
            String method = "GET";
            String requestPath = TRADE_PATH + "/orders-history" + "?instId=" + symbol;

            if (status != null && !status.isEmpty()) {
                requestPath = requestPath + "&state=" + status;
            }

            if (limit != null && limit > 0) {
                requestPath = requestPath + "&limit=" + limit;
            }

            Map<String, String> headers = buildHeaders(timestamp, method, requestPath, null, false);

            String response = HttpUtil.get(okHttpClient, url, headers);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (!"0".equals(jsonResponse.getString("code"))) {
                throw new OkxApiException(jsonResponse.getIntValue("code"), jsonResponse.getString("msg"));
            }

            JSONArray dataArray = jsonResponse.getJSONArray("data");
            List<Order> result = new ArrayList<>();

            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject item = dataArray.getJSONObject(i);

                Order order = new Order();
                order.setOrderId(item.getString("ordId"));
                order.setClientOrderId(item.getString("clOrdId"));
                order.setSymbol(item.getString("instId"));
                order.setPrice(new BigDecimal(item.getString("px")));
                order.setOrigQty(new BigDecimal(item.getString("sz")));
                order.setExecutedQty(new BigDecimal(item.getString("accFillSz")));

                // 成交金额需要计算
                BigDecimal avgPx = new BigDecimal(item.getString("avgPx"));
                order.setCummulativeQuoteQty(order.getExecutedQty().multiply(avgPx));

                // 状态映射，OKX可能使用不同的状态码
                String okxStatus = item.getString("state");
                order.setStatus(mapOrderStatus(okxStatus));

                // 订单类型映射
                String okxType = item.getString("ordType");
                order.setType(mapOrderType(okxType));

                // 交易方向映射
                String okxSide = item.getString("side");
                order.setSide(okxSide.toUpperCase());

                // 其他字段
                order.setStopPrice(new BigDecimal(item.getString("slTriggerPx")));
                order.setTriggerPrice(new BigDecimal(item.getString("tpTriggerPx")));
                order.setTimeInForce(item.getString("tgtCcy"));

                // 时间转换
                long cTime = item.getLongValue("cTime");
                order.setCreateTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(cTime),
                        ZoneId.systemDefault()));

                long uTime = item.getLongValue("uTime");
                order.setUpdateTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(uTime),
                        ZoneId.systemDefault()));

                // 设置模拟标志
                order.setSimulated(false);

                // 手续费信息
                order.setFee(new BigDecimal(item.getString("fee")));
                order.setFeeCurrency(item.getString("feeCcy"));

                result.add(order);
            }

            return result;
        } catch (OkxApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取订单列表异常", e);
            throw new OkxApiException("获取订单列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建现货订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    @Override
    public Order createSpotOrder(OrderRequest orderRequest) {
        // 设置交易品种为现货
        return createOrder(orderRequest, "SPOT", orderRequest.getSimulated() != null && orderRequest.getSimulated());
    }

    /**
     * 创建合约订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    @Override
    public Order createFuturesOrder(OrderRequest orderRequest) {
        // 设置交易品种为永续合约
        return createOrder(orderRequest, "SWAP", orderRequest.getSimulated() != null && orderRequest.getSimulated());
    }

    /**
     * 创建订单的通用方法
     *
     * @param orderRequest 订单请求参数
     * @param instType 交易品种：SPOT, SWAP等
     * @param isSimulated 是否为模拟交易
     * @return 创建的订单
     */
    private Order createOrder(OrderRequest orderRequest, String instType, boolean isSimulated) {
        try {
            String url = okxApiConfig.getBaseUrl() + TRADE_PATH + "/order";
            // 按金额下单,按数量下单,限价单,市价单

            JSONObject requestBody = new JSONObject();
            requestBody.put("instId", orderRequest.getSymbol());
            requestBody.put("tdMode", "cash"); // 资金模式，cash为现钞
            requestBody.put("side", orderRequest.getSide().toLowerCase());
            requestBody.put("ordType", mapToOkxOrderType(orderRequest.getType()));
            requestBody.put("sz", orderRequest.getQuantity().toString());

            if ("limit".equals(mapToOkxOrderType(orderRequest.getType()))) {
                requestBody.put("px", orderRequest.getPrice().toString());
            }

            if (orderRequest.getClientOrderId() != null) {
                requestBody.put("clOrdId", orderRequest.getClientOrderId());
            }

            // 设置杠杆倍数（合约交易）
            if ("SWAP".equals(instType) && orderRequest.getLeverage() != null) {
                requestBody.put("lever", orderRequest.getLeverage().toString());
            }

            // 设置订单有效期
//            if (orderRequest.getTimeInForce() != null) {
//                requestBody.put("tgtCcy", mapToOkxTimeInForce(orderRequest.getTimeInForce()));
//            }

            // 设置被动委托
            if (orderRequest.getPostOnly() != null && orderRequest.getPostOnly()) {
                requestBody.put("postOnly", "1");
            }

            String requestBodyStr = requestBody.toJSONString();
            String timestamp = SignatureUtil.getIsoTimestamp();
            String method = "POST";
            String requestPath = TRADE_PATH + "/order";

            Map<String, String> headers = buildHeaders(timestamp, method, requestPath, requestBodyStr, isSimulated);

            String response = HttpUtil.post(okHttpClient, url, headers, requestBodyStr);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (!"0".equals(jsonResponse.getString("code"))) {
                JSONArray data = jsonResponse.getJSONArray("data");
                if(data.size()>0){
                    JSONObject msg = data.getJSONObject(0);
                    throw new OkxApiException(msg.getIntValue("sCode"), String.format("创建订单失败: %s", msg.getString("sMsg")));
                }

            }

            JSONObject data = jsonResponse.getJSONArray("data").getJSONObject(0);

            // 构建返回的订单对象
            Order order = new Order();
            order.setOrderId(data.getString("ordId"));
            order.setClientOrderId(data.getString("clOrdId"));
            order.setSymbol(orderRequest.getSymbol());
            order.setPrice(orderRequest.getPrice());
            order.setOrigQty(orderRequest.getQuantity());
            order.setExecutedQty(BigDecimal.ZERO); // 新订单未成交
            order.setCummulativeQuoteQty(BigDecimal.ZERO); // 新订单未成交
            order.setStatus("NEW");
            order.setType(orderRequest.getType());
            order.setSide(orderRequest.getSide());
            order.setTimeInForce(orderRequest.getTimeInForce());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            order.setSimulated(isSimulated);

            return order;
        } catch (OkxApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建订单异常", e);
            throw new OkxApiException("创建订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消订单
     *
     * @param symbol  交易对，如BTC-USDT
     * @param orderId 订单ID
     * @return 是否成功
     */
    @Override
    public boolean cancelOrder(String symbol, String orderId) {
        try {
            String url = okxApiConfig.getBaseUrl() + TRADE_PATH + "/cancel-order";

            JSONObject requestBody = new JSONObject();
            requestBody.put("instId", symbol);
            requestBody.put("ordId", orderId);

            String requestBodyStr = requestBody.toJSONString();
            String timestamp = SignatureUtil.getIsoTimestamp();
            String method = "POST";
            String requestPath = TRADE_PATH + "/cancel-order";

            Map<String, String> headers = buildHeaders(timestamp, method, requestPath, requestBodyStr, false);

            String response = HttpUtil.post(okHttpClient, url, headers, requestBodyStr);
            JSONObject jsonResponse = JSON.parseObject(response);

            if (!"0".equals(jsonResponse.getString("code"))) {
                throw new OkxApiException(jsonResponse.getIntValue("code"), jsonResponse.getString("msg"));
            }

            JSONObject data = jsonResponse.getJSONArray("data").getJSONObject(0);

            // 判断是否取消成功
            return "0".equals(data.getString("sCode"));
        } catch (OkxApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消订单异常", e);
            throw new OkxApiException("取消订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求头
     *
     * @param timestamp    ISO格式的时间戳
     * @param method       HTTP方法
     * @param requestPath  请求路径
     * @param body         请求体
     * @param isSimulated  是否为模拟交易
     * @return 包含认证信息的请求头
     */
    private Map<String, String> buildHeaders(String timestamp, String method, String requestPath, String body, boolean isSimulated) {
        Map<String, String> headers = new HashMap<>();

        headers.put("OK-ACCESS-KEY", okxApiConfig.getApiKey());
        headers.put("OK-ACCESS-SIGN", SignatureUtil.sign(timestamp, method, requestPath, body, okxApiConfig.getSecretKey()));
        headers.put("OK-ACCESS-TIMESTAMP", timestamp);
        headers.put("OK-ACCESS-PASSPHRASE", okxApiConfig.getPassphrase());
        headers.put("Content-Type", "application/json");

        // 如果是模拟交易，设置模拟交易的标志
        if (isSimulated) {
            headers.put("x-simulated-trading", "1");
        }

        return headers;
    }

    /**
     * 映射OKX订单状态到标准状态
     *
     * @param okxStatus OKX订单状态
     * @return 标准订单状态
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
     * 映射OKX订单类型到标准类型
     *
     * @param okxType OKX订单类型
     * @return 标准订单类型
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
     *
     * @param standardType 标准订单类型
     * @return OKX订单类型
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

    /**
     * 映射标准TimeInForce到OKX TimeInForce
     *
     * @param standardTif 标准TimeInForce
     * @return OKX TimeInForce
     */
    private String mapToOkxTimeInForce(String standardTif) {
        if (standardTif == null) {
            return "gtc";
        }

        switch (standardTif.toUpperCase()) {
            case "GTC":
                return "gtc";
            case "IOC":
                return "ioc";
            case "FOK":
                return "fok";
            default:
                return standardTif.toLowerCase();
        }
    }
}
