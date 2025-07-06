package com.okx.trading.model.entity;


import jakarta.persistence.*;
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
     * 综合评分 (0-10分) - 基于科学合理的评分体系
     */
    @Column(name = "comprehensive_score", precision = 3, scale = 2)
    private BigDecimal comprehensiveScore;

    // ====================== 新增高级风险指标字段 ======================

    /**
     * 峰度 - 衡量收益率分布的尾部风险
     */
    @Column(name = "kurtosis", precision = 10, scale = 4)
    private BigDecimal kurtosis;

    /**
     * 条件风险价值 (CVaR) - 极端损失的期望值
     */
    @Column(name = "cvar", precision = 10, scale = 4)
    private BigDecimal cvar;

    /**
     * 95%置信度下的风险价值 (VaR95%)
     */
    @Column(name = "var95", precision = 10, scale = 4)
    private BigDecimal var95;

    /**
     * 99%置信度下的风险价值 (VaR99%)
     */
    @Column(name = "var99", precision = 10, scale = 4)
    private BigDecimal var99;

    /**
     * 信息比率 - 超额收益相对于跟踪误差的比率
     */
    @Column(name = "information_ratio", precision = 10, scale = 4)
    private BigDecimal informationRatio;

    /**
     * 跟踪误差 - 策略与基准收益率的标准差
     */
    @Column(name = "tracking_error", precision = 10, scale = 4)
    private BigDecimal trackingError;

    /**
     * Sterling比率 - 年化收益与平均最大回撤的比率
     */
    @Column(name = "sterling_ratio", precision = 10, scale = 4)
    private BigDecimal sterlingRatio;

    /**
     * Burke比率 - 年化收益与平方根回撤的比率
     */
    @Column(name = "burke_ratio", precision = 10, scale = 4)
    private BigDecimal burkeRatio;

    /**
     * 修正夏普比率 - 考虑偏度和峰度的夏普比率
     */
    @Column(name = "modified_sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal modifiedSharpeRatio;

    /**
     * 下行偏差 - 只考虑负收益的标准差
     */
    @Column(name = "downside_deviation", precision = 10, scale = 4)
    private BigDecimal downsideDeviation;

    /**
     * 上涨捕获率 - 基准上涨时策略的表现
     */
    @Column(name = "uptrend_capture", precision = 10, scale = 4)
    private BigDecimal uptrendCapture;

    /**
     * 下跌捕获率 - 基准下跌时策略的表现
     */
    @Column(name = "downtrend_capture", precision = 10, scale = 4)
    private BigDecimal downtrendCapture;

    /**
     * 最大回撤持续期 - 从峰值到恢复的最长时间（天数）
     */
    @Column(name = "max_drawdown_duration", precision = 10, scale = 2)
    private BigDecimal maxDrawdownDuration;

    /**
     * 痛苦指数 - 回撤深度与持续时间的综合指标
     */
    @Column(name = "pain_index", precision = 10, scale = 4)
    private BigDecimal painIndex;

    /**
     * 风险调整收益 - 综合多种风险因素的收益评估
     */
    @Column(name = "risk_adjusted_return", precision = 10, scale = 4)
    private BigDecimal riskAdjustedReturn;

    /**
     * 是否实盘策略，默认-1不是，为1的时候是实盘策略
     */
    @Column(name = "is_real", precision = 4, scale = 4)
    private int is_real = -1;

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
