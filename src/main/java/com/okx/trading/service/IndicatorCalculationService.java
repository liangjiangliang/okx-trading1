//package com.okx.trading.service;
//
//import com.okx.trading.model.market.Candlestick;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * 指标计算服务接口
// * 负责K线数据的技术指标计算、缓存和订阅
// */
//public interface IndicatorCalculationService {
//
//    /**
//     * 启动指标计算服务，初始化并执行首次计算
//     * 由CommandLineRunner在应用启动时调用
//     */
//    void startService();
//
//    /**
//     * 停止指标计算服务，并清理资源
//     */
//    void stopService();
//
//    /**
//     * 检查K线数据的连续性
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 是否连续
//     */
//    void checkKlineContinuityAndFill(List<Candlestick> klineList);
//
//    /**
//     * 计算指定交易对和时间间隔的技术指标
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 计算是否成功
//     */
//    boolean calculateIndicators(String symbol, String interval);
//
//    /**
//     * 批量计算多个交易对和时间间隔的技术指标
//     *
//     * @param symbolIntervalMap 交易对和时间间隔的映射
//     * @return 计算成功的交易对和时间间隔数量
//     */
//    int batchCalculateIndicators(Map<String, List<String>> symbolIntervalMap);
//
//    /**
//     * 获取指定交易对和时间间隔的MACD指标值
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return MACD指标值的Map，包含macdLine, signalLine和histogram
//     */
//    Map<String, Object> getMACDIndicator(String symbol, String interval);
//
//    /**
//     * 获取指定交易对和时间间隔的RSI指标值
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @param period RSI周期，常用值为14
//     * @return RSI指标值
//     */
//    List<Double> getRSIIndicator(String symbol, String interval, int period);
//
//    /**
//     * 获取指定交易对和时间间隔的KDJ指标值
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return KDJ指标值的Map，包含K, D和J值
//     */
//    Map<String, Object> getKDJIndicator(String symbol, String interval);
//
//    /**
//     * 获取指定交易对和时间间隔的布林带指标值
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 布林带指标值的Map，包含上轨、中轨和下轨
//     */
//    Map<String, Object> getBollingerBandsIndicator(String symbol, String interval);
//
//    /**
//     * 订阅指定交易对和时间间隔的指标计算
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 订阅是否成功
//     */
//    boolean subscribeIndicatorCalculation(String symbol, String interval);
//
//    /**
//     * 取消订阅指定交易对和时间间隔的指标计算
//     *
//     * @param symbol 交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 取消订阅是否成功
//     */
//    boolean unsubscribeIndicatorCalculation(String symbol, String interval);
//
//    /**
//     * 获取所有已订阅的指标计算任务
//     *
//     * @return 已订阅的指标计算任务，Map的key为交易对，value为时间间隔列表
//     */
//    Map<String, List<String>> getAllSubscribedIndicators();
//}
