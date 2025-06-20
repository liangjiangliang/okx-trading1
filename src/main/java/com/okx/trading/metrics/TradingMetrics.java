package com.okx.trading.metrics;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.SMAIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易指标计算类
 * 统一计算和管理所有交易相关的风险和收益指标
 */
public class TradingMetrics {

    // 基础参数
    private final BarSeries series;
    private final TradingRecord tradingRecord;
    private final BigDecimal initialAmount;
    private final BigDecimal finalAmount;
    private final BigDecimal totalPnL;
    private final BigDecimal totalFees;
    private final BigDecimal riskFreeRate;

    // 交易统计
    private final int numberOfTrades;
    private final int profitableTrades;
    private final int losingTrades;
    private final double winRate;

    // 风险指标
    private final BigDecimal sharpeRatio;
    private final BigDecimal sortinoRatio;
    private final BigDecimal calmarRatio;
    private final BigDecimal maxDrawdown;
    private final BigDecimal volatility;
    private final BigDecimal maximumSingleLoss;
    private final BigDecimal profitLossRatio;
    private final double treynorRatio;
    private final double skewness;
    private final BigDecimal omega;

    // Alpha和Beta
    private final double alpha;
    private final double beta;

    /**
     * 构造器：传入必要参数并计算所有指标
     */
    public TradingMetrics(BarSeries series, TradingRecord tradingRecord,
                          BigDecimal initialAmount, BigDecimal finalAmount,
                          BigDecimal totalPnL, BigDecimal totalFees,
                          BigDecimal riskFreeRate) {
        this.series = series;
        this.tradingRecord = tradingRecord;
        this.initialAmount = initialAmount;
        this.finalAmount = finalAmount;
        this.totalPnL = totalPnL;
        this.totalFees = totalFees;
        this.riskFreeRate = riskFreeRate;

        // 计算交易统计
        this.numberOfTrades = tradingRecord.getPositionCount();
        int[] tradeCounts = calculateTradeCounts();
        this.profitableTrades = tradeCounts[0];
        this.losingTrades = tradeCounts[1];
        this.winRate = calculateWinRate();

        // 计算全周期策略收益率序列
        List<BigDecimal> fullPeriodReturns = calculateFullPeriodStrategyReturns();

        // 计算风险指标
        this.sharpeRatio = calculateSharpeRatio(fullPeriodReturns, riskFreeRate, 252);
        this.sortinoRatio = calculateSortinoRatio(fullPeriodReturns, riskFreeRate, 252);
        this.calmarRatio = calculateCalmarRatio(fullPeriodReturns, riskFreeRate);
        this.maxDrawdown = calculateMaxDrawdown();
        this.volatility = calculateVolatility();
        this.maximumSingleLoss = calculateMaximumSingleLoss();
        this.profitLossRatio = calculateProfitLossRatio();
        this.treynorRatio = calculateTreynorRatio(fullPeriodReturns, riskFreeRate, 1.0);
        this.skewness = calculateSkewness(fullPeriodReturns);
        this.omega = calculateOmegaRatio(fullPeriodReturns, riskFreeRate);

        // 暂时设置默认值，避免API限制
        this.alpha = 0.0;
        this.beta = 1.0;
    }

