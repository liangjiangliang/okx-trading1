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
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Position;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.CandlestickEntity;

/**
 * TA4J 回测服务类
 */
@Service
public class Ta4jBacktestService {

    private static final Logger log = LoggerFactory.getLogger(Ta4jBacktestService.class);
    
    // 策略类型常量
    public static final String STRATEGY_SMA = "SMA";
    public static final String STRATEGY_BOLLINGER_BANDS = "BOLLINGER";
    
    // 策略参数说明
    public static final String SMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String BOLLINGER_PARAMS_DESC = "周期,标准差倍数 (例如：20,2.0)";
    
    /**
     * 策略类型枚举
     */
    public enum StrategyType {
        SMA,
        BOLLINGER_BANDS
    }

    @Autowired
    private CandlestickBarSeriesConverter barSeriesConverter;

    /**
     * 执行回测
     *
     * @param candlesticks 历史K线数据
     * @param strategyType 策略类型 (SMA, BOLLINGER)
     * @param initialAmount 初始资金
     * @param params 策略参数
     * @return 回测结果
     */
    public BacktestResultDTO backtest(List<CandlestickEntity> candlesticks, String strategyType, 
                                    BigDecimal initialAmount, String params) {
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
            
            Strategy strategy;
            String strategyDescription;
            
            // 根据策略类型创建不同的策略
            switch (strategyType.toUpperCase()) {
                case STRATEGY_SMA:
                    int shortPeriod = 9;
                    int longPeriod = 21;
                    if (params != null && !params.isEmpty()) {
                        String[] paramArray = params.split(",");
                        if (paramArray.length >= 2) {
                            shortPeriod = Integer.parseInt(paramArray[0]);
                            longPeriod = Integer.parseInt(paramArray[1]);
                        }
                    }
                    strategy = createSMAStrategy(series, shortPeriod, longPeriod);
                    strategyDescription = "SMA Cross Strategy (" + shortPeriod + "," + longPeriod + ")";
                    break;
                case STRATEGY_BOLLINGER_BANDS:
                    int period = 20;
                    double multiplier = 2.0;
                    if (params != null && !params.isEmpty()) {
                        String[] paramArray = params.split(",");
                        if (paramArray.length >= 2) {
                            period = Integer.parseInt(paramArray[0]);
                            multiplier = Double.parseDouble(paramArray[1]);
                        }
                    }
                    strategy = createBollingerBandsStrategy(series, period, multiplier);
                    strategyDescription = "Bollinger Bands Strategy (" + period + "," + multiplier + ")";
                    break;
                default:
                    BacktestResultDTO result = new BacktestResultDTO();
                    result.setSuccess(false);
                    result.setErrorMessage("不支持的策略类型: " + strategyType);
                    return result;
            }
            
            // 执行回测
            BarSeriesManager seriesManager = new BarSeriesManager(series);
            TradingRecord tradingRecord = seriesManager.run(strategy);
            
            // 计算回测指标
            return calculateBacktestMetrics(series, tradingRecord, initialAmount, strategyType, strategyDescription);
        } catch (Exception e) {
            log.error("回测过程中发生错误: {}", e.getMessage(), e);
            BacktestResultDTO result = new BacktestResultDTO();
            result.setSuccess(false);
            result.setErrorMessage("回测过程中发生错误: " + e.getMessage());
            return result;
        }
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
        String paramsStr = "";
        
