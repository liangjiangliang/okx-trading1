package com.okx.trading.service;

import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * K线缓存服务接口
 * 负责管理K线数据的缓存、订阅和查询
 */
public interface KlineCacheService {

    /**
     * 订阅指定交易对和时间间隔的K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 订阅是否成功
     */
    boolean subscribeKline(String symbol, String interval);

    /**
     * 批量订阅K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param intervals K线时间间隔列表，如 ["1m", "5m", "15m"]
     * @return 订阅成功的间隔列表
     */
    List<String> batchSubscribeKline(String symbol, List<String> intervals);

    /**
     * 取消订阅指定交易对和时间间隔的K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 取消订阅是否成功
     */
    boolean unsubscribeKline(String symbol, String interval);

    /**
     * 批量取消订阅K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param intervals K线时间间隔列表，如 ["1m", "5m", "15m"]
     * @return 取消订阅成功的间隔列表
     */
    List<String> batchUnsubscribeKline(String symbol, List<String> intervals);

    /**
     * 获取所有已订阅的K线信息
     *
     * @return Map, key为symbol, value为该symbol订阅的时间间隔列表
     */
    Set<String> getAllSubscribedKlines();

    /**
     * 获取指定交易对的所有已订阅K线间隔
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @return 已订阅的K线间隔列表
     */
    Set<String> getSubscribedIntervals(String symbol);

    List<CandlestickEntity> getKlineData(String symbol, String interval, int klineLimit);
    /**
     * 将K线数据缓存到Redis
     *
     * @param candlestick K线数据对象
     * @return 缓存是否成功
     */
    boolean cacheKlineData(Candlestick candlestick);

    /**
     * 批量缓存K线数据到Redis
     *
     * @param candlesticks K线数据对象列表
     * @return 成功缓存的条数
     */
    int batchCacheKlineData(List<Candlestick> candlesticks);

    /**
     * 获取指定交易对和时间间隔的最新K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit 获取条数，默认1
     * @return K线数据列表
     */
    List<CandlestickEntity> getLatestKlineData(String symbol, String interval, int limit);

    /**
     * 获取指定交易对和时间间隔的历史K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime 开始时间戳(毫秒)
     * @param endTime 结束时间戳(毫秒)
     * @param limit 获取条数
     * @return K线数据列表
     */
    List<CandlestickEntity> getHistoricalKlineData(String symbol, String interval, Long startTime, Long endTime, Integer limit);

    /**
     * 初始化默认K线订阅
     * 为默认交易对和时间间隔创建订阅
     */
    void initDefaultKlineSubscriptions();

    /**
     * 检查K线订阅状态
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 是否已订阅
     */
    boolean isKlineSubscribed(String symbol, String interval);

    /**
     * 清除指定交易对和时间间隔的K线缓存
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 清除是否成功
     */
    boolean clearKlineCache(String symbol, String interval);

}
