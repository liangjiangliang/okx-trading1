package com.okx.trading.model.entity;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ta4j.core.Strategy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 实时运行策略实体
 * 用于保存实时运行策略的详细信息，包括策略代码、运行状态、时间范围等
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "real_time_strategy")
public class RealTimeStrategyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的策略代码，不是这条数据的代码
     */
    @Column(name = "strategy_code", nullable = false, unique = true, length = 50)
    private String strategyCode;

    @Column(name = "strategy_name", nullable = false, unique = false, length = 50)
    private String strategyName;

    /**
     * 交易对符号，如BTC-USDT
     */
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /**
     * K线周期，如1m, 5m, 1h等
     */
    @Column(name = "interval_val", nullable = false, length = 10)
    private String interval;

    /**
     * 投资金额
     */
    @Column(name = "trade_amount")
    private Double tradeAmount = 0.0;

    /**
     * 交易金额
     */
    @Column(name = "last_trade_amount")
    private Double lastTradeAmount = 0.0;


    /**
     * 最后交易价格
     */
    @Column(name = "last_trade_price")
    private Double lastTradePrice = 0.0;


    /**
     * 最后一次交易数量
     */
    @Column(name = "last_trade_quantity")
    private Double lastTradeQuantity = 0.0;


    /**
     * 最后一次交易类型：BUY(买入), SELL(卖出)
     */
    @Column(name = "last_trade_type", length = 10)
    private String lastTradeType;

    /**
     * 最后一次交易类型：BUY(买入), SELL(卖出)
     */
    @Column(name = "last_trade_time", length = 10)
    private String lastTradeTime;


    /**
     * 总盈利金额（USDT）
     */
    @Column(name = "total_profit")
    @Builder.Default
    private Double totalProfit = 0.0;


    /**
     * 总手续费（USDT）
     */
    @Column(name = "total_fees")
    @Builder.Default
    private Double totalFees = 0.0;

    /**
     * 总交易次数
     */
    @Column(name = "total_trades")
    @Builder.Default
    private Integer totalTrades = 0;

    /**
     * 成功交易次数
     */
    @Column(name = "successful_trades")
    @Builder.Default
    private Integer successfulTrades = 0;


    @Column(name = "is_active", length = 20)
    @Builder.Default
    private Boolean isActive = true;
    /**
     * 策略运行状态：RUNNING(运行中), STOPPED(已停止), COMPLETED(已完成), ERROR(错误)
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "RUNNING";

    /**
     * 策略运行开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 策略运行结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

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

    @Transient
    private CompletableFuture<Map<String, Object>> future;

    @Transient
    private Strategy strategy;

    @Transient
    private Boolean isInPosition = false;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        if (this.startTime == null) {
            this.startTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }


    public RealTimeStrategyEntity(String strategyCode, String symbol, String interval, LocalDateTime startTime, Double tradeAmount, String strategyName) {
        this.strategyCode = strategyCode;
        this.symbol = symbol;
        this.interval = interval;
        this.startTime = startTime;
        this.tradeAmount = tradeAmount;
        this.strategyName = strategyName;
    }
}