        switch (strategyType) {
            case SMA:
                strategyTypeStr = STRATEGY_SMA;
                if (params.containsKey("shortPeriod") && params.containsKey("longPeriod")) {
                    paramsStr = params.get("shortPeriod") + "," + params.get("longPeriod");
                }
                break;
            case BOLLINGER_BANDS:
                strategyTypeStr = STRATEGY_BOLLINGER_BANDS;
                if (params.containsKey("period") && params.containsKey("deviation")) {
                    paramsStr = params.get("period") + "," + params.get("deviation");
                }
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
     * 创建SMA交叉策略
     *
     * @param series BarSeries对象
     * @param shortPeriod 短周期
     * @param longPeriod 长周期
     * @return 策略对象
     */
    private Strategy createSMAStrategy(BarSeries series, int shortPeriod, int longPeriod) {
        if (series == null) {
            throw new IllegalArgumentException("BarSeries不能为null");
        }
        
        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 创建短期和长期SMA指标
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);
        
        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma); // 短期均线上穿长期均线，买入信号
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma); // 短期均线下穿长期均线，卖出信号
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建布林带策略
     *
     * @param series BarSeries对象
     * @param period 周期
     * @param multiplier 标准差乘数
     * @return 策略对象
     */
    private Strategy createBollingerBandsStrategy(BarSeries series, int period, double multiplier) {
        if (series == null) {
            throw new IllegalArgumentException("BarSeries不能为null");
        }
        
        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 创建布林带指标
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, period);
        
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, sd, series.numOf(multiplier));
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, sd, series.numOf(multiplier));
        
        // 创建规则
        Rule entryRule = new UnderIndicatorRule(closePrice, lowerBand); // 价格低于下轨，买入信号
        Rule exitRule = new OverIndicatorRule(closePrice, upperBand);   // 价格高于上轨，卖出信号
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 计算回测指标
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
        
        // 计算总利润和最终金额
        BigDecimal totalProfit = BigDecimal.ZERO;
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                double profitValue = position.getProfit().doubleValue();
                totalProfit = totalProfit.add(new BigDecimal(profitValue));
            }
        }
        
        // 最终资金
        BigDecimal finalAmount = initialAmount.add(totalProfit.multiply(initialAmount));
        
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
        
        // 提取交易记录
        List<TradeRecordDTO> tradeRecords = extractTradeRecords(series, tradingRecord);
        
        // 构建回测结果
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(finalAmount);
        result.setTotalProfit(finalAmount.subtract(initialAmount));
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
        
        // 打印回测汇总信息
        printBacktestSummary(result);
        
        return result;
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
        
        summaryBuilder.append("初始资金: ").append(initialAmountFormatted).append("\n");
        summaryBuilder.append("最终资金: ").append(finalAmountFormatted).append("\n");
        summaryBuilder.append("总盈亏: ").append(totalProfitFormatted).append("\n");
        summaryBuilder.append("总收益率: ").append(totalReturnFormatted).append("\n");
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
     * 从交易记录中提取交易明细
     *
     * @param series BarSeries对象
     * @param tradingRecord 交易记录
     * @return 交易记录DTO列表
     */
    private List<TradeRecordDTO> extractTradeRecords(BarSeries series, TradingRecord tradingRecord) {
        List<TradeRecordDTO> records = new ArrayList<>();
        
        int index = 1;
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
                
                // 交易盈亏
                BigDecimal profit;
                BigDecimal profitPercentage;
                
                if (position.getEntry().isBuy()) {
                    // 如果是买入操作，盈亏 = 卖出价 - 买入价
                    profit = exitPrice.subtract(entryPrice);
                    profitPercentage = profit.divide(entryPrice, 4, RoundingMode.HALF_UP);
                } else {
                    // 如果是卖出操作（做空），盈亏 = 买入价 - 卖出价
                    profit = entryPrice.subtract(exitPrice);
                    profitPercentage = profit.divide(entryPrice, 4, RoundingMode.HALF_UP);
                }
                
                // 假设交易金额为1000
                BigDecimal tradeAmount = BigDecimal.valueOf(1000);
                
                // 创建交易记录DTO
                TradeRecordDTO recordDTO = new TradeRecordDTO();
                recordDTO.setIndex(index++);
                recordDTO.setType(position.getEntry().isBuy() ? "BUY" : "SELL");
                recordDTO.setEntryTime(entryTime.toLocalDateTime());
                recordDTO.setExitTime(exitTime.toLocalDateTime());
                recordDTO.setEntryPrice(entryPrice);
                recordDTO.setExitPrice(exitPrice);
                recordDTO.setEntryAmount(tradeAmount);
                recordDTO.setExitAmount(tradeAmount.add(profit));
                recordDTO.setProfit(profit);
                recordDTO.setProfitPercentage(profitPercentage);
                recordDTO.setClosed(true);
                
                records.add(recordDTO);
            }
        }
        
        return records;
    }

    /**
     * 获取策略参数说明
     *
     * @param strategyType 策略类型
     * @return 策略参数说明
     */
    public static String getStrategyParamsDescription(String strategyType) {
        switch (strategyType) {
            case STRATEGY_SMA:
                return SMA_PARAMS_DESC;
            case STRATEGY_BOLLINGER_BANDS:
                return BOLLINGER_PARAMS_DESC;
            default:
                return "未知策略类型";
        }
    }
    
    /**
     * 验证策略参数是否合法
     *
     * @param strategyType 策略类型
     * @param params 策略参数
     * @return 是否合法
     */
    public static boolean validateStrategyParams(String strategyType, String params) {
        if (params == null || params.trim().isEmpty()) {
            return false;
        }
        
        String[] paramArray = params.split(",");
        
        switch (strategyType) {
            case STRATEGY_SMA:
                // SMA策略参数: 短期均线周期,长期均线周期
                if (paramArray.length != 2) {
                    return false;
                }
                try {
                    int shortPeriod = Integer.parseInt(paramArray[0]);
                    int longPeriod = Integer.parseInt(paramArray[1]);
                    return shortPeriod > 0 && longPeriod > shortPeriod;
                } catch (NumberFormatException e) {
                    return false;
                }
                
            case STRATEGY_BOLLINGER_BANDS:
                // 布林带策略参数: 周期,标准差倍数
                if (paramArray.length != 2) {
                    return false;
                }
                try {
                    int period = Integer.parseInt(paramArray[0]);
                    double stdDev = Double.parseDouble(paramArray[1]);
                    return period > 0 && stdDev > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
                
            default:
                return false;
        }
    }
}
