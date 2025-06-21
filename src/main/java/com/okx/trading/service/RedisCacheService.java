package com.okx.trading.service;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Redis缓存服务接口
 * 用于实时价格数据的缓存操作
 */
public interface RedisCacheService {

    /**
     * 更新币种实时价格
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param price 最新价格
     */
    void updateCoinPrice(String symbol, BigDecimal price);


    void updateCandlestick(  Candlestick candlestick);
    /**
     * 获取所有币种的实时价格
     *
     * @return 所有币种的实时价格Map，key为币种，value为价格
     */
    Map<String, BigDecimal> getAllCoinPrices();

    /**
     * 获取指定币种的实时价格
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @return 实时价格，如果不存在返回null
     */
    BigDecimal getCoinPrice(String symbol);

    /**
     * 获取所有订阅的币种
     *
     * @return 订阅的币种Set
     */
    Set<String> getSubscribedCoins();

    /**
     * 添加订阅币种
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @return 添加是否成功
     */
    boolean addSubscribedCoin(String symbol);

    /**
     * 移除订阅币种
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @return 移除是否成功
     */
    boolean removeSubscribedCoin(String symbol);

    /**
     * 初始化默认订阅币种
     * 仅当订阅列表为空时添加默认币种
     */
    void initDefaultSubscribedCoins();

    /**
     * 设置缓存数据
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeoutMinutes 过期时间（分钟）
     */
    void setCache(String key, Object value, long timeoutMinutes);

    /**
     * 获取缓存数据
     *
     * @param key 缓存键
     * @param clazz 返回值类型
     * @return 缓存值，如果不存在或已过期返回null
     */
    <T> T getCache(String key, Class<T> clazz);

    /**
     * 删除缓存数据
     *
     * @param key 缓存键
     * @return 删除是否成功
     */
    boolean deleteCache(String key);

    /**
     * 批量添加K线数据到Redis Sorted Set
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线间隔，如 1m, 5m, 1H, 1D
     * @param candlesticks K线数据列表
     * @param timeoutMinutes 过期时间（分钟）
     */
    void batchAddKlineToSortedSet(String symbol, String interval, java.util.List<com.okx.trading.model.entity.CandlestickEntity> candlesticks, long timeoutMinutes);

    /**
     * 从Redis Sorted Set获取K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线间隔，如 1m, 5m, 1H, 1D
     * @param startScore 开始时间戳
     * @param endScore 结束时间戳
     * @return K线数据JSON字符串列表
     */
    Set<String> getKlineFromSortedSet(String symbol, String interval, double startScore, double endScore);

    /**
     * 清除指定符号和间隔的K线数据
     *
     * @param symbol 交易对符号，如 BTC-USDT
     * @param interval K线间隔，如 1m, 5m, 1H, 1D
     * @return 删除是否成功
     */
    boolean clearKlineSortedSet(String symbol, String interval);
}
