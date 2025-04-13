package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 布林带策略参数DTO
 * 用于接收和传递布林带策略的参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BollingerBandsStrategyParamDTO {
    /**
     * 布林带周期
     */
    private int period = 20;
    
    /**
     * 布林带标准差乘数
     */
    private double multiplier = 2.0;
    
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
    
    /**
     * 交易模式：0=突破模式，1=回归模式
     * 突破模式：当价格突破上轨做空，突破下轨做多
     * 回归模式：当价格突破上轨后回落至中轨做多，突破下轨后回升至中轨做空
     */
    private int tradingMode = 0;
} 