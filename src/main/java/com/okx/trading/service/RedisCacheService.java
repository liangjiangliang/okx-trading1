package com.okx.trading.service;

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
} 