package com.okx.trading.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.okx.trading.strategy.StrategyRegisterCenter;
import com.okx.trading.adapter.CandlestickBarSeriesConverter;
import com.okx.trading.util.BenchmarkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import org.ta4j.core.cost.ZeroCostModel;
import ta4jexamples.logging.StrategyExecutionLogging;

/**
 * TA4J 回测服务类
 */
@Service
@Component
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
    public BacktestResultDTO backtest(BarSeries series, String strategyType, BigDecimal initialAmount, BigDecimal feeRatio) {

        // loadLoggerConfiguration();
        try {
            // 使用策略工厂创建策略
            Strategy strategy = StrategyRegisterCenter.createStrategy(series, strategyType);

            // 执行回测
            BarSeriesManager seriesManager = new BarSeriesManager(series, new ZeroCostModel(), new ZeroCostModel());
            TradingRecord tradingRecord = seriesManager.run(strategy, Trade.TradeType.BUY);

            // unloadLoggerConfiguration();
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
                                                       String paramDescription, BigDecimal feeRatio) throws Exception {
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

        // 计算最大损失和最大回撤（基于收盘价，不考虑手续费）
        List<ArrayList<BigDecimal>> list = calculateMaximumLossAndDrawdown(series, tradingRecord);

        // 计算交易指标
        int tradeCount = tradeRecords.size();
        int profitableTrades = 0;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal finalAmount = initialAmount;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;  // 总盈利
        BigDecimal totalGrossLoss = BigDecimal.ZERO;    // 总亏损
        BigDecimal riskFreeRate = BigDecimal.valueOf(0.0001);


        for (int i = 0; i < tradeRecords.size(); i++) {
            TradeRecordDTO trade = tradeRecords.get(i);

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
            trade.setMaxLoss(list.get(0).get(i));
            trade.setMaxDrowdown(list.get(1).get(i));
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

        // 计算年化收益率
        BigDecimal annualizedReturn = calculateAnnualizedReturn(
                totalReturn,
                series.getFirstBar().getEndTime().toLocalDateTime(),
                series.getLastBar().getEndTime().toLocalDateTime()
        );


        BigDecimal maximumLoss = list.get(0).stream().reduce(BigDecimal::min).get();
        BigDecimal maxDrawdown = list.get(1).stream().reduce(BigDecimal::min).get();


        // 计算Calmar比率
        BigDecimal calmarRatio = calculateCalmarRatio(annualizedReturn, maxDrawdown.abs());


        BigDecimal winRate = BigDecimal.ZERO;
        if (tradeCount > 0) {
            winRate = new BigDecimal(profitableTrades).divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP);
        }

        BigDecimal averageProfit = BigDecimal.ZERO;
        if (tradeCount > 0) {
            averageProfit = totalReturn.divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 计算夏普比率
        ArrayList<BigDecimal> dailyPrice = new ArrayList<>();
        for (int i = 0; i <= series.getEndIndex(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            dailyPrice.add(BigDecimal.valueOf(closePrice));
        }
        List<BigDecimal> dailyReturns = computeDailyReturns(dailyPrice, true);
        BigDecimal sharpeRatio = calculateSharpeRatio(dailyReturns, riskFreeRate, 252);
        BigDecimal omega = calculateOmegaRatio(dailyReturns, riskFreeRate); // 以0.01%为阈值

        // 计算Sortino比率
        BigDecimal sortinoRatio = calculateSortinoRatio(dailyReturns, riskFreeRate, 252);

        // 计算波动率（基于收盘价）
        BigDecimal volatility = calculateVolatility(series);

        // Alpha 表示策略超额收益，Beta 表示策略相对于基准收益的敏感度（风险）
        List<BigDecimal> benchmarkReturns = BenchmarkUtils.fetchHistoricalClosePrices("000300.SS", series.getFirstBar().getEndTime(), series.getLastBar().getEndTime());
        double[] alphaBeta = calculateAlphaBeta(dailyReturns, benchmarkReturns);

        //  * 计算 Treynor 比率
        //  * 用 Beta 衡量系统性风险，计算单位系统风险的超额收益
        double treynorRatio = calculateTreynorRatio(dailyReturns, riskFreeRate, alphaBeta[1]);


        //  * 计算 Ulcer Index
        //  * Ulcer Index 主要衡量资产的最大回撤的深度和持续时间，数值越大代表风险越大。
        double ulcerIndex = calculateUlcerIndex(dailyPrice);

        //  * 计算收益率序列的偏度 (Skewness)
        //  * 偏度反映数据分布的非对称性，正偏说明右尾重，负偏说明左尾重
        double skewness = calculateSkewness(dailyReturns);

        // 构建回测结果
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(finalAmount);
        result.setTotalProfit(totalProfit);
        result.setTotalReturn(totalReturn);
        result.setAnnualizedReturn(annualizedReturn);
        result.setNumberOfTrades(tradeCount);
        result.setProfitableTrades(profitableTrades);
        result.setUnprofitableTrades(tradeCount - profitableTrades);
        result.setWinRate(winRate);
        result.setAverageProfit(averageProfit);
        result.setMaxDrawdown(maxDrawdown);
        result.setSharpeRatio(sharpeRatio);
        result.setSortinoRatio(sortinoRatio);
        result.setCalmarRatio(calmarRatio);
        result.setOmega(omega);
        result.setMaximumLoss(maximumLoss);
        result.setVolatility(volatility);
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
     * 计算最大损失（基于收盘价，不考虑手续费）
     * <p>
     * 最大损失是指单笔交易中的最大亏损百分比，用于评估策略的最坏情况风险。
     * 它反映了策略在单次交易中可能遭受的最大损失，是风险管理的重要指标。
     * 此方法直接基于K线收盘价计算，不考虑手续费等交易成本。
     * <p>
     * 使用场景：
     * 1. 设置止损点位和风险控制阈值
     * 2. 评估策略的极端风险暴露
     * 3. 计算资金需求和风险承受能力
     * <p>
     * 解读：
     * - 值越小越好（绝对值越小）
     * - 应与账户规模相比较，评估其影响
     * - 可用于设置每笔交易的最大风险敞口
     *
     * @param series        BarSeries对象
     * @param tradingRecord 交易记录
     * @return 最大损失百分比（负值，表示亏损）
     */
    public static List<ArrayList<BigDecimal>> calculateMaximumLossAndDrawdown(BarSeries series, TradingRecord tradingRecord) {

        if (series == null || series.getBarCount() == 0 || tradingRecord == null || tradingRecord.getPositionCount() == 0) {
            return Arrays.asList();
        }
        ArrayList<BigDecimal> maxLossList = new ArrayList<>();
        ArrayList<BigDecimal> drawdownList = new ArrayList<>();

        // 遍历每个已关闭的交易
        for (Position position : tradingRecord.getPositions()) {
            BigDecimal maxLoss = BigDecimal.ZERO;
            BigDecimal maxDrawdown = BigDecimal.ZERO;
            // 计算收益率
            BigDecimal lossRate = BigDecimal.ZERO;
            // 计算亏损率
            BigDecimal highestPrice = BigDecimal.ZERO;
            BigDecimal lowestPrice = BigDecimal.valueOf(Long.MAX_VALUE);
            BigDecimal drawDownRate = BigDecimal.ZERO;
            if (position.isClosed()) {
                // 获取入场和出场信息
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();
                BarSeries subSeries = series.getSubSeries(entryIndex, exitIndex + 1);

                // 获取入场和出场价格
                BigDecimal entryPrice = new BigDecimal(subSeries.getFirstBar().getClosePrice().doubleValue());
                BigDecimal exitPrice = new BigDecimal(subSeries.getLastBar().getClosePrice().doubleValue());

                for (int i = 0; i < subSeries.getBarCount(); i++) {
                    BigDecimal closePrice = BigDecimal.valueOf(subSeries.getBar(i).getClosePrice().doubleValue());

                    if (closePrice.compareTo(highestPrice) > 0) {
                        highestPrice = closePrice;
                    }
                    if (closePrice.compareTo(lowestPrice) <= 0) {
                        lowestPrice = closePrice;
                    }

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
     * 计算最大损失
     * <p>
     * 最大损失是指单笔交易中的最大亏损金额，用于评估策略的最坏情况风险。
     * 它反映了策略在单次交易中可能遭受的最大损失，是风险管理的重要指标。
     * <p>
     * 使用场景：
     * 1. 设置止损点位和风险控制阈值
     * 2. 评估策略的极端风险暴露
     * 3. 计算资金需求和风险承受能力
     * <p>
     * 解读：
     * - 值越小越好（绝对值越小）
     * - 应与账户规模相比较，评估其影响
     * - 可用于设置每笔交易的最大风险敞口
     *
     * @param trades 交易记录列表
     * @return 最大损失金额（负值，表示亏损）
     */
    public static BigDecimal calculateMaximumLossFromTrades(List<TradeRecordDTO> trades) {
        if (trades == null || trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxLoss = BigDecimal.ZERO;

        for (TradeRecordDTO trade : trades) {
            BigDecimal profit = trade.getProfit();

            // 只考虑亏损交易
            if (profit != null && profit.compareTo(BigDecimal.ZERO) < 0) {
                // 如果当前亏损大于已记录的最大亏损（更负），则更新最大亏损
                if (profit.compareTo(maxLoss) < 0) {
                    maxLoss = profit;
                }
            }
        }

        return maxLoss;
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
        String maximumLossFormatted = String.format("%,.2f", result.getMaximumLoss() != null ? result.getMaximumLoss() : BigDecimal.ZERO);

        summaryBuilder.append("交易次数: ").append(result.getNumberOfTrades()).append("\n");
        summaryBuilder.append("盈利交易: ").append(result.getProfitableTrades()).append("\n");
        summaryBuilder.append("亏损交易: ").append(result.getUnprofitableTrades()).append("\n");
        summaryBuilder.append("胜率: ").append(winRateFormatted).append("\n");

        // 风险指标
        summaryBuilder.append("------------------------------------------------------\n");
        summaryBuilder.append("风险评估指标:\n");
        summaryBuilder.append("夏普比率: ").append(String.format("%.4f", result.getSharpeRatio())).append("\n");
        summaryBuilder.append("索提诺比率: ").append(String.format("%.4f", result.getSortinoRatio() != null ? result.getSortinoRatio() : BigDecimal.ZERO)).append("\n");
        summaryBuilder.append("卡玛比率: ").append(String.format("%.4f", result.getCalmarRatio() != null ? result.getCalmarRatio() : BigDecimal.ZERO)).append("\n");
        summaryBuilder.append("最大回撤: ").append(maxDrawdownFormatted).append("\n");
        summaryBuilder.append("最大单笔亏损: ").append(maximumLossFormatted).append("\n");
        summaryBuilder.append("盈亏比: ").append(String.format("%.4f", result.getProfitFactor() != null ? result.getProfitFactor() : BigDecimal.ONE)).append("\n");

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

    /**
     * 计算年化 Sortino 比率
     * <p>
     * Sortino比率是对夏普比率的改进，只考虑下行风险（负收益），而不是总体波动率。
     * 它衡量了投资组合的超额收益相对于下行风险的比率，更适合评估非对称收益分布的策略。
     * <p>
     * 使用场景：
     * 1. 评估非对称收益分布的策略（如期权策略、对冲策略等）
     * 2. 当投资者更关注亏损风险而非总体波动率时
     * 3. 比较不同风险特性的策略时，提供比夏普比率更精确的风险调整收益指标
     * <p>
     * 解读：
     * - 值越高越好，表示每单位下行风险获得的超额收益越多
     * - 通常大于1被认为是良好的，大于2是优秀的
     * - 负值表示策略表现不如无风险资产
     *
     * @param dailyReturns        每日收益率序列（建议为对数收益率）
     * @param riskFreeRate        无风险日收益率
     * @param annualizationFactor 年化因子（例如 252 表示按每日）
     * @return Sortino 比率（保留 6 位小数）
     */
    public static BigDecimal calculateSortinoRatio(List<BigDecimal> dailyReturns,
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

        // 计算下行方差（只考虑负收益）
        BigDecimal sumSquaredDownsideDiff = BigDecimal.ZERO;
        int downsideCount = 0;

        for (BigDecimal r : dailyReturns) {
            // 只考虑低于目标收益率（通常为0或无风险利率）的收益
            if (r.compareTo(riskFreeRate) < 0) {
                BigDecimal diff = r.subtract(riskFreeRate);
                sumSquaredDownsideDiff = sumSquaredDownsideDiff.add(diff.multiply(diff));
                downsideCount++;
            }
        }

        // 如果没有下行偏差，返回一个较大值或零
        if (downsideCount == 0) {
            return new BigDecimal("999.999999"); // 表示极高的比率，因为没有下行风险
        }

        // 计算下行标准差
        BigDecimal downsideVariance = sumSquaredDownsideDiff.divide(BigDecimal.valueOf(downsideCount), 10, RoundingMode.HALF_UP);
        BigDecimal downsideDeviation = BigDecimal.valueOf(Math.sqrt(downsideVariance.doubleValue()));

        if (downsideDeviation.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("999.999999");
        }

        // Sortino = (mean - riskFree) / downsideDeviation × √annualizationFactor
        BigDecimal sortino = avgReturn.subtract(riskFreeRate).divide(downsideDeviation, 10, RoundingMode.HALF_UP);
        BigDecimal sqrtAnnualFactor = BigDecimal.valueOf(Math.sqrt(annualizationFactor));

        return sortino.multiply(sqrtAnnualFactor).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 计算 Omega 比率
     *
     * @param dailyReturns 日收益序列（建议使用对数收益）
     * @param threshold    目标收益率阈值（如 0 或无风险日收益率）
     * @return Omega 比率（保留 6 位小数）
     */
    public static BigDecimal calculateOmegaRatio(List<BigDecimal> dailyReturns, BigDecimal threshold) {
        if (dailyReturns == null || dailyReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal gainSum = BigDecimal.ZERO;
        BigDecimal lossSum = BigDecimal.ZERO;

        for (BigDecimal r : dailyReturns) {
            int cmp = r.compareTo(threshold);
            if (cmp >= 0) {
                gainSum = gainSum.add(r.subtract(threshold));
            } else {
                lossSum = lossSum.add(threshold.subtract(r));
            }
        }

        // 如果没有亏损，则 Omega 无限大（无下行风险）
        if (lossSum.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("999.999999");
        }

        return gainSum.divide(lossSum, 10, RoundingMode.HALF_UP).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 计算 Calmar 比率
     * <p>
     * Calmar比率是年化收益率与最大回撤的比值，用于评估风险调整后的收益表现。
     * 它衡量了投资组合每承担一单位最大回撤风险所获得的超额收益。
     * <p>
     * 使用场景：
     * 1. 评估长期投资策略的风险调整收益
     * 2. 对比不同策略在极端市场条件下的表现
     * 3. 适合风险厌恶型投资者，关注策略在最坏情况下的表现
     * <p>
     * 解读：
     * - 值越高越好，通常大于0.5被认为是良好的，大于1是优秀的
     * - 负值表示策略整体亏损
     * - 相比夏普比率，Calmar比率更关注极端风险而非平均波动
     *
     * @param annualizedReturn 年化收益率
     * @param maxDrawdown      最大回撤（正值，例如0.2表示20%的回撤）
     * @return Calmar 比率（保留 6 位小数）
     */
    public static BigDecimal calculateCalmarRatio(BigDecimal annualizedReturn, BigDecimal maxDrawdown) {
        if (maxDrawdown == null || maxDrawdown.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("999.999999"); // 如果没有回撤，返回一个较大值
        }

        // Calmar = 年化收益率 / 最大回撤
        return annualizedReturn.divide(maxDrawdown, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算波动率（基于收盘价）
     * <p>
     * 波动率是衡量价格变动幅度的指标，通常用收盘价的标准差来计算。
     * 它反映了资产价格的不稳定性，是风险评估的重要指标。
     * <p>
     * 使用场景：
     * 1. 评估市场或资产的风险水平
     * 2. 构建风险管理模型和止损策略
     * 3. 比较不同资产或策略的风险特性
     * <p>
     * 解读：
     * - 值越小表示价格波动越小，相对稳定
     * - 值越大表示价格波动越大，风险越高
     * - 通常与收益率结合评估风险调整后的收益
     *
     * @param series BarSeries对象
     * @return 波动率（收盘价的标准差）
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
     *
     * @param totalReturn 总收益率（百分比）
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 年化收益率（百分比）
     */
    private BigDecimal calculateAnnualizedReturn(BigDecimal totalReturn, LocalDateTime startTime, LocalDateTime endTime) {
        if (totalReturn == null || startTime == null || endTime == null || startTime.isAfter(endTime)) {
            log.warn("计算年化收益率的参数无效");
            return BigDecimal.ZERO;
        }
//        long days = ChronoUnit.DAYS.between(startDate, endDate);
//        if (days <= 0 || totalReturn <= 0) return 0.0;
//
//        double ratio = 1 + totalReturn; // 总收益率 + 1
//        double years = 365.0 / days;
//        return Math.pow(ratio, years) - 1;

        // 计算回测持续的天数
        long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);

        // 避免除以零错误
        if (daysBetween <= 0) {
            return totalReturn; // 如果时间跨度小于1天，直接返回总收益率
        }

        // 计算年化收益率: (1 + totalReturn/100)^(365/daysBetween) - 1

        // 计算(1 + returnRate)
        BigDecimal base = BigDecimal.ONE.add(totalReturn);

        // 计算指数(365/daysBetween)
        BigDecimal exponent = new BigDecimal("365").divide(new BigDecimal(daysBetween), 8, RoundingMode.HALF_UP);

        // 计算(1 + returnRate)^(365/daysBetween)
        // 使用对数计算幂: exp(exponent * ln(base))
        BigDecimal result;
        try {
            double baseDouble = base.doubleValue();
            double exponentDouble = exponent.doubleValue();
            double power = Math.pow(baseDouble, exponentDouble);

            // 转换回BigDecimal并减去1
            result = new BigDecimal(power).subtract(BigDecimal.ONE);

        } catch (Exception e) {
            log.error("计算年化收益率时出错", e);
            return BigDecimal.ZERO;
        }

        return result;
    }

    /**
     * 计算 Alpha 和 Beta
     * Alpha 表示策略超额收益，Beta 表示策略相对于基准收益的敏感度（风险）
     *
     * @param strategyReturns  策略每日收益率序列，例如[0.002, -0.001, 0.003, ...]
     * @param benchmarkReturns 基准每日收益率序列（指数等）
     * @return double数组，索引0为Alpha，索引1为Beta
     * @throws IllegalArgumentException 如果输入长度不匹配或为空
     */
    public static double[] calculateAlphaBeta(List<BigDecimal> strategyReturns, List<BigDecimal> benchmarkReturns) {
        if (strategyReturns == null || benchmarkReturns == null || strategyReturns.size() != benchmarkReturns.size() || strategyReturns.isEmpty()) {
            throw new IllegalArgumentException("收益率序列长度不匹配或为空");
        }

        int n = strategyReturns.size();

        // 计算策略和基准的平均收益率
        double meanStrategy = strategyReturns.stream().mapToDouble(d -> d.doubleValue()).average().orElse(0.0);
        double meanBenchmark = benchmarkReturns.stream().mapToDouble(d -> d.doubleValue()).average().orElse(0.0);

        double covariance = 0.0;        // 协方差 numerator部分
        double varianceBenchmark = 0.0; // 基准收益率的方差 denominator部分

        // 计算协方差和基准方差
        for (int i = 0; i < n; i++) {
            double sDiff = strategyReturns.get(i).doubleValue() - meanStrategy;
            double bDiff = benchmarkReturns.get(i).doubleValue() - meanBenchmark;

            covariance += sDiff * bDiff;
            varianceBenchmark += bDiff * bDiff;
        }

        covariance /= n;        // 求平均协方差
        varianceBenchmark /= n; // 求平均方差

        // 防止除以0
        double beta = varianceBenchmark == 0 ? 0 : covariance / varianceBenchmark;

        // Alpha = 策略平均收益 - Beta * 基准平均收益
        double alpha = meanStrategy - beta * meanBenchmark;

        return new double[]{alpha, beta};
    }

    /**
     * 计算 Treynor 比率
     * 用 Beta 衡量系统性风险，计算单位系统风险的超额收益
     * Treynor Ratio = (策略平均收益率 - 无风险收益率) / Beta
     *
     * @param strategyReturns 策略每日收益率序列
     * @param riskFreeRate    无风险日收益率，比如0.0001（约3.65%年化）
     * @param beta            策略的Beta值
     * @return Treynor比率，如果Beta为0返回0
     */
    public static double calculateTreynorRatio(List<BigDecimal> strategyReturns, BigDecimal riskFreeRate, double beta) {
        if (strategyReturns == null || strategyReturns.isEmpty() || beta == 0) {
            return 0.0;
        }
        // 计算策略平均收益率
        double avgReturn = strategyReturns.stream().mapToDouble(d -> d.doubleValue()).average().orElse(0.0);

        // 计算超额收益率，除以系统风险Beta
        return (avgReturn - riskFreeRate.doubleValue()) / beta;
    }

    /**
     * 计算 Ulcer Index
     * Ulcer Index 主要衡量资产的最大回撤的深度和持续时间，数值越大代表风险越大。
     *
     * @param prices 收盘价序列
     * @return Ulcer Index（百分比形式）
     */
    public static double calculateUlcerIndex(List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) return 0.0;

        BigDecimal maxPeak = prices.get(0);           // 当前观察到的最大峰值价格
        double sumSquaredDrawdown = 0.0;          // 回撤平方和，用于计算均方根
        int n = prices.size();

        for (BigDecimal price : prices) {
            // 更新峰值，如果当前价格高于峰值则更新峰值
            if (price.compareTo(maxPeak) > 0) {
                maxPeak = price;
            }
            // 计算当前回撤百分比 (负数表示跌幅)
            double drawdownPercent = price.subtract(maxPeak).divide(maxPeak).doubleValue() * 100.0;
            // 累计回撤平方（深度和持续时间都会放大结果）
            sumSquaredDrawdown += drawdownPercent * drawdownPercent;
        }

        // 计算均方根回撤，返回Ulcer Index
        return Math.sqrt(sumSquaredDrawdown / n);
    }

    /**
     * 计算收益率序列的偏度 (Skewness)
     * 偏度反映数据分布的非对称性，正偏说明右尾重，负偏说明左尾重
     *
     * @param returns 日收益率序列
     * @return 偏度值，接近0表示近似对称分布
     */
    public static double calculateSkewness(List<BigDecimal> returns) {
        if (returns == null || returns.size() < 3) return 0.0;

        int n = returns.size();
        double mean = returns.stream().mapToDouble(d -> d.doubleValue()).average().orElse(0.0);
        double m2 = 0.0;  // 二阶中心矩（方差的分子）
        double m3 = 0.0;  // 三阶中心矩

        for (BigDecimal x : returns) {
            double diff = x.doubleValue() - mean;
            m2 += diff * diff;
            m3 += diff * diff * diff;
        }

        m2 /= n;
        m3 /= n;

        double sd = Math.sqrt(m2); // 标准差
        if (sd == 0.0) return 0.0;

        return m3 / (sd * sd * sd);
    }

    /**
     * 计算收益率序列的峰度 (Kurtosis)
     * 峰度衡量数据尾部厚度，超过3表示重尾分布（极端事件更频繁）
     * 返回的是“超额峰度”，即峰度减3，正值表示重尾，负值表示轻尾
     *
     * @param returns 日收益率序列
     * @return 超额峰度值
     */
    public static double calculateKurtosis(List<Double> returns) {
        if (returns == null || returns.size() < 4) return 0.0;

        int n = returns.size();
        double mean = returns.stream().mapToDouble(d -> d).average().orElse(0.0);
        double m2 = 0.0;  // 二阶中心矩
        double m4 = 0.0;  // 四阶中心矩

        for (double x : returns) {
            double diff = x - mean;
            m2 += diff * diff;
            m4 += diff * diff * diff * diff;
        }

        m2 /= n;
        m4 /= n;

        if (m2 == 0.0) return 0.0;

        return (m4 / (m2 * m2)) - 3.0; // 超额峰度
    }

    /**
     * 计算收益率序列的稳定性指标（收益的线性回归决定系数 R²）
     * R² 越接近1说明收益趋势越稳定，接近0说明无明显线性趋势
     *
     * @param returns 日收益率序列
     * @return R平方值，范围[0,1]
     */
    public static double calculateStabilityOfReturn(List<Double> returns) {
        if (returns == null || returns.size() < 2) return 0.0;

        int n = returns.size();

        // 时间序列X，收益序列Y
        double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumX2 = 0.0, sumY2 = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;           // 时间从1开始
            double y = returns.get(i);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }

        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

        if (denominator == 0.0) return 0.0;

        double r = numerator / denominator;

        return r * r;  // 返回 R²
    }
}
