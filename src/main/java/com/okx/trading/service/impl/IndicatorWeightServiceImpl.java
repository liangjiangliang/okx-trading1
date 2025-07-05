package com.okx.trading.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.okx.trading.model.dto.IndicatorWeightConfig;
import com.okx.trading.service.IndicatorWeightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标权重服务实现类
 * 
 * @author OKX Trading System
 * @since 2025-01-26
 */
@Slf4j
@Service
public class IndicatorWeightServiceImpl implements IndicatorWeightService {

    private static final String CONFIG_FILE = "indicator-weights.yml";
    private final ObjectMapper yamlMapper;
    private volatile IndicatorWeightConfig currentConfig;

    public IndicatorWeightServiceImpl() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @PostConstruct
    public void init() {
        log.info("初始化指标权重服务...");
        if (loadWeightConfig() != null) {
            log.info("指标权重配置加载成功，版本: {}", 
                    currentConfig.getConfig() != null ? currentConfig.getConfig().getVersion() : "unknown");
            logConfigSummary();
        } else {
            log.error("指标权重配置加载失败！");
        }
    }

    @Override
    public IndicatorWeightConfig loadWeightConfig() {
        try {
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
            if (!resource.exists()) {
                log.error("权重配置文件不存在: {}", CONFIG_FILE);
                return null;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                IndicatorWeightConfig config = yamlMapper.readValue(inputStream, IndicatorWeightConfig.class);
                
                if (validateConfig(config)) {
                    this.currentConfig = config;
                    log.info("权重配置加载成功");
                    return config;
                } else {
                    log.error("权重配置验证失败");
                    return null;
                }
            }
        } catch (IOException e) {
            log.error("加载权重配置文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public IndicatorWeightConfig getCurrentConfig() {
        return currentConfig;
    }

    @Override
    public boolean reloadConfig() {
        log.info("重新加载权重配置...");
        IndicatorWeightConfig newConfig = loadWeightConfig();
        if (newConfig != null) {
            this.currentConfig = newConfig;
            log.info("权重配置重新加载成功");
            return true;
        } else {
            log.error("权重配置重新加载失败");
            return false;
        }
    }

    @Override
    public double getIndicatorWeight(String indicatorName) {
        if (currentConfig == null) {
            log.warn("权重配置未加载，返回默认权重0");
            return 0.0;
        }

        // 遍历所有维度查找指标
        for (Map.Entry<String, Map<String, IndicatorWeightConfig.IndicatorWeight>> dimensionEntry 
                : currentConfig.getIndicators().entrySet()) {
            String dimension = dimensionEntry.getKey();
            Map<String, IndicatorWeightConfig.IndicatorWeight> indicators = dimensionEntry.getValue();
            
            if (indicators.containsKey(indicatorName)) {
                return currentConfig.getIndicatorAbsoluteWeight(dimension, indicatorName);
            }
        }

        log.debug("未找到指标 {} 的权重配置", indicatorName);
        return 0.0;
    }

    @Override
    public BigDecimal calculateComprehensiveScore(Map<String, BigDecimal> indicatorValues, 
                                                Map<String, Double> indicatorScores) {
        if (currentConfig == null) {
            log.warn("权重配置未加载，无法计算综合评分");
            return BigDecimal.ZERO;
        }

        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        // 按维度计算加权评分
        for (Map.Entry<String, Double> dimensionEntry : currentConfig.getDimensions().entrySet()) {
            String dimension = dimensionEntry.getKey();
            double dimensionWeight = dimensionEntry.getValue();
            
            Map<String, IndicatorWeightConfig.IndicatorWeight> dimensionIndicators = 
                    currentConfig.getIndicators().get(dimension);
            if (dimensionIndicators == null) {
                continue;
            }

            double dimensionScore = 0.0;
            double dimensionTotalWeight = 0.0;

            // 计算维度内的加权评分
            for (Map.Entry<String, IndicatorWeightConfig.IndicatorWeight> indicatorEntry 
                    : dimensionIndicators.entrySet()) {
                String indicatorName = indicatorEntry.getKey();
                IndicatorWeightConfig.IndicatorWeight indicatorWeight = indicatorEntry.getValue();

                Double score = indicatorScores.get(indicatorName);
                if (score != null) {
                    // 处理特殊规则
                    score = applySpecialRules(indicatorName, indicatorValues.get(indicatorName), score);
                    
                    dimensionScore += score * indicatorWeight.getWeight();
                    dimensionTotalWeight += indicatorWeight.getWeight();
                }
            }

            // 维度评分标准化并加权
            if (dimensionTotalWeight > 0) {
                double normalizedDimensionScore = dimensionScore / dimensionTotalWeight;
                totalWeightedScore += normalizedDimensionScore * dimensionWeight;
                totalWeight += dimensionWeight;
            }
        }

        // 计算最终评分（转换为10分制）
        if (totalWeight > 0) {
            double finalScore = (totalWeightedScore / totalWeight) * 10.0 / 8.0; // 从8分制转为10分制
            return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Map<String, Object> getDimensionScoreDetails(Map<String, BigDecimal> indicatorValues,
                                                       Map<String, Double> indicatorScores) {
        Map<String, Object> details = new LinkedHashMap<>();
        
        if (currentConfig == null) {
            return details;
        }

        for (Map.Entry<String, Double> dimensionEntry : currentConfig.getDimensions().entrySet()) {
            String dimension = dimensionEntry.getKey();
            double dimensionWeight = dimensionEntry.getValue();
            
            Map<String, IndicatorWeightConfig.IndicatorWeight> dimensionIndicators = 
                    currentConfig.getIndicators().get(dimension);
            if (dimensionIndicators == null) {
                continue;
            }

            Map<String, Object> dimensionDetail = new LinkedHashMap<>();
            List<Map<String, Object>> indicatorDetails = new ArrayList<>();
            
            double dimensionScore = 0.0;
            double dimensionTotalWeight = 0.0;
            int validIndicatorCount = 0;

            for (Map.Entry<String, IndicatorWeightConfig.IndicatorWeight> indicatorEntry 
                    : dimensionIndicators.entrySet()) {
                String indicatorName = indicatorEntry.getKey();
                IndicatorWeightConfig.IndicatorWeight indicatorWeight = indicatorEntry.getValue();

                BigDecimal value = indicatorValues.get(indicatorName);
                Double score = indicatorScores.get(indicatorName);
                
                if (score != null) {
                    validIndicatorCount++;
                    
                    // 应用特殊规则
                    double adjustedScore = applySpecialRules(indicatorName, value, score);
                    
                    dimensionScore += adjustedScore * indicatorWeight.getWeight();
                    dimensionTotalWeight += indicatorWeight.getWeight();

                    Map<String, Object> indicatorDetail = new LinkedHashMap<>();
                    indicatorDetail.put("name", indicatorName);
                    indicatorDetail.put("displayName", indicatorWeight.getDisplayName());
                    indicatorDetail.put("value", value);
                    indicatorDetail.put("originalScore", score);
                    indicatorDetail.put("adjustedScore", adjustedScore);
                    indicatorDetail.put("weight", indicatorWeight.getWeight());
                    indicatorDetail.put("weightedScore", adjustedScore * indicatorWeight.getWeight());
                    indicatorDetail.put("absoluteWeight", indicatorWeight.getAbsoluteWeight(dimensionWeight));
                    
                    indicatorDetails.add(indicatorDetail);
                }
            }

            if (dimensionTotalWeight > 0) {
                double normalizedScore = dimensionScore / dimensionTotalWeight;
                dimensionDetail.put("score", BigDecimal.valueOf(normalizedScore).setScale(2, RoundingMode.HALF_UP));
                dimensionDetail.put("weight", dimensionWeight);
                dimensionDetail.put("weightedScore", BigDecimal.valueOf(normalizedScore * dimensionWeight).setScale(2, RoundingMode.HALF_UP));
                dimensionDetail.put("indicatorCount", validIndicatorCount);
                dimensionDetail.put("indicators", indicatorDetails);
                
                details.put(dimension, dimensionDetail);
            }
        }

        return details;
    }

    @Override
    public boolean validateConfig(IndicatorWeightConfig config) {
        if (config == null) {
            log.error("配置对象为空");
            return false;
        }

        // 验证维度权重
        if (CollectionUtils.isEmpty(config.getDimensions())) {
            log.error("维度权重配置为空");
            return false;
        }

        double totalDimensionWeight = config.getDimensions().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        
        if (Math.abs(totalDimensionWeight - 1.0) > 0.001) {
            log.error("维度权重总和不等于1.0，实际值: {}", totalDimensionWeight);
            return false;
        }

        // 验证指标权重
        if (CollectionUtils.isEmpty(config.getIndicators())) {
            log.error("指标权重配置为空");
            return false;
        }

        for (Map.Entry<String, Map<String, IndicatorWeightConfig.IndicatorWeight>> dimensionEntry 
                : config.getIndicators().entrySet()) {
            String dimension = dimensionEntry.getKey();
            Map<String, IndicatorWeightConfig.IndicatorWeight> indicators = dimensionEntry.getValue();
            
            if (!config.getDimensions().containsKey(dimension)) {
                log.error("维度 {} 在指标配置中存在但在维度权重中不存在", dimension);
                return false;
            }

            double dimensionIndicatorWeightSum = indicators.values().stream()
                    .mapToDouble(IndicatorWeightConfig.IndicatorWeight::getWeight)
                    .sum();
            
            if (Math.abs(dimensionIndicatorWeightSum - 1.0) > 0.001) {
                log.error("维度 {} 的指标权重总和不等于1.0，实际值: {}", dimension, dimensionIndicatorWeightSum);
                return false;
            }
        }

        log.info("权重配置验证通过");
        return true;
    }

    @Override
    public Map<String, Object> getConfigStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        if (currentConfig == null) {
            stats.put("status", "未加载");
            return stats;
        }

        stats.put("status", "已加载");
        stats.put("version", currentConfig.getConfig() != null ? currentConfig.getConfig().getVersion() : "unknown");
        stats.put("dimensionCount", currentConfig.getDimensions().size());
        
        int totalIndicatorCount = currentConfig.getIndicators().values().stream()
                .mapToInt(Map::size)
                .sum();
        stats.put("totalIndicatorCount", totalIndicatorCount);
        
        Map<String, Integer> dimensionIndicatorCounts = currentConfig.getIndicators().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        entry -> entry.getValue().size()
                ));
        stats.put("dimensionIndicatorCounts", dimensionIndicatorCounts);
        
        stats.put("specialRuleCount", currentConfig.getSpecialRules() != null ? 
                currentConfig.getSpecialRules().size() : 0);

        return stats;
    }

    /**
     * 应用特殊处理规则
     */
    private double applySpecialRules(String indicatorName, BigDecimal value, double originalScore) {
        IndicatorWeightConfig.SpecialRule rule = currentConfig.getSpecialRule(indicatorName);
        if (rule == null || value == null) {
            return originalScore;
        }

        double doubleValue = value.doubleValue();

        // 处理最佳范围规则（如交易次数）
        if (rule.getOptimalRange() != null && rule.getOptimalRange().length == 2) {
            double min = rule.getOptimalRange()[0];
            double max = rule.getOptimalRange()[1];
            
            if (doubleValue >= min && doubleValue <= max) {
                return originalScore; // 在最佳范围内，不调整
            } else {
                // 超出范围，应用惩罚
                double penalty = (rule.getPenaltyFactor() != null) ? rule.getPenaltyFactor() : 0.5;
                return originalScore * (1.0 - penalty);
            }
        }

        // 处理最佳值规则（如Beta系数）
        if (rule.getOptimalValue() != null) {
            double optimalValue = rule.getOptimalValue();
            double tolerance = (rule.getTolerance() != null) ? rule.getTolerance() : 0.1;
            
            if (Math.abs(doubleValue - optimalValue) <= tolerance) {
                return originalScore; // 在容忍范围内，不调整
            } else {
                // 超出容忍范围，应用惩罚
                double penalty = (rule.getPenaltyFactor() != null) ? rule.getPenaltyFactor() : 0.3;
                double deviation = Math.abs(doubleValue - optimalValue);
                double penaltyMultiplier = Math.min(deviation / tolerance - 1.0, 1.0) * penalty;
                return originalScore * (1.0 - penaltyMultiplier);
            }
        }

        return originalScore;
    }

    /**
     * 记录配置摘要信息
     */
    private void logConfigSummary() {
        if (currentConfig == null) return;

        log.info("=== 指标权重配置摘要 ===");
        log.info("维度数量: {}", currentConfig.getDimensions().size());
        
        for (Map.Entry<String, Double> entry : currentConfig.getDimensions().entrySet()) {
            String dimension = entry.getKey();
            double weight = entry.getValue();
            int indicatorCount = currentConfig.getIndicators().get(dimension) != null ? 
                    currentConfig.getIndicators().get(dimension).size() : 0;
            log.info("- {}: 权重={}, 指标数量={}", dimension, weight, indicatorCount);
        }
        
        int totalIndicators = currentConfig.getIndicators().values().stream()
                .mapToInt(Map::size)
                .sum();
        log.info("总指标数量: {}", totalIndicators);
        
        if (currentConfig.getSpecialRules() != null) {
            log.info("特殊规则数量: {}", currentConfig.getSpecialRules().size());
        }
        log.info("========================");
    }
} 