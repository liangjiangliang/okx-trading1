package com.okx.trading.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标分布实体 - 存储各个指标的分位数分布信息
 * 用于动态评分算法的数据基础
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "indicator_distribution")
public class IndicatorDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 指标名称 (如: annualizedReturn, maxDrawdown, winRate 等)
     */
    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;

    /**
     * 指标中文名称
     */
    @Column(name = "indicator_display_name", length = 100)
    private String indicatorDisplayName;

    /**
     * 指标类型: POSITIVE(越大越好), NEGATIVE(越小越好), NEUTRAL(中性)
     */
    @Column(name = "indicator_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IndicatorType indicatorType;

    /**
     * 样本总数
     */
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    /**
     * 最小值
     */
    @Column(name = "min_value", precision = 20, scale = 8)
    private BigDecimal minValue;

    /**
     * 最大值
     */
    @Column(name = "max_value", precision = 20, scale = 8)
    private BigDecimal maxValue;

    /**
     * 平均值
     */
    @Column(name = "avg_value", precision = 20, scale = 8)
    private BigDecimal avgValue;

    /**
     * 10%分位数
     */
    @Column(name = "p10", precision = 20, scale = 8)
    private BigDecimal p10;

    /**
     * 20%分位数
     */
    @Column(name = "p20", precision = 20, scale = 8)
    private BigDecimal p20;

    /**
     * 30%分位数
     */
    @Column(name = "p30", precision = 20, scale = 8)
    private BigDecimal p30;

    /**
     * 40%分位数
     */
    @Column(name = "p40", precision = 20, scale = 8)
    private BigDecimal p40;

    /**
     * 50%分位数(中位数)
     */
    @Column(name = "p50", precision = 20, scale = 8)
    private BigDecimal p50;

    /**
     * 60%分位数
     */
    @Column(name = "p60", precision = 20, scale = 8)
    private BigDecimal p60;

    /**
     * 70%分位数
     */
    @Column(name = "p70", precision = 20, scale = 8)
    private BigDecimal p70;

    /**
     * 80%分位数
     */
    @Column(name = "p80", precision = 20, scale = 8)
    private BigDecimal p80;

    /**
     * 90%分位数
     */
    @Column(name = "p90", precision = 20, scale = 8)
    private BigDecimal p90;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 版本号 - 用于区分不同时期的分布数据
     */
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 是否为当前版本
     */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    public enum IndicatorType {
        POSITIVE,  // 越大越好 (如年化收益率、胜率)
        NEGATIVE,  // 越小越好 (如最大回撤、波动率)
        NEUTRAL    // 中性指标 (如交易次数)
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }

    /**
     * 根据指标值计算所属分位数范围 (1-8分)
     * @param value 指标值
     * @return 1-8分的评分
     */
    public double calculateScore(BigDecimal value) {
        if (value == null) return 4.0; // 无数据时给中等分

        double val = value.doubleValue();
        
        // 根据指标类型确定评分逻辑
        if (indicatorType == IndicatorType.POSITIVE) {
            // 越大越好的指标
            return calculatePositiveScore(val);
        } else if (indicatorType == IndicatorType.NEGATIVE) {
            // 越小越好的指标
            return calculateNegativeScore(val);
        } else {
            // 中性指标，靠近中位数最好
            return calculateNeutralScore(val);
        }
    }

    private double calculatePositiveScore(double value) {
        if (p90 != null && value >= p90.doubleValue()) return 8.0;
        if (p80 != null && value >= p80.doubleValue()) return 7.0;
        if (p70 != null && value >= p70.doubleValue()) return 6.0;
        if (p60 != null && value >= p60.doubleValue()) return 5.0;
        if (p50 != null && value >= p50.doubleValue()) return 4.0;
        if (p40 != null && value >= p40.doubleValue()) return 3.0;
        if (p30 != null && value >= p30.doubleValue()) return 2.0;
        if (p20 != null && value >= p20.doubleValue()) return 1.5;
        return 1.0;
    }

    private double calculateNegativeScore(double value) {
        if (p20 != null && value <= p20.doubleValue()) return 8.0;
        if (p30 != null && value <= p30.doubleValue()) return 7.0;
        if (p40 != null && value <= p40.doubleValue()) return 6.0;
        if (p50 != null && value <= p50.doubleValue()) return 5.0;
        if (p60 != null && value <= p60.doubleValue()) return 4.0;
        if (p70 != null && value <= p70.doubleValue()) return 3.0;
        if (p80 != null && value <= p80.doubleValue()) return 2.0;
        if (p90 != null && value <= p90.doubleValue()) return 1.5;
        return 1.0;
    }

    private double calculateNeutralScore(double value) {
        if (p50 == null) return 4.0;
        
        double median = p50.doubleValue();
        double distance = Math.abs(value - median);
        
        // 距离中位数越近分数越高
        if (p30 != null && p70 != null) {
            double range = Math.abs(p70.doubleValue() - p30.doubleValue());
            if (range > 0) {
                double normalizedDistance = distance / range;
                return Math.max(1.0, 8.0 - normalizedDistance * 7.0);
            }
        }
        
        return 4.0;
    }
} 