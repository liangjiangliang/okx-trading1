package com.okx.trading.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金中心数据实体
 * 用于记录每10分钟的总投资金额和总收益
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fund_data")
public class FundDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 总投资金额
     */
    @Column(name = "total_investment", precision = 20, scale = 8, nullable = false)
    private BigDecimal totalInvestment;

    /**
     * 总收益金额
     */
    @Column(name = "total_profit", precision = 20, scale = 8, nullable = false)
    private BigDecimal totalProfit;

    /**
     * 总资金（投资金额+收益的总和）
     */
    @Column(name = "total_fund", precision = 20, scale = 8, nullable = false)
    private BigDecimal totalFund;

    /**
     * 记录时间
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @PrePersist
    public void prePersist() {
        this.recordTime = LocalDateTime.now();
    }
} 