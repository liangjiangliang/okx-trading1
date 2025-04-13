package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 回测结果数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResultDTO {
    /**
     * 初始投资金额
     */
    private double initialAmount;
    
    /**
     * 最终资产金额
     */
    private double finalAmount;
    
    /**
     * 总收益
     */
    private double totalProfit;
    
    /**
     * 总收益率 (%)
     */
    private double totalReturn;
    
    /**
     * 交易次数
     */
    private int numberOfTrades;
    
    /**
     * 盈利交易次数
     */
    private int profitableTrades;
    
    /**
     * 亏损交易次数
     */
    private int losingTrades;
    
    /**
     * 胜率 (%)
     */
    private double winRate;
    
    /**
     * 平均盈利 (%)
     */
    private double averageProfit;
    
    /**
     * 夏普比率
     */
    private double sharpeRatio;
    
    /**
     * 回撤率 (%)
     */
    private double maxDrawdown;
    
    /**
     * 买入持有策略收益率 (%)
     */
    private double buyAndHoldReturn;
    
    /**
     * 初始余额
     */
    private double initialBalance;
    
    /**
     * 最终余额
     */
    private double finalBalance;
    
    /**
     * 数据点数量
     */
    private int totalBars;
    
    /**
     * 总交易次数
     */
    private int totalTrades;
    
    /**
     * 平均每笔交易收益
     */
    private double averageProfitPerTrade;
    
    /**
     * 交易记录列表
     */
    private List<TradeRecordDTO> trades;
} 