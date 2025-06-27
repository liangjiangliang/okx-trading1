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
    private BigDecimal totalProfit=BigDecimal.ZERO;

    /**
     * 总回报率（百分比）
     */
    private BigDecimal totalReturn=BigDecimal.ZERO;

    /**
     * 年化收益率（百分比）
     */
    private BigDecimal annualizedReturn=BigDecimal.ZERO;

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
    private BigDecimal winRate=BigDecimal.ZERO;

    /**
     * 平均盈利（百分比）
     */
    private BigDecimal averageProfit;

    /**
     * 最大回撤（百分比）
     */
    private BigDecimal maxDrawdown=BigDecimal.ZERO;

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

    /**
     * 峰度 - 衡量收益率分布的尾部风险
     */
    private BigDecimal kurtosis;

    /**
     * 条件风险价值 (CVaR) - 极端损失的期望值
     */
    private BigDecimal cvar;

    /**
     * 95%置信度下的风险价值 (VaR95%)
     */
    private BigDecimal var95;

    /**
     * 99%置信度下的风险价值 (VaR99%)
     */
    private BigDecimal var99;

    /**
     * 信息比率 - 超额收益相对于跟踪误差的比率
     */
    private BigDecimal informationRatio;

    /**
     * 跟踪误差 - 策略与基准收益率的标准差
     */
    private BigDecimal trackingError;

    /**
     * Sterling比率 - 年化收益与平均最大回撤的比率
     */
    private BigDecimal sterlingRatio;

    /**
     * Burke比率 - 年化收益与平方根回撤的比率
     */
    private BigDecimal burkeRatio;

    /**
     * 修正夏普比率 - 考虑偏度和峰度的夏普比率
     */
    private BigDecimal modifiedSharpeRatio;

    /**
     * 下行偏差 - 只考虑负收益的标准差
     */
    private BigDecimal downsideDeviation;

    /**
     * 上涨捕获率 - 基准上涨时策略的表现
     */
    private BigDecimal uptrendCapture;

    /**
     * 下跌捕获率 - 基准下跌时策略的表现
     */
    private BigDecimal downtrendCapture;

    /**
     * 最大回撤持续期 - 从峰值到恢复的最长时间
     */
    private BigDecimal maxDrawdownDuration;

    /**
     * 痛苦指数 - 回撤深度与持续时间的综合指标
     */
    private BigDecimal painIndex;

    /**
     * 风险调整收益 - 综合多种风险因素的收益评估
     */
    private BigDecimal riskAdjustedReturn;

    /**
     * 综合评分 (0-10分) - 基于科学合理的评分体系
     */
    private BigDecimal comprehensiveScore;

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

    public BigDecimal getKurtosis() {
        return kurtosis;
    }

    public void setKurtosis(BigDecimal kurtosis) {
        this.kurtosis = kurtosis;
    }

    public BigDecimal getCvar() {
        return cvar;
    }

    public void setCvar(BigDecimal cvar) {
        this.cvar = cvar;
    }

    public BigDecimal getVar95() {
        return var95;
    }

    public void setVar95(BigDecimal var95) {
        this.var95 = var95;
    }

    public BigDecimal getVar99() {
        return var99;
    }

    public void setVar99(BigDecimal var99) {
        this.var99 = var99;
    }

    public BigDecimal getInformationRatio() {
        return informationRatio;
    }

    public void setInformationRatio(BigDecimal informationRatio) {
        this.informationRatio = informationRatio;
    }

    public BigDecimal getTrackingError() {
        return trackingError;
    }

    public void setTrackingError(BigDecimal trackingError) {
        this.trackingError = trackingError;
    }

    public BigDecimal getSterlingRatio() {
        return sterlingRatio;
    }

    public void setSterlingRatio(BigDecimal sterlingRatio) {
        this.sterlingRatio = sterlingRatio;
    }

    public BigDecimal getBurkeRatio() {
        return burkeRatio;
    }

    public void setBurkeRatio(BigDecimal burkeRatio) {
        this.burkeRatio = burkeRatio;
    }

    public BigDecimal getModifiedSharpeRatio() {
        return modifiedSharpeRatio;
    }

    public void setModifiedSharpeRatio(BigDecimal modifiedSharpeRatio) {
        this.modifiedSharpeRatio = modifiedSharpeRatio;
    }

    public BigDecimal getDownsideDeviation() {
        return downsideDeviation;
    }

    public void setDownsideDeviation(BigDecimal downsideDeviation) {
        this.downsideDeviation = downsideDeviation;
    }

    public BigDecimal getUptrendCapture() {
        return uptrendCapture;
    }

    public void setUptrendCapture(BigDecimal uptrendCapture) {
        this.uptrendCapture = uptrendCapture;
    }

    public BigDecimal getDowntrendCapture() {
        return downtrendCapture;
    }

    public void setDowntrendCapture(BigDecimal downtrendCapture) {
        this.downtrendCapture = downtrendCapture;
    }

    public BigDecimal getMaxDrawdownDuration() {
        return maxDrawdownDuration;
    }

    public void setMaxDrawdownDuration(BigDecimal maxDrawdownDuration) {
        this.maxDrawdownDuration = maxDrawdownDuration;
    }

    public BigDecimal getPainIndex() {
        return painIndex;
    }

    public void setPainIndex(BigDecimal painIndex) {
        this.painIndex = painIndex;
    }

    public BigDecimal getRiskAdjustedReturn() {
        return riskAdjustedReturn;
    }

    public void setRiskAdjustedReturn(BigDecimal riskAdjustedReturn) {
        this.riskAdjustedReturn = riskAdjustedReturn;
    }

    public BigDecimal getComprehensiveScore() {
        return comprehensiveScore;
    }

    public void setComprehensiveScore(BigDecimal comprehensiveScore) {
        this.comprehensiveScore = comprehensiveScore;
    }
}
