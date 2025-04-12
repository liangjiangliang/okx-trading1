package com.okx.trading.service;

import java.util.Set;

/**
 * 价格更新服务接口
 * 用于在独立线程中更新缓存价格
 */
public interface PriceUpdateService {
    
    /**
     * 启动价格更新线程
     */
    void startPriceUpdateThread();
    
    /**
     * 停止价格更新线程
     */
    void stopPriceUpdateThread();
    
    /**
     * 获取当前订阅的币种列表
     *
     * @return 订阅的币种列表
     */
    Set<String> getSubscribedCoins();
    
    /**
     * 添加订阅币种
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 添加是否成功
     */
    boolean addSubscribedCoin(String symbol);
    
    /**
     * 移除订阅币种
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 移除是否成功
     */
    boolean removeSubscribedCoin(String symbol);
    
    /**
     * 强制更新所有订阅币种的价格
     * 用于WebSocket连接重建后的价格恢复
     */
    void forceUpdateAllPrices();
} 