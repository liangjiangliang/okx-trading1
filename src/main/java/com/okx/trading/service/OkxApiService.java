package com.okx.trading.service;

import java.util.List;

import com.okx.trading.model.account.AccountBalance;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;

/**
 * OKX API服务接口
 * 定义与OKX交易所API交互的方法
 */
public interface OkxApiService {

    /**
     * 获取K线数据
     *
     * @param symbol    交易对，如BTC-USDT
     * @param interval  K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit     获取数据条数，最大为1000
     * @return K线数据列表
     */
    List<Candlestick> getKlineData(String symbol, String interval, Integer limit);

    /**
     * 获取最新行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 行情数据
     */
    Ticker getTicker(String symbol);

    /**
     * 获取账户余额
     *
     * @return 账户余额信息
     */
    AccountBalance getAccountBalance();

    /**
     * 获取模拟账户余额
     *
     * @return 模拟账户余额信息
     */
    AccountBalance getSimulatedAccountBalance();

    /**
     * 获取订单列表
     *
     * @param symbol    交易对，如BTC-USDT
     * @param status    订单状态：live, partially_filled, filled, cancelled
     * @param limit     获取数据条数，最大为100
     * @return 订单列表
     */
    List<Order> getOrders(String symbol, String status, Integer limit);

    /**
     * 创建现货订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    Order createSpotOrder(OrderRequest orderRequest);

    /**
     * 创建合约订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    Order createFuturesOrder(OrderRequest orderRequest);

    /**
     * 取消订单
     *
     * @param symbol  交易对，如BTC-USDT
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean cancelOrder(String symbol, String orderId);

    /**
     * 取消订阅行情信息
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 是否成功取消订阅
     */
    boolean unsubscribeTicker(String symbol);

    /**
     * 取消订阅K线数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 是否成功取消订阅
     */
    boolean unsubscribeKlineData(String symbol, String interval);
}
