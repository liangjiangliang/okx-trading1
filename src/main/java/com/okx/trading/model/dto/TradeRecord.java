package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO类，用于表示单次交易记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRecord {
    /**
     * 交易ID
     */
    private Long id;
    
    /**
     * 交易类型 (BUY/SELL)
     */
    private String type;
    
    /**
     * 交易价格
     */
    private BigDecimal price;
    
    /**
     * 交易数量
     */
    private BigDecimal amount;
    
    /**
     * 交易时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 交易产生的利润/亏损
     */
    private BigDecimal profitLoss;
    
    /**
     * 交易佣金/手续费
     */
    private BigDecimal commission;
    
    /**
     * 交易后余额
     */
    private BigDecimal balance;
    
    /**
     * 交易所使用的信号
     */
    private String signal;
    
    /**
     * 交易盈亏百分比
     */
    private BigDecimal profitPercentage;
} 