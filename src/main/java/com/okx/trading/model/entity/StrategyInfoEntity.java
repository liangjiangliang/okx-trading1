package com.okx.trading.model.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 策略信息实体
 * 用于保存交易策略的详细信息，包括策略代码、名称、描述、参数说明、默认参数和分类等
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "strategy_info")
public class StrategyInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 策略代码，如SMA, MACD等
     */
    @Column(name = "strategy_code", nullable = false, unique = true)
    private String strategyCode;
    
    /**
     * 策略名称，如简单移动平均线策略
     */
    @Column(name = "strategy_name", nullable = false)
    private String strategyName;
    
    /**
     * 策略描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 参数说明
     */
    @Column(name = "params_desc", columnDefinition = "TEXT")
    private String paramsDesc;
    
    /**
     * 默认参数值
     */
    @Column(name = "default_params")
    private String defaultParams;
    
    /**
     * 策略分类，如移动平均线、震荡指标等
     */
    @Column(name = "category")
    private String category;
    
    /**
     * 策略源代码，存储lambda函数的序列化字符串
     */
    @Column(name = "source_code", columnDefinition = "TEXT")
    private String sourceCode;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}