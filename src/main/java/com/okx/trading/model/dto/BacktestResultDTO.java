package com.okx.trading.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 回测结果数据传输对象
 */
@Data
public class BacktestResultDTO {

    /**
     * 回测是否成功
     */
    private boolean success;

    /**
     * 错误信息（如果回测失败）
     */
    private String errorMessage;

    private String backtestId;

    /**
     * 初始资金
     */
    private BigDecimal initialAmount;

    /**
     * 最终资金
     */
    private BigDecimal finalAmount;

    /**
     * 总盈亏（金额）
     */
    private BigDecimal totalProfit;

    /**
     * 总回报率（百分比）
     */
    private BigDecimal totalReturn;

    /**
     * 年化收益率（百分比）
     */
    private BigDecimal annualizedReturn;

    /**
     * 交易总次数
     */
    private int numberOfTrades;

    /**
     * 盈利交易次数
     */
    private int profitableTrades;

    /**
     * 亏损交易次数
     */
    private int unprofitableTrades;

    /**
     * 胜率（百分比）
     */
    private BigDecimal winRate;

    /**
     * 平均盈利（百分比）
     */
    private BigDecimal averageProfit;

    /**
     * 最大回撤（百分比）
     */
    private BigDecimal maxDrawdown;

    /**
     * 夏普比率
     */
    private BigDecimal sharpeRatio;


    private BigDecimal omega;

    private BigDecimal alpha;

    private BigDecimal beta;

    private BigDecimal treynorRatio;

    private BigDecimal ulcerIndex;

    private BigDecimal skewness;

    /**
     * Sortino比率（只考虑下行风险的风险调整收益指标）
     */
    private BigDecimal sortinoRatio;

    /**
     * Calmar比率（年化收益与最大回撤的比值）
     */
    private BigDecimal calmarRatio;

    /**
     * 最大损失（单笔交易中的最大亏损金额）
     */
    private BigDecimal maximumLoss;

    /**
     * 波动率（收盘价标准差）
     */
    private BigDecimal volatility;

    /**
     * 盈利因子（总盈利/总亏损）
     */
    private BigDecimal profitFactor;

    /**
     * 策略名称
     */
    private String strategyName;

    private String strategyCode;

    /**
     * 参数描述
     */
    private String parameterDescription;

    /**
     * 交易记录列表
     */
    private List<TradeRecordDTO> trades;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    public BacktestResultDTO() {
    }

    public BigDecimal getAnnualizedReturn() {
        return annualizedReturn;
    }

    public void setAnnualizedReturn(BigDecimal annualizedReturn) {
        this.annualizedReturn = annualizedReturn;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public String getBacktestId() {
        return backtestId;
    }

    public void setBacktestId(String backtestId) {
        this.backtestId = backtestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public BigDecimal getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(BigDecimal initialAmount) {
        this.initialAmount = initialAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }

    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(int numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    public int getProfitableTrades() {
        return profitableTrades;
    }

    public void setProfitableTrades(int profitableTrades) {
        this.profitableTrades = profitableTrades;
    }

    public int getUnprofitableTrades() {
        return unprofitableTrades;
    }

    public void setUnprofitableTrades(int unprofitableTrades) {
        this.unprofitableTrades = unprofitableTrades;
    }

    public BigDecimal getWinRate() {
        return winRate;
    }

    public void setWinRate(BigDecimal winRate) {
        this.winRate = winRate;
    }

    public BigDecimal getAverageProfit() {
        return averageProfit;
    }

    public void setAverageProfit(BigDecimal averageProfit) {
        this.averageProfit = averageProfit;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public BigDecimal getSharpeRatio() {
        return sharpeRatio;
    }

    public void setSharpeRatio(BigDecimal sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }

    public BigDecimal getSortinoRatio() {
        return sortinoRatio;
    }

    public void setSortinoRatio(BigDecimal sortinoRatio) {
        this.sortinoRatio = sortinoRatio;
    }

    public BigDecimal getCalmarRatio() {
        return calmarRatio;
    }

    public void setCalmarRatio(BigDecimal calmarRatio) {
        this.calmarRatio = calmarRatio;
    }

    public BigDecimal getMaximumLoss() {
        return maximumLoss;
    }

    public void setMaximumLoss(BigDecimal maximumLoss) {
        this.maximumLoss = maximumLoss;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public void setVolatility(BigDecimal volatility) {
        this.volatility = volatility;
    }

    public BigDecimal getProfitFactor() {
        return profitFactor;
    }

    public void setProfitFactor(BigDecimal profitFactor) {
        this.profitFactor = profitFactor;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getParameterDescription() {
        return parameterDescription;
    }

    public void setParameterDescription(String parameterDescription) {
        this.parameterDescription = parameterDescription;
    }

    public List<TradeRecordDTO> getTrades() {
        return trades;
    }

    public void setTrades(List<TradeRecordDTO> trades) {
        this.trades = trades;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public BigDecimal getOmega() {
        return omega;
    }

    public void setOmega(BigDecimal omega) {
        this.omega = omega;
    }

    public BigDecimal getAlpha() {
        return alpha;
    }

    public void setAlpha(BigDecimal alpha) {
        this.alpha = alpha;
    }

    public BigDecimal getBeta() {
        return beta;
    }

    public void setBeta(BigDecimal beta) {
        this.beta = beta;
    }

    public BigDecimal getTreynorRatio() {
        return treynorRatio;
    }

    public void setTreynorRatio(BigDecimal treynorRatio) {
        this.treynorRatio = treynorRatio;
    }

    public BigDecimal getUlcerIndex() {
        return ulcerIndex;
    }

    public void setUlcerIndex(BigDecimal ulcerIndex) {
        this.ulcerIndex = ulcerIndex;
    }

    public BigDecimal getSkewness() {
        return skewness;
    }

    public void setSkewness(BigDecimal skewness) {
        this.skewness = skewness;
    }
}
