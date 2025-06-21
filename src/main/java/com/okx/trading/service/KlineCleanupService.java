//package com.okx.trading.service;
//
///**
// * K线数据清理服务
// * 负责定期清理Redis中的K线数据，防止内存占用过大
// */
//public interface KlineCleanupService {
//
//    /**
//     * 启动清理线程
//     */
//    void startCleanupThread();
//
//
//
//    void initCleanupTask();
//    /**
//     * 停止清理线程
//     */
//    void stopCleanupThread();
//
//    /**
//     * 执行K线数据清理
//     * 清理所有已订阅的交易对和时间间隔的K线数据
//     */
//    void cleanupKlineData();
//
//
//    void destroyCleanupTask();
//    /**
//     * 执行指定交易对和时间间隔的K线数据清理
//     *
//     * @param symbol 交易对，如 BTC-USDT
//     * @param interval 时间间隔，如 1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
//     * @return 已清理的数据条数
//     */
//    int cleanupKlineData(String symbol, String interval);
//
//    /**
//     * 设置K线数据清理的时间间隔（单位：秒）
//     *
//     * @param intervalSeconds 清理时间间隔（单位：秒），最小为10秒
//     */
//    void setCleanupInterval(int intervalSeconds);
//
//    /**
//     * 设置每个K线键保留的最大数据条数
//     *
//     * @param maxCount 最大数据条数，最小为100
//     */
//    void setMaxKlineCount(int maxCount);
//
//    /**
//     * 获取当前配置的清理时间间隔（单位：秒）
//     *
//     * @return 清理时间间隔（单位：秒）
//     */
//    int getCleanupInterval();
//
//    /**
//     * 获取当前配置的每个K线键保留的最大数据条数
//     *
//     * @return 最大数据条数
//     */
//    int getMaxKlineCount();
//}
