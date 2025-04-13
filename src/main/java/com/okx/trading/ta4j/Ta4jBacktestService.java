package com.okx.trading.ta4j;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.*;
import org.ta4j.core.analysis.criteria.pnl.AverageProfitCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ta4j回测服务类
 * 使用Ta4j框架进行策略回测
 */
@Service
public class Ta4jBacktestService {

    private static final Logger log = LoggerFactory.getLogger(Ta4jBacktestService.class);

    private final CandlestickBarSeriesConverter barSeriesConverter;

    public enum StrategyType {
        SMA,
        BOLLINGER_BANDS
    }

    @Autowired
    public Ta4jBacktestService(CandlestickBarSeriesConverter barSeriesConverter) {
        this.barSeriesConverter = barSeriesConverter;
    }

    /**
     * 执行回测
     * @param candlesticks K线数据
     * @param strategyType 策略类型
     * @param strategyParams 策略参数
     * @param initialAmount 初始资金
     * @return 回测结果
     */
    public BacktestResultDTO backtest(List<CandlestickEntity> candlesticks, StrategyType strategyType,
                                   Map<String, Object> strategyParams, double initialAmount) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            log.error("回测数据为空，无法执行回测");
            return null;
        }

        // 创建BarSeries名称
        String symbol = CandlestickAdapter.getSymbol(candlesticks.get(0));
        String interval = CandlestickAdapter.getIntervalVal(candlesticks.get(0));
        String seriesName = CandlestickBarSeriesConverter.createSeriesName(symbol, interval);

        // 转换K线数据
        BarSeries series = barSeriesConverter.convert(candlesticks, seriesName);
        if (series.getBarCount() == 0) {
            log.error("转换后的BarSeries为空，无法执行回测");
            return null;
        }

        // 创建策略
        Strategy strategy;
        switch (strategyType) {
            case SMA:
                strategy = createSMAStrategy(series, strategyParams);
                break;
            case BOLLINGER_BANDS:
                strategy = createBollingerBandsStrategy(series, strategyParams);
                break;
            default:
                log.error("不支持的策略类型: {}", strategyType);
                return null;
        }

        // 执行回测
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);

        // 计算回测指标
        return calculateBacktestMetrics(series, tradingRecord, initialAmount);
    }

    /**
     * 创建简单移动平均线策略
     * @param series K线数据
     * @param params 策略参数
     * @return 策略
     */
    private Strategy createSMAStrategy(BarSeries series, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }

        // 获取策略参数
        int shortPeriod = params.containsKey("shortPeriod") ? ((Number) params.get("shortPeriod")).intValue() : 5;
        int longPeriod = params.containsKey("longPeriod") ? ((Number) params.get("longPeriod")).intValue() : 20;

        // 收盘价指标
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 短期和长期SMA指标
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);

        // 创建交叉规则
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);

        return new BaseStrategy(buyingRule, sellingRule);
    }

    /**
     * 创建布林带策略
     * @param series K线数据
     * @param params 策略参数
     * @return 策略
     */
    private Strategy createBollingerBandsStrategy(BarSeries series, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }

        // 获取策略参数
        int period = params.containsKey("period") ? ((Number) params.get("period")).intValue() : 20;
        double deviation = params.containsKey("deviation") ? ((Number) params.get("deviation")).doubleValue() : 2.0;
        String signalType = params.containsKey("signalType") ? (String) params.get("signalType") : "BANDS_BREAKOUT";

        // 收盘价指标
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 布林带指标
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, period);
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(sma);

        // 使用DecimalNum创建标准差乘数
        Num deviationMultiplier = DecimalNum.valueOf(deviation);
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, standardDeviation, deviationMultiplier);
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, standardDeviation, deviationMultiplier);

        Rule buyingRule;
        Rule sellingRule;

        // 根据信号类型创建规则
        switch (signalType) {
            case "BANDS_BREAKOUT":
                // 突破下轨买入，突破上轨卖出
                buyingRule = new UnderIndicatorRule(closePrice, lowerBand);
                sellingRule = new OverIndicatorRule(closePrice, upperBand);
                break;
            case "MIDDLE_CROSS":
                // 价格上穿中轨买入，下穿中轨卖出
                buyingRule = new CrossedUpIndicatorRule(closePrice, middleBand);
                sellingRule = new CrossedDownIndicatorRule(closePrice, middleBand);
                break;
            case "MEAN_REVERSION":
                // 突破下轨后又回到下轨之上买入，突破上轨后又回到上轨之下卖出
                buyingRule = new CrossedUpIndicatorRule(closePrice, lowerBand);
                sellingRule = new CrossedDownIndicatorRule(closePrice, upperBand);
                break;
            default:
                buyingRule = new UnderIndicatorRule(closePrice, lowerBand);
                sellingRule = new OverIndicatorRule(closePrice, upperBand);
                break;
        }

        return new BaseStrategy(buyingRule, sellingRule);
    }

    /**
     * 计算回测指标
     * @param series K线数据
     * @param tradingRecord 交易记录
     * @param initialAmount 初始资金
     * @return 回测结果
     */
    private BacktestResultDTO calculateBacktestMetrics(BarSeries series, TradingRecord tradingRecord, double initialAmount) {
        BacktestResultDTO result = new BacktestResultDTO();
        result.setInitialAmount(initialAmount);
        result.setInitialBalance(initialAmount);

        if (series != null) {
            result.setTotalBars(series.getBarCount());
        }

        if (tradingRecord == null || series == null || series.getBarCount() == 0) {
            result.setFinalAmount(initialAmount);
            result.setFinalBalance(initialAmount);
            result.setTotalProfit(0.0);
            result.setTotalReturn(0.0);
            result.setNumberOfTrades(0);
            result.setTotalTrades(0);
            return result;
        }

        try {
            // 计算交易次数
            int tradeCount = tradingRecord.getTradeCount();
            result.setNumberOfTrades(tradeCount);
            result.setTotalTrades(tradeCount);

            if (tradeCount > 0) {
                // 计算总收益率
                Num totalReturnValue = new TotalReturnCriterion().calculate(series, tradingRecord);
                result.setTotalReturn(totalReturnValue.doubleValue() * 100); // 转为百分比

                // 计算买入持有策略收益率
                Num buyAndHoldReturnValue = new BuyAndHoldReturnCriterion().calculate(series, tradingRecord);
                result.setBuyAndHoldReturn(buyAndHoldReturnValue.doubleValue() * 100); // 转为百分比

                // 计算最大回撤
                Num maxDrawdownValue = new MaximumDrawdownCriterion().calculate(series, tradingRecord);
                result.setMaxDrawdown(maxDrawdownValue.doubleValue() * 100); // 转为百分比

                // 计算盈利交易比例
                Num profitableTradesRatio = new ProfitableTradesRatioCriterion().calculate(series, tradingRecord);
                result.setWinRate(profitableTradesRatio.doubleValue() * 100); // 转为百分比

                // 计算盈利交易数和亏损交易数
                int profitableTradesCount = (int) Math.round(profitableTradesRatio.doubleValue() * tradeCount);
                result.setProfitableTrades(profitableTradesCount);
                result.setLosingTrades(tradeCount - profitableTradesCount);

                // 计算夏普比率 (如果可用)
                try {
                    Num sharpeRatioValue = new SharpeRatioCriterion().calculate(series, tradingRecord);
                    result.setSharpeRatio(sharpeRatioValue.doubleValue());
                } catch (Exception e) {
                    log.warn("计算夏普比率时出错: {}", e.getMessage());
                    result.setSharpeRatio(0.0);
                }

                // 计算平均收益
                Num averageProfitValue = new AverageProfitCriterion().calculate(series, tradingRecord);
                result.setAverageProfit(averageProfitValue.doubleValue() * 100); // 转为百分比
                result.setAverageProfitPerTrade(averageProfitValue.doubleValue() * 100);

                // 计算最终金额
                double finalAmount = initialAmount * (1 + totalReturnValue.doubleValue());
                result.setFinalAmount(finalAmount);
                result.setFinalBalance(finalAmount);

                // 计算总收益金额
                double totalProfit = finalAmount - initialAmount;
                result.setTotalProfit(totalProfit);
            }
        } catch (Exception e) {
            log.error("计算回测指标时出错: {}", e.getMessage());
        }

        return result;
    }
}
