package com.okx.trading.ta4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.ta4j.strategy.StrategyFactory;

/**
 * TA4J 回测服务类
 */
@Service
public class Ta4jBacktestService {

    private static final Logger log = LoggerFactory.getLogger(Ta4jBacktestService.class);

    /**
     * 策略类型枚举 - 扩展以包含所有策略
     */
    public enum StrategyType {
        SMA,
        BOLLINGER_BANDS,
        MACD,
        RSI,
        STOCHASTIC,
        ADX,
        CCI,
        WILLIAMS_R,
        TRIPLE_EMA,
        ICHIMOKU,
        PARABOLIC_SAR,
        CHANDELIER_EXIT,
        MACD_WITH_BOLLINGER,
        HANGING_MAN,
        VWAP
    }

    @Autowired
    private CandlestickBarSeriesConverter barSeriesConverter;

    /**
     * 执行回测
     *
     * @param candlesticks  历史K线数据
     * @param strategyType  策略类型
     * @param initialAmount 初始资金
     * @param params        策略参数
     * @param feeRatio      交易手续费率（例如0.001表示0.1%）
     * @return 回测结果
     */
    public BacktestResultDTO backtest(List<CandlestickEntity> candlesticks, String strategyType,
                                      BigDecimal initialAmount, String params, BigDecimal feeRatio) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            BacktestResultDTO result = new BacktestResultDTO();
            result.setSuccess(false);
            result.setErrorMessage("没有足够的历史数据进行回测");
            return result;
        }

        try {
            // 生成唯一的系列名称
            String seriesName = CandlestickAdapter.getSymbol(candlesticks.get(0)) + "_" +
                               CandlestickAdapter.getIntervalVal(candlesticks.get(0));

            // 使用转换器将蜡烛图实体转换为条形系列
            BarSeries series = barSeriesConverter.convert(candlesticks, seriesName);

            // 使用策略工厂解析参数, 使用方法自带的
            Map<String, Object> paramMap = new HashMap<>();

            // 使用策略工厂创建策略
            Strategy strategy = StrategyFactory.createStrategy(series, strategyType, paramMap);

            // 执行回测
            BarSeriesManager seriesManager = new BarSeriesManager(series);
            TradingRecord tradingRecord = seriesManager.run(strategy);

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
                                      BigDecimal initialAmount, String params) {
        return backtest(candlesticks, strategyType, initialAmount, params, BigDecimal.ZERO);
    }

    /**
     * 根据参数键构建参数字符串
     *
     * @param params 参数映射
     * @param keys   参数键数组
     * @return 参数字符串
     */
    private String buildParamsString(Map<String, Object> params, String... keys) {
        StringBuilder paramsStr = new StringBuilder();
        boolean firstParam = true;

        for (String key : keys) {
            if (params.containsKey(key)) {
                if (!firstParam) {
                    paramsStr.append(",");
                }
                paramsStr.append(params.get(key));
                firstParam = false;
            }
        }

        return paramsStr.toString();
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

        // 手动计算交易统计信息
        int tradeCount = 0;
        int profitableTrades = 0;

        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                tradeCount++;
                if (position.getProfit().isGreaterThan(series.numOf(0))) {
                    profitableTrades++;
                }
            }
        }

        // 提取交易记录（考虑手续费）
        List<TradeRecordDTO> tradeRecords = extractTradeRecords(series, tradingRecord, initialAmount, feeRatio);

        // 计算总利润和总手续费
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;

        for (TradeRecordDTO trade : tradeRecords) {
            if (trade.getProfit() != null) {
                totalProfit = totalProfit.add(trade.getProfit());
            }
            if (trade.getFee() != null) {
                totalFee = totalFee.add(trade.getFee());
            }
        }

        // 最终资金
        BigDecimal finalAmount = initialAmount.add(totalProfit);

        // 计算其他指标
        BigDecimal totalReturn = BigDecimal.ZERO;
        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalReturn = finalAmount.subtract(initialAmount)
                    .divide(initialAmount, 4, RoundingMode.HALF_UP);
        }

        // 计算胜率
        BigDecimal winRate = BigDecimal.ZERO;
        if (tradeCount > 0) {
            winRate = new BigDecimal(profitableTrades)
                    .divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 平均利润
        BigDecimal averageProfit = BigDecimal.ZERO;
        if (tradeCount > 0) {
            averageProfit = totalProfit.divide(new BigDecimal(tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 计算最大回撤
        BigDecimal maxDrawdown = calculateMaxDrawdown(tradeRecords, initialAmount);

        // 计算夏普比率
        BigDecimal sharpeRatio = calculateSharpeRatio(tradeRecords, initialAmount);

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
        result.setMaxDrawdown(maxDrawdown);
        result.setSharpeRatio(sharpeRatio);
        result.setStrategyName(strategyType.toString());
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
     * @param trades        交易记录列表
     * @param initialAmount 初始资金
     * @return 最大回撤（百分比）
     */
    private BigDecimal calculateMaxDrawdown(List<TradeRecordDTO> trades, BigDecimal initialAmount) {
        if (trades == null || trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal highestValue = initialAmount;
        BigDecimal currentValue = initialAmount;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        // 遍历每笔交易，计算每个时点的资产价值和最大回撤
        for (TradeRecordDTO trade : trades) {
            // 更新当前资产价值（考虑手续费）
            if (trade.getProfit() != null) {
                currentValue = currentValue.add(trade.getProfit());
            }

            // 更新历史最高资产价值
            if (currentValue.compareTo(highestValue) > 0) {
                highestValue = currentValue;
            }

            // 计算当前回撤
            if (highestValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDrawdown = highestValue.subtract(currentValue)
                        .divide(highestValue, 4, RoundingMode.HALF_UP);

                // 更新最大回撤
                if (currentDrawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = currentDrawdown;
                }
            }
        }

        return maxDrawdown;
    }

    /**
     * 计算回测指标（不带手续费参数的重载方法）
     *
     * @param series           BarSeries对象
     * @param tradingRecord    交易记录
     * @param initialAmount    初始资金
     * @param strategyType     策略类型
     * @param paramDescription 参数描述
     * @return 回测结果DTO
     */
    private BacktestResultDTO calculateBacktestMetrics(BarSeries series, TradingRecord tradingRecord,
                                                       BigDecimal initialAmount, String strategyType,
                                                       String paramDescription) {
        return calculateBacktestMetrics(series, tradingRecord, initialAmount, strategyType, paramDescription, BigDecimal.ZERO);
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
}
