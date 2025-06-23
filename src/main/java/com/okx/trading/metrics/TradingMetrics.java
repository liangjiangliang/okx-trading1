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
import java.util.Collections;
import java.util.List;

/**
 * 交易指标计算类
 * 统一计算和管理所有交易相关的风险和收益指标
 * 提供全面的量化分析指标，用于评估交易策略的风险与收益特征
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

    // 交易统计指标
    private final int numberOfTrades;          // 交易次数
    private final int profitableTrades;        // 盈利交易次数
    private final int losingTrades;            // 亏损交易次数
    private final double winRate;              // 胜率 (%)

    // 风险收益指标
    private final BigDecimal sharpeRatio;      // 夏普比率：衡量单位风险下的超额收益
    private final BigDecimal sortinoRatio;    // 索提诺比率：衡量单位下行风险下的超额收益
    private final BigDecimal calmarRatio;     // 卡玛比率：年化收益率与最大回撤的比值
    private final BigDecimal maxDrawdown;     // 最大回撤：从峰值到谷值的最大下跌幅度
    private final BigDecimal volatility;      // 波动率：收益率的标准差
    private final BigDecimal maximumSingleLoss; // 最大单笔亏损
    private final BigDecimal profitLossRatio; // 盈亏比：平均盈利与平均亏损的比值
    private final double treynorRatio;        // 特雷诺比率：单位系统风险下的超额收益
    private final double skewness;            // 偏度：收益率分布的偏斜程度
    private final BigDecimal omega;           // Omega比率：收益与损失的比值

    // 市场风险指标
    private final double alpha;               // Alpha：相对基准的超额收益
    private final double beta;                // Beta：系统性风险系数

    // 新增高级风险指标
    private final BigDecimal informationRatio; // 信息比率：主动收益与跟踪误差的比值
    private final BigDecimal var95;           // 95%置信水平的在险价值 (VaR)
    private final BigDecimal var99;           // 99%置信水平的在险价值 (VaR)
    private final BigDecimal cvar95;          // 95%置信水平的条件在险价值 (CVaR)
    private final BigDecimal cvar99;          // 99%置信水平的条件在险价值 (CVaR)
    private final BigDecimal downsideVolatility; // 下行波动率：只考虑负收益的波动率
    private final BigDecimal romad;           // RoMaD：年化收益率与最大回撤的比值

    /**
     * 构造器：传入必要参数并计算所有指标
     * @param series 价格序列数据
     * @param tradingRecord 交易记录
     * @param initialAmount 初始资金
     * @param finalAmount 最终资金
     * @param totalPnL 总盈亏
     * @param totalFees 总手续费
     * @param riskFreeRate 无风险利率
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

        // 计算基础风险指标
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

        // 计算新增高级风险指标
        this.informationRatio = calculateInformationRatio(fullPeriodReturns, riskFreeRate);
        this.var95 = calculateVaR(fullPeriodReturns, 0.95);
        this.var99 = calculateVaR(fullPeriodReturns, 0.99);
        this.cvar95 = calculateCVaR(fullPeriodReturns, 0.95);
        this.cvar99 = calculateCVaR(fullPeriodReturns, 0.99);
        this.downsideVolatility = calculateDownsideVolatility(fullPeriodReturns);
        this.romad = calculateRoMaD();

        // 暂时设置默认值，避免API限制
        this.alpha = 0.0;
        this.beta = 1.0;
    }

    /**
     * 计算全周期策略收益率序列
     * 基于交易记录构建完整的收益率时间序列，用于后续风险指标计算
     * @return 按时间顺序的日收益率列表
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
     * 统计盈利交易和亏损交易的数量，为胜率计算提供基础数据
     * @return 数组：[盈利交易数, 亏损交易数]
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
     * 胜率是盈利交易次数占总交易次数的百分比，反映策略的成功率
     * @return 胜率百分比（0-100）
     */
    private double calculateWinRate() {
        if (numberOfTrades == 0) return 0.0;
        return (double) profitableTrades / numberOfTrades * 100;
    }

    /**
     * 计算夏普比率 (Sharpe Ratio)
     * 夏普比率衡量每单位风险获得的超额收益，是最常用的风险调整收益指标
     * 计算公式：SR = (策略年化收益率 - 无风险利率) / 策略年化波动率
     * @param returns 收益率序列
     * @param riskFreeRate 无风险利率
     * @param periodsPerYear 年化因子（日频数据通常为252）
     * @return 夏普比率
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
     * 计算索提诺比率 (Sortino Ratio)
     * 索提诺比率是夏普比率的改进版本，只考虑下行波动率，更好地衡量下行风险
     * 计算公式：Sortino = (策略年化收益率 - 无风险利率) / 下行波动率
     * @param returns 收益率序列
     * @param riskFreeRate 无风险利率
     * @param periodsPerYear 年化因子
     * @return 索提诺比率
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
     * 计算卡玛比率 (Calmar Ratio)
     * 卡玛比率是年化收益率与最大回撤绝对值的比值，衡量风险调整后的收益
     * 计算公式：Calmar = 年化收益率 / |最大回撤|
     * @param returns 收益率序列
     * @param riskFreeRate 无风险利率
     * @return 卡玛比率
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
     * 计算整个投资期间的总收益率
     * @return 总收益率（小数形式，如0.15表示15%）
     */
    private BigDecimal calculateTotalReturn() {
        if (initialAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPnL.divide(initialAmount, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算最大回撤 (Maximum Drawdown)
     * 最大回撤是从资产净值峰值到谷值的最大跌幅，衡量策略的最大风险暴露
     * 计算公式：MDD = max((Peak - Trough) / Peak)
     * @return 最大回撤（正数，如0.15表示15%的回撤）
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
     * 计算波动率 (Volatility)
     * 波动率是收益率的标准差，衡量收益率的变动程度
     * 反映投资组合收益的不确定性和风险水平
     * @return 波动率（年化）
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
     * 使用样本标准差公式计算收益率的波动性
     * @param returns 收益率序列
     * @return 波动率
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
     * 基于价格序列计算日度收益率，支持简单收益率和对数收益率
     * @param prices 价格序列
     * @param useLogReturn 是否使用对数收益率
     * @return 日收益率序列
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
     * 找出所有交易中单次损失最大的交易，用于评估极端风险
     * @return 最大单笔亏损金额（负数）
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
     * 计算盈亏比 (Profit-Loss Ratio)
     * 盈亏比是平均盈利与平均亏损的比值，衡量策略的盈利能力
     * 计算公式：PLR = 平均盈利 / 平均亏损
     * @return 盈亏比（大于1表示平均盈利大于平均亏损）
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
     * 计算特雷诺比率 (Treynor Ratio)
     * 特雷诺比率衡量每单位系统风险（Beta）获得的超额收益
     * 计算公式：TR = (策略收益率 - 无风险利率) / Beta
     * @param returns 收益率序列
     * @param riskFreeRate 无风险利率
     * @param beta 系统风险系数
     * @return 特雷诺比率
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
     * 计算偏度 (Skewness)
     * 偏度衡量收益率分布的对称性，反映极端收益的倾向
     * 正偏度：右尾较长，有较多极端正收益
     * 负偏度：左尾较长，有较多极端负收益
     * @param returns 收益率序列
     * @return 偏度值
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
     * Omega比率是收益与损失的比值，衡量策略的收益-风险特征
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

    /**
     * 计算信息比率 (Information Ratio)
     * 信息比率衡量主动管理的效果，等于主动收益与跟踪误差的比值
     * 计算公式：IR = (策略收益率 - 基准收益率) / 跟踪误差
     * 注意：这里简化使用无风险利率作为基准，实际应使用市场基准指数
     */
    private BigDecimal calculateInformationRatio(List<BigDecimal> returns, BigDecimal benchmarkReturn) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 将日收益率转换为日基准收益率（无风险利率年化后转日化）
        BigDecimal dailyBenchmark = benchmarkReturn.divide(BigDecimal.valueOf(252), 10, RoundingMode.HALF_UP);

        // 计算平均超额收益
        BigDecimal avgExcessReturn = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            avgExcessReturn = avgExcessReturn.add(ret.subtract(dailyBenchmark));
        }
        avgExcessReturn = avgExcessReturn.divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        // 计算跟踪误差（超额收益的标准差）
        BigDecimal trackingError = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal excessReturn = ret.subtract(dailyBenchmark);
            BigDecimal diff = excessReturn.subtract(avgExcessReturn);
            trackingError = trackingError.add(diff.multiply(diff));
        }
        
        if (returns.size() <= 1) {
            return BigDecimal.ZERO;
        }
        
        trackingError = trackingError.divide(BigDecimal.valueOf(returns.size() - 1), 10, RoundingMode.HALF_UP);
        trackingError = BigDecimal.valueOf(Math.sqrt(trackingError.doubleValue()));

        if (trackingError.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 年化信息比率
        return avgExcessReturn.multiply(BigDecimal.valueOf(Math.sqrt(252)))
                .divide(trackingError, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算在险价值 (Value at Risk, VaR)
     * VaR表示在给定置信水平下，投资组合在一定时间内可能遭受的最大损失
     * @param returns 收益率序列
     * @param confidenceLevel 置信水平（如0.95表示95%）
     */
    private BigDecimal calculateVaR(List<BigDecimal> returns, double confidenceLevel) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sortedReturns = new ArrayList<>(returns);
        Collections.sort(sortedReturns);

        int index = (int) Math.floor((1 - confidenceLevel) * returns.size());
        if (index >= sortedReturns.size()) {
            index = sortedReturns.size() - 1;
        }

        // VaR是负值，表示潜在损失
        return sortedReturns.get(index).negate();
    }

    /**
     * 计算条件在险价值 (Conditional Value at Risk, CVaR)
     * CVaR是VaR的改进版本，计算超过VaR阈值的平均损失
     * 也称为期望短缺 (Expected Shortfall, ES)
     * @param returns 收益率序列
     * @param confidenceLevel 置信水平（如0.95表示95%）
     */
    private BigDecimal calculateCVaR(List<BigDecimal> returns, double confidenceLevel) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sortedReturns = new ArrayList<>(returns);
        Collections.sort(sortedReturns);

        int varIndex = (int) Math.floor((1 - confidenceLevel) * returns.size());
        if (varIndex >= sortedReturns.size()) {
            varIndex = sortedReturns.size() - 1;
        }

        // 计算超过VaR的平均损失（尾部期望）
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = 0; i <= varIndex; i++) {
            sum = sum.add(sortedReturns.get(i));
            count++;
        }

        if (count == 0) {
            // 如果没有损失数据，返回VaR值
            return calculateVaR(returns, confidenceLevel);
        }

        // CVaR是期望损失的负值
        BigDecimal cvarValue = sum.divide(BigDecimal.valueOf(count), 10, RoundingMode.HALF_UP);
        BigDecimal cvar = cvarValue.compareTo(BigDecimal.ZERO) < 0 ? cvarValue.negate() : BigDecimal.ZERO;
        
        // 确保CVaR不小于VaR（由于精度问题可能出现的情况）
        BigDecimal var = calculateVaR(returns, confidenceLevel);
        return cvar.compareTo(var) >= 0 ? cvar : var;
    }

    /**
     * 计算下行波动率 (Downside Volatility)
     * 下行波动率只考虑负收益的波动性，更好地衡量下行风险
     * 只计算收益率低于目标收益率（通常为0或无风险利率）的波动性
     * 注意：计算时应该使用总样本数而不是仅下行样本数，以保持与总波动率的可比性
     */
    private BigDecimal calculateDownsideVolatility(List<BigDecimal> returns) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal target = BigDecimal.ZERO; // 目标收益率设为0
        BigDecimal downsideVariance = BigDecimal.ZERO;

        // 计算下行偏差平方和
        for (BigDecimal ret : returns) {
            if (ret.compareTo(target) < 0) {
                BigDecimal downside = ret.subtract(target);
                downsideVariance = downsideVariance.add(downside.multiply(downside));
            }
            // 如果收益率大于目标，偏差为0，不影响计算
        }

        if (returns.size() <= 1) {
            return BigDecimal.ZERO;
        }

        // 使用总样本数计算方差，保持与标准波动率的一致性
        downsideVariance = downsideVariance.divide(BigDecimal.valueOf(returns.size() - 1), 10, RoundingMode.HALF_UP);
        
        // 年化下行波动率
        BigDecimal downsideStd = BigDecimal.valueOf(Math.sqrt(downsideVariance.doubleValue()));
        return downsideStd.multiply(BigDecimal.valueOf(Math.sqrt(252)));
    }

    /**
     * 计算RoMaD (Return over Maximum Drawdown)
     * RoMaD是年化收益率与最大回撤的比值，衡量每单位回撤风险获得的收益
     * 类似于卡玛比率，但更直观地反映收益-回撤关系
     */
    private BigDecimal calculateRoMaD() {
        if (initialAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 计算总收益率
        BigDecimal totalReturn = finalAmount.subtract(initialAmount).divide(initialAmount, 6, RoundingMode.HALF_UP);
        
        // 如果没有回撤且有正收益，返回较大值；如果有回撤，计算比值
        if (maxDrawdown.compareTo(BigDecimal.ZERO) == 0) {
            if (totalReturn.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(999.9999); // 表示无回撤的正收益
            } else {
                return BigDecimal.ZERO; // 无回撤但无收益
            }
        }
        
        // 计算收益与回撤的比值
        return totalReturn.divide(maxDrawdown, 6, RoundingMode.HALF_UP);
    }

    // ==================== Getter方法 ====================
    
    // 基础风险收益指标
    /**
     * 获取夏普比率
     * @return 夏普比率
     */
    public BigDecimal getSharpeRatio() {
        return sharpeRatio;
    }

    /**
     * 获取索提诺比率
     * @return 索提诺比率
     */
    public BigDecimal getSortinoRatio() {
        return sortinoRatio;
    }

    /**
     * 获取卡玛比率
     * @return 卡玛比率
     */
    public BigDecimal getCalmarRatio() {
        return calmarRatio;
    }

    /**
     * 获取最大回撤
     * @return 最大回撤
     */
    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    /**
     * 获取波动率
     * @return 波动率
     */
    public BigDecimal getVolatility() {
        return volatility;
    }

    /**
     * 获取最大单笔亏损
     * @return 最大单笔亏损
     */
    public BigDecimal getMaximumSingleLoss() {
        return maximumSingleLoss;
    }

    /**
     * 获取盈亏比
     * @return 盈亏比
     */
    public BigDecimal getProfitLossRatio() {
        return profitLossRatio;
    }

    /**
     * 获取特雷诺比率
     * @return 特雷诺比率
     */
    public double getTreynorRatio() {
        return treynorRatio;
    }

    /**
     * 获取偏度
     * @return 偏度
     */
    public double getSkewness() {
        return skewness;
    }

    /**
     * 获取Omega比率
     * @return Omega比率
     */
    public BigDecimal getOmega() {
        return omega;
    }

    /**
     * 获取Alpha值
     * @return Alpha
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * 获取Beta值
     * @return Beta
     */
    public double getBeta() {
        return beta;
    }

    // 新增高级风险指标的getter方法
    /**
     * 获取信息比率
     * @return 信息比率
     */
    public BigDecimal getInformationRatio() {
        return informationRatio;
    }

    /**
     * 获取95%置信水平的VaR
     * @return 95% VaR
     */
    public BigDecimal getVar95() {
        return var95;
    }

    /**
     * 获取99%置信水平的VaR
     * @return 99% VaR
     */
    public BigDecimal getVar99() {
        return var99;
    }

    /**
     * 获取95%置信水平的CVaR
     * @return 95% CVaR
     */
    public BigDecimal getCvar95() {
        return cvar95;
    }

    /**
     * 获取99%置信水平的CVaR
     * @return 99% CVaR
     */
    public BigDecimal getCvar99() {
        return cvar99;
    }

    /**
     * 获取下行波动率
     * @return 下行波动率
     */
    public BigDecimal getDownsideVolatility() {
        return downsideVolatility;
    }

    /**
     * 获取RoMaD
     * @return RoMaD（收益回撤比）
     */
    public BigDecimal getRomad() {
        return romad;
    }

    // 交易统计指标
    /**
     * 获取交易次数
     * @return 交易次数
     */
    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    /**
     * 获取盈利交易次数
     * @return 盈利交易次数
     */
    public int getProfitableTrades() {
        return profitableTrades;
    }

    /**
     * 获取亏损交易次数
     * @return 亏损交易次数
     */
    public int getLosingTrades() {
        return losingTrades;
    }

    /**
     * 获取胜率
     * @return 胜率（百分比）
     */
    public double getWinRate() {
        return winRate;
    }
}
