package com.okx.trading.service;

/**
 * K线数据更新服务接口
 * 负责定期从交易所API获取K线数据并更新缓存
 */
public interface KlineUpdateService {
    
    /**
     * 启动K线数据更新线程
     */
    void startUpdateThread();
    
    /**
     * 停止K线数据更新线程
     */
    void stopUpdateThread();
    
    /**
     * 手动触发一次K线数据更新
     */
    void updateKlineData();
    
    /**
     * 处理K线订阅事件
     *
     * @param symbol 交易对符号
     * @param interval K线时间间隔
     * @param isSubscribe 是否为订阅事件，false表示取消订阅
     */
    void handleKlineSubscription(String symbol, String interval, boolean isSubscribe);
    
    /**
     * 获取当前更新频率（秒）
     * 
     * @return 更新频率，单位秒
     */
    int getUpdateInterval();
    
    /**
     * 设置更新频率
     * 
     * @param seconds 更新频率，单位秒
     */
    void setUpdateInterval(int seconds);
    
    /**
     * 检查更新线程是否正在运行
     * 
     * @return 是否正在运行
     */
    boolean isRunning();
} 