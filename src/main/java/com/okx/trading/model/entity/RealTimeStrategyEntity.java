package com.okx.trading.model.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
     * 实时策略唯一代码
     */
    @Column(name = "strategy_code", nullable = false, unique = true, length = 50)
    private String strategyCode;

    /**
     * 关联的策略信息代码
     */
    @Column(name = "strategy_info_code", nullable = false, length = 50)
    private String strategyInfoCode;

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
     * 是否有效/启用
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 是否为模拟交易
     */
    @Column(name = "is_simulated", nullable = false)
    @Builder.Default
    private Boolean isSimulated = true;

    /**
     * 订单类型：market(市价), limit(限价)
     */
    @Column(name = "order_type", length = 10)
    @Builder.Default
    private String orderType = "market";

    /**
     * 交易金额
     */
    @Column(name = "trade_amount")
    private Double tradeAmount;

    /**
     * 策略运行状态：RUNNING(运行中), STOPPED(已停止), COMPLETED(已完成), ERROR(错误)
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "STOPPED";

    /**
     * 策略描述
     */
    @Column(name = "description", columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String errorMessage;

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
        if (this.startTime == null) {
            this.startTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
