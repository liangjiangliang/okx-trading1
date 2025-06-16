package com.okx.trading.ta4j.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.candles.BearishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BearishHaramiIndicator;
import org.ta4j.core.indicators.candles.BullishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BullishHaramiIndicator;
import org.ta4j.core.indicators.candles.DojiIndicator;
import org.ta4j.core.indicators.candles.ThreeBlackCrowsIndicator;
import org.ta4j.core.indicators.candles.ThreeWhiteSoldiersIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.ichimoku.IchimokuKijunSenIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanAIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanBIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuTenkanSenIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelLowerIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelUpperIndicator;
import org.ta4j.core.indicators.statistics.CovarianceIndicator;
import org.ta4j.core.indicators.statistics.PearsonCorrelationIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.statistics.StandardErrorIndicator;
import org.ta4j.core.indicators.statistics.VarianceIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;

import static com.okx.trading.constant.IndicatorInfo.*;

/**
 * 策略工厂类
 * 用于创建和管理各种交易策略
 */
public class StrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(StrategyFactory.class);


    // 策略创建函数映射
    private static final Map<String, Function<BarSeries,  Strategy>> strategyCreators = new HashMap<>();

    static {
        // 注册所有策略创建函数
        // 移动平均线策略
        strategyCreators.put(STRATEGY_SMA, StrategyFactory::createSMAStrategy);
        strategyCreators.put(STRATEGY_EMA, StrategyFactory::createEMAStrategy);
        strategyCreators.put(STRATEGY_TRIPLE_EMA, StrategyFactory::createTripleEMAStrategy);
        strategyCreators.put(STRATEGY_WMA, StrategyFactory::createWMAStrategy);
        strategyCreators.put(STRATEGY_HMA, StrategyFactory::createHMAStrategy);
        strategyCreators.put(STRATEGY_KAMA, StrategyFactory::createKAMAStrategy);
        strategyCreators.put(STRATEGY_ZLEMA, StrategyFactory::createZLEMAStrategy);
        strategyCreators.put(STRATEGY_DEMA, StrategyFactory::createDEMAStrategy);
        strategyCreators.put(STRATEGY_TEMA, StrategyFactory::createTEMAStrategy);
        strategyCreators.put(STRATEGY_VWAP, StrategyFactory::createVWAPStrategy);

        // 震荡指标策略
        strategyCreators.put(STRATEGY_RSI, StrategyFactory::createRSIStrategy);
        strategyCreators.put(STRATEGY_STOCHASTIC, StrategyFactory::createStochasticStrategy);
        strategyCreators.put(STRATEGY_STOCHASTIC_RSI, StrategyFactory::createStochasticRSIStrategy);
        strategyCreators.put(STRATEGY_WILLIAMS_R, StrategyFactory::createWilliamsRStrategy);
        strategyCreators.put(STRATEGY_CCI, StrategyFactory::createCCIStrategy);
        strategyCreators.put(STRATEGY_CMO, StrategyFactory::createCMOStrategy);
        strategyCreators.put(STRATEGY_ROC, StrategyFactory::createROCStrategy);
        strategyCreators.put(STRATEGY_MACD, StrategyFactory::createMACDStrategy);
        strategyCreators.put(STRATEGY_PPO, StrategyFactory::createPPOStrategy);
        strategyCreators.put(STRATEGY_DPO, StrategyFactory::createDPOStrategy);
        strategyCreators.put(STRATEGY_TRIX, StrategyFactory::createTRIXStrategy);

        // 趋势指标策略
        strategyCreators.put(STRATEGY_ADX, StrategyFactory::createADXStrategy);
        strategyCreators.put(STRATEGY_AROON, StrategyFactory::createAroonStrategy);
        strategyCreators.put(STRATEGY_ICHIMOKU, StrategyFactory::createIchimokuStrategy);
        strategyCreators.put(STRATEGY_PARABOLIC_SAR, StrategyFactory::createParabolicSARStrategy);
        strategyCreators.put(STRATEGY_DMA, StrategyFactory::createDMAStrategy);
        strategyCreators.put(STRATEGY_DMI, StrategyFactory::createDMIStrategy);
        strategyCreators.put(STRATEGY_SUPERTREND, StrategyFactory::createSupertrendStrategy);
        strategyCreators.put(STRATEGY_ICHIMOKU_CLOUD_BREAKOUT, StrategyFactory::createIchimokuCloudBreakoutStrategy);
        strategyCreators.put(STRATEGY_AWESOME_OSCILLATOR, StrategyFactory::createAwesomeOscillatorStrategy);

        // 波动指标策略
        strategyCreators.put(STRATEGY_BOLLINGER_BANDS, StrategyFactory::createBollingerBandsStrategy);
        strategyCreators.put(STRATEGY_CHANDELIER_EXIT, StrategyFactory::createChandelierExitStrategy);
        strategyCreators.put(STRATEGY_ULCER_INDEX, StrategyFactory::createUlcerIndexStrategy);
        strategyCreators.put(STRATEGY_KELTNER_CHANNEL, StrategyFactory::createKeltnerChannelStrategy);
        strategyCreators.put(STRATEGY_ATR, StrategyFactory::createATRStrategy);

        // 成交量指标策略
        strategyCreators.put(STRATEGY_OBV, StrategyFactory::createOBVStrategy);
        strategyCreators.put(STRATEGY_MASS_INDEX, StrategyFactory::createMassIndexStrategy);
        strategyCreators.put(STRATEGY_KDJ, StrategyFactory::createKDJStrategy);

        // 蜡烛图形态策略
        strategyCreators.put(STRATEGY_DOJI, StrategyFactory::createDojiStrategy);
        strategyCreators.put(STRATEGY_BULLISH_ENGULFING, StrategyFactory::createBullishEngulfingStrategy);
        strategyCreators.put(STRATEGY_BEARISH_ENGULFING, StrategyFactory::createBearishEngulfingStrategy);
        strategyCreators.put(STRATEGY_BULLISH_HARAMI, StrategyFactory::createBullishHaramiStrategy);
        strategyCreators.put(STRATEGY_BEARISH_HARAMI, StrategyFactory::createBearishHaramiStrategy);
        strategyCreators.put(STRATEGY_THREE_WHITE_SOLDIERS, StrategyFactory::createThreeWhiteSoldiersStrategy);
        strategyCreators.put(STRATEGY_THREE_BLACK_CROWS, StrategyFactory::createThreeBlackCrowsStrategy);
        strategyCreators.put(STRATEGY_HANGING_MAN, StrategyFactory::createHangingManStrategy);

        // 组合策略
        strategyCreators.put(STRATEGY_TURTLE_TRADING, StrategyFactory::createTurtleTradingStrategy);
        strategyCreators.put(STRATEGY_TREND_FOLLOWING, StrategyFactory::createTrendFollowingStrategy);
        strategyCreators.put(STRATEGY_BREAKOUT, StrategyFactory::createBreakoutStrategy);
        strategyCreators.put(STRATEGY_GOLDEN_CROSS, StrategyFactory::createGoldenCrossStrategy);
        strategyCreators.put(STRATEGY_DEATH_CROSS, StrategyFactory::createDeathCrossStrategy);
        strategyCreators.put(STRATEGY_DUAL_MA_WITH_RSI, StrategyFactory::createDualMAWithRSIStrategy);
        strategyCreators.put(STRATEGY_MACD_WITH_BOLLINGER, StrategyFactory::createMACDWithBollingerStrategy);
        
        // 添加新的移动平均线策略
        strategyCreators.put(STRATEGY_TRIMA, StrategyFactory::createTrimaStrategy);
        strategyCreators.put(STRATEGY_T3, StrategyFactory::createT3Strategy);
        strategyCreators.put(STRATEGY_MAMA, StrategyFactory::createMamaStrategy);
        strategyCreators.put(STRATEGY_VIDYA, StrategyFactory::createVidyaStrategy);
        strategyCreators.put(STRATEGY_WILDERS, StrategyFactory::createWildersStrategy);
        
        // 添加新的震荡指标策略
        strategyCreators.put(STRATEGY_FISHER, StrategyFactory::createFisherStrategy);
        strategyCreators.put(STRATEGY_FOSC, StrategyFactory::createFoscStrategy);
        strategyCreators.put(STRATEGY_EOM, StrategyFactory::createEomStrategy);
        strategyCreators.put(STRATEGY_CHOP, StrategyFactory::createChopStrategy);
        strategyCreators.put(STRATEGY_KVO, StrategyFactory::createKvoStrategy);
        strategyCreators.put(STRATEGY_RVGI, StrategyFactory::createRvgiStrategy);
        strategyCreators.put(STRATEGY_STC, StrategyFactory::createStcStrategy);
        
        // 添加新的趋势指标策略
        strategyCreators.put(STRATEGY_VORTEX, StrategyFactory::createVortexStrategy);
        strategyCreators.put(STRATEGY_QSTICK, StrategyFactory::createQstickStrategy);
        strategyCreators.put(STRATEGY_WILLIAMS_ALLIGATOR, StrategyFactory::createWilliamsAlligatorStrategy);
        strategyCreators.put(STRATEGY_HT_TRENDLINE, StrategyFactory::createHtTrendlineStrategy);
        
        // 添加新的波动指标策略
        strategyCreators.put(STRATEGY_NATR, StrategyFactory::createNatrStrategy);
        strategyCreators.put(STRATEGY_MASS, StrategyFactory::createMassStrategy);
        strategyCreators.put(STRATEGY_STDDEV, StrategyFactory::createStddevStrategy);
        strategyCreators.put(STRATEGY_SQUEEZE, StrategyFactory::createSqueezeStrategy);
        strategyCreators.put(STRATEGY_BBW, StrategyFactory::createBbwStrategy);
        strategyCreators.put(STRATEGY_VOLATILITY, StrategyFactory::createVolatilityStrategy);
        strategyCreators.put(STRATEGY_DONCHIAN_CHANNELS, StrategyFactory::createDonchianChannelsStrategy);
        
        // 添加新的成交量指标策略
        strategyCreators.put(STRATEGY_AD, StrategyFactory::createAdStrategy);
        strategyCreators.put(STRATEGY_ADOSC, StrategyFactory::createAdoscStrategy);
        strategyCreators.put(STRATEGY_NVI, StrategyFactory::createNviStrategy);
        strategyCreators.put(STRATEGY_PVI, StrategyFactory::createPviStrategy);
        strategyCreators.put(STRATEGY_VWMA, StrategyFactory::createVwmaStrategy);
        strategyCreators.put(STRATEGY_VOSC, StrategyFactory::createVoscStrategy);
        strategyCreators.put(STRATEGY_MARKETFI, StrategyFactory::createMarketfiStrategy);
        
        // 添加新的蜡烛图形态策略
        strategyCreators.put(STRATEGY_HAMMER, StrategyFactory::createHammerStrategy);
        strategyCreators.put(STRATEGY_INVERTED_HAMMER, StrategyFactory::createInvertedHammerStrategy);
        strategyCreators.put(STRATEGY_SHOOTING_STAR, StrategyFactory::createShootingStarStrategy);
        strategyCreators.put(STRATEGY_MORNING_STAR, StrategyFactory::createMorningStarStrategy);
        strategyCreators.put(STRATEGY_EVENING_STAR, StrategyFactory::createEveningStarStrategy);
        strategyCreators.put(STRATEGY_PIERCING, StrategyFactory::createPiercingStrategy);
        strategyCreators.put(STRATEGY_DARK_CLOUD_COVER, StrategyFactory::createDarkCloudCoverStrategy);
        strategyCreators.put(STRATEGY_MARUBOZU, StrategyFactory::createMarubozuStrategy);
        
        // 添加统计函数策略
        strategyCreators.put(STRATEGY_BETA, StrategyFactory::createBetaStrategy);
        strategyCreators.put(STRATEGY_CORREL, StrategyFactory::createCorrelStrategy);
        strategyCreators.put(STRATEGY_LINEARREG, StrategyFactory::createLinearregStrategy);
        strategyCreators.put(STRATEGY_LINEARREG_ANGLE, StrategyFactory::createLinearregAngleStrategy);
        strategyCreators.put(STRATEGY_LINEARREG_INTERCEPT, StrategyFactory::createLinearregInterceptStrategy);
        strategyCreators.put(STRATEGY_LINEARREG_SLOPE, StrategyFactory::createLinearregSlopeStrategy);
        strategyCreators.put(STRATEGY_TSF, StrategyFactory::createTsfStrategy);
        strategyCreators.put(STRATEGY_VAR, StrategyFactory::createVarStrategy);
        
        // 添加希尔伯特变换策略
        strategyCreators.put(STRATEGY_HT_DCPERIOD, StrategyFactory::createHtDcperiodStrategy);
        strategyCreators.put(STRATEGY_HT_DCPHASE, StrategyFactory::createHtDcphaseStrategy);
        strategyCreators.put(STRATEGY_HT_PHASOR, StrategyFactory::createHtPhasorStrategy);
        strategyCreators.put(STRATEGY_HT_SINE, StrategyFactory::createHtSineStrategy);
        strategyCreators.put(STRATEGY_HT_TRENDMODE, StrategyFactory::createHtTrendmodeStrategy);
        strategyCreators.put(STRATEGY_MSW, StrategyFactory::createMswStrategy);
    }

    /**
     * 创建策略
     *
     * @param series       BarSeries对象
     * @param strategyType 策略类型
     * @param params       策略参数
     * @return 策略对象
     */
    public static Strategy createStrategy(BarSeries series, String strategyType) {
        Function<BarSeries, Strategy> strategyCreator = strategyCreators.get(strategyType);

        if (strategyCreator == null) {
            throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }

        if (series == null || series.getBarCount() == 0) {
            throw new IllegalArgumentException("K线数据不能为空");
        }

        return strategyCreator.apply(series);
    }

    /**
     * 创建SMA交叉策略
     */
    private static Strategy createSMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (longPeriod + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期SMA指标
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建布林带策略
     */
    private static Strategy createBollingerBandsStrategy(BarSeries series) {
        int period = (int) ( 20);
        double multiplier = (double) ( 2.0);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (period + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建布林带指标
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, period);

        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, sd, series.numOf(multiplier));
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, sd, series.numOf(multiplier));

        // 创建规则
        Rule entryRule = new UnderIndicatorRule(closePrice, lowerBand);
        Rule exitRule = new OverIndicatorRule(closePrice, upperBand);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建MACD策略
     */
    private static Strategy createMACDStrategy(BarSeries series) {
        int shortPeriod = (int) ( 12);
        int longPeriod = (int) ( 26);
        int signalPeriod = (int) ( 9);

        if (series.getBarCount() <= longPeriod + signalPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建MACD指标
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortPeriod);
        EMAIndicator longEma = new EMAIndicator(closePrice, longPeriod);
        MACDIndicator macd = new MACDIndicator(closePrice, shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(macd, signal);
        Rule exitRule = new CrossedDownIndicatorRule(macd, signal);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建RSI策略
     */
    private static Strategy createRSIStrategy(BarSeries series) {
        int period = (int) ( 14);
        int oversold = (int) ( 30);
        int overbought = (int) ( 70);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建RSI指标
        RSIIndicator rsi = new RSIIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(rsi, series.numOf(oversold));
        Rule exitRule = new CrossedDownIndicatorRule(rsi, series.numOf(overbought));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建随机指标策略
     */
    private static Strategy createStochasticStrategy(BarSeries series) {
        int kPeriod = (int) ( 14);
        int kSmooth = (int) ( 3);
        int dSmooth = (int) ( 3);
        int oversold = (int) ( 20);
        int overbought = (int) ( 80);

        if (series.getBarCount() <= kPeriod + kSmooth + dSmooth) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        // 创建随机指标
        StochasticOscillatorKIndicator stochasticK = new StochasticOscillatorKIndicator(series, kPeriod);
        SMAIndicator stochasticD = new SMAIndicator(stochasticK, dSmooth);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(stochasticK, stochasticD)
                .and(new UnderIndicatorRule(stochasticK, series.numOf(oversold)));

        Rule exitRule = new CrossedDownIndicatorRule(stochasticK, stochasticD)
                .and(new OverIndicatorRule(stochasticK, series.numOf(overbought)));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建ADX策略
     */
    private static Strategy createADXStrategy(BarSeries series) {
        int adxPeriod = (int) ( 14);
        int diPeriod = (int) ( 14);
        int threshold = (int) ( 25);

        if (series.getBarCount() <= Math.max(adxPeriod, diPeriod) + 1) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        // 创建ADX指标
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // 使用自定义实现替代缺失的指标类
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // 使用可用指标替代，或者简化ADX策略
        // 这里使用RSI和SMA指标替代缺失的ADX相关指标
        RSIIndicator rsi = new RSIIndicator(closePrice, adxPeriod);
        SMAIndicator sma = new SMAIndicator(closePrice, diPeriod);

        // 创建规则
        Rule entryRule = new OverIndicatorRule(rsi, series.numOf(threshold))
                .and(new OverIndicatorRule(closePrice, sma));

        Rule exitRule = new UnderIndicatorRule(rsi, series.numOf(threshold))
                .and(new UnderIndicatorRule(closePrice, sma));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建CCI策略
     */
    private static Strategy createCCIStrategy(BarSeries series) {
        int period = (int) ( 20);
        int oversold = (int) ( -100);
        int overbought = (int) ( 100);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        // 创建CCI指标
        CCIIndicator cci = new CCIIndicator(series, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(cci, series.numOf(oversold));
        Rule exitRule = new CrossedDownIndicatorRule(cci, series.numOf(overbought));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建威廉指标策略
     */
    private static Strategy createWilliamsRStrategy(BarSeries series) {
        int period = (int) ( 14);
        int oversold = (int) ( -80);
        int overbought = (int) ( -20);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        // 创建威廉指标
        WilliamsRIndicator williamsR = new WilliamsRIndicator(series, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(williamsR, series.numOf(oversold));
        Rule exitRule = new CrossedDownIndicatorRule(williamsR, series.numOf(overbought));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建三重EMA策略
     */
    private static Strategy createTripleEMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 5);
        int middlePeriod = (int) ( 10);
        int longPeriod = (int) ( 20);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建三个EMA指标
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortPeriod);
        EMAIndicator middleEma = new EMAIndicator(closePrice, middlePeriod);
        EMAIndicator longEma = new EMAIndicator(closePrice, longPeriod);

        // 创建规则 (短EMA > 中EMA > 长EMA 买入，反之卖出)
        Rule entryRule = new OverIndicatorRule(shortEma, middleEma)
                .and(new OverIndicatorRule(middleEma, longEma));

        Rule exitRule = new UnderIndicatorRule(shortEma, middleEma)
                .and(new UnderIndicatorRule(middleEma, longEma));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建一目均衡表策略
     */
    private static Strategy createIchimokuStrategy(BarSeries series) {
        int conversionPeriod = (int) ( 9);
        int basePeriod = (int) ( 26);
        int laggingSpan = (int) ( 52);

        if (series.getBarCount() <= laggingSpan) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建自定义转换线和基准线指标
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // 使用可用指标替代缺失的HighestValueIndicator和LowestValueIndicator
        MaxPriceIndicator maxPrice9 = new MaxPriceIndicator(series, conversionPeriod);
        MinPriceIndicator minPrice9 = new MinPriceIndicator(series, conversionPeriod);
        MaxPriceIndicator maxPrice26 = new MaxPriceIndicator(series, basePeriod);
        MinPriceIndicator minPrice26 = new MinPriceIndicator(series, basePeriod);

        // 转换线和基准线交叉作为买卖信号
        Rule entryRule = new CrossedUpIndicatorRule(
                closePrice,
                new SMAIndicator(closePrice, basePeriod));

        Rule exitRule = new CrossedDownIndicatorRule(
                closePrice,
                new SMAIndicator(closePrice, basePeriod));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建EMA策略
     */
    private static Strategy createEMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期EMA指标
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortPeriod);
        EMAIndicator longEma = new EMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建WMA策略 (加权移动平均线)
     */
    private static Strategy createWMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期WMA指标
        WMAIndicator shortWma = new WMAIndicator(closePrice, shortPeriod);
        WMAIndicator longWma = new WMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortWma, longWma);
        Rule exitRule = new CrossedDownIndicatorRule(shortWma, longWma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建HMA策略 (Hull移动平均线)
     */
    private static Strategy createHMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期HMA指标
        HMAIndicator shortHma = new HMAIndicator(closePrice, shortPeriod);
        HMAIndicator longHma = new HMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortHma, longHma);
        Rule exitRule = new CrossedDownIndicatorRule(shortHma, longHma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建KAMA策略 (考夫曼自适应移动平均线)
     */
    private static Strategy createKAMAStrategy(BarSeries series) {
        int period = (int) ( 10);
        int fastEMA = (int) ( 2);
        int slowEMA = (int) ( 30);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建KAMA指标
        KAMAIndicator kama = new KAMAIndicator(closePrice, period, fastEMA, slowEMA);
        SMAIndicator sma = new SMAIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(kama, sma);
        Rule exitRule = new CrossedDownIndicatorRule(kama, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建ZLEMA策略 (零滞后指数移动平均线)
     */
    private static Strategy createZLEMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期ZLEMA指标
        ZLEMAIndicator shortZlema = new ZLEMAIndicator(closePrice, shortPeriod);
        ZLEMAIndicator longZlema = new ZLEMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortZlema, longZlema);
        Rule exitRule = new CrossedDownIndicatorRule(shortZlema, longZlema);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建DEMA策略 (双重指数移动平均线)
     */
    private static Strategy createDEMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期DEMA指标
        DoubleEMAIndicator shortDema = new DoubleEMAIndicator(closePrice, shortPeriod);
        DoubleEMAIndicator longDema = new DoubleEMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortDema, longDema);
        Rule exitRule = new CrossedDownIndicatorRule(shortDema, longDema);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建TEMA策略 (三重指数移动平均线)
     */
    private static Strategy createTEMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期TEMA指标
        TripleEMAIndicator shortTema = new TripleEMAIndicator(closePrice, shortPeriod);
        TripleEMAIndicator longTema = new TripleEMAIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortTema, longTema);
        Rule exitRule = new CrossedDownIndicatorRule(shortTema, longTema);

        return new BaseStrategy(entryRule, exitRule);
    }


    /**
     * 创建随机RSI策略
     */
    private static Strategy createStochasticRSIStrategy(BarSeries series) {
        int rsiPeriod = (int) ( 14);
        int stochasticPeriod = (int) ( 14);
        int kPeriod = (int) ( 3);
        int dPeriod = (int) ( 3);
        int overbought = (int) ( 80);
        int oversold = (int) ( 20);

        if (series.getBarCount() <= rsiPeriod + stochasticPeriod + kPeriod + dPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);
        StochasticRSIIndicator stochRsi = new StochasticRSIIndicator(rsi, stochasticPeriod);
        SMAIndicator k = new SMAIndicator(stochRsi, kPeriod);
        SMAIndicator d = new SMAIndicator(k, dPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(k, d)
                .and(new UnderIndicatorRule(k, oversold));

        Rule exitRule = new CrossedDownIndicatorRule(k, d)
                .and(new OverIndicatorRule(k, overbought));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建CMO策略 (钱德动量震荡指标)
     */
    private static Strategy createCMOStrategy(BarSeries series) {
        int period = (int) ( 14);
        int overbought = (int) ( 50);
        int oversold = (int) ( -50);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        CMOIndicator cmo = new CMOIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(cmo, oversold);
        Rule exitRule = new CrossedDownIndicatorRule(cmo, overbought);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建ROC策略 (变动率指标)
     */
    private static Strategy createROCStrategy(BarSeries series) {
        int period = (int) ( 12);
        double threshold = (double) ( 0.0);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ROCIndicator roc = new ROCIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(roc, threshold);
        Rule exitRule = new CrossedDownIndicatorRule(roc, threshold);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建PPO策略 (百分比价格震荡指标)
     */
    private static Strategy createPPOStrategy(BarSeries series) {
        int shortPeriod = (int) ( 12);
        int longPeriod = (int) ( 26);
        int signalPeriod = (int) ( 9);

        if (series.getBarCount() <= longPeriod + signalPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        PPOIndicator ppo = new PPOIndicator(closePrice, shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(ppo, signalPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(ppo, signal);
        Rule exitRule = new CrossedDownIndicatorRule(ppo, signal);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建DPO策略 (区间震荡指标)
     */
    private static Strategy createDPOStrategy(BarSeries series) {
        int period = (int) ( 20);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        DPOIndicator dpo = new DPOIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(dpo, 0);
        Rule exitRule = new CrossedDownIndicatorRule(dpo, 0);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建Aroon策略
     */
    private static Strategy createAroonStrategy(BarSeries series) {
        int period = (int) ( 25);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        AroonUpIndicator aroonUp = new AroonUpIndicator(series, period);
        AroonDownIndicator aroonDown = new AroonDownIndicator(series, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(aroonUp, aroonDown);
        Rule exitRule = new CrossedDownIndicatorRule(aroonUp, aroonDown);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建DMA策略 (差异移动平均线)
     */
    private static Strategy createDMAStrategy(BarSeries series) {
        int shortPeriod = (int) ( 10);
        int longPeriod = (int) ( 50);
        int signalPeriod = (int) ( 10);

        if (series.getBarCount() <= longPeriod + signalPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);

        // DMA = 短期均线 - 长期均线
        DifferenceIndicator dma = new DifferenceIndicator(shortSma, longSma);
        SMAIndicator signal = new SMAIndicator(dma, signalPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(dma, signal);
        Rule exitRule = new CrossedDownIndicatorRule(dma, signal);

        return new BaseStrategy(entryRule, exitRule);
    }


    /**
     * 创建溃疡指数策略
     */
    private static Strategy createUlcerIndexStrategy(BarSeries series) {
        int period = (int) ( 14);
        double threshold = (double) ( 5.0);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        UlcerIndexIndicator ulcerIndex = new UlcerIndexIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new UnderIndicatorRule(ulcerIndex, threshold);
        Rule exitRule = new OverIndicatorRule(ulcerIndex, threshold);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建OBV策略 (能量潮指标)
     */
    private static Strategy createOBVStrategy(BarSeries series) {
        int period = (int) ( 20);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        OnBalanceVolumeIndicator obv = new OnBalanceVolumeIndicator(series);
        SMAIndicator obvSma = new SMAIndicator(obv, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(obv, obvSma);
        Rule exitRule = new CrossedDownIndicatorRule(obv, obvSma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建质量指数策略
     */
    private static Strategy createMassIndexStrategy(BarSeries series) {
        int emaPeriod = (int) ( 9);
        int massIndexPeriod = (int) ( 25);
        double threshold = (double) ( 27.0);

        if (series.getBarCount() <= emaPeriod * 2 + massIndexPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        MassIndexIndicator massIndex = new MassIndexIndicator(series, emaPeriod, massIndexPeriod);

        // 创建规则 - 当质量指数从高于阈值交叉到低于阈值时买入，反之卖出
        Rule entryRule = new CrossedDownIndicatorRule(massIndex, threshold);
        Rule exitRule = new CrossedUpIndicatorRule(massIndex, threshold);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建十字星策略
     */
    private static Strategy createDojiStrategy(BarSeries series) {
        double tolerance = (double) ( 0.05);

        DojiIndicator doji = new DojiIndicator(series, 10, tolerance);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现十字星且价格低于20日均线时买入，当价格高于20日均线时卖出
        Rule entryRule = new BooleanIndicatorRule(doji)
                .and(new UnderIndicatorRule(closePrice, sma));

        Rule exitRule = new OverIndicatorRule(closePrice, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建看涨吞没策略
     */
    private static Strategy createBullishEngulfingStrategy(BarSeries series) {
        BullishEngulfingIndicator bullishEngulfing = new BullishEngulfingIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现看涨吞没形态且价格低于20日均线时买入，当价格高于20日均线时卖出
        Rule entryRule = new BooleanIndicatorRule(bullishEngulfing)
                .and(new UnderIndicatorRule(closePrice, sma));

        Rule exitRule = new OverIndicatorRule(closePrice, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建看跌吞没策略
     */
    private static Strategy createBearishEngulfingStrategy(BarSeries series) {
        BearishEngulfingIndicator bearishEngulfing = new BearishEngulfingIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现看跌吞没形态且价格高于20日均线时卖出，当价格低于20日均线时买入
        Rule entryRule = new UnderIndicatorRule(closePrice, sma);

        Rule exitRule = new BooleanIndicatorRule(bearishEngulfing)
                .and(new OverIndicatorRule(closePrice, sma));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建看涨孕线策略
     */
    private static Strategy createBullishHaramiStrategy(BarSeries series) {
        BullishHaramiIndicator bullishHarami = new BullishHaramiIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现看涨孕线形态且价格低于20日均线时买入，当价格高于20日均线时卖出
        Rule entryRule = new BooleanIndicatorRule(bullishHarami)
                .and(new UnderIndicatorRule(closePrice, sma));

        Rule exitRule = new OverIndicatorRule(closePrice, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建看跌孕线策略
     */
    private static Strategy createBearishHaramiStrategy(BarSeries series) {
        BearishHaramiIndicator bearishHarami = new BearishHaramiIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现看跌孕线形态且价格高于20日均线时卖出，当价格低于20日均线时买入
        Rule entryRule = new UnderIndicatorRule(closePrice, sma);

        Rule exitRule = new BooleanIndicatorRule(bearishHarami)
                .and(new OverIndicatorRule(closePrice, sma));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建三白兵策略
     */
    private static Strategy createThreeWhiteSoldiersStrategy(BarSeries series) {
        ThreeWhiteSoldiersIndicator threeWhiteSoldiers = new ThreeWhiteSoldiersIndicator(series, 5, DecimalNum.valueOf(0.3));
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现三白兵形态时买入，当价格低于20日均线时卖出
        Rule entryRule = new BooleanIndicatorRule(threeWhiteSoldiers);
        Rule exitRule = new UnderIndicatorRule(closePrice, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建三黑乌鸦策略
     */
    private static Strategy createThreeBlackCrowsStrategy(BarSeries series) {
        ThreeBlackCrowsIndicator threeBlackCrows = new ThreeBlackCrowsIndicator(series, 5, 0.3);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则 - 当出现三黑乌鸦形态时卖出，当价格高于20日均线时买入
        Rule entryRule = new OverIndicatorRule(closePrice, sma);
        Rule exitRule = new BooleanIndicatorRule(threeBlackCrows);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建双推策略
     */
    private static Strategy createDoublePushStrategy(BarSeries series) {
        int shortPeriod = (int) ( 5);
        int longPeriod = (int) ( 20);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        // 创建规则 - 当短期均线上穿长期均线且RSI大于50时买入，当短期均线下穿长期均线且RSI小于50时卖出
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma)
                .and(new OverIndicatorRule(rsi, 50));

        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .and(new UnderIndicatorRule(rsi, 50));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建海龟交易策略
     */
    private static Strategy createTurtleTradingStrategy(BarSeries series) {
        int entryPeriod = (int) ( 20);
        int exitPeriod = (int) ( 10);

        if (series.getBarCount() <= entryPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // 创建最高价和最低价指标
        MaxPriceIndicator highestHigh = new MaxPriceIndicator(series, entryPeriod);
        MinPriceIndicator lowestLow = new MinPriceIndicator(series, exitPeriod);

        // 创建规则 - 当价格突破N日最高价时买入，当价格跌破N/2日最低价时卖出
        Rule entryRule = new OverIndicatorRule(closePrice, highestHigh);
        Rule exitRule = new UnderIndicatorRule(closePrice, lowestLow);

        return new BaseStrategy(entryRule, exitRule);
    }


    /**
     * 创建趋势跟踪策略
     */
    private static Strategy createTrendFollowingStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 26);
        int signalPeriod = (int) ( 9);

        if (series.getBarCount() <= longPeriod + signalPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortPeriod);
        EMAIndicator longEma = new EMAIndicator(closePrice, longPeriod);

        // 计算MACD指标
        MACDIndicator macd = new MACDIndicator(closePrice, shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);

        // 创建ADX指标（使用RSI替代）
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        // 创建规则 - 当MACD上穿信号线且RSI大于50时买入，当MACD下穿信号线且RSI小于50时卖出
        Rule entryRule = new CrossedUpIndicatorRule(macd, signal)
                .and(new OverIndicatorRule(rsi, 50));

        Rule exitRule = new CrossedDownIndicatorRule(macd, signal)
                .or(new UnderIndicatorRule(rsi, 30));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建突破策略
     */
    private static Strategy createBreakoutStrategy(BarSeries series) {
        int period = (int) ( 20);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MaxPriceIndicator highestHigh = new MaxPriceIndicator(series, period);
        MinPriceIndicator lowestLow = new MinPriceIndicator(series, period);

        // 创建规则 - 当价格突破N日最高价时买入，当价格跌破N日最低价时卖出
        Rule entryRule = new OverIndicatorRule(closePrice, highestHigh);
        Rule exitRule = new UnderIndicatorRule(closePrice, lowestLow);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建金叉策略
     */
    private static Strategy createGoldenCrossStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 26);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);

        // 创建规则 - 当短期均线上穿长期均线时买入
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建死叉策略
     */
    private static Strategy createDeathCrossStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 26);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);

        // 创建规则 - 当短期均线下穿长期均线时卖出
        Rule entryRule = new CrossedUpIndicatorRule(longSma, shortSma);
        Rule exitRule = new CrossedDownIndicatorRule(longSma, shortSma);

        return new BaseStrategy(entryRule, exitRule);
    }


    /**
     * 创建TRIX策略
     */
    private static Strategy createTRIXStrategy(BarSeries series) {
        int period = (int) ( 15);
        int signalPeriod = (int) ( 9);

        if (series.getBarCount() <= period * 3 + signalPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建三重EMA
        EMAIndicator ema1 = new EMAIndicator(closePrice, period);
        EMAIndicator ema2 = new EMAIndicator(ema1, period);
        EMAIndicator ema3 = new EMAIndicator(ema2, period);

        // 创建TRIX (当前值与前一个值的百分比变化)
        ROCIndicator trix = new ROCIndicator(ema3, 1);

        // 创建信号线
        SMAIndicator signal = new SMAIndicator(trix, signalPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(trix, signal);
        Rule exitRule = new CrossedDownIndicatorRule(trix, signal);

        return new BaseStrategy(entryRule, exitRule);
    }


    /**
     * 创建双均线RSI策略
     */
    private static Strategy createDualMAWithRSIStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);
        int rsiPeriod = (int) ( 14);
        int rsiThreshold = (int) ( 50);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(closePrice, longPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);

        // 创建规则 - 当短期均线上穿长期均线且RSI大于阈值时买入，当短期均线下穿长期均线或RSI小于阈值时卖出
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma)
                .and(new OverIndicatorRule(rsi, rsiThreshold));

        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .or(new UnderIndicatorRule(rsi, rsiThreshold));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建抛物线SAR策略
     */
    private static Strategy createParabolicSARStrategy(BarSeries series) {
        double step = (double) ( 0.02);
        double max = (double) ( 0.2);

        if (series.getBarCount() <= 2) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        // 创建抛物线SAR指标
        ParabolicSarIndicator sar = new ParabolicSarIndicator(series, series.numOf(step), series.numOf(max));
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(closePrice, sar);
        Rule exitRule = new CrossedDownIndicatorRule(closePrice, sar);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建吊灯线退出策略
     */
    private static Strategy createChandelierExitStrategy(BarSeries series) {
        // 确保period参数至少为1，避免TimePeriod为null的错误
        int period = Math.max(1, (int) ( 22));
        double multiplier = (double) ( 3.0);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建价格指标
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // 计算最高价和最低价
        MaxPriceIndicator highestHigh = new MaxPriceIndicator(series, period);
        MinPriceIndicator lowestLow = new MinPriceIndicator(series, period);

        // 计算ATR - 确保period大于0
        ATRIndicator atr = new ATRIndicator(series, period);

        // 创建自定义指标 - 多头吊灯线退出位置 (最高价 - ATR * multiplier)
        class LongChandelierExitIndicator extends CachedIndicator<Num> {
            private final MaxPriceIndicator highestHigh;
            private final ATRIndicator atr;
            private final Num multiplier;

            public LongChandelierExitIndicator(MaxPriceIndicator highestHigh, ATRIndicator atr, double multiplier, BarSeries series) {
                super(highestHigh);
                this.highestHigh = highestHigh;
                this.atr = atr;
                this.multiplier = series.numOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                if (index < period) {
                    return highestHigh.getValue(index);
                }
                return highestHigh.getValue(index).minus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        // 创建自定义指标 - 空头吊灯线退出位置 (最低价 + ATR * multiplier)
        class ShortChandelierExitIndicator extends CachedIndicator<Num> {
            private final MinPriceIndicator lowestLow;
            private final ATRIndicator atr;
            private final Num multiplier;

            public ShortChandelierExitIndicator(MinPriceIndicator lowestLow, ATRIndicator atr, double multiplier, BarSeries series) {
                super(lowestLow);
                this.lowestLow = lowestLow;
                this.atr = atr;
                this.multiplier = series.numOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                if (index < period) {
                    return lowestLow.getValue(index);
                }
                return lowestLow.getValue(index).plus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        // 创建吊灯线指标
        LongChandelierExitIndicator longExit = new LongChandelierExitIndicator(highestHigh, atr, multiplier, series);
        ShortChandelierExitIndicator shortExit = new ShortChandelierExitIndicator(lowestLow, atr, multiplier, series);

        // 创建入场规则 - 使用简单的突破规则作为入场条件
        Rule entryRule = new CrossedUpIndicatorRule(closePrice, new MaxPriceIndicator(series, period / 2))
                .or(new CrossedDownIndicatorRule(closePrice, new MinPriceIndicator(series, period / 2)));

        // 创建多头止损规则 - 当价格跌破吊灯线止损位时退出
        Rule longExitRule = new CrossedDownIndicatorRule(closePrice, longExit);

        // 创建空头止损规则 - 当价格上涨突破吊灯线止损位时退出
        Rule shortExitRule = new CrossedUpIndicatorRule(closePrice, shortExit);

        // 组合退出规则 - 多头或空头止损触发时退出
        Rule exitRule = longExitRule.or(shortExitRule);

        // 创建策略
        return new BaseStrategy(entryRule, exitRule);
    }

    // 自定义最大价格指标
    private static class MaxPriceIndicator extends CachedIndicator<Num> {
        private final HighPriceIndicator highPrice;
        private final int period;

        public MaxPriceIndicator(BarSeries series, int period) {
            super(series);
            this.highPrice = new HighPriceIndicator(series);
            this.period = period;
        }

        @Override
        protected Num calculate(int index) {
            int startIndex = Math.max(0, index - period + 1);
            Num highest = highPrice.getValue(startIndex);

            for (int i = startIndex + 1; i <= index; i++) {
                Num current = highPrice.getValue(i);
                if (current.isGreaterThan(highest)) {
                    highest = current;
                }
            }

            return highest;
        }
    }

    // 自定义最小价格指标
    private static class MinPriceIndicator extends CachedIndicator<Num> {
        private final LowPriceIndicator lowPrice;
        private final int period;

        public MinPriceIndicator(BarSeries series, int period) {
            super(series);
            this.lowPrice = new LowPriceIndicator(series);
            this.period = period;
        }

        @Override
        protected Num calculate(int index) {
            int startIndex = Math.max(0, index - period + 1);
            Num lowest = lowPrice.getValue(startIndex);

            for (int i = startIndex + 1; i <= index; i++) {
                Num current = lowPrice.getValue(i);
                if (current.isLessThan(lowest)) {
                    lowest = current;
                }
            }

            return lowest;
        }
    }

    /**
     * 创建MACD与布林带组合策略
     */
    private static Strategy createMACDWithBollingerStrategy(BarSeries series) {
        // 获取MACD相关参数
        int shortPeriod = (int) ( 12);
        int longPeriod = (int) ( 26);
        int signalPeriod = (int) ( 9);

        // 获取布林带相关参数
        int bollingerPeriod = (int) ( 20);
        double bollingerDeviation = (double) ( 2.0);

        // 验证数据点数量是否足够
        if (series.getBarCount() <= Math.max(longPeriod + signalPeriod, bollingerPeriod)) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建MACD指标
        MACDIndicator macd = new MACDIndicator(closePrice, shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);

        // 创建布林带指标
        SMAIndicator sma = new SMAIndicator(closePrice, bollingerPeriod);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, bollingerPeriod);

        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, sd, series.numOf(bollingerDeviation));
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, sd, series.numOf(bollingerDeviation));

        // 创建组合规则
        // 买入规则: MACD上穿信号线 且 价格位于布林带下轨以下
        Rule entryRule = new CrossedUpIndicatorRule(macd, signal)
                .and(new UnderIndicatorRule(closePrice, lowerBand));

        // 卖出规则: MACD下穿信号线 或 价格位于布林带上轨以上
        Rule exitRule = new CrossedDownIndicatorRule(macd, signal)
                .or(new OverIndicatorRule(closePrice, upperBand));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建吊锤形态策略
     */
    private static Strategy createHangingManStrategy(BarSeries series) {
        double upperShadowRatio = (double) ( 0.1);
        double lowerShadowRatio = (double) ( 2.0);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPrice = new OpenPriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);

        // 创建自定义的吊锤形态指标
        class HangingManIndicator extends CachedIndicator<Boolean> {
            private final HighPriceIndicator highPrice;
            private final LowPriceIndicator lowPrice;
            private final OpenPriceIndicator openPrice;
            private final ClosePriceIndicator closePrice;
            private final double upperShadowRatio;
            private final double lowerShadowRatio;

            public HangingManIndicator(BarSeries series, double upperShadowRatio, double lowerShadowRatio) {
                super(series);
                this.highPrice = new HighPriceIndicator(series);
                this.lowPrice = new LowPriceIndicator(series);
                this.openPrice = new OpenPriceIndicator(series);
                this.closePrice = new ClosePriceIndicator(series);
                this.upperShadowRatio = upperShadowRatio;
                this.lowerShadowRatio = lowerShadowRatio;
            }

            @Override
            protected Boolean calculate(int index) {
                if (index <= 0) {
                    return false;
                }

                Num open = openPrice.getValue(index);
                Num close = closePrice.getValue(index);
                Num high = highPrice.getValue(index);
                Num low = lowPrice.getValue(index);

                // 计算实体部分
                Num body = open.isGreaterThan(close) ? open.minus(close) : close.minus(open);

                // 如果实体太小，不符合吊锤特征
                if (body.isEqual(series.numOf(0))) {
                    return false;
                }

                // 计算上影线与下影线
                Num upperShadow = high.minus(open.isGreaterThan(close) ? open : close);
                Num lowerShadow = (open.isLessThan(close) ? open : close).minus(low);

                // 上影线应该很短，下影线应该很长（至少是实体的2倍）
                boolean isShortUpperShadow = upperShadow.dividedBy(body).isLessThanOrEqual(series.numOf(upperShadowRatio));
                boolean isLongLowerShadow = lowerShadow.dividedBy(body).isGreaterThanOrEqual(series.numOf(lowerShadowRatio));

                // 前一日收盘价趋势（可以确认是在上升趋势中出现）
                boolean isUptrend = index > 5 && closePrice.getValue(index - 1).isGreaterThan(closePrice.getValue(index - 5));

                // 满足吊锤形态的条件：短上影线，长下影线，在上升趋势中
                return isShortUpperShadow && isLongLowerShadow && isUptrend;
            }
        }

        // 创建吊锤形态指标
        HangingManIndicator hangingMan = new HangingManIndicator(series, upperShadowRatio, lowerShadowRatio);

        // 创建规则 - 当出现吊锤形态且价格高于20日均线时，视为顶部反转信号，卖出
        Rule entryRule = new UnderIndicatorRule(closePrice, sma20);  // 价格低于均线时买入
        Rule exitRule = new BooleanIndicatorRule(hangingMan)
                .and(new OverIndicatorRule(closePrice, sma20));  // 出现吊锤形态且价格高于均线时卖出

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建VWAP策略
     */
    private static Strategy createVWAPStrategy(BarSeries series) {
        int period = (int) ( 14);

        // 创建VWAP指标
        VWAPIndicator vwap = new VWAPIndicator(series, period);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 买入规则：价格上穿VWAP
        Rule entryRule = new CrossedUpIndicatorRule(closePrice, vwap);

        // 卖出规则：价格下穿VWAP
        Rule exitRule = new CrossedDownIndicatorRule(closePrice, vwap);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建肯特纳通道策略
     */
    private static Strategy createKeltnerChannelStrategy(BarSeries series) {
        int emaPeriod = (int) ( 20);
        int atrPeriod = (int) ( 10);
        double multiplier = 0.2;

        // 创建肯特纳通道指标
        EMAIndicator ema = new EMAIndicator(new ClosePriceIndicator(series), emaPeriod);
        ATRIndicator atr = new ATRIndicator(series, atrPeriod);

        KeltnerChannelMiddleIndicator middle = new KeltnerChannelMiddleIndicator(ema, 20);
        KeltnerChannelUpperIndicator upper = new KeltnerChannelUpperIndicator(middle, multiplier, 14);
        KeltnerChannelLowerIndicator lower = new KeltnerChannelLowerIndicator(middle, multiplier, 14);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 买入规则：价格跌破下轨
        Rule entryRule = new CrossedDownIndicatorRule(closePrice, lower);

        // 卖出规则：价格突破上轨
        Rule exitRule = new CrossedUpIndicatorRule(closePrice, upper);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建ATR策略
     */
    private static Strategy createATRStrategy(BarSeries series) {
        int period = (int) ( 14);
        double multiplier = 2.0;

        // 创建ATR指标
        ATRIndicator atr = new ATRIndicator(series, period);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建自定义指标 - 上轨 (收盘价 + ATR * multiplier)
        class UpperBandIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final ATRIndicator atr;
            private final Num multiplier;

            public UpperBandIndicator(ClosePriceIndicator closePrice, ATRIndicator atr, double multiplier, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.atr = atr;
                this.multiplier = series.numOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return closePrice.getValue(index).plus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        // 创建自定义指标 - 下轨 (收盘价 - ATR * multiplier)
        class LowerBandIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final ATRIndicator atr;
            private final Num multiplier;

            public LowerBandIndicator(ClosePriceIndicator closePrice, ATRIndicator atr, double multiplier, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.atr = atr;
                this.multiplier = series.numOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return closePrice.getValue(index).minus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        UpperBandIndicator upperBand = new UpperBandIndicator(closePrice, atr, multiplier, series);
        LowerBandIndicator lowerBand = new LowerBandIndicator(closePrice, atr, multiplier, series);

        // 买入规则：价格跌破下轨
        Rule entryRule = new CrossedDownIndicatorRule(closePrice, lowerBand);

        // 卖出规则：价格突破上轨
        Rule exitRule = new CrossedUpIndicatorRule(closePrice, upperBand);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建KDJ策略
     */
    private static Strategy createKDJStrategy(BarSeries series) {
        int kPeriod = (int) ( 9);
        int dPeriod = (int) ( 3);
        int jPeriod = (int) ( 3);
        int oversold = (int) ( 20);
        int overbought = (int) ( 80);

        // 创建KDJ指标
        StochasticOscillatorKIndicator k = new StochasticOscillatorKIndicator(series, kPeriod);
        StochasticOscillatorDIndicator d = new StochasticOscillatorDIndicator(k);

        // J = 3 * K - 2 * D
        class JIndicator extends CachedIndicator<Num> {
            private final StochasticOscillatorKIndicator k;
            private final StochasticOscillatorDIndicator d;
            private final Num three;
            private final Num two;

            public JIndicator(StochasticOscillatorKIndicator k, StochasticOscillatorDIndicator d, BarSeries series) {
                super(series);
                this.k = k;
                this.d = d;
                this.three = series.numOf(3);
                this.two = series.numOf(2);
            }

            @Override
            protected Num calculate(int index) {
                return k.getValue(index).multipliedBy(three).minus(d.getValue(index).multipliedBy(two));
            }
        }

        JIndicator j = new JIndicator(k, d, series);

        // 创建超买超卖阈值
        Num overboughtThreshold = series.numOf(overbought);
        Num oversoldThreshold = series.numOf(oversold);

        // 买入规则：J值上穿超卖线
        Rule entryRule = new CrossedUpIndicatorRule(j, oversoldThreshold);

        // 卖出规则：J值下穿超买线
        Rule exitRule = new CrossedDownIndicatorRule(j, overboughtThreshold);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建神奇震荡指标策略
     */
    private static Strategy createAwesomeOscillatorStrategy(BarSeries series) {
        int shortPeriod = (int) ( 5);
        int longPeriod = (int) ( 34);

        // 创建中间价指标 (high + low) / 2
        MedianPriceIndicator medianPrice = new MedianPriceIndicator(series);

        // 创建短期和长期SMA
        SMAIndicator shortSma = new SMAIndicator(medianPrice, shortPeriod);
        SMAIndicator longSma = new SMAIndicator(medianPrice, longPeriod);

        // 创建神奇震荡指标 (短期SMA - 长期SMA)
        class AwesomeOscillatorIndicator extends CachedIndicator<Num> {
            private final SMAIndicator shortSma;
            private final SMAIndicator longSma;

            public AwesomeOscillatorIndicator(SMAIndicator shortSma, SMAIndicator longSma, BarSeries series) {
                super(series);
                this.shortSma = shortSma;
                this.longSma = longSma;
            }

            @Override
            protected Num calculate(int index) {
                return shortSma.getValue(index).minus(longSma.getValue(index));
            }
        }

        AwesomeOscillatorIndicator ao = new AwesomeOscillatorIndicator(shortSma, longSma, series);

        // 创建零线指标
        ConstantIndicator<Num> zeroLine = new ConstantIndicator<>(series, series.numOf(0));

        // 买入规则：神奇震荡指标上穿零线
        Rule entryRule = new CrossedUpIndicatorRule(ao, zeroLine);

        // 卖出规则：神奇震荡指标下穿零线
        Rule exitRule = new CrossedDownIndicatorRule(ao, zeroLine);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建方向运动指标策略
     */
    private static Strategy createDMIStrategy(BarSeries series) {
        int period = (int) ( 14);
        int adxThreshold = (int) ( 20);

        // 创建ADX指标
        ADXIndicator adx = new ADXIndicator(series, period);

        // 自定义实现+DI和-DI指标
        class DirectionalMovementPlusIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final ATRIndicator atr;
            private final int period;

            public DirectionalMovementPlusIndicator(BarSeries series, int period) {
                super(series);
                this.highPrice = new HighPriceIndicator(series);
                this.atr = new ATRIndicator(series, period);
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < 1) {
                    return series.numOf(0);
                }

                // +DM = 如果(当日最高价-前日最高价) > (前日最低价-当日最低价)，取较大值，否则为0
                Num highDiff = highPrice.getValue(index).minus(highPrice.getValue(index - 1));
                Num lowDiff = new LowPriceIndicator(series).getValue(index - 1).minus(new LowPriceIndicator(series).getValue(index));

                Num plusDM = series.numOf(0);
                if (highDiff.isGreaterThan(series.numOf(0)) && highDiff.isGreaterThan(lowDiff)) {
                    plusDM = highDiff;
                }

                // +DI = 100 * EMA(+DM) / ATR
                return plusDM.multipliedBy(series.numOf(100)).dividedBy(atr.getValue(index));
            }
        }

        class DirectionalMovementMinusIndicator extends CachedIndicator<Num> {
            private final LowPriceIndicator lowPrice;
            private final ATRIndicator atr;
            private final int period;

            public DirectionalMovementMinusIndicator(BarSeries series, int period) {
                super(series);
                this.lowPrice = new LowPriceIndicator(series);
                this.atr = new ATRIndicator(series, period);
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < 1) {
                    return series.numOf(0);
                }

                // -DM = 如果(前日最低价-当日最低价) > (当日最高价-前日最高价)，取较大值，否则为0
                Num lowDiff = lowPrice.getValue(index - 1).minus(lowPrice.getValue(index));
                Num highDiff = new HighPriceIndicator(series).getValue(index).minus(new HighPriceIndicator(series).getValue(index - 1));

                Num minusDM = series.numOf(0);
                if (lowDiff.isGreaterThan(series.numOf(0)) && lowDiff.isGreaterThan(highDiff)) {
                    minusDM = lowDiff;
                }

                // -DI = 100 * EMA(-DM) / ATR
                return minusDM.multipliedBy(series.numOf(100)).dividedBy(atr.getValue(index));
            }
        }

        // 创建自定义的+DI和-DI指标
        DirectionalMovementPlusIndicator plusDI = new DirectionalMovementPlusIndicator(series, period);
        DirectionalMovementMinusIndicator minusDI = new DirectionalMovementMinusIndicator(series, period);

        // 创建阈值指标
        ConstantIndicator<Num> threshold = new ConstantIndicator<>(series, series.numOf(adxThreshold));

        // 买入规则：+DI上穿-DI且ADX大于阈值
        Rule entryRule = new CrossedUpIndicatorRule(plusDI, minusDI)
                .and(new OverIndicatorRule(adx, threshold));

        // 卖出规则：-DI上穿+DI且ADX大于阈值
        Rule exitRule = new CrossedUpIndicatorRule(minusDI, plusDI)
                .and(new OverIndicatorRule(adx, threshold));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建超级趋势指标策略
     */
    private static Strategy createSupertrendStrategy(BarSeries series) {
        int period = (int) ( 10);
        double multiplier = 3.0;

        // 创建ATR指标
        ATRIndicator atr = new ATRIndicator(series, period);

        // 创建中间价指标 (high + low) / 2
        MedianPriceIndicator medianPrice = new MedianPriceIndicator(series);

        // 创建上轨和下轨指标
        class UpperBandIndicator extends CachedIndicator<Num> {
            private final MedianPriceIndicator medianPrice;
            private final ATRIndicator atr;
            private final Num multiplier;

            public UpperBandIndicator(MedianPriceIndicator medianPrice, ATRIndicator atr, double multiplier, BarSeries series) {
                super(series);
                this.medianPrice = medianPrice;
                this.atr = atr;
                this.multiplier = series.numOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return medianPrice.getValue(index).plus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        class LowerBandIndicator extends CachedIndicator<Num> {
            private final MedianPriceIndicator medianPrice;
            private final ATRIndicator atr;
            private final Num multiplier;

            public LowerBandIndicator(MedianPriceIndicator medianPrice, ATRIndicator atr, double multiplier, BarSeries series) {
                super(series);
                this.medianPrice = medianPrice;
                this.atr = atr;
                this.multiplier = series.numOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return medianPrice.getValue(index).minus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        UpperBandIndicator upperBand = new UpperBandIndicator(medianPrice, atr, multiplier, series);
        LowerBandIndicator lowerBand = new LowerBandIndicator(medianPrice, atr, multiplier, series);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 买入规则：收盘价上穿上轨
        Rule entryRule = new CrossedUpIndicatorRule(closePrice, upperBand);

        // 卖出规则：收盘价下穿下轨
        Rule exitRule = new CrossedDownIndicatorRule(closePrice, lowerBand);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建一目均衡表云突破策略
     */
    private static Strategy createIchimokuCloudBreakoutStrategy(BarSeries series) {
        int conversionPeriod = (int) ( 9);
        int basePeriod = (int) ( 26);
        int spanPeriod = (int) ( 52);
        int displacement = (int) ( 26);

        // 创建一目均衡表指标
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 转换线 (Conversion Line, Tenkan-sen) = (n日高点 + n日低点) / 2，一般n取9
        class ConversionLineIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final LowPriceIndicator lowPrice;
            private final int period;
            private final Num two;

            public ConversionLineIndicator(HighPriceIndicator highPrice, LowPriceIndicator lowPrice, int period, BarSeries series) {
                super(series);
                this.highPrice = highPrice;
                this.lowPrice = lowPrice;
                this.period = period;
                this.two = series.numOf(2);
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return series.numOf(0);
                }

                Num highest = highPrice.getValue(index);
                Num lowest = lowPrice.getValue(index);

                for (int i = index - period + 1; i < index; i++) {
                    highest = highest.max(highPrice.getValue(i));
                    lowest = lowest.min(lowPrice.getValue(i));
                }

                return highest.plus(lowest).dividedBy(two);
            }
        }

        // 基准线 (Base Line, Kijun-sen) = (n日高点 + n日低点) / 2，一般n取26
        class BaseLineIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final LowPriceIndicator lowPrice;
            private final int period;
            private final Num two;

            public BaseLineIndicator(HighPriceIndicator highPrice, LowPriceIndicator lowPrice, int period, BarSeries series) {
                super(series);
                this.highPrice = highPrice;
                this.lowPrice = lowPrice;
                this.period = period;
                this.two = series.numOf(2);
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return series.numOf(0);
                }

                Num highest = highPrice.getValue(index);
                Num lowest = lowPrice.getValue(index);

                for (int i = index - period + 1; i < index; i++) {
                    highest = highest.max(highPrice.getValue(i));
                    lowest = lowest.min(lowPrice.getValue(i));
                }

                return highest.plus(lowest).dividedBy(two);
            }
        }

        ConversionLineIndicator conversionLine = new ConversionLineIndicator(highPrice, lowPrice, conversionPeriod, series);
        BaseLineIndicator baseLine = new BaseLineIndicator(highPrice, lowPrice, basePeriod, series);

        // 先行带1号 (Leading Span A, Senkou Span A) = (转换线 + 基准线) / 2，向前平移26日
        class LeadingSpanAIndicator extends CachedIndicator<Num> {
            private final ConversionLineIndicator conversionLine;
            private final BaseLineIndicator baseLine;
            private final int displacement;
            private final Num two;

            public LeadingSpanAIndicator(ConversionLineIndicator conversionLine, BaseLineIndicator baseLine, int displacement, BarSeries series) {
                super(series);
                this.conversionLine = conversionLine;
                this.baseLine = baseLine;
                this.displacement = displacement;
                this.two = series.numOf(2);
            }

            @Override
            protected Num calculate(int index) {
                if (index < 0) {
                    return series.numOf(0);
                }

                return conversionLine.getValue(index).plus(baseLine.getValue(index)).dividedBy(two);
            }
        }

        // 先行带2号 (Leading Span B, Senkou Span B) = (n日高点 + n日低点) / 2，一般n取52，向前平移26日
        class LeadingSpanBIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final LowPriceIndicator lowPrice;
            private final int period;
            private final int displacement;
            private final Num two;

            public LeadingSpanBIndicator(HighPriceIndicator highPrice, LowPriceIndicator lowPrice, int period, int displacement, BarSeries series) {
                super(series);
                this.highPrice = highPrice;
                this.lowPrice = lowPrice;
                this.period = period;
                this.displacement = displacement;
                this.two = series.numOf(2);
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return series.numOf(0);
                }

                Num highest = highPrice.getValue(index);
                Num lowest = lowPrice.getValue(index);

                for (int i = index - period + 1; i < index; i++) {
                    highest = highest.max(highPrice.getValue(i));
                    lowest = lowest.min(lowPrice.getValue(i));
                }

                return highest.plus(lowest).dividedBy(two);
            }
        }

        LeadingSpanAIndicator leadingSpanA = new LeadingSpanAIndicator(conversionLine, baseLine, displacement, series);
        LeadingSpanBIndicator leadingSpanB = new LeadingSpanBIndicator(highPrice, lowPrice, spanPeriod, displacement, series);

        // 买入规则：收盘价上穿云带上轨(LeadingSpanA)，即价格突破云带
        Rule entryRule = new CrossedUpIndicatorRule(closePrice, leadingSpanA);

        // 卖出规则：收盘价下穿云带下轨(LeadingSpanB)，即价格跌破云带
        Rule exitRule = new CrossedDownIndicatorRule(closePrice, leadingSpanB);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建三角移动平均线策略
     * 三角移动平均线是一种平滑的移动平均线，它对价格变化的反应比简单移动平均线更平滑
     */
    private static Strategy createTrimaStrategy(BarSeries series) {
        int shortPeriod = (int) ( 9);
        int longPeriod = (int) ( 21);

        if (series.getBarCount() <= longPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (longPeriod + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建短期和长期三角移动平均线指标
        TriangularMovingAverageIndicator shortTrima = new TriangularMovingAverageIndicator(closePrice, shortPeriod);
        TriangularMovingAverageIndicator longTrima = new TriangularMovingAverageIndicator(closePrice, longPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(shortTrima, longTrima);
        Rule exitRule = new CrossedDownIndicatorRule(shortTrima, longTrima);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建T3移动平均线策略
     * T3是一种三重指数平滑移动平均线，提供更平滑的价格曲线
     */
    private static Strategy createT3Strategy(BarSeries series) {
        int period = (int) ( 10);
        double volumeFactor = (double) ( 0.7); // 体积因子，一般在0.5-0.9之间

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (period + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建T3指标
        T3Indicator t3 = new T3Indicator(closePrice, period, series.numOf(volumeFactor));
        SMAIndicator sma = new SMAIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(t3, sma);
        Rule exitRule = new CrossedDownIndicatorRule(t3, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建MESA自适应移动平均线策略
     * MAMA是一种自适应移动平均线，能够根据市场条件自动调整
     */
    private static Strategy createMamaStrategy(BarSeries series) {
        double fastLimit = (double) ( 0.5);
        double slowLimit = (double) ( 0.05);

        if (series.getBarCount() <= 30) { // MAMA需要足够的数据点
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 31 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建MAMA和FAMA指标
        MESAAdaptiveMovingAverageIndicator mama = new MESAAdaptiveMovingAverageIndicator(closePrice, series.numOf(fastLimit), series.numOf(slowLimit));
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(mama, sma);
        Rule exitRule = new CrossedDownIndicatorRule(mama, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建可变指数动态平均线策略
     * VIDYA是一种基于波动率的移动平均线，在波动较大时反应更快
     */
    private static Strategy createVidyaStrategy(BarSeries series) {
        int shortCMAPeriod = (int) ( 9);
        int longCMAPeriod = (int) ( 12);
        double alpha = (double) ( 0.2);

        if (series.getBarCount() <= longCMAPeriod) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (longCMAPeriod + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建VIDYA指标
        VIDYAIndicator vidya = new VIDYAIndicator(closePrice, shortCMAPeriod, longCMAPeriod, series.numOf(alpha));
        SMAIndicator sma = new SMAIndicator(closePrice, longCMAPeriod);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(vidya, sma);
        Rule exitRule = new CrossedDownIndicatorRule(vidya, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建威尔德平滑移动平均线策略
     * 威尔德平滑是一种特殊的指数移动平均线，用于计算RSI等指标
     */
    private static Strategy createWildersStrategy(BarSeries series) {
        int period = (int) ( 14);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (period + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 创建威尔德平滑指标
        WilderIndicator wilders = new WilderIndicator(closePrice, period);
        SMAIndicator sma = new SMAIndicator(closePrice, period);

        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(wilders, sma);
        Rule exitRule = new CrossedDownIndicatorRule(wilders, sma);

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建Fisher变换策略
     * Fisher变换是一种将价格转换为正态分布的指标，有助于识别超买超卖状态
     */
    private static Strategy createFisherStrategy(BarSeries series) {
        int period = (int) ( 10);

        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (period + 1) + " 个数据点");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 创建自定义Fisher变换指标
        class FisherTransformIndicator extends CachedIndicator<Num> {
            private final Indicator<Num> indicator;
            private final int period;
            private final Num one;
            private final Num half;
            
            public FisherTransformIndicator(Indicator<Num> indicator, int period, BarSeries series) {
                super(indicator);
                this.indicator = indicator;
                this.period = period;
                this.one = series.numOf(1);
                this.half = series.numOf(0.5);
            }
            
            @Override
            protected Num calculate(int index) {
                if (index < period) {
                    return series.numOf(0);
                }
                
                // 找出period内的最高价和最低价
                Num highest = indicator.getValue(index);
                Num lowest = indicator.getValue(index);
                
                for (int i = index - period + 1; i < index; i++) {
                    Num val = indicator.getValue(i);
                    highest = highest.max(val);
                    lowest = lowest.min(val);
                }
                
                // 如果最高价等于最低价，返回0
                if (highest.equals(lowest)) {
                    return series.numOf(0);
                }
                
                // 归一化价格到-1到1之间
                Num range = highest.minus(lowest);
                Num normalizedPrice = indicator.getValue(index).minus(lowest).dividedBy(range).multipliedBy(series.numOf(2)).minus(one);
                
                // 应用Fisher变换
                Num value = series.numOf(0);
                if (index > 0) {
                    Num prevValue = getValue(index - 1);
                    value = half.multipliedBy(series.numOf(Math.log((one.plus(normalizedPrice)).dividedBy(one.minus(normalizedPrice)).doubleValue())
                            .plus(prevValue.doubleValue()));
                }
                
                return value;
            }
        }
        
        // 创建Fisher变换指标
        FisherTransformIndicator fisher = new FisherTransformIndicator(closePrice, period, series);
        
        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(fisher, series.numOf(0));
        Rule exitRule = new CrossedDownIndicatorRule(fisher, series.numOf(0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建预测振荡器策略
     * 预测振荡器衡量当前价格与线性回归预测价格的偏差
     */
    private static Strategy createFoscStrategy(BarSeries series) {
        int period = (int) ( 14);
        
        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (period + 1) + " 个数据点");
        }
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 创建自定义预测振荡器指标
        class ForecastOscillatorIndicator extends CachedIndicator<Num> {
            private final Indicator<Num> indicator;
            private final int period;
            private final Num hundred;
            
            public ForecastOscillatorIndicator(Indicator<Num> indicator, int period, BarSeries series) {
                super(indicator);
                this.indicator = indicator;
                this.period = period;
                this.hundred = series.numOf(100);
            }
            
            @Override
            protected Num calculate(int index) {
                if (index < period) {
                    return series.numOf(0);
                }
                
                // 计算线性回归预测值
                double sumX = 0;
                double sumY = 0;
                double sumXY = 0;
                double sumX2 = 0;
                
                for (int i = index - period + 1; i <= index; i++) {
                    double x = i - (index - period + 1);
                    double y = indicator.getValue(i).doubleValue();
                    sumX += x;
                    sumY += y;
                    sumXY += x * y;
                    sumX2 += x * x;
                }
                
                double meanX = sumX / period;
                double meanY = sumY / period;
                
                double slope = (sumXY - sumX * meanY) / (sumX2 - sumX * meanX);
                double intercept = meanY - slope * meanX;
                
                // 预测下一个值
                double forecast = slope * period + intercept;
                
                // 计算振荡器值
                double currentPrice = indicator.getValue(index).doubleValue();
                double oscillator = ((currentPrice - forecast) / forecast) * 100;
                
                return series.numOf(oscillator);
            }
        }
        
        // 创建预测振荡器指标
        ForecastOscillatorIndicator fosc = new ForecastOscillatorIndicator(closePrice, period, series);
        
        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(fosc, series.numOf(0));
        Rule exitRule = new CrossedDownIndicatorRule(fosc, series.numOf(0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建移动便利性指标策略
     * 移动便利性指标衡量价格变动的难易程度
     */
    private static Strategy createEomStrategy(BarSeries series) {
        int period = (int) ( 14);
        double divisor = (double) ( 100000000);
        
        if (series.getBarCount() <= period) {
            throw new IllegalArgumentException("数据点不足以计算指标: 至少需要 " + (period + 1) + " 个数据点");
        }
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 创建自定义移动便利性指标
        class EaseOfMovementIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final LowPriceIndicator lowPrice;
            private final VolumeIndicator volume;
            private final int period;
            private final Num divisor;
            
            public EaseOfMovementIndicator(BarSeries series, int period, double divisor) {
                super(series);
                this.highPrice = new HighPriceIndicator(series);
                this.lowPrice = new LowPriceIndicator(series);
                this.volume = new VolumeIndicator(series);
                this.period = period;
                this.divisor = series.numOf(divisor);
            }
            
            @Override
            protected Num calculate(int index) {
                if (index < 1) {
                    return series.numOf(0);
                }
                
                // 计算当前和前一个K线的中点价格
                Num currentMiddlePoint = highPrice.getValue(index).plus(lowPrice.getValue(index)).dividedBy(series.numOf(2));
                Num prevMiddlePoint = highPrice.getValue(index - 1).plus(lowPrice.getValue(index - 1)).dividedBy(series.numOf(2));
                
                // 计算价格变动
                Num priceChange = currentMiddlePoint.minus(prevMiddlePoint);
                
                // 计算当前K线的高低价差
                Num boxRatio = highPrice.getValue(index).minus(lowPrice.getValue(index));
                
                // 避免除以零
                if (boxRatio.isZero() || volume.getValue(index).isZero()) {
                    return series.numOf(0);
                }
                
                // 计算单日移动便利性
                Num dailyEom = priceChange.multipliedBy(divisor).dividedBy(volume.getValue(index).dividedBy(boxRatio));
                
                // 如果需要计算移动平均
                if (period > 1 && index >= period) {
                    Num sum = series.numOf(0);
                    for (int i = index - period + 1; i <= index; i++) {
                        Num mp = highPrice.getValue(i).plus(lowPrice.getValue(i)).dividedBy(series.numOf(2));
                        Num prevMp = highPrice.getValue(i - 1).plus(lowPrice.getValue(i - 1)).dividedBy(series.numOf(2));
                        Num pc = mp.minus(prevMp);
                        Num br = highPrice.getValue(i).minus(lowPrice.getValue(i));
                        
                        if (!br.isZero() && !volume.getValue(i).isZero()) {
                            sum = sum.plus(pc.multipliedBy(divisor).dividedBy(volume.getValue(i).dividedBy(br)));
                        }
                    }
                    return sum.dividedBy(series.numOf(period));
                }
                
                return dailyEom;
            }
        }
        
        // 创建移动便利性指标
        EaseOfMovementIndicator eom = new EaseOfMovementIndicator(series, period, divisor);
        
        // 创建规则
        Rule entryRule = new CrossedUpIndicatorRule(eom, series.numOf(0));
        Rule exitRule = new CrossedDownIndicatorRule(eom, series.numOf(0));
        
        return new BaseStrategy(entryRule, exitRule);
    }
}
