package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 交易记录数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRecordDTO {
    /**
     * 交易类型：买入/卖出
     */
    private String type;
    
    /**
     * 交易时间
     */
    private LocalDateTime time;
    
    /**
     * 交易价格
     */
    private double price;
    
    /**
     * 交易数量
     */
    private double amount;
    
    /**
     * 交易总额
     */
    private double total;
    
    /**
     * 交易手续费
     */
    private double fee;
    
    /**
     * 订单号/交易ID
     */
    private String orderId;
    
    /**
     * 交易后账户余额
     */
    private double balanceAfterTrade;
    
    /**
     * 收益金额（仅针对卖出订单）
     */
    private double profit;
    
    /**
     * 收益率（仅针对卖出订单）
     */
    private double profitRate;
} 