    /**
     * 计算全周期策略收益率序列
     */
    private List<BigDecimal> calculateFullPeriodStrategyReturns() {
        List<BigDecimal> returns = new ArrayList<>();

        if (series == null || series.getBarCount() < 2) {
            return returns;
        }

        // 如果没有交易记录，整个期间都是0收益
        if (tradingRecord == null || tradingRecord.getPositionCount() == 0) {
            for (int i = 1; i < series.getBarCount(); i++) {
                returns.add(BigDecimal.ZERO);
            }
            return returns;
        }

        // 创建持仓期间标记数组
        boolean[] isInPosition = new boolean[series.getBarCount()];

        // 标记所有持仓期间（从入场下一天开始到出场当天）
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();

                // 从入场后的下一天开始标记持仓，到出场当天结束
                for (int i = entryIndex + 1; i <= exitIndex; i++) {
                    if (i < series.getBarCount()) {
                        isInPosition[i] = true;
                    }
                }
            }
        }

        // 计算每日收益率
        for (int i = 1; i < series.getBarCount(); i++) {
            if (isInPosition[i]) {
                // 持仓期间：计算价格变化收益率
                Num prevPrice = series.getBar(i - 1).getClosePrice();
                Num currentPrice = series.getBar(i).getClosePrice();

                if (prevPrice.isZero()) {
                    returns.add(BigDecimal.ZERO);
                } else {
                    Num returnRate = currentPrice.minus(prevPrice).dividedBy(prevPrice);
                    returns.add(new BigDecimal(returnRate.toString()));
                }
            } else {
                // 未持仓期间：收益率为0
                returns.add(BigDecimal.ZERO);
            }
        }

        return returns;
    }

    /**
     * 计算交易统计数据
     */
    private int[] calculateTradeCounts() {
        int profitable = 0;
        int losing = 0;

        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                Num pnl = position.getProfit();
                if (pnl.isGreaterThan(series.numOf(0))) {
                    profitable++;
                } else if (pnl.isLessThan(series.numOf(0))) {
                    losing++;
                }
            }
        }

        return new int[]{profitable, losing};
    }

    /**
     * 计算胜率
     */
    private double calculateWinRate() {
        if (numberOfTrades == 0) return 0.0;
        return (double) profitableTrades / numberOfTrades * 100;
    }

    /**
     * 计算夏普比率
     */
    private BigDecimal calculateSharpeRatio(List<BigDecimal> returns, BigDecimal riskFreeRate, int periodsPerYear) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算平均收益率
        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        // 年化平均收益率
        BigDecimal annualizedReturn = avgReturn.multiply(BigDecimal.valueOf(periodsPerYear));

        // 计算收益率标准差
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(avgReturn);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        // 年化波动率
        BigDecimal annualizedVolatility = BigDecimal.valueOf(Math.sqrt(variance.doubleValue() * periodsPerYear));


        if (annualizedVolatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 夏普比率 = (年化收益率 - 无风险收益率) / 年化波动率
        return annualizedReturn.subtract(riskFreeRate).divide(annualizedVolatility, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算索提诺比率
     */
    private BigDecimal calculateSortinoRatio(List<BigDecimal> returns, BigDecimal riskFreeRate, int periodsPerYear) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算平均收益率
        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        // 年化平均收益率
        BigDecimal annualizedReturn = avgReturn.multiply(BigDecimal.valueOf(periodsPerYear));

        // 计算下行方差（只考虑负收益）
        BigDecimal downVar = BigDecimal.ZERO;
        int downCount = 0;
        for (BigDecimal ret : returns) {
            if (ret.compareTo(BigDecimal.ZERO) < 0) {
                downVar = downVar.add(ret.multiply(ret));
                downCount++;
            }
        }

        if (downCount == 0) {
            return BigDecimal.valueOf(999.9999); // 没有负收益时返回极大值
        }

        downVar = downVar.divide(BigDecimal.valueOf(downCount), 10, RoundingMode.HALF_UP);
        BigDecimal downVolatility = BigDecimal.valueOf(Math.sqrt(downVar.doubleValue() * periodsPerYear));

        if (downVolatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 索提诺比率 = (年化收益率 - 无风险收益率) / 下行波动率
        return annualizedReturn.subtract(riskFreeRate).divide(downVolatility, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算卡玛比率
     */
    private BigDecimal calculateCalmarRatio(List<BigDecimal> returns, BigDecimal riskFreeRate) {
        BigDecimal totalReturn = calculateTotalReturn();
        BigDecimal maxDD = calculateMaxDrawdown();

        if (maxDD.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalReturn.divide(maxDD.abs(), 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算总收益率
     */
    private BigDecimal calculateTotalReturn() {
        if (initialAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPnL.divide(initialAmount, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算最大回撤
     */
    private BigDecimal calculateMaxDrawdown() {
        if (series == null || series.getBarCount() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = initialAmount;
        BigDecimal currentValue = initialAmount;

        // 模拟资产净值变化
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                Num profit = position.getProfit();
                currentValue = currentValue.add(new BigDecimal(profit.toString()));

                if (currentValue.compareTo(peak) > 0) {
                    peak = currentValue;
                }

                BigDecimal drawdown = peak.subtract(currentValue).divide(peak, 6, RoundingMode.HALF_UP);
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }

        return maxDrawdown;
    }

    /**
     * 计算波动率
     */
    private BigDecimal calculateVolatility() {
        List<BigDecimal> dailyPrice = new ArrayList<>();
        for (int i = 0; i <= series.getEndIndex(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            dailyPrice.add(BigDecimal.valueOf(closePrice));
        }

        List<BigDecimal> dailyReturns = computeDailyReturns(dailyPrice, true);
        return calculateVolatilityFromReturns(dailyReturns);
    }

    /**
     * 从收益率序列计算波动率
     */
    private BigDecimal calculateVolatilityFromReturns(List<BigDecimal> returns) {
        if (returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(mean);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(returns.size() - 1), 10, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    /**
     * 计算日收益率
     */
    private List<BigDecimal> computeDailyReturns(List<BigDecimal> prices, boolean useLogReturn) {
        List<BigDecimal> returns = new ArrayList<>();

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i);
            BigDecimal previousPrice = prices.get(i - 1);

            if (previousPrice.compareTo(BigDecimal.ZERO) == 0) {
                returns.add(BigDecimal.ZERO);
                continue;
            }

            BigDecimal returnValue;
            if (useLogReturn) {
                double logReturn = Math.log(currentPrice.doubleValue() / previousPrice.doubleValue());
                returnValue = BigDecimal.valueOf(logReturn);
            } else {
                returnValue = currentPrice.subtract(previousPrice).divide(previousPrice, 10, RoundingMode.HALF_UP);
            }
            returns.add(returnValue);
        }

        return returns;
    }

    /**
     * 计算最大单笔亏损
     */
    private BigDecimal calculateMaximumSingleLoss() {
        BigDecimal maxLoss = BigDecimal.ZERO;

        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                Num pnl = position.getProfit();
                BigDecimal loss = new BigDecimal(pnl.toString());
                if (loss.compareTo(maxLoss) < 0) {
                    maxLoss = loss;
                }
            }
        }

        return maxLoss;
    }

    /**
     * 计算盈亏比
     */
    private BigDecimal calculateProfitLossRatio() {
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;

        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                Num pnl = position.getProfit();
                BigDecimal profit = new BigDecimal(pnl.toString());
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    totalProfit = totalProfit.add(profit);
                } else if (profit.compareTo(BigDecimal.ZERO) < 0) {
                    totalLoss = totalLoss.add(profit.abs());
                }
            }
        }

        if (totalLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(999.9999);
        }

        return totalProfit.divide(totalLoss, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算Treynor比率
     */
    private double calculateTreynorRatio(List<BigDecimal> returns, BigDecimal riskFreeRate, double beta) {
        if (returns.isEmpty() || beta == 0.0) {
            return 0.0;
        }

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        BigDecimal annualizedReturn = avgReturn.multiply(BigDecimal.valueOf(252));
        BigDecimal excessReturn = annualizedReturn.subtract(riskFreeRate);

        return excessReturn.doubleValue() / beta;
    }

    /**
     * 计算偏度
     */
    private double calculateSkewness(List<BigDecimal> returns) {
        if (returns.size() < 3) {
            return 0.0;
        }

        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        double variance = 0.0;
        double skewnessSum = 0.0;
        int n = returns.size();

        for (BigDecimal ret : returns) {
            double diff = ret.subtract(mean).doubleValue();
            variance += diff * diff;
            skewnessSum += diff * diff * diff;
        }

        variance /= (n - 1);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0.0) {
            return 0.0;
        }

        return (skewnessSum / n) / Math.pow(stdDev, 3);
    }

    /**
     * 计算Omega比率
     */
    private BigDecimal calculateOmegaRatio(List<BigDecimal> returns, BigDecimal threshold) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal upside = BigDecimal.ZERO;
        BigDecimal downside = BigDecimal.ZERO;

        for (BigDecimal ret : returns) {
            BigDecimal excess = ret.subtract(threshold);
            if (excess.compareTo(BigDecimal.ZERO) > 0) {
                upside = upside.add(excess);
            } else {
                downside = downside.add(excess.abs());
            }
        }

        if (downside.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(999.9999);
        }

        return upside.divide(downside, 6, RoundingMode.HALF_UP);
    }

    // Getter方法
    public BigDecimal getSharpeRatio() {
        return sharpeRatio;
    }

    public BigDecimal getSortinoRatio() {
        return sortinoRatio;
    }

    public BigDecimal getCalmarRatio() {
        return calmarRatio;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public BigDecimal getMaximumSingleLoss() {
        return maximumSingleLoss;
    }

    public BigDecimal getProfitLossRatio() {
        return profitLossRatio;
    }

    public double getTreynorRatio() {
        return treynorRatio;
    }

    public double getSkewness() {
        return skewness;
    }

    public BigDecimal getOmega() {
        return omega;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    public int getProfitableTrades() {
        return profitableTrades;
    }

    public int getLosingTrades() {
        return losingTrades;
    }

    public double getWinRate() {
        return winRate;
    }
}
