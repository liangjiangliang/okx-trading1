package com.okx.trading.service.impl;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.Bar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * 回测指标计算器
 * 负责计算各种回测指标，包括收益率、风险指标、交易统计等
 */
public class BacktestMetricsCalculator {

    private static final Logger log = LoggerFactory.getLogger(BacktestMetricsCalculator.class);

    // 计算结果
    private BacktestResultDTO result;

    // 输入参数
    private final BarSeries series;
    private final TradingRecord tradingRecord;
    private final BigDecimal initialAmount;
    private final String strategyType;
    private final String paramDescription;
    private final BigDecimal feeRatio;

    // 中间计算结果
    private List<TradeRecordDTO> tradeRecords;
    private List<ArrayList<BigDecimal>> maxLossAndDrawdownList;
    private List<BigDecimal> fullPeriodStrategyReturns;
    private ArrayList<BigDecimal> dailyPrices;
    private ReturnMetrics returnMetrics;
    private RiskMetrics riskMetrics;
    private TradeStatistics tradeStats;

    /**
     * 构造器 - 在构造时完成所有指标计算
     *
     * @param series           BarSeries对象
     * @param tradingRecord    交易记录
     * @param initialAmount    初始资金
     * @param strategyType     策略类型
     * @param paramDescription 参数描述
     * @param feeRatio         交易手续费率
     */
    public BacktestMetricsCalculator(BarSeries series, TradingRecord tradingRecord,
                                     BigDecimal initialAmount, String strategyType,
                                     String paramDescription, BigDecimal feeRatio) {
        this.series = series;
        this.tradingRecord = tradingRecord;
        this.initialAmount = initialAmount;
        this.strategyType = strategyType;
        this.paramDescription = paramDescription;
        this.feeRatio = feeRatio;

        // 在构造器中完成所有指标计算
        calculateAllMetrics();
    }

    /**
     * 计算所有回测指标
     */
    private void calculateAllMetrics() {
        try {
            // 如果没有交易，返回简单结果
            if (tradingRecord.getPositionCount() == 0) {
                result = createEmptyResult();
                return;
            }

            // 1. 提取交易明细（包含手续费计算）
            tradeRecords = extractTradeRecords();

            // 2. 计算最大损失和最大回撤
            maxLossAndDrawdownList = calculateMaximumLossAndDrawdown();

            // 3. 计算交易统计指标
            tradeStats = calculateTradeStatistics();

            // 4. 计算收益率相关指标
            returnMetrics = calculateReturnMetrics(tradeStats);

            // 5. 计算风险指标
            riskMetrics = calculateRiskMetrics();

            // 6. 构建最终结果
            result = buildFinalResult();

        } catch (Exception e) {
            log.error("计算回测指标时发生错误: {}", e.getMessage(), e);
            result = createErrorResult(e.getMessage());
        }
    }

    /**
     * 创建空结果（无交易情况）
     */
    private BacktestResultDTO createEmptyResult() {
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(initialAmount);
        result.setTotalProfit(BigDecimal.ZERO);
        result.setTotalReturn(BigDecimal.ZERO);
        result.setNumberOfTrades(0);
        result.setProfitableTrades(0);
        result.setUnprofitableTrades(0);
        result.setWinRate(BigDecimal.ZERO);
        result.setAverageProfit(BigDecimal.ZERO);
        result.setMaxDrawdown(BigDecimal.ZERO);
        result.setSharpeRatio(BigDecimal.ZERO);
        result.setStrategyName(strategyType);
        result.setParameterDescription(paramDescription);
        result.setTrades(new ArrayList<>());
        result.setTotalFee(BigDecimal.ZERO);
        return result;
    }

    /**
     * 创建错误结果
     */
    private BacktestResultDTO createErrorResult(String errorMessage) {
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(false);
        result.setErrorMessage("计算回测指标时发生错误: " + errorMessage);
        return result;
    }

    /**
     * 从交易记录中提取交易明细（带手续费计算）
     */
    private List<TradeRecordDTO> extractTradeRecords() {
        List<TradeRecordDTO> records = new ArrayList<>();
        int index = 1;
        BigDecimal tradeAmount = initialAmount;

        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                // 获取入场和出场信息
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();

                Bar entryBar = series.getBar(entryIndex);
                Bar exitBar = series.getBar(exitIndex);

                ZonedDateTime entryTime = entryBar.getEndTime();
                ZonedDateTime exitTime = exitBar.getEndTime();

                BigDecimal entryPrice = new BigDecimal(entryBar.getClosePrice().doubleValue());
                BigDecimal exitPrice = new BigDecimal(exitBar.getClosePrice().doubleValue());

                // 计算入场手续费
                BigDecimal entryFee = tradeAmount.multiply(feeRatio);

                // 扣除入场手续费后的实际交易金额
                BigDecimal actualTradeAmount = tradeAmount.subtract(entryFee);

                // 交易盈亏百分比
                BigDecimal profitPercentage;

                if (position.getEntry().isBuy()) {
                    // 如果是买入操作，盈亏百分比 = (卖出价 - 买入价) / 买入价
                    profitPercentage = exitPrice.subtract(entryPrice)
                            .divide(entryPrice, 4, RoundingMode.HALF_UP);
                } else {
                    // 如果是卖出操作（做空），盈亏百分比 = (买入价 - 卖出价) / 买入价
                    profitPercentage = entryPrice.subtract(exitPrice)
                            .divide(entryPrice, 4, RoundingMode.HALF_UP);
                }

                // 计算出场金额（包含盈亏）
                BigDecimal exitAmount = actualTradeAmount.add(actualTradeAmount.multiply(profitPercentage));

                // 计算出场手续费
                BigDecimal exitFee = exitAmount.multiply(feeRatio);

                // 扣除出场手续费后的实际出场金额
                BigDecimal actualExitAmount = exitAmount.subtract(exitFee);

                // 总手续费
                BigDecimal totalFee = entryFee.add(exitFee);

                // 实际盈亏（考虑手续费）
                BigDecimal actualProfit = actualExitAmount.subtract(tradeAmount);

                // 创建交易记录DTO
                TradeRecordDTO recordDTO = new TradeRecordDTO();
                recordDTO.setIndex(index++);
                recordDTO.setType(position.getEntry().isBuy() ? "BUY" : "SELL");
                recordDTO.setEntryTime(entryTime.toLocalDateTime());
                recordDTO.setExitTime(exitTime.toLocalDateTime());
                recordDTO.setEntryPrice(entryPrice);
                recordDTO.setExitPrice(exitPrice);
                recordDTO.setEntryAmount(tradeAmount);
                recordDTO.setExitAmount(actualExitAmount);
                recordDTO.setProfit(actualProfit);
                recordDTO.setProfitPercentage(profitPercentage);
                recordDTO.setClosed(true);
                recordDTO.setFee(totalFee);

                records.add(recordDTO);

                // 更新下一次交易的资金（全仓交易）
                tradeAmount = actualExitAmount;
            }
        }

        return records;
    }

    /**
     * 计算最大损失和最大回撤
     */
    private List<ArrayList<BigDecimal>> calculateMaximumLossAndDrawdown() {
        if (series == null || series.getBarCount() == 0 || tradingRecord == null || tradingRecord.getPositionCount() == 0) {
            return Arrays.asList();
        }

        ArrayList<BigDecimal> maxLossList = new ArrayList<>();
        ArrayList<BigDecimal> drawdownList = new ArrayList<>();

        // 遍历每个已关闭的交易
        for (Position position : tradingRecord.getPositions()) {
            BigDecimal maxLoss = BigDecimal.ZERO;
            BigDecimal maxDrawdown = BigDecimal.ZERO;

            if (position.isClosed()) {
                // 获取入场和出场信息
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();
                BarSeries subSeries = series.getSubSeries(entryIndex, exitIndex + 1);

                // 获取入场和出场价格
                BigDecimal entryPrice = new BigDecimal(subSeries.getFirstBar().getClosePrice().doubleValue());
                BigDecimal exitPrice = new BigDecimal(subSeries.getLastBar().getClosePrice().doubleValue());

                BigDecimal highestPrice = BigDecimal.ZERO;
                BigDecimal lowestPrice = BigDecimal.valueOf(Long.MAX_VALUE);

                for (int i = 0; i < subSeries.getBarCount(); i++) {
                    BigDecimal closePrice = BigDecimal.valueOf(subSeries.getBar(i).getClosePrice().doubleValue());

                    if (closePrice.compareTo(highestPrice) > 0) {
                        highestPrice = closePrice;
                    }
                    if (closePrice.compareTo(lowestPrice) <= 0) {
                        lowestPrice = closePrice;
                    }

                    BigDecimal lossRate;
                    BigDecimal drawDownRate;

                    if (position.getEntry().isBuy()) {
                        // 如果是买入操作，收益率 = (卖出价 - 买入价) / 买入价
                        lossRate = closePrice.subtract(entryPrice).divide(entryPrice, 8, RoundingMode.HALF_UP);
                        drawDownRate = closePrice.subtract(highestPrice).divide(highestPrice, 8, RoundingMode.HALF_UP);
                    } else {
                        // 如果是卖出操作（做空），收益率 = (买入价 - 卖出价) / 买入价
                        lossRate = closePrice.subtract(exitPrice).divide(entryPrice, 8, RoundingMode.HALF_UP);
                        drawDownRate = closePrice.subtract(lowestPrice).divide(lowestPrice, 8, RoundingMode.HALF_UP);
                    }

                    // 只关注亏损交易
                    if (lossRate.compareTo(BigDecimal.ZERO) < 0) {
                        // 如果当前亏损大于已记录的最大亏损（更负），则更新最大亏损
                        if (lossRate.compareTo(maxLoss) < 0) {
                            maxLoss = lossRate;
                        }
                    }
                    if (drawDownRate.compareTo(BigDecimal.ZERO) < 0) {
                        if (drawDownRate.compareTo(maxDrawdown) < 0) {
                            maxDrawdown = drawDownRate;
                        }
                    }
                }
                maxLossList.add(maxLoss);
                drawdownList.add(maxDrawdown);
            }
        }

        List<ArrayList<BigDecimal>> list = new ArrayList<>();
        list.add(maxLossList);
        list.add(drawdownList);
        return list;
    }

    /**
     * 交易统计指标
     */
    private static class TradeStatistics {
        int tradeCount;
        int profitableTrades;
        BigDecimal totalProfit;
        BigDecimal totalFee;
        BigDecimal finalAmount;
        BigDecimal totalGrossProfit;
        BigDecimal totalGrossLoss;
        BigDecimal profitFactor;
        BigDecimal winRate;
        BigDecimal averageProfit;
        BigDecimal maximumLoss;
        BigDecimal maxDrawdown;
    }

    /**
     * 计算交易统计指标
     */
    private TradeStatistics calculateTradeStatistics() {
        TradeStatistics stats = new TradeStatistics();

        // 设置最大损失和最大回撤到交易记录中
        for (int i = 0; i < tradeRecords.size(); i++) {
            TradeRecordDTO trade = tradeRecords.get(i);
            trade.setMaxLoss(maxLossAndDrawdownList.get(0).get(i));
            trade.setMaxDrowdown(maxLossAndDrawdownList.get(1).get(i));
        }

        stats.tradeCount = tradeRecords.size();
        stats.profitableTrades = 0;
        stats.totalProfit = BigDecimal.ZERO;
        stats.totalFee = BigDecimal.ZERO;
        stats.finalAmount = initialAmount;
        stats.totalGrossProfit = BigDecimal.ZERO;
        stats.totalGrossLoss = BigDecimal.ZERO;

        for (TradeRecordDTO trade : tradeRecords) {
            BigDecimal profit = trade.getProfit();

            if (profit != null) {
                stats.totalProfit = stats.totalProfit.add(profit);

                // 分别累计总盈利和总亏损
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    stats.profitableTrades++;
                    stats.totalGrossProfit = stats.totalGrossProfit.add(profit);
                } else {
                    stats.totalGrossLoss = stats.totalGrossLoss.add(profit.abs());
                }
            }

            if (trade.getFee() != null) {
                stats.totalFee = stats.totalFee.add(trade.getFee());
            }
        }

        stats.finalAmount = initialAmount.add(stats.totalProfit);

        // 计算盈利因子 (Profit Factor)
        stats.profitFactor = BigDecimal.ONE;
        if (stats.totalGrossLoss.compareTo(BigDecimal.ZERO) > 0) {
            stats.profitFactor = stats.totalGrossProfit.divide(stats.totalGrossLoss, 4, RoundingMode.HALF_UP);
        } else if (stats.totalGrossProfit.compareTo(BigDecimal.ZERO) > 0) {
            stats.profitFactor = new BigDecimal("999.9999");
        }

        // 计算胜率
        stats.winRate = BigDecimal.ZERO;
        if (stats.tradeCount > 0) {
            stats.winRate = new BigDecimal(stats.profitableTrades).divide(new BigDecimal(stats.tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 计算平均盈利
        stats.averageProfit = BigDecimal.ZERO;
        if (stats.tradeCount > 0) {
            BigDecimal totalReturn = BigDecimal.ZERO;
            if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
                totalReturn = stats.totalProfit.divide(initialAmount, 4, RoundingMode.HALF_UP);
            }
            stats.averageProfit = totalReturn.divide(new BigDecimal(stats.tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 计算最大损失和最大回撤
        stats.maximumLoss = maxLossAndDrawdownList.get(0).stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
        stats.maxDrawdown = maxLossAndDrawdownList.get(1).stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);

        return stats;
    }

    /**
     * 收益率指标
     */
    private static class ReturnMetrics {
        BigDecimal totalReturn;
        BigDecimal annualizedReturn;
    }

    /**
     * 计算收益率相关指标
     */
    private ReturnMetrics calculateReturnMetrics(TradeStatistics stats) {
        ReturnMetrics metrics = new ReturnMetrics();

        // 计算总收益率
        metrics.totalReturn = BigDecimal.ZERO;
        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            metrics.totalReturn = stats.totalProfit.divide(initialAmount, 4, RoundingMode.HALF_UP);
        }

        // 计算年化收益率
        metrics.annualizedReturn = calculateAnnualizedReturn(
                metrics.totalReturn,
                series.getFirstBar().getEndTime().toLocalDateTime(),
                series.getLastBar().getEndTime().toLocalDateTime()
        );

        return metrics;
    }

    /**
     * 风险指标
     */
    private static class RiskMetrics {
        BigDecimal sharpeRatio;
        BigDecimal sortinoRatio;
        BigDecimal calmarRatio;
        BigDecimal omega;
        BigDecimal volatility;
        double[] alphaBeta;
        double treynorRatio;
        double ulcerIndex;
        double skewness;
    }

    /**
     * 计算风险指标
     */
    private RiskMetrics calculateRiskMetrics() {
        RiskMetrics metrics = new RiskMetrics();

        BigDecimal riskFreeRate = BigDecimal.valueOf(0.0001);

        // 计算夏普比率和索提诺比例 - 全周期策略收益率（包括未持仓期间的0收益）
        fullPeriodStrategyReturns = calculateFullPeriodStrategyReturns(series, tradingRecord, true);
        metrics.sharpeRatio = Ta4jBacktestService.calculateSharpeRatio(fullPeriodStrategyReturns, riskFreeRate, 252);
        metrics.omega = Ta4jBacktestService.calculateOmegaRatio(fullPeriodStrategyReturns, riskFreeRate);

        // 计算Sortino比率
        metrics.sortinoRatio = Ta4jBacktestService.calculateSortinoRatio(fullPeriodStrategyReturns, riskFreeRate, 252);

        // 计算所有日期的价格数据用于其他指标计算
        dailyPrices = new ArrayList<>();
        for (int i = 0; i <= series.getEndIndex(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            dailyPrices.add(BigDecimal.valueOf(closePrice));
        }

        // 计算波动率（基于收盘价）
        metrics.volatility = calculateVolatility(series);

        // Alpha 表示策略超额收益，Beta 表示策略相对于基准收益的敏感度（风险）
        // 暂时禁用基准数据获取，避免API限制问题
        metrics.alphaBeta = new double[]{0.0, 1.0}; // 默认Alpha=0, Beta=1

        // 计算 Treynor 比率
        metrics.treynorRatio = Ta4jBacktestService.calculateTreynorRatio(fullPeriodStrategyReturns, riskFreeRate, metrics.alphaBeta[1]);

        // 计算 Ulcer Index
        metrics.ulcerIndex = Ta4jBacktestService.calculateUlcerIndex(dailyPrices);

        // 计算收益率序列的偏度 (Skewness)
        metrics.skewness = Ta4jBacktestService.calculateSkewness(fullPeriodStrategyReturns);

        // 计算Calmar比率
        metrics.calmarRatio = Ta4jBacktestService.calculateCalmarRatio(returnMetrics.annualizedReturn, tradeStats.maxDrawdown.abs());

        return metrics;
    }

    /**
     * 构建最终结果
     */
    private BacktestResultDTO buildFinalResult() {


        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(tradeStats.finalAmount);
        result.setTotalProfit(tradeStats.totalProfit);
        result.setTotalReturn(returnMetrics.totalReturn);
        result.setAnnualizedReturn(returnMetrics.annualizedReturn);
        result.setNumberOfTrades(tradeStats.tradeCount);
        result.setProfitableTrades(tradeStats.profitableTrades);
        result.setUnprofitableTrades(tradeStats.tradeCount - tradeStats.profitableTrades);
        result.setWinRate(tradeStats.winRate);
        result.setAverageProfit(tradeStats.averageProfit);
        result.setMaxDrawdown(tradeStats.maxDrawdown);
        result.setSharpeRatio(riskMetrics.sharpeRatio);
        result.setSortinoRatio(riskMetrics.sortinoRatio);
        result.setCalmarRatio(riskMetrics.calmarRatio);
        result.setOmega(riskMetrics.omega);
        result.setMaximumLoss(tradeStats.maximumLoss);
        result.setVolatility(riskMetrics.volatility);
        result.setProfitFactor(tradeStats.profitFactor);
        result.setStrategyName(strategyType);
        result.setParameterDescription(paramDescription);
        result.setTrades(tradeRecords);
        result.setTotalFee(tradeStats.totalFee);

        return result;
    }

    /**
     * 计算全周期策略收益率序列
     */
    private List<BigDecimal> calculateFullPeriodStrategyReturns(BarSeries series, TradingRecord tradingRecord, boolean useLogReturn) {
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
        boolean[] isEntryDay = new boolean[series.getBarCount()];
        boolean[] isExitDay = new boolean[series.getBarCount()];

        // 标记所有持仓期间、买入日和卖出日
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();

                // 标记买入日和卖出日
                if (entryIndex < isEntryDay.length) {
                    isEntryDay[entryIndex] = true;
                }
                if (exitIndex < isExitDay.length) {
                    isExitDay[exitIndex] = true;
                }

                // 从入场时间点到出场时间点都标记为持仓状态
                for (int i = entryIndex; i <= exitIndex; i++) {
                    if (i < isInPosition.length) {
                        isInPosition[i] = true;
                    }
                }
            }
        }

        // 计算每个时间点的收益率
        for (int i = 1; i < series.getBarCount(); i++) {
            BigDecimal dailyReturn = BigDecimal.ZERO;

            // 边界条件1：持仓第一天（买入日）收益率为0，因为只是买入，没有收益
            if (isEntryDay[i]) {
                dailyReturn = BigDecimal.ZERO;
            }
            // 边界条件2：卖出日的后一天收益率为0（已经没有持仓）
            else if (i > 0 && isExitDay[i - 1]) {
                dailyReturn = BigDecimal.ZERO;
            }
            // 正常持仓期间：计算价格收益率（排除买入日）
            else if (isInPosition[i] && !isEntryDay[i]) {
                BigDecimal today = BigDecimal.valueOf(series.getBar(i).getClosePrice().doubleValue());
                BigDecimal yesterday = BigDecimal.valueOf(series.getBar(i - 1).getClosePrice().doubleValue());

                if (yesterday.compareTo(BigDecimal.ZERO) > 0) {
                    if (useLogReturn) {
                        double logR = Math.log(today.doubleValue() / yesterday.doubleValue());
                        dailyReturn = BigDecimal.valueOf(logR);
                    } else {
                        BigDecimal change = today.subtract(yesterday).divide(yesterday, 10, RoundingMode.HALF_UP);
                        dailyReturn = change;
                    }
                } else {
                    dailyReturn = BigDecimal.ZERO;
                }
            }
            // 未持仓期间：收益率为0
            else {
                dailyReturn = BigDecimal.ZERO;
            }

            returns.add(dailyReturn);
        }

        return returns;
    }

    /**
     * 计算波动率（基于收盘价）
     */
    private BigDecimal calculateVolatility(BarSeries series) {
        if (series == null || series.getBarCount() < 2) {
            return BigDecimal.ZERO;
        }

        // 收集所有收盘价
        List<BigDecimal> closePrices = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            closePrices.add(BigDecimal.valueOf(closePrice));
        }

        // 计算收盘价的对数收益率
        List<BigDecimal> logReturns = new ArrayList<>();
        for (int i = 1; i < closePrices.size(); i++) {
            BigDecimal today = closePrices.get(i);
            BigDecimal yesterday = closePrices.get(i - 1);

            if (yesterday.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            double logReturn = Math.log(today.doubleValue() / yesterday.doubleValue());
            logReturns.add(BigDecimal.valueOf(logReturn));
        }

        if (logReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算对数收益率的平均值
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal r : logReturns) {
            sum = sum.add(r);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(logReturns.size()), 10, RoundingMode.HALF_UP);

        // 计算对数收益率的方差
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal r : logReturns) {
            BigDecimal diff = r.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(logReturns.size()), 10, RoundingMode.HALF_UP);

        // 计算标准差（波动率）
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // 年化波动率（假设252个交易日）
        BigDecimal annualizedVolatility = stdDev.multiply(BigDecimal.valueOf(Math.sqrt(252)));

        return annualizedVolatility.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算年化收益率
     */
    private BigDecimal calculateAnnualizedReturn(BigDecimal totalReturn, LocalDateTime startTime, LocalDateTime endTime) {
        if (totalReturn == null || startTime == null || endTime == null || startTime.isAfter(endTime)) {
            log.warn("计算年化收益率的参数无效");
            return BigDecimal.ZERO;
        }

        // 计算回测持续的天数
        long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);

        // 避免除以零错误
        if (daysBetween <= 0) {
            return totalReturn; // 如果时间跨度小于1天，直接返回总收益率
        }

        // 计算年化收益率: (1 + totalReturn)^(365/daysBetween) - 1
        BigDecimal base = BigDecimal.ONE.add(totalReturn);
        BigDecimal exponent = new BigDecimal("365").divide(new BigDecimal(daysBetween), 8, RoundingMode.HALF_UP);

        BigDecimal result;
        try {
            double baseDouble = base.doubleValue();
            double exponentDouble = exponent.doubleValue();
            double power = Math.pow(baseDouble, exponentDouble);

            result = new BigDecimal(power).subtract(BigDecimal.ONE);
        } catch (Exception e) {
            log.error("计算年化收益率时出错", e);
            return BigDecimal.ZERO;
        }

        return result;
    }

    /**
     * 获取计算结果
     */
    public BacktestResultDTO getResult() {
        return result;
    }
}
