//package com.okx.trading.service;
//
//import com.okx.trading.model.dto.IndicatorValueDTO;
//import com.okx.trading.model.entity.CandlestickEntity;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * 技术指标服务接口
// * 负责计算各种技术指标值
// */
//public interface TechnicalIndicatorService {
//
//    /**
//     * 计算指定交易对和时间间隔的最新K线技术指标值
//     *
//     * @param symbol    交易对
//     * @param interval  时间间隔
//     * @param indicatorType 指标类型
//     * @param params    指标参数
//     * @return 技术指标值DTO
//     */
//    IndicatorValueDTO calculateLastIndicator(String symbol, String interval, String indicatorType, String params);
//
//    /**
//     * 批量计算多个指标的值
//     *
//     * @param symbol    交易对
//     * @param interval  时间间隔
//     * @param indicators 指标类型和参数的映射
//     * @return 指标类型到值的映射
//     */
//    Map<String, IndicatorValueDTO> calculateMultipleIndicators(String symbol, String interval, Map<String, String> indicators);
//
//    /**
//     * 计算指定K线数据的技术指标值
//     *
//     * @param candlesticks K线数据列表
//     * @param indicatorType 指标类型
//     * @param params     指标参数
//     * @return 技术指标值DTO
//     */
//    IndicatorValueDTO calculateIndicator(List<CandlestickEntity> candlesticks, String indicatorType, String params);
//
//    /**
//     * 获取所有支持的技术指标类型
//     *
//     * @return 支持的技术指标类型列表
//     */
//    List<String> getSupportedIndicators();
//
//    /**
//     * 获取指定技术指标的参数说明
//     *
//     * @param indicatorType 指标类型
//     * @return 参数说明
//     */
//    String getIndicatorParamsDescription(String indicatorType);
//}
