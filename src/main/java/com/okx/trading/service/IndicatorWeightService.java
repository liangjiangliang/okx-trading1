package com.okx.trading.service;

import com.okx.trading.model.dto.IndicatorWeightConfig;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 指标权重服务接口
 * 
 * @author OKX Trading System
 * @since 2025-01-26
 */
public interface IndicatorWeightService {

    /**
     * 加载权重配置
     * @return 权重配置对象
     */
    IndicatorWeightConfig loadWeightConfig();

    /**
     * 获取当前权重配置
     * @return 权重配置对象
     */
    IndicatorWeightConfig getCurrentConfig();

    /**
     * 重新加载权重配置
     * @return 是否加载成功
     */
    boolean reloadConfig();

    /**
     * 获取指标的绝对权重
     * @param indicatorName 指标名称
     * @return 绝对权重值
     */
    double getIndicatorWeight(String indicatorName);

    /**
     * 计算综合评分（基于所有指标）
     * @param indicatorValues 指标值映射
     * @param indicatorScores 指标评分映射（8分制）
     * @return 综合评分（10分制）
     */
    BigDecimal calculateComprehensiveScore(Map<String, BigDecimal> indicatorValues, 
                                         Map<String, Double> indicatorScores);

    /**
     * 获取维度评分详情
     * @param indicatorValues 指标值映射
     * @param indicatorScores 指标评分映射
     * @return 各维度评分详情
     */
    Map<String, Object> getDimensionScoreDetails(Map<String, BigDecimal> indicatorValues,
                                                Map<String, Double> indicatorScores);

    /**
     * 验证权重配置的有效性
     * @param config 权重配置
     * @return 验证结果
     */
    boolean validateConfig(IndicatorWeightConfig config);

    /**
     * 获取权重配置统计信息
     * @return 统计信息
     */
    Map<String, Object> getConfigStatistics();
} 