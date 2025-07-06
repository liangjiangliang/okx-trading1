package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单移动平均线策略参数DTO
 * 用于接收和传递SMA策略的参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SMAStrategyParamDTO {
    /**
     * 快速均线周期
     */
    
    private int shortPeriod = 9;
    
    /**
     * 慢速均线周期
     */
    
    private int longPeriod = 26;
    
    /**
     * 初始投资金额
     */
    
    private double initialAmount = 10000;
    
    /**
     * 交易手续费率
     */
    
    private double tradingFeeRate = 0.001;
    
    /**
     * 止损比例 (%)，默认为0表示不启用止损
     */
    
    private double stopLossPercent = 0;
    
    /**
     * 止盈比例 (%)，默认为0表示不启用止盈
     */
    
    private double takeProfitPercent = 0;
} 
