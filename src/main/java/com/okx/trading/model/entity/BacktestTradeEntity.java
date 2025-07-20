package com.okx.trading.model.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 回测交易详情实体
 * 用于保存每次回测交易的详细信息
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "backtest_trade")
public class BacktestTradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 回测ID - 用于关联同一次回测的所有交易
     */
    @Column(name = "backtest_id", nullable = false)
    private String backtestId;

    /**
     * 策略名称
     */
    @Column(name = "strategy_name", nullable = false)
    private String strategyName;

    @Column(name = "strategy_code", nullable = false)
    private String strategyCode;

    /**
     * 策略参数
     */
    @Column(name = "strategy_params")
    private String strategyParams;

    /**
     * 交易索引号
     */
    @Column(name = "trade_index")
    private Integer index;

    /**
     * 交易类型：买入/卖出
     */
    @Column(name = "trade_type", nullable = false)
    private String type;

    /**
     * 交易的交易对
     */
    @Column(name = "symbol", nullable = false)
    private String symbol;

    /**
     * 进场时间
     */
    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    /**
     * 进场价格
     */
    @Column(name = "entry_price", precision = 20, scale = 8)
    private BigDecimal entryPrice;

    /**
     * 进场金额
     */
    @Column(name = "entry_amount", precision = 20, scale = 8)
    private BigDecimal entryAmount;

    /**
     * 进场仓位百分比
     */
    @Column(name = "entry_position_percentage", precision = 10, scale = 4)
    private BigDecimal entryPositionPercentage;

    /**
     * 出场时间
     */
    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    /**
     * 出场价格
     */
    @Column(name = "exit_price", precision = 20, scale = 8)
    private BigDecimal exitPrice;

    /**
     * 出场金额
     */
    @Column(name = "exit_amount", precision = 20, scale = 8)
    private BigDecimal exitAmount;

    /**
     * 交易盈亏（绝对值）
     */
    @Column(name = "profit", precision = 20, scale = 8)
    private BigDecimal profit;

    /**
     * 交易盈亏百分比
     */
    @Column(name = "profit_percentage", precision = 10, scale = 4)
    private BigDecimal profitPercentage;

    /**
     * 交易持续周期
     */
    @Column(name = "periods", precision = 10, scale = 4)
    private BigDecimal periods;

    /**
     * 盈亏百分比（每周期）
     */
    @Column(name = "profit_percentage_per_period", precision = 10, scale = 4)
    private BigDecimal profitPercentagePerPeriod;

    /**
     * 交易后的总资产
     */
    @Column(name = "total_assets", precision = 20, scale = 8)
    private BigDecimal totalAssets;

    /**
     * 交易后的最大回撤
     */
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    /**
     * 交易后的最大回撤
     */
    @Column(name = "max_loss", precision = 10, scale = 4)
    private BigDecimal maxLoss;

    @Column(name = "max_drawdown_period", precision = 10, scale = 4)
    private BigDecimal maxDrawdownPeriod;

    @Column(name = "max_loss_period", precision = 10, scale = 4)
    private BigDecimal maxLossPeriod;

    /**
     * 交易是否已平仓
     */
    @Column(name = "closed")
    private Boolean closed;

    /**
     * 交易成交量
     */
    @Column(name = "volume", precision = 20, scale = 8)
    private BigDecimal volume;

    /**
     * 交易费用
     */
    @Column(name = "fee", precision = 20, scale = 8)
    private BigDecimal fee;

    /**
     * 交易备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

    // 添加Lombok未生成的基本getter和setter方法
    public String getBacktestId() {
        return backtestId;
    }

    public void setBacktestId(String backtestId) {
        this.backtestId = backtestId;
    }

    public static BacktestTradeEntityBuilder builder() {
        return new BacktestTradeEntityBuilder();
    }

    public static class BacktestTradeEntityBuilder {
        private Long id;
        private String backtestId;
        private String strategyName;
        private String strategyCode;
        private String strategyParams;
        private Integer index;
        private String type;
        private String symbol;
        private LocalDateTime entryTime;
        private BigDecimal entryPrice;
        private BigDecimal entryAmount;
        private BigDecimal entryPositionPercentage;
        private LocalDateTime exitTime;
        private BigDecimal exitPrice;
        private BigDecimal exitAmount;
        private BigDecimal profit;
        private BigDecimal periods;
        private BigDecimal profitPercentagePerPeriod;
        private BigDecimal profitPercentage;
        private BigDecimal totalAssets;
        private BigDecimal maxDrawdown;
        private BigDecimal maxLoss;
        private BigDecimal maxDrawdownPeriod;
        private BigDecimal maxLossPeriod;
        private Boolean closed;
        private BigDecimal volume;
        private BigDecimal fee;
        private String remark;
        private LocalDateTime createTime;

        BacktestTradeEntityBuilder() {
        }

        public BacktestTradeEntityBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BacktestTradeEntityBuilder backtestId(String backtestId) {
            this.backtestId = backtestId;
            return this;
        }

        public BacktestTradeEntityBuilder strategyName(String strategyName) {
            this.strategyName = strategyName;
            return this;
        }

        public BacktestTradeEntityBuilder strategyCode(String strategyCode) {
            this.strategyCode = strategyCode;
            return this;
        }

        public BacktestTradeEntityBuilder strategyParams(String strategyParams) {
            this.strategyParams = strategyParams;
            return this;
        }

        public BacktestTradeEntityBuilder index(Integer index) {
            this.index = index;
            return this;
        }

        public BacktestTradeEntityBuilder type(String type) {
            this.type = type;
            return this;
        }

        public BacktestTradeEntityBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public BacktestTradeEntityBuilder entryTime(LocalDateTime entryTime) {
            this.entryTime = entryTime;
            return this;
        }

        public BacktestTradeEntityBuilder entryPrice(BigDecimal entryPrice) {
            this.entryPrice = entryPrice;
            return this;
        }

        public BacktestTradeEntityBuilder entryAmount(BigDecimal entryAmount) {
            this.entryAmount = entryAmount;
            return this;
        }

        public BacktestTradeEntityBuilder entryPositionPercentage(BigDecimal entryPositionPercentage) {
            this.entryPositionPercentage = entryPositionPercentage;
            return this;
        }

        public BacktestTradeEntityBuilder exitTime(LocalDateTime exitTime) {
            this.exitTime = exitTime;
            return this;
        }

        public BacktestTradeEntityBuilder exitPrice(BigDecimal exitPrice) {
            this.exitPrice = exitPrice;
            return this;
        }

        public BacktestTradeEntityBuilder exitAmount(BigDecimal exitAmount) {
            this.exitAmount = exitAmount;
            return this;
        }

        public BacktestTradeEntityBuilder profit(BigDecimal profit) {
            this.profit = profit;
            return this;
        }

        public BacktestTradeEntityBuilder profitPercentage(BigDecimal profitPercentage) {
            this.profitPercentage = profitPercentage;
            return this;
        }

        public BacktestTradeEntityBuilder totalAssets(BigDecimal totalAssets) {
            this.totalAssets = totalAssets;
            return this;
        }

        public BacktestTradeEntityBuilder maxDrawdown(BigDecimal maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
            return this;
        }

        public BacktestTradeEntityBuilder maxLoss(BigDecimal maxLoss) {
            this.maxLoss = maxLoss;
            return this;
        }


        public BacktestTradeEntityBuilder maxDrawdownPeriod(BigDecimal maxDrawdownPeriod) {
            this.maxDrawdownPeriod = maxDrawdownPeriod;
            return this;
        }

        public BacktestTradeEntityBuilder maxLossPeriod(BigDecimal maxLossPeriod) {
            this.maxLossPeriod = maxLossPeriod;
            return this;
        }


        public BacktestTradeEntityBuilder closed(Boolean closed) {
            this.closed = closed;
            return this;
        }

        public BacktestTradeEntityBuilder volume(BigDecimal volume) {
            this.volume = volume;
            return this;
        }

        public BacktestTradeEntityBuilder fee(BigDecimal fee) {
            this.fee = fee;
            return this;
        }

        public BacktestTradeEntityBuilder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public BacktestTradeEntityBuilder createTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public BacktestTradeEntityBuilder periods(BigDecimal periods) {
            this.periods = periods;
            return this;
        }

        public BacktestTradeEntityBuilder profitPercentagePerPeriod(BigDecimal profitPercentagePerPeriod) {
            this.profitPercentagePerPeriod = profitPercentagePerPeriod;
            return this;
        }


        public BacktestTradeEntity build() {
            return new BacktestTradeEntity(id, backtestId, strategyName, strategyCode, strategyParams, index, type, symbol,
                    entryTime, entryPrice, entryAmount, entryPositionPercentage, exitTime,
                    exitPrice, exitAmount, profit, profitPercentage, periods, profitPercentagePerPeriod, totalAssets, maxDrawdown, maxLoss,
                    maxDrawdownPeriod, maxLossPeriod, closed, volume, fee, remark, createTime);
        }
    }
}
