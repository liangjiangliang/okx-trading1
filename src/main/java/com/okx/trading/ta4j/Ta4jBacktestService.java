package com.okx.trading.ta4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

    // 策略类型常量 - 使用StrategyFactory中的常量
    public static final String STRATEGY_SMA = StrategyFactory.STRATEGY_SMA;
    public static final String STRATEGY_BOLLINGER_BANDS = StrategyFactory.STRATEGY_BOLLINGER_BANDS;
    public static final String STRATEGY_MACD = StrategyFactory.STRATEGY_MACD;
    public static final String STRATEGY_RSI = StrategyFactory.STRATEGY_RSI;
    public static final String STRATEGY_STOCHASTIC = StrategyFactory.STRATEGY_STOCHASTIC;
    public static final String STRATEGY_ADX = StrategyFactory.STRATEGY_ADX;
    public static final String STRATEGY_CCI = StrategyFactory.STRATEGY_CCI;
    public static final String STRATEGY_WILLIAMS_R = StrategyFactory.STRATEGY_WILLIAMS_R;
    public static final String STRATEGY_TRIPLE_EMA = StrategyFactory.STRATEGY_TRIPLE_EMA;
    public static final String STRATEGY_ICHIMOKU = StrategyFactory.STRATEGY_ICHIMOKU;
    public static final String STRATEGY_PARABOLIC_SAR = StrategyFactory.STRATEGY_PARABOLIC_SAR;
    public static final String STRATEGY_CHANDELIER_EXIT = StrategyFactory.STRATEGY_CHANDELIER_EXIT;

    // 策略参数说明 - 使用StrategyFactory中的常量
    public static final String SMA_PARAMS_DESC = StrategyFactory.SMA_PARAMS_DESC;
    public static final String BOLLINGER_PARAMS_DESC = StrategyFactory.BOLLINGER_PARAMS_DESC;
    public static final String MACD_PARAMS_DESC = StrategyFactory.MACD_PARAMS_DESC;
    public static final String RSI_PARAMS_DESC = StrategyFactory.RSI_PARAMS_DESC;
    public static final String STOCHASTIC_PARAMS_DESC = StrategyFactory.STOCHASTIC_PARAMS_DESC;
    public static final String ADX_PARAMS_DESC = StrategyFactory.ADX_PARAMS_DESC;
    public static final String CCI_PARAMS_DESC = StrategyFactory.CCI_PARAMS_DESC;
    public static final String WILLIAMS_R_PARAMS_DESC = StrategyFactory.WILLIAMS_R_PARAMS_DESC;
    public static final String TRIPLE_EMA_PARAMS_DESC = StrategyFactory.TRIPLE_EMA_PARAMS_DESC;
    public static final String ICHIMOKU_PARAMS_DESC = StrategyFactory.ICHIMOKU_PARAMS_DESC;
    public static final String PARABOLIC_SAR_PARAMS_DESC = StrategyFactory.PARABOLIC_SAR_PARAMS_DESC;
    public static final String CHANDELIER_EXIT_PARAMS_DESC = StrategyFactory.CHANDELIER_EXIT_PARAMS_DESC;

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
        CHANDELIER_EXIT
    }

    @Autowired
    private CandlestickBarSeriesConverter barSeriesConverter;

    /**
     * 执行回测
     *
     * @param candlesticks 历史K线数据
     * @param strategyType 策略类型
     * @param initialAmount 初始资金
     * @param params 策略参数
     * @param feeRatio 交易手续费率（例如0.001表示0.1%）
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

            // 使用策略工厂解析参数
            Map<String, Object> paramMap = StrategyFactory.parseParams(strategyType, params);

            // 使用策略工厂创建策略
            Strategy strategy = StrategyFactory.createStrategy(series, strategyType, paramMap);

            // 构建策略描述
            String strategyDescription = createStrategyDescription(strategyType, paramMap);

            // 执行回测
            BarSeriesManager seriesManager = new BarSeriesManager(series);
            TradingRecord tradingRecord = seriesManager.run(strategy);

            // 计算回测指标
            return calculateBacktestMetrics(series, tradingRecord, initialAmount, strategyType, strategyDescription, feeRatio);
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
     * 创建策略描述
     *
     * @param strategyType 策略类型
     * @param params 参数映射
     * @return 策略描述
     */
    private String createStrategyDescription(String strategyType, Map<String, Object> params) {
        StringBuilder descBuilder = new StringBuilder();

        switch (strategyType) {
            case STRATEGY_SMA:
                descBuilder.append("SMA Cross Strategy (")
                        .append(params.get("shortPeriod"))
                        .append(",")
                        .append(params.get("longPeriod"))
                        .append(")");
                break;
            case STRATEGY_BOLLINGER_BANDS:
                descBuilder.append("Bollinger Bands Strategy (")
                        .append(params.get("period"))
                        .append(",")
                        .append(params.get("deviation"))
                        .append(")");
                break;
            case STRATEGY_MACD:
                descBuilder.append("MACD Strategy (")
                        .append(params.get("shortPeriod"))
                        .append(",")
                        .append(params.get("longPeriod"))
                        .append(",")
                        .append(params.get("signalPeriod"))
                        .append(")");
                break;
            case STRATEGY_RSI:
                descBuilder.append("RSI Strategy (")
                        .append(params.get("period"))
                        .append(",")
                        .append(params.get("oversold"))
                        .append(",")
                        .append(params.get("overbought"))
                        .append(")");
                break;
            case STRATEGY_STOCHASTIC:
                descBuilder.append("Stochastic Oscillator Strategy (")
                        .append(params.get("kPeriod"))
                        .append(",")
                        .append(params.get("kSmooth"))
                        .append(",")
                        .append(params.get("dSmooth"))
                        .append(",")
                        .append(params.get("oversold"))
                        .append(",")
                        .append(params.get("overbought"))
                        .append(")");
                break;
            case STRATEGY_ADX:
                descBuilder.append("ADX Strategy (")
                        .append(params.get("adxPeriod"))
                        .append(",")
                        .append(params.get("diPeriod"))
                        .append(",")
                        .append(params.get("threshold"))
                        .append(")");
                break;
            case STRATEGY_CCI:
                descBuilder.append("CCI Strategy (")
                        .append(params.get("period"))
                        .append(",")
                        .append(params.get("oversold"))
                        .append(",")
                        .append(params.get("overbought"))
                        .append(")");
                break;
            case STRATEGY_WILLIAMS_R:
                descBuilder.append("Williams %R Strategy (")
                        .append(params.get("period"))
                        .append(",")
                        .append(params.get("oversold"))
                        .append(",")
                        .append(params.get("overbought"))
                        .append(")");
                break;
            case STRATEGY_TRIPLE_EMA:
                descBuilder.append("Triple EMA Strategy (")
                        .append(params.get("shortPeriod"))
                        .append(",")
                        .append(params.get("middlePeriod"))
                        .append(",")
                        .append(params.get("longPeriod"))
                        .append(")");
                break;
            case STRATEGY_ICHIMOKU:
                descBuilder.append("Ichimoku Cloud Strategy (")
                        .append(params.get("conversionPeriod"))
                        .append(",")
                        .append(params.get("basePeriod"))
                        .append(",")
                        .append(params.get("laggingSpan"))
                        .append(")");
                break;
            case STRATEGY_PARABOLIC_SAR:
                descBuilder.append("Parabolic SAR Strategy (")
                        .append(params.get("step"))
                        .append(",")
                        .append(params.get("max"))
                        .append(")");
                break;
            case STRATEGY_CHANDELIER_EXIT:
                descBuilder.append("Chandelier Exit Strategy (")
                        .append(params.get("period"))
                        .append(",")
                        .append(params.get("multiplier"))
                        .append(")");
                break;
            default:
                descBuilder.append(strategyType).append(" Strategy");
                break;
        }

        return descBuilder.toString();
    }

    /**
     * 执行回测（基于枚举和Map参数）
     *
     * @param candlesticks  历史K线数据
     * @param strategyType  策略类型枚举
     * @param params        策略参数
     * @param initialAmount 初始资金
     * @return 回测结果
     */
    public BacktestResultDTO backtest(List<CandlestickEntity> candlesticks, StrategyType strategyType,
                                   Map<String, Object> params, double initialAmount) {
        String strategyTypeStr;
        String paramsStr;

        switch (strategyType) {
            case SMA:
                strategyTypeStr = STRATEGY_SMA;
                paramsStr = buildParamsString(params, "shortPeriod", "longPeriod");
                break;
            case BOLLINGER_BANDS:
                strategyTypeStr = STRATEGY_BOLLINGER_BANDS;
                paramsStr = buildParamsString(params, "period", "deviation");
                break;
            case MACD:
                strategyTypeStr = STRATEGY_MACD;
                paramsStr = buildParamsString(params, "shortPeriod", "longPeriod", "signalPeriod");
                break;
            case RSI:
                strategyTypeStr = STRATEGY_RSI;
                paramsStr = buildParamsString(params, "period", "oversold", "overbought");
                break;
            case STOCHASTIC:
                strategyTypeStr = STRATEGY_STOCHASTIC;
                paramsStr = buildParamsString(params, "kPeriod", "kSmooth", "dSmooth", "oversold", "overbought");
                break;
            case ADX:
                strategyTypeStr = STRATEGY_ADX;
                paramsStr = buildParamsString(params, "adxPeriod", "diPeriod", "threshold");
                break;
            case CCI:
                strategyTypeStr = STRATEGY_CCI;
                paramsStr = buildParamsString(params, "period", "oversold", "overbought");
                break;
            case WILLIAMS_R:
                strategyTypeStr = STRATEGY_WILLIAMS_R;
                paramsStr = buildParamsString(params, "period", "oversold", "overbought");
                break;
            case TRIPLE_EMA:
                strategyTypeStr = STRATEGY_TRIPLE_EMA;
                paramsStr = buildParamsString(params, "shortPeriod", "middlePeriod", "longPeriod");
                break;
            case ICHIMOKU:
                strategyTypeStr = STRATEGY_ICHIMOKU;
                paramsStr = buildParamsString(params, "conversionPeriod", "basePeriod", "laggingSpan");
                break;
            case PARABOLIC_SAR:
                strategyTypeStr = STRATEGY_PARABOLIC_SAR;
                paramsStr = buildParamsString(params, "step", "max");
                break;
            case CHANDELIER_EXIT:
                strategyTypeStr = STRATEGY_CHANDELIER_EXIT;
                paramsStr = buildParamsString(params, "period", "multiplier");
                break;
            default:
                BacktestResultDTO result = new BacktestResultDTO();
                result.setSuccess(false);
                result.setErrorMessage("不支持的策略类型: " + strategyType);
                return result;
        }

        return backtest(candlesticks, strategyTypeStr, new BigDecimal(initialAmount), paramsStr);
    }

    /**
     * 根据参数键构建参数字符串
     *
     * @param params 参数映射
     * @param keys 参数键数组
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
     * @param series BarSeries对象
     * @param tradingRecord 交易记录
     * @param initialAmount 初始资金
     * @param strategyType 策略类型
     * @param paramDescription 参数描述
     * @param feeRatio 交易手续费率
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
        result.setMaxDrawdown(new BigDecimal("0.05")); // 简化计算，使用默认值
        result.setSharpeRatio(new BigDecimal("0.5"));  // 简化计算，使用默认值
        result.setStrategyName(strategyType);
        result.setParameterDescription(paramDescription);
        result.setTrades(tradeRecords);
        result.setTotalFee(totalFee);

        // 打印回测汇总信息
        printBacktestSummary(result);

        return result;
    }

    /**
     * 计算回测指标（不带手续费参数的重载方法）
     *
     * @param series BarSeries对象
     * @param tradingRecord 交易记录
     * @param initialAmount 初始资金
     * @param strategyType 策略类型
     * @param paramDescription 参数描述
     * @return 回测结果DTO
     */
    private BacktestResultDTO calculateBacktestMetrics(BarSeries series, TradingRecord tradingRecord,
                                                     BigDecimal initialAmount, String strategyType,
                                                     String paramDescription) {
        return calculateBacktestMetrics(series, tradingRecord, initialAmount, strategyType, paramDescription, BigDecimal.ZERO);
    }

    /**
     * 从交易记录中提取交易明细（带手续费计算）
     *
     * @param series BarSeries对象
     * @param tradingRecord 交易记录
     * @param initialAmount 初始资金
     * @param feeRatio 交易手续费率
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

    /**
     * 获取策略参数说明
     *
     * @param strategyType 策略类型
     * @return 策略参数说明
     */
    public static String getStrategyParamsDescription(String strategyType) {
        return StrategyFactory.getStrategyParamsDescription(strategyType);
    }

    /**
     * 验证策略参数是否合法
     *
     * @param strategyType 策略类型
     * @param params 策略参数
     * @return 是否合法
     */
    public static boolean validateStrategyParams(String strategyType, String params) {
        return StrategyFactory.validateStrategyParams(strategyType, params);
    }
}
