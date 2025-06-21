package com.okx.trading.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时交易订单实体类
 * 用于存储实时策略交易产生的订单信息
 */
@Entity
@Table(name = "real_time_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 策略代码
     */
    @Column(name = "strategy_code", nullable = false, length = 50)
    private String strategyCode;

    /**
     * 交易对
     */
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /**
     * 订单ID
     */
    @Column(name = "order_id", length = 50)
    private String orderId;

    /**
     * 客户端订单ID
     */
    @Column(name = "client_order_id", length = 50)
    private String clientOrderId;

    /**
     * 订单类型 (MARKET, LIMIT)
     */
    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;

    /**
     * 交易方向 (BUY, SELL)
     */
    @Column(name = "side", nullable = false, length = 10)
    private String side;

    /**
     * 价格
     */
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;

    /**
     * 数量
     */
    @Column(name = "quantity", precision = 20, scale = 8)
    private BigDecimal quantity;

    /**
     * 金额
     */
    @Column(name = "amount", precision = 20, scale = 8)
    private BigDecimal amount;

    /**
     * 订单状态
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 已执行数量
     */
    @Column(name = "executed_qty", precision = 20, scale = 8)
    private BigDecimal executedQty;

    /**
     * 已执行金额
     */
    @Column(name = "executed_amount", precision = 20, scale = 8)
    private BigDecimal executedAmount;

    /**
     * 手续费
     */
    @Column(name = "fee", precision = 20, scale = 8)
    private BigDecimal fee;

    /**
     * 手续费币种
     */
    @Column(name = "fee_currency", length = 10)
    private String feeCurrency;

    /**
     * 交易信号类型 (BUY_SIGNAL, SELL_SIGNAL)
     */
    @Column(name = "signal_type", length = 20)
    private String signalType;

    /**
     * 触发信号时的价格
     */
    @Column(name = "signal_price", precision = 20, scale = 8)
    private BigDecimal signalPrice;

    /**
     * 是否为模拟交易
     */
    @Column(name = "simulated", nullable = false)
    private Boolean simulated = false;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}