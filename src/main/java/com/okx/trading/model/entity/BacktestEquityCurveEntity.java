package com.okx.trading.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 回测资金曲线实体
 * 用于保存每次回测的资金曲线数据，与回测ID关联
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "backtest_equity_curve")
public class BacktestEquityCurveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 回测ID，与回测汇总关联
     */
    @Column(name = "backtest_id", nullable = false)
    private String backtestId;

    /**
     * 时间点
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * 资金值
     */
    @Column(name = "equity_value", precision = 20, scale = 8, nullable = false)
    private BigDecimal equityValue;

    /**
     * 在序列中的索引位置
     */
    @Column(name = "index_position")
    private Integer indexPosition;
} 