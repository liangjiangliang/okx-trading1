package com.okx.trading.ta4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.ta4j.strategy.StrategyFactory;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.*;
import org.ta4j.core.cost.CostModel;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.cost.ZeroCostModel;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import ta4jexamples.logging.StrategyExecutionLogging;

/**
 * TA4J 回测服务类
 */
@Service
public class Ta4jBacktestService {

    private static final Logger log = LoggerFactory.getLogger(Ta4jBacktestService.class);

    private static final URL LOGBACK_CONF_FILE = StrategyExecutionLogging.class.getClassLoader()
            .getResource("logback-traces.xml");

    @Autowired
    private CandlestickBarSeriesConverter barSeriesConverter;

    /**
     * 执行回测
     *
     * @param candlesticks  历史K线数据
     * @param strategyType  策略类型
     * @param initialAmount 初始资金
     * @param feeRatio      交易手续费率（例如0.001表示0.1%）
     * @return 回测结果
     */
    public BacktestResultDTO backtest(List<CandlestickEntity> candlesticks, String strategyType,
                                      BigDecimal initialAmount, BigDecimal feeRatio) {

        //   loadLoggerConfiguration();
        if (candlesticks == null || candlesticks.isEmpty()) {
            BacktestResultDTO result = new BacktestResultDTO();
            result.setSuccess(false);
            result.setErrorMessage("没有足够的历史数据进行回测");
            return result;
        }

        try {
            // 生成唯一的系列名称
            String seriesName = CandlestickAdapter.getSymbol(candlesticks.get(0)) + "_" + CandlestickAdapter.getIntervalVal(candlesticks.get(0));

            // 使用转换器将蜡烛图实体转换为条形系列
            BarSeries series = barSeriesConverter.convert(candlesticks, seriesName);

            // 使用策略工厂创建策略
            Strategy strategy = StrategyFactory.createStrategy(series, strategyType);

            // 执行回测
            BarSeriesManager seriesManager = new BarSeriesManager(series, new ZeroCostModel(), new ZeroCostModel());
            TradingRecord tradingRecord = seriesManager.run(strategy, Trade.TradeType.BUY);

            //   unloadLoggerConfiguration();
            // 计算回测指标
            return calculateBacktestMetrics(series, tradingRecord, initialAmount, strategyType.toString(), "", feeRatio);
        } catch (Exception e) {
            log.error("回测过程中发生错误: {}", e.getMessage(), e);
            BacktestResultDTO result = new BacktestResultDTO();
            result.setSuccess(false);
            result.setErrorMessage("回测过程中发生错误: " + e.getMessage());
            return result;
        }
    }

    /**
     * 执行回测（不带手续费参数的重载方法，使用默认手续费率0）
     *
     * @param candlesticks  历史K线数据
     * @param strategyType  策略类型
     * @param initialAmount 初始资金
     * @param params        策略参数
     * @return 回测结果
     */
    public BacktestResultDTO backtest(List<CandlestickEntity> candlesticks, String strategyType,
                                      BigDecimal initialAmount) {
        return backtest(candlesticks, strategyType, initialAmount, BigDecimal.ZERO);
    }

    /**
     * 计算回测指标
     *
     * @param series           BarSeries对象
     * @param tradingRecord    交易记录
     * @param initialAmount    初始资金
     * @param strategyType     策略类型
     * @param paramDescription 参数描述
     * @param feeRatio         交易手续费率
     * @return 回测结果DTO
     */
    private BacktestResultDTO calculateBacktestMetrics(BarSeries series, TradingRecord tradingRecord,
                                                       BigDecimal initialAmount, String strategyType,
                                                       String paramDescription, BigDecimal feeRatio) {
        // 如果没有交易，返回简单结果
        if (tradingRecord.getPositionCount() == 0) {
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

            // 打印回测汇总信息
            printBacktestSummary(result);

            return result;
        }

        // 提取交易明细（包含手续费计算）
        List<TradeRecordDTO> tradeRecords = extractTradeRecords(series, tradingRecord, initialAmount, feeRatio);

        // 计算交易指标
        int tradeCount = tradeRecords.size();
        int profitableTrades = 0;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal finalAmount = initialAmount;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;  // 总盈利
        BigDecimal totalGrossLoss = BigDecimal.ZERO;    // 总亏损

        for (TradeRecordDTO trade : tradeRecords) {
            BigDecimal profit = trade.getProfit();

            if (profit != null) {
                totalProfit = totalProfit.add(profit);

                // 分别累计总盈利和总亏损
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    profitableTrades++;
                    totalGrossProfit = totalGrossProfit.add(profit);
                } else {
                    totalGrossLoss = totalGrossLoss.add(profit.abs());
                }
            }

            if (trade.getFee() != null) {
                totalFee = totalFee.add(trade.getFee());
            }
        }

        finalAmount = initialAmount.add(totalProfit);

        // 计算盈利因子 (Profit Factor)
        BigDecimal profitFactor = BigDecimal.ONE;  // 默认为1
        if (totalGrossLoss.compareTo(BigDecimal.ZERO) > 0) {
            profitFactor = totalGrossProfit.divide(totalGrossLoss, 4, RoundingMode.HALF_UP);
        } else if (totalGrossProfit.compareTo(BigDecimal.ZERO) > 0) {
            // 如果没有亏损交易但有盈利交易，设置为较大值表示无穷大
            profitFactor = new BigDecimal("999.9999");
        }

        // 计算各项指标
        BigDecimal totalReturn = BigDecimal.ZERO;
        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalReturn = totalProfit.divide(initialAmount, 4, RoundingMode.HALF_UP);
        }

        BigDecimal winRate = BigDecimal.ZERO;
        if (tradeCount > 0) {
            winRate = new BigDecimal(profitableTrades).divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP);
        }

        BigDecimal averageProfit = BigDecimal.ZERO;
        if (tradeCount > 0) {
            averageProfit = totalReturn.divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 计算最大回撤
//        BigDecimal maxDrawdown = calculateMaxDrawdown(series, tradeRecords, initialAmount);
        // 最大回撤
        double maxiunDrawdown = new MaximumDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
//        double returnOverMaxDrawdown = new ReturnOverMaxDrawdownCriterion().calculate(series, tradingRecord).doubleValue();
//        double buyAndHoldReturnCriterion = new BuyAndHoldReturnCriterion().calculate(series, tradingRecord).doubleValue();
        //每根K线平均收益率（单位时间收益）
//        double averageReturnPerBarCriterion = new AverageReturnPerBarCriterion().calculate(series, tradingRecord).doubleValue();
        //连续盈利最大次数
//        int numberOfConsecutiveWinningPositions = new NumberOfConsecutiveWinningPositionsCriterion().calculate(series, tradingRecord).intValue();
//        double expectancyCriterion = new ExpectancyCriterion().calculate(series, tradingRecord).doubleValue();
        //线性手续费损耗影响评估（成本）
//        double linearTransactionCostCriterion = new LinearTransactionCostCriterion(initialAmount.doubleValue(),feeRatio.doubleValue()).calculate(series, tradingRecord).doubleValue();
        //    | 类名                                 | 含义                        |
        //    | ---------------------------------- | ------------------------- |
        //    | **AverageReturnPerBarCriterion**   | 每根K线平均收益率（单位时间收益）         |
        //    | **BuyAndHoldReturnCriterion**      | “买入并持有”策略的总收益率，用作基准       |
        //    | **VersusBuyAndHoldCriterion**      | 当前策略相对于买入并持有的表现比值         |
        //    | **ReturnOverMaxDrawdownCriterion** | 年化收益 / 最大回撤，比 Calmar 指标类似 |
        //    | ---------------------------------- | ---------------------- |
        //    | **MaximumDrawdownCriterion**       | 最大回撤（最大历史亏损）           |
        //    | **ValueAtRiskCriterion**           | VaR：某置信水平下最大可能损失（统计风险） |
        //    | **ExpectedShortfallCriterion**     | CVaR：条件在VaR之下的平均亏损     |
        //    | **LinearTransactionCostCriterion** | 线性手续费损耗影响评估（成本）        |
        //    | ---------------------------------- | ---------------------- |
        //    | **NumberOfBarsCriterion**               | 使用的K线根数（活跃度） |
        //    | **NumberOfPositionsCriterion**          | 总交易次数（开仓次数）  |
        //    | **NumberOfBreakEvenPositionsCriterion** | 盈亏为零的平仓次数    |
        //    | ----------------------------- | ------------------------------------------- |
        //    | **ExpectancyCriterion**       | 期望收益率（考虑胜率和盈亏比）                             |
        //    | **AbstractAnalysisCriterion** | 所有评估类的抽象基类                                  |


        // 计算夏普比率
//        BigDecimal sharpeRatio = calculateSharpeRatio(tradeRecords, initialAmount);
        ArrayList<BigDecimal> dailyPrice = new ArrayList<>();
        for (int i = 0; i <= series.getEndIndex(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            dailyPrice.add(BigDecimal.valueOf(closePrice));
        }
        List<BigDecimal> dailyReturns = computeDailyReturns(dailyPrice, true);
        BigDecimal sharpeRatio1 = calculateSharpeRatio(dailyReturns, BigDecimal.valueOf(0.0001), 252);

        // 构建回测结果
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(finalAmount);
        result.setTotalProfit(totalProfit);
        result.setTotalReturn(totalReturn);
        result.setNumberOfTrades(tradeCount);
        result.setProfitableTrades(profitableTrades);
        result.setUnprofitableTrades(tradeCount - profitableTrades);
        result.setWinRate(winRate);
        result.setAverageProfit(averageProfit);
        result.setMaxDrawdown(BigDecimal.valueOf(maxiunDrawdown));
        result.setSharpeRatio(sharpeRatio1);
        result.setProfitFactor(profitFactor);
        result.setStrategyName(strategyType);
        result.setParameterDescription(paramDescription);
        result.setTrades(tradeRecords);
        result.setTotalFee(totalFee);

        // 打印回测汇总信息
        printBacktestSummary(result);

        return result;
    }

    /**
     * 计算最大回撤
     *
     * @param series        BarSeries对象
     * @param trades        交易记录列表
     * @param initialAmount 初始资金
     * @return 最大回撤（百分比）
     */
    private BigDecimal calculateMaxDrawdown(BarSeries series, List<TradeRecordDTO> trades, BigDecimal initialAmount) {
        if (trades == null || trades.isEmpty() || series == null || series.getBarCount() == 0) {
            return BigDecimal.ZERO;
        }

        // 创建一个映射，用于存储每个时间点的资产价值
        Map<ZonedDateTime, BigDecimal> equityCurve = new HashMap<>();

        // 初始资产价值
        BigDecimal currentValue = initialAmount;
        equityCurve.put(series.getFirstBar().getEndTime(), currentValue);

        // 遍历每笔交易，计算交易期间的资产价值变化
        for (TradeRecordDTO trade : trades) {
            // 获取交易的进场和出场时间
            LocalDateTime entryLocalTime = trade.getEntryTime();
            LocalDateTime exitLocalTime = trade.getExitTime();
            if (entryLocalTime == null || exitLocalTime == null) {
                continue;
            }

            // 转换为ZonedDateTime以匹配BarSeries中的时间格式
            ZonedDateTime entryTime = entryLocalTime.atZone(ZoneId.systemDefault());
            ZonedDateTime exitTime = exitLocalTime.atZone(ZoneId.systemDefault());

            // 找到进场和出场时间对应的Bar索引
            int entryIndex = -1;
            int exitIndex = -1;

            for (int i = 0; i < series.getBarCount(); i++) {
                ZonedDateTime barTime = series.getBar(i).getEndTime();

                // 找到最接近进场时间的Bar
                if (entryIndex == -1 && (barTime.equals(entryTime) || barTime.isAfter(entryTime))) {
                    entryIndex = i;
                }

                // 找到最接近出场时间的Bar
                if (barTime.equals(exitTime) || barTime.isAfter(exitTime)) {
                    exitIndex = i;
                    break;
                }
            }

            // 如果找不到对应的Bar，则继续下一笔交易
            if (entryIndex == -1 || exitIndex == -1 || entryIndex >= exitIndex) {
                continue;
            }

            // 计算交易期间的资产价值变化
            BigDecimal entryAmount = trade.getEntryAmount();
            BigDecimal profitPercentage = trade.getProfitPercentage();

            // 记录进场时的资产价值
            equityCurve.put(series.getBar(entryIndex).getEndTime(), currentValue);

            // 计算交易期间每个Bar的资产价值
            for (int i = entryIndex + 1; i <= exitIndex; i++) {
                Bar bar = series.getBar(i);
                ZonedDateTime barTime = bar.getEndTime();

                // 计算当前Bar时的盈亏百分比
                BigDecimal currentBarPrice = new BigDecimal(bar.getClosePrice().doubleValue());
                BigDecimal entryPrice = trade.getEntryPrice();

                BigDecimal currentProfitPercentage;
                if ("BUY".equals(trade.getType())) {
                    // 如果是买入操作，盈亏百分比 = (当前价 - 买入价) / 买入价
                    currentProfitPercentage = currentBarPrice.subtract(entryPrice)
                            .divide(entryPrice, 8, RoundingMode.HALF_UP);
                } else {
                    // 如果是卖出操作（做空），盈亏百分比 = (买入价 - 当前价) / 买入价
                    currentProfitPercentage = entryPrice.subtract(currentBarPrice)
                            .divide(entryPrice, 8, RoundingMode.HALF_UP);
                }

                // 计算手续费（如果有）
                BigDecimal fee = BigDecimal.ZERO;
                if (trade.getFee() != null) {
                    // 假设手续费在交易期间平均分配
                    fee = trade.getFee().divide(new BigDecimal(exitIndex - entryIndex + 1), 8, RoundingMode.HALF_UP);
                }

                // 计算当前Bar时的资产价值
                BigDecimal barValue = entryAmount.add(entryAmount.multiply(currentProfitPercentage)).subtract(fee);

                // 更新当前资产价值（考虑非交易资金）
                BigDecimal nonTradingAmount = currentValue.subtract(entryAmount);
                BigDecimal totalValue = nonTradingAmount.add(barValue);

                // 记录当前Bar时的资产价值
                equityCurve.put(barTime, totalValue);
            }

            // 更新当前资产价值（交易结束后）
            if (trade.getExitAmount() != null) {
                currentValue = trade.getExitAmount();
                // 记录出场时的资产价值
                equityCurve.put(series.getBar(exitIndex).getEndTime(), currentValue);
            }
        }

        // 计算最大回撤
        BigDecimal highestValue = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        // 按时间顺序排序所有时间点
        List<ZonedDateTime> timePoints = new ArrayList<>(equityCurve.keySet());
        timePoints.sort(Comparator.naturalOrder());

        for (ZonedDateTime time : timePoints) {
            BigDecimal value = equityCurve.get(time);

            // 更新历史最高资产价值
            if (value.compareTo(highestValue) > 0) {
                highestValue = value;
            }

            // 计算当前回撤
            if (highestValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDrawdown = highestValue.subtract(value)
                        .divide(highestValue, 8, RoundingMode.HALF_UP);

                // 更新最大回撤
                if (currentDrawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = currentDrawdown;
                }
            }
        }

        return maxDrawdown;
    }

    /**
     * 计算夏普比率
     * 夏普比率 = (投资组合收益率 - 无风险收益率) / 投资组合标准差
     *
     * @param trades        交易记录列表
     * @param initialAmount 初始资金
     * @return 夏普比率
     */
    private BigDecimal calculateSharpeRatio(List<TradeRecordDTO> trades, BigDecimal initialAmount) {
        if (trades == null || trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算每笔交易的收益率
        List<BigDecimal> returns = new ArrayList<>();

        for (TradeRecordDTO trade : trades) {
            if (trade.getProfitPercentage() != null) {
                returns.add(trade.getProfitPercentage());
            }
        }

        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算平均收益率
        BigDecimal sumReturns = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            sumReturns = sumReturns.add(ret);
        }
        BigDecimal avgReturn = sumReturns.divide(new BigDecimal(returns.size()), 8, RoundingMode.HALF_UP);

        // 计算标准差
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(avgReturn);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }

        BigDecimal variance = sumSquaredDiff.divide(new BigDecimal(returns.size()), 8, RoundingMode.HALF_UP);
        BigDecimal stdDev = new BigDecimal(Math.sqrt(variance.doubleValue()));

        // 无风险收益率（假设为0）
        BigDecimal riskFreeRate = BigDecimal.ZERO;

        // 计算夏普比率
        if (stdDev.compareTo(BigDecimal.ZERO) > 0) {
            return avgReturn.subtract(riskFreeRate).divide(stdDev, 4, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算年化 Sharpe 比率
     *
     * @param dailyReturns        每日收益率序列（建议为对数收益率），如 [0.0023, -0.0011, 0.0005, ...]
     * @param riskFreeRate        无风险日收益率（例如设为 0.0001；或 0 表示忽略）
     * @param annualizationFactor 年化因子（例如 252 表示按每日）
     * @return Sharpe 比率（保留 6 位小数）
     */
    public static BigDecimal calculateSharpeRatio(List<BigDecimal> dailyReturns,
                                                  BigDecimal riskFreeRate,
                                                  int annualizationFactor) {
        if (dailyReturns == null || dailyReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 平均收益率
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal r : dailyReturns) {
            sum = sum.add(r);
        }
        BigDecimal avgReturn = sum.divide(BigDecimal.valueOf(dailyReturns.size()), 10, RoundingMode.HALF_UP);

        // 收益率标准差（波动率）
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal r : dailyReturns) {
            BigDecimal diff = r.subtract(avgReturn);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(dailyReturns.size()), 10, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Sharpe = (mean - riskFree) / stdDev × √annualizationFactor
        BigDecimal sharpe = avgReturn.subtract(riskFreeRate).divide(stdDev, 10, RoundingMode.HALF_UP);
        BigDecimal sqrtAnnualFactor = BigDecimal.valueOf(Math.sqrt(annualizationFactor));

        return sharpe.multiply(sqrtAnnualFactor).setScale(6, RoundingMode.HALF_UP);
    }

    public static List<BigDecimal> computeDailyReturns(List<BigDecimal> prices, boolean useLogReturn) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal today = prices.get(i);
            BigDecimal yesterday = prices.get(i - 1);

            if (yesterday.compareTo(BigDecimal.ZERO) == 0) continue;

            if (useLogReturn) {
                double logR = Math.log(today.doubleValue() / yesterday.doubleValue());
                returns.add(BigDecimal.valueOf(logR));
            } else {
                BigDecimal change = today.subtract(yesterday).divide(yesterday, 10, RoundingMode.HALF_UP);
                returns.add(change);
            }
        }
        return returns;
    }


    /**
     * 从交易记录中提取交易明细（带手续费计算）
     *
     * @param series        BarSeries对象
     * @param tradingRecord 交易记录
     * @param initialAmount 初始资金
     * @param feeRatio      交易手续费率
     * @return 交易记录DTO列表
     */
    private List<TradeRecordDTO> extractTradeRecords(BarSeries series, TradingRecord tradingRecord,
                                                     BigDecimal initialAmount, BigDecimal feeRatio) {
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
     * 打印回测汇总信息到控制台
     *
     * @param result 回测结果
     */
    private void printBacktestSummary(BacktestResultDTO result) {
        StringBuilder summaryBuilder = new StringBuilder();

        // 构建分隔线
        String separator = "================================================================";

        summaryBuilder.append("\n").append(separator).append("\n");
        summaryBuilder.append("==================== 回测汇总信息 ====================\n");
        summaryBuilder.append(separator).append("\n");

        // 策略信息
        summaryBuilder.append("策略名称: ").append(result.getStrategyName()).append("\n");
        summaryBuilder.append("策略参数: ").append(result.getParameterDescription()).append("\n");
        summaryBuilder.append("------------------------------------------------------\n");

        // 财务指标
        String initialAmountFormatted = String.format("%,.2f", result.getInitialAmount());
        String finalAmountFormatted = String.format("%,.2f", result.getFinalAmount());
        String totalProfitFormatted = String.format("%,.2f", result.getTotalProfit());
        String totalReturnFormatted = String.format("%.2f%%", result.getTotalReturn().multiply(new BigDecimal("100")));
        String totalFeeFormatted = String.format("%,.2f", result.getTotalFee() != null ? result.getTotalFee() : BigDecimal.ZERO);

        summaryBuilder.append("初始资金: ").append(initialAmountFormatted).append("\n");
        summaryBuilder.append("最终资金: ").append(finalAmountFormatted).append("\n");
        summaryBuilder.append("总盈亏: ").append(totalProfitFormatted).append("\n");
        summaryBuilder.append("总收益率: ").append(totalReturnFormatted).append("\n");
        summaryBuilder.append("总手续费: ").append(totalFeeFormatted).append("\n");
        summaryBuilder.append("------------------------------------------------------\n");

        // 交易指标
        String winRateFormatted = String.format("%.2f%%", result.getWinRate().multiply(new BigDecimal("100")));
        String maxDrawdownFormatted = String.format("%.2f%%", result.getMaxDrawdown().multiply(new BigDecimal("100")));

        summaryBuilder.append("交易次数: ").append(result.getNumberOfTrades()).append("\n");
        summaryBuilder.append("盈利交易: ").append(result.getProfitableTrades()).append("\n");
        summaryBuilder.append("亏损交易: ").append(result.getUnprofitableTrades()).append("\n");
        summaryBuilder.append("胜率: ").append(winRateFormatted).append("\n");
        summaryBuilder.append("夏普比率: ").append(String.format("%.4f", result.getSharpeRatio())).append("\n");
        summaryBuilder.append("最大回撤: ").append(maxDrawdownFormatted).append("\n");
        summaryBuilder.append(separator).append("\n");

        // 输出汇总信息
        log.info(summaryBuilder.toString());
    }

    private static void loadLoggerConfiguration() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(LOGBACK_CONF_FILE);
        } catch (JoranException je) {
            java.util.logging.Logger.getLogger(StrategyExecutionLogging.class.getName()).log(Level.SEVERE,
                    "Unable to load Logback configuration", je);
        }
    }

    private static void unloadLoggerConfiguration() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
    }
}
