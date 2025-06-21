package com.okx.trading.model.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 回测汇总信息实体
 * 用于保存每次回测的汇总数据，包括盈亏、回撤、交易次数等指标
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "backtest_summary")
public class BacktestSummaryEntity implements Comparable<BacktestSummaryEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 回测ID，与交易明细关联
     */
    @Column(name = "backtest_id", nullable = false, unique = true)
    private String backtestId;

    /**
     * 批量回测ID，用于关联同一批次的所有回测
     */
    @Column(name = "batch_backtest_id")
    private String batchBacktestId;

    /**
     * 策略名称
     */
    @Column(name = "strategy_name", nullable = false)
    private String strategyName;

    /**
     * 策略名称
     */
    @Column(name = "strategy_code", nullable = false)
    private String strategyCode;

    /**
     * 策略参数
     */
    @Column(name = "strategy_params")
    private String strategyParams;

    /**
     * 交易对
     */
    @Column(name = "symbol", nullable = false)
    private String symbol;

    /**
     * 时间间隔
     */
    @Column(name = "interval_val", nullable = false)
    private String intervalVal;

    /**
     * 回测开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 回测结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 初始资金
     */
    @Column(name = "initial_amount", precision = 20, scale = 8, nullable = false)
    private BigDecimal initialAmount;

    /**
     * 最终资金
     */
    @Column(name = "final_amount", precision = 20, scale = 8)
    private BigDecimal finalAmount;

    /**
     * 总盈亏（金额）
     */
    @Column(name = "total_profit", precision = 20, scale = 8)
    private BigDecimal totalProfit;

    /**
     * 总回报率（百分比）
     */
    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn;

    /**
     * 年化收益率（百分比）
     */
    @Column(name = "annualized_return", precision = 10, scale = 4)
    private BigDecimal annualizedReturn;

    /**
     * 交易总次数
     */
    @Column(name = "number_of_trades")
    private Integer numberOfTrades;

    /**
     * 盈利交易次数
     */
    @Column(name = "profitable_trades")
    private Integer profitableTrades;

    /**
     * 亏损交易次数
     */
    @Column(name = "unprofitable_trades")
    private Integer unprofitableTrades;

    /**
     * 胜率（百分比）
     */
    @Column(name = "win_rate", precision = 10, scale = 4)
    private BigDecimal winRate;

    /**
     * 平均盈利（百分比）
     */
    @Column(name = "average_profit", precision = 10, scale = 4)
    private BigDecimal averageProfit;

    /**
     * 最大回撤（百分比）
     */
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    /**
     * 夏普比率
     */
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    /**
     * Sortino比率（只考虑下行风险的风险调整收益指标）
     */
    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    /**
     * Calmar比率（年化收益与最大回撤的比值）
     */
    @Column(name = "calmar_ratio", precision = 10, scale = 4)
    private BigDecimal calmarRatio;

    /**
     * 最大损失（单笔交易中的最大亏损金额）
     */
    @Column(name = "maximum_loss", precision = 20, scale = 8)
    private BigDecimal maximumLoss;

    /**
     * 波动率（收盘价标准差）
     */
    @Column(name = "volatility", precision = 10, scale = 4)
    private BigDecimal volatility;

    /**
     * 总手续费
     */
    @Column(name = "total_fee", precision = 20, scale = 8)
    private BigDecimal totalFee;

    /**
     * Omega比率（收益与风险的比值）
     */
    @Column(name = "omega", precision = 10, scale = 4)
    private BigDecimal omega;

    /**
     * Alpha值（超额收益）
     */
    @Column(name = "alpha", precision = 10, scale = 4)
    private BigDecimal alpha;

    /**
     * Beta值（系统性风险）
     */
    @Column(name = "beta", precision = 10, scale = 4)
    private BigDecimal beta;

    /**
     * Treynor比率（风险调整收益指标）
     */
    @Column(name = "treynor_ratio", precision = 10, scale = 4)
    private BigDecimal treynorRatio;

    /**
     * Ulcer指数（回撤深度和持续时间的综合指标）
     */
    @Column(name = "ulcer_index", precision = 10, scale = 4)
    private BigDecimal ulcerIndex;

    /**
     * 偏度（收益分布的偏斜程度）
     */
    @Column(name = "skewness", precision = 10, scale = 4)
    private BigDecimal skewness;

    /**
     * 盈利因子（总盈利/总亏损）
     */
    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

    @Override
    public int compareTo(@NotNull BacktestSummaryEntity o) {
        return o.getTotalReturn().compareTo(this.getTotalReturn());
    }
}
