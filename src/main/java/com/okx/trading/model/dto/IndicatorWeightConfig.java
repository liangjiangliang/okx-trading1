package com.okx.trading.model.dto;

import lombok.Data;
import java.util.Map;

/**
 * 指标权重配置数据模型
 * 
 * @author OKX Trading System
 * @since 2025-01-26
 */
@Data
public class IndicatorWeightConfig {

    /**
     * 主要维度权重
     */
    private Map<String, Double> dimensions;
    
    /**
     * 各维度下的指标权重
     */
    private Map<String, Map<String, IndicatorWeight>> indicators;
    
    /**
     * 特殊处理规则
     */
    private Map<String, SpecialRule> specialRules;
    
    /**
     * 配置信息
     */
    private ConfigInfo config;

    /**
     * 单个指标权重信息
     */
    @Data
    public static class IndicatorWeight {
        /**
         * 权重值（在维度内的相对权重）
         */
        private Double weight;
        
        /**
         * 显示名称
         */
        private String displayName;
        
        /**
         * 指标类型：POSITIVE, NEGATIVE, NEUTRAL
         */
        private String type;
        
        /**
         * 获取绝对权重（维度权重 * 指标权重）
         */
        public double getAbsoluteWeight(double dimensionWeight) {
            return weight * dimensionWeight;
        }
    }

    /**
     * 特殊处理规则
     */
    @Data
    public static class SpecialRule {
        /**
         * 最佳范围（适用于NEUTRAL类型指标）
         */
        private double[] optimalRange;
        
        /**
         * 最佳值（适用于NEUTRAL类型指标）
         */
        private Double optimalValue;
        
        /**
         * 容忍范围
         */
        private Double tolerance;
        
        /**
         * 惩罚系数
         */
        private Double penaltyFactor;
    }

    /**
     * 配置信息
     */
    @Data
    public static class ConfigInfo {
        /**
         * 版本号
         */
        private String version;
        
        /**
         * 最后更新时间
         */
        private String lastUpdated;
        
        /**
         * 描述
         */
        private String description;
        
        /**
         * 作者
         */
        private String author;
    }
    
    /**
     * 获取指标的绝对权重
     * @param dimension 维度名称
     * @param indicatorName 指标名称
     * @return 绝对权重值
     */
    public double getIndicatorAbsoluteWeight(String dimension, String indicatorName) {
        Double dimensionWeight = dimensions.get(dimension);
        if (dimensionWeight == null) {
            return 0.0;
        }
        
        Map<String, IndicatorWeight> dimensionIndicators = indicators.get(dimension);
        if (dimensionIndicators == null) {
            return 0.0;
        }
        
        IndicatorWeight indicatorWeight = dimensionIndicators.get(indicatorName);
        if (indicatorWeight == null) {
            return 0.0;
        }
        
        return indicatorWeight.getAbsoluteWeight(dimensionWeight);
    }
    
    /**
     * 获取指标的特殊处理规则
     * @param indicatorName 指标名称
     * @return 特殊规则，如果没有则返回null
     */
    public SpecialRule getSpecialRule(String indicatorName) {
        return specialRules != null ? specialRules.get(indicatorName) : null;
    }
    
    /**
     * 获取指标的类型
     * @param dimension 维度名称
     * @param indicatorName 指标名称
     * @return 指标类型
     */
    public String getIndicatorType(String dimension, String indicatorName) {
        Map<String, IndicatorWeight> dimensionIndicators = indicators.get(dimension);
        if (dimensionIndicators == null) {
            return null;
        }
        
        IndicatorWeight indicatorWeight = dimensionIndicators.get(indicatorName);
        return indicatorWeight != null ? indicatorWeight.getType() : null;
    }
} 