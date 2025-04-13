package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO类，用于表示回测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResult {
    /**
     * 总收益
     */
    private BigDecimal totalProfit;
    
    /**
     * 总回报率 (%)
     */
    private BigDecimal totalReturn;
    
    /**
     * 交易次数
     */
    private int numberOfTrades;
    
    /**
     * 盈利交易次数
     */
    private int numberOfWinningTrades;
    
    /**
     * 亏损交易次数
     */
    private int numberOfLosingTrades;
    
    /**
     * 胜率 (%)
     */
    private BigDecimal winRate;
    
    /**
     * 最大回撤 (%)
     */
    private BigDecimal maxDrawdown;
    
    /**
     * 夏普比率
     */
    private BigDecimal sharpeRatio;
    
    /**
     * 利润因子
     */
    private BigDecimal profitFactor;
    
    /**
     * 交易记录
     */
    private List<TradeRecord> trades;
    
    /**
     * 初始余额
     */
    private BigDecimal initialBalance;
    
    /**
     * 最终余额
     */
    private BigDecimal finalBalance;
    
    /**
     * 平均盈利金额
     */
    private BigDecimal averageProfit;
    
    /**
     * 平均亏损金额
     */
    private BigDecimal averageLoss;
} 