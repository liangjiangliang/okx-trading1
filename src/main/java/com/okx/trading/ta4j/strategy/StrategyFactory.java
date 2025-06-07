package com.okx.trading.ta4j.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.*;
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
import org.ta4j.core.indicators.keltner.KeltnerChannelLowerIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelUpperIndicator;
import org.ta4j.core.indicators.statistics.CovarianceIndicator;
import org.ta4j.core.indicators.statistics.PearsonCorrelationIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.statistics.StandardErrorIndicator;
import org.ta4j.core.indicators.statistics.VarianceIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;

/**
 * 策略工厂类
 * 用于创建和管理各种交易策略
 */
public class StrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(StrategyFactory.class);

    // 策略类型常量
    // 移动平均线策略
    public static final String STRATEGY_SMA = "SMA";
    public static final String STRATEGY_EMA = "EMA";
    public static final String STRATEGY_TRIPLE_EMA = "TRIPLE_EMA";
    public static final String STRATEGY_WMA = "WMA";
    public static final String STRATEGY_HMA = "HMA";
    public static final String STRATEGY_KAMA = "KAMA";
    public static final String STRATEGY_ZLEMA = "ZLEMA";
    public static final String STRATEGY_DEMA = "DEMA";
    public static final String STRATEGY_TEMA = "TEMA";
    public static final String STRATEGY_VWAP = "VWAP";

    // 震荡指标策略
    public static final String STRATEGY_RSI = "RSI";
    public static final String STRATEGY_STOCHASTIC = "STOCHASTIC";
    public static final String STRATEGY_STOCHASTIC_RSI = "STOCHASTIC_RSI";
    public static final String STRATEGY_WILLIAMS_R = "WILLIAMS_R";
    public static final String STRATEGY_CCI = "CCI";
    public static final String STRATEGY_CMO = "CMO";
    public static final String STRATEGY_ROC = "ROC";
    public static final String STRATEGY_MACD = "MACD";
    public static final String STRATEGY_PPO = "PPO";
    public static final String STRATEGY_DPO = "DPO";
    public static final String STRATEGY_TRIX = "TRIX";
    public static final String STRATEGY_AWESOME_OSCILLATOR = "AWESOME_OSCILLATOR";

    // 趋势指标策略
    public static final String STRATEGY_ADX = "ADX";
    public static final String STRATEGY_AROON = "AROON";
    public static final String STRATEGY_ICHIMOKU = "ICHIMOKU";
    public static final String STRATEGY_ICHIMOKU_CLOUD_BREAKOUT = "ICHIMOKU_CLOUD_BREAKOUT";
    public static final String STRATEGY_PARABOLIC_SAR = "PARABOLIC_SAR";
    public static final String STRATEGY_DMA = "DMA";
    public static final String STRATEGY_DMI = "DMI";
    public static final String STRATEGY_SUPERTREND = "SUPERTREND";

    // 波动指标策略
    public static final String STRATEGY_BOLLINGER_BANDS = "BOLLINGER";
    public static final String STRATEGY_KELTNER_CHANNEL = "KELTNER";
    public static final String STRATEGY_CHANDELIER_EXIT = "CHANDELIER_EXIT";
    public static final String STRATEGY_ULCER_INDEX = "ULCER_INDEX";
    public static final String STRATEGY_ATR = "ATR";
    public static final String STRATEGY_KDJ = "KDJ";

    // 成交量指标策略
    public static final String STRATEGY_OBV = "OBV";
    public static final String STRATEGY_MASS_INDEX = "MASS_INDEX";

    // 蜡烛图形态策略
    public static final String STRATEGY_DOJI = "DOJI";
    public static final String STRATEGY_BULLISH_ENGULFING = "BULLISH_ENGULFING";
    public static final String STRATEGY_BEARISH_ENGULFING = "BEARISH_ENGULFING";
    public static final String STRATEGY_BULLISH_HARAMI = "BULLISH_HARAMI";
    public static final String STRATEGY_BEARISH_HARAMI = "BEARISH_HARAMI";
    public static final String STRATEGY_THREE_WHITE_SOLDIERS = "THREE_WHITE_SOLDIERS";
    public static final String STRATEGY_THREE_BLACK_CROWS = "THREE_BLACK_CROWS";

    // 组合策略
    public static final String STRATEGY_DUAL_THRUST = "DUAL_THRUST";
    public static final String STRATEGY_TURTLE_TRADING = "TURTLE_TRADING";
    public static final String STRATEGY_MEAN_REVERSION = "MEAN_REVERSION";
    public static final String STRATEGY_TREND_FOLLOWING = "TREND_FOLLOWING";
    public static final String STRATEGY_BREAKOUT = "BREAKOUT";
    public static final String STRATEGY_GOLDEN_CROSS = "GOLDEN_CROSS";
    public static final String STRATEGY_DEATH_CROSS = "DEATH_CROSS";
    public static final String STRATEGY_DUAL_MA_WITH_RSI = "DUAL_MA_WITH_RSI";
    public static final String STRATEGY_MACD_WITH_BOLLINGER = "MACD_WITH_BOLLINGER";

    // 策略参数说明
    // 移动平均线策略参数
    public static final String SMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String EMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String TRIPLE_EMA_PARAMS_DESC = "短期EMA,中期EMA,长期EMA (例如：5,10,20)";
    public static final String WMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String HMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String KAMA_PARAMS_DESC = "周期,快速EMA周期,慢速EMA周期 (例如：10,2,30)";
    public static final String ZLEMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String DEMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String TEMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String VWAP_PARAMS_DESC = "周期 (例如：14)";

    // 震荡指标策略参数
    public static final String RSI_PARAMS_DESC = "RSI周期,超卖阈值,超买阈值 (例如：14,30,70)";
    public static final String STOCHASTIC_PARAMS_DESC = "K周期,%K平滑周期,%D平滑周期,超卖阈值,超买阈值 (例如：14,3,3,20,80)";
    public static final String STOCHASTIC_RSI_PARAMS_DESC = "RSI周期,随机指标周期,K平滑周期,D平滑周期,超卖阈值,超买阈值 (例如：14,14,3,3,20,80)";
    public static final String WILLIAMS_R_PARAMS_DESC = "周期,超卖阈值,超买阈值 (例如：14,-80,-20)";
    public static final String CCI_PARAMS_DESC = "CCI周期,超卖阈值,超买阈值 (例如：20,-100,100)";
    public static final String CMO_PARAMS_DESC = "周期,超卖阈值,超买阈值 (例如：14,-30,30)";
    public static final String ROC_PARAMS_DESC = "周期,阈值 (例如：12,0)";
    public static final String MACD_PARAMS_DESC = "短周期,长周期,信号周期 (例如：12,26,9)";
    public static final String PPO_PARAMS_DESC = "短周期,长周期,信号周期 (例如：12,26,9)";
    public static final String DPO_PARAMS_DESC = "周期 (例如：20)";
    public static final String TRIX_PARAMS_DESC = "TRIX周期,信号周期 (例如：15,9)";
    public static final String AWESOME_OSCILLATOR_PARAMS_DESC = "短周期,长周期 (例如：5,34)";

    // 趋势指标策略参数
    public static final String ADX_PARAMS_DESC = "ADX周期,DI周期,阈值 (例如：14,14,25)";
    public static final String AROON_PARAMS_DESC = "周期,阈值 (例如：25,70)";
    public static final String ICHIMOKU_PARAMS_DESC = "转换线周期,基准线周期,延迟跨度 (例如：9,26,52)";
    public static final String ICHIMOKU_CLOUD_BREAKOUT_PARAMS_DESC = "转换线周期,基准线周期,延迟跨度 (例如：9,26,52)";
    public static final String PARABOLIC_SAR_PARAMS_DESC = "步长,最大步长 (例如：0.02,0.2)";
    public static final String DMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：10,50)";
    public static final String DMI_PARAMS_DESC = "周期,ADX阈值 (例如：14,20)";
    public static final String SUPERTREND_PARAMS_DESC = "ATR周期,乘数 (例如：10,3.0)";

    // 波动指标策略参数
    public static final String BOLLINGER_PARAMS_DESC = "周期,标准差倍数 (例如：20,2.0)";
    public static final String KELTNER_CHANNEL_PARAMS_DESC = "EMA周期,ATR周期,乘数 (例如：20,10,2.0)";
    public static final String CHANDELIER_EXIT_PARAMS_DESC = "周期,乘数 (例如：22,3.0)";
    public static final String ULCER_INDEX_PARAMS_DESC = "周期,阈值 (例如：14,5.0)";
    public static final String ATR_PARAMS_DESC = "周期,乘数 (例如：14,2.0)";
    public static final String KDJ_PARAMS_DESC = "K周期,D周期,J周期,超卖阈值,超买阈值 (例如：9,3,3,20,80)";

    // 成交量指标策略参数
    public static final String OBV_PARAMS_DESC = "短期OBV周期,长期OBV周期 (例如：5,20)";
    public static final String MASS_INDEX_PARAMS_DESC = "EMA周期,累积周期,阈值 (例如：9,25,27)";

    // 蜡烛图形态策略参数
    public static final String DOJI_PARAMS_DESC = "影线比例阈值 (例如：0.1)";
    public static final String BULLISH_ENGULFING_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String BEARISH_ENGULFING_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String BULLISH_HARAMI_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String BEARISH_HARAMI_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String THREE_WHITE_SOLDIERS_PARAMS_DESC = "确认周期 (例如：3)";
    public static final String THREE_BLACK_CROWS_PARAMS_DESC = "确认周期 (例如：3)";

    // 组合策略参数
    public static final String DUAL_THRUST_PARAMS_DESC = "周期,K1,K2 (例如：14,0.5,0.5)";
    public static final String TURTLE_TRADING_PARAMS_DESC = "入场周期,出场周期,ATR周期,ATR乘数 (例如：20,10,14,2.0)";
    public static final String MEAN_REVERSION_PARAMS_DESC = "均线周期,标准差倍数 (例如：20,2.0)";
    public static final String TREND_FOLLOWING_PARAMS_DESC = "短期均线周期,长期均线周期,ADX周期,ADX阈值 (例如：5,20,14,25)";
    public static final String BREAKOUT_PARAMS_DESC = "突破周期,确认周期 (例如：20,3)";
    public static final String GOLDEN_CROSS_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：50,200)";
    public static final String DEATH_CROSS_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：50,200)";
    public static final String DUAL_MA_WITH_RSI_PARAMS_DESC = "短期均线周期,长期均线周期,RSI周期,RSI阈值 (例如：5,20,14,50)";
    public static final String MACD_WITH_BOLLINGER_PARAMS_DESC = "MACD短周期,MACD长周期,MACD信号周期,布林带周期,布林带标准差倍数 (例如：12,26,9,20,2.0)";

    // 策略创建函数映射
    private static final Map<String, BiFunction<BarSeries, Map<String, Object>, Strategy>> strategyCreators = new HashMap<>();

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

        // 波动指标策略
        strategyCreators.put(STRATEGY_BOLLINGER_BANDS, StrategyFactory::createBollingerBandsStrategy);
        strategyCreators.put(STRATEGY_CHANDELIER_EXIT, StrategyFactory::createChandelierExitStrategy);
        strategyCreators.put(STRATEGY_ULCER_INDEX, StrategyFactory::createUlcerIndexStrategy);

        // 成交量指标策略
        strategyCreators.put(STRATEGY_OBV, StrategyFactory::createOBVStrategy);
        strategyCreators.put(STRATEGY_MASS_INDEX, StrategyFactory::createMassIndexStrategy);

        // 蜡烛图形态策略
        strategyCreators.put(STRATEGY_DOJI, StrategyFactory::createDojiStrategy);
        strategyCreators.put(STRATEGY_BULLISH_ENGULFING, StrategyFactory::createBullishEngulfingStrategy);
        strategyCreators.put(STRATEGY_BEARISH_ENGULFING, StrategyFactory::createBearishEngulfingStrategy);
        strategyCreators.put(STRATEGY_BULLISH_HARAMI, StrategyFactory::createBullishHaramiStrategy);
        strategyCreators.put(STRATEGY_BEARISH_HARAMI, StrategyFactory::createBearishHaramiStrategy);
        strategyCreators.put(STRATEGY_THREE_WHITE_SOLDIERS, StrategyFactory::createThreeWhiteSoldiersStrategy);
        strategyCreators.put(STRATEGY_THREE_BLACK_CROWS, StrategyFactory::createThreeBlackCrowsStrategy);

        // 组合策略
        strategyCreators.put(STRATEGY_TURTLE_TRADING, StrategyFactory::createTurtleTradingStrategy);
        strategyCreators.put(STRATEGY_TREND_FOLLOWING, StrategyFactory::createTrendFollowingStrategy);
        strategyCreators.put(STRATEGY_BREAKOUT, StrategyFactory::createBreakoutStrategy);
        strategyCreators.put(STRATEGY_GOLDEN_CROSS, StrategyFactory::createGoldenCrossStrategy);
        strategyCreators.put(STRATEGY_DEATH_CROSS, StrategyFactory::createDeathCrossStrategy);
        strategyCreators.put(STRATEGY_DUAL_MA_WITH_RSI, StrategyFactory::createDualMAWithRSIStrategy);
    }

    /**
     * 创建策略
     *
     * @param series       BarSeries对象
     * @param strategyType 策略类型
     * @param params       策略参数
     * @return 策略对象
     */
    public static Strategy createStrategy(BarSeries series, String strategyType, Map<String, Object> params) {
        BiFunction<BarSeries, Map<String, Object>, Strategy> strategyCreator = strategyCreators.get(strategyType);

        if (strategyCreator == null) {
            throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }

        if (series == null || series.getBarCount() == 0) {
            throw new IllegalArgumentException("K线数据不能为空");
        }

        return strategyCreator.apply(series, params);
    }

    /**
     * 将字符串参数转换为参数映射
     *
     * @param strategyType 策略类型
     * @param paramString  参数字符串 (以逗号分隔)
     * @return 参数映射
     */
    public static Map<String, Object> parseParams(String strategyType, String paramString) {
        if (paramString == null || paramString.trim().isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }

        String[] params = paramString.split(",");
        Map<String, Object> paramMap = new HashMap<>();

        switch (strategyType) {
            // 移动平均线策略
            case STRATEGY_SMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("SMA策略需要至少2个参数: " + SMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_EMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("EMA策略需要至少2个参数: " + EMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_TRIPLE_EMA:
                if (params.length < 3) {
                    throw new IllegalArgumentException("三重EMA策略需要至少3个参数: " + TRIPLE_EMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("middlePeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_WMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("WMA策略需要至少2个参数: " + WMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_HMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("HMA策略需要至少2个参数: " + HMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_KAMA:
                if (params.length < 3) {
                    throw new IllegalArgumentException("KAMA策略需要至少3个参数: " + KAMA_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("fastEMA", Integer.parseInt(params[1].trim()));
                paramMap.put("slowEMA", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_ZLEMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("ZLEMA策略需要至少2个参数: " + ZLEMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_DEMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("DEMA策略需要至少2个参数: " + DEMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_TEMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("TEMA策略需要至少2个参数: " + TEMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_VWAP:
                if (params.length < 1) {
                    throw new IllegalArgumentException("VWAP策略需要至少1个参数: " + VWAP_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                break;

            // 震荡指标策略
            case STRATEGY_RSI:
                if (params.length < 3) {
                    throw new IllegalArgumentException("RSI策略需要至少3个参数: " + RSI_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("oversold", Integer.parseInt(params[1].trim()));
                paramMap.put("overbought", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_STOCHASTIC:
                if (params.length < 5) {
                    throw new IllegalArgumentException("随机指标策略需要至少5个参数: " + STOCHASTIC_PARAMS_DESC);
                }
                paramMap.put("kPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("kSmooth", Integer.parseInt(params[1].trim()));
                paramMap.put("dSmooth", Integer.parseInt(params[2].trim()));
                paramMap.put("oversold", Integer.parseInt(params[3].trim()));
                paramMap.put("overbought", Integer.parseInt(params[4].trim()));
                break;

            case STRATEGY_STOCHASTIC_RSI:
                if (params.length < 6) {
                    throw new IllegalArgumentException("随机RSI策略需要至少6个参数: " + STOCHASTIC_RSI_PARAMS_DESC);
                }
                paramMap.put("rsiPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("stochasticPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("kSmooth", Integer.parseInt(params[2].trim()));
                paramMap.put("dSmooth", Integer.parseInt(params[3].trim()));
                paramMap.put("oversold", Integer.parseInt(params[4].trim()));
                paramMap.put("overbought", Integer.parseInt(params[5].trim()));
                break;

            case STRATEGY_WILLIAMS_R:
                if (params.length < 3) {
                    throw new IllegalArgumentException("威廉指标策略需要至少3个参数: " + WILLIAMS_R_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("oversold", Integer.parseInt(params[1].trim()));
                paramMap.put("overbought", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_CCI:
                if (params.length < 3) {
                    throw new IllegalArgumentException("CCI策略需要至少3个参数: " + CCI_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("oversold", Integer.parseInt(params[1].trim()));
                paramMap.put("overbought", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_CMO:
                if (params.length < 3) {
                    throw new IllegalArgumentException("CMO策略需要至少3个参数: " + CMO_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("oversold", Integer.parseInt(params[1].trim()));
                paramMap.put("overbought", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_ROC:
                if (params.length < 2) {
                    throw new IllegalArgumentException("ROC策略需要至少2个参数: " + ROC_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("threshold", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_MACD:
                if (params.length < 3) {
                    throw new IllegalArgumentException("MACD策略需要至少3个参数: " + MACD_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("signalPeriod", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_PPO:
                if (params.length < 3) {
                    throw new IllegalArgumentException("PPO策略需要至少3个参数: " + PPO_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("signalPeriod", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_DPO:
                if (params.length < 1) {
                    throw new IllegalArgumentException("DPO策略需要至少1个参数: " + DPO_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                break;

            // 趋势指标策略
            case STRATEGY_ADX:
                if (params.length < 3) {
                    throw new IllegalArgumentException("ADX策略需要至少3个参数: " + ADX_PARAMS_DESC);
                }
                paramMap.put("adxPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("diPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("threshold", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_AROON:
                if (params.length < 2) {
                    throw new IllegalArgumentException("Aroon策略需要至少2个参数: " + AROON_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("threshold", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_ICHIMOKU:
                if (params.length < 3) {
                    throw new IllegalArgumentException("一目均衡表策略需要至少3个参数: " + ICHIMOKU_PARAMS_DESC);
                }
                paramMap.put("conversionPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("basePeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("laggingSpan", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_PARABOLIC_SAR:
                if (params.length < 2) {
                    throw new IllegalArgumentException("抛物线SAR策略需要至少2个参数: " + PARABOLIC_SAR_PARAMS_DESC);
                }
                paramMap.put("step", Double.parseDouble(params[0].trim()));
                paramMap.put("max", Double.parseDouble(params[1].trim()));
                break;

            case STRATEGY_DMA:
                if (params.length < 2) {
                    throw new IllegalArgumentException("DMA策略需要至少2个参数: " + DMA_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            // 波动指标策略
            case STRATEGY_BOLLINGER_BANDS:
                if (params.length < 2) {
                    throw new IllegalArgumentException("布林带策略需要至少2个参数: " + BOLLINGER_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("deviation", Double.parseDouble(params[1].trim()));
                break;

            case STRATEGY_KELTNER_CHANNEL:
                if (params.length < 3) {
                    throw new IllegalArgumentException("肯特纳通道策略需要至少3个参数: " + KELTNER_CHANNEL_PARAMS_DESC);
                }
                paramMap.put("emaPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("atrPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("multiplier", Double.parseDouble(params[2].trim()));
                break;

            case STRATEGY_CHANDELIER_EXIT:
                if (params.length < 2) {
                    throw new IllegalArgumentException("吊灯线退出策略需要至少2个参数: " + CHANDELIER_EXIT_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("multiplier", Double.parseDouble(params[1].trim()));
                break;

            case STRATEGY_ULCER_INDEX:
                if (params.length < 2) {
                    throw new IllegalArgumentException("溃疡指数策略需要至少2个参数: " + ULCER_INDEX_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("threshold", Double.parseDouble(params[1].trim()));
                break;

            // 成交量指标策略
            case STRATEGY_OBV:
                if (params.length < 2) {
                    throw new IllegalArgumentException("OBV策略需要至少2个参数: " + OBV_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_MASS_INDEX:
                if (params.length < 3) {
                    throw new IllegalArgumentException("质量指数策略需要至少3个参数: " + MASS_INDEX_PARAMS_DESC);
                }
                paramMap.put("emaPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("sumPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("threshold", Integer.parseInt(params[2].trim()));
                break;

            // 蜡烛图形态策略
            case STRATEGY_DOJI:
                if (params.length < 1) {
                    throw new IllegalArgumentException("十字星策略需要至少1个参数: " + DOJI_PARAMS_DESC);
                }
                paramMap.put("shadowRatio", Double.parseDouble(params[0].trim()));
                break;

            case STRATEGY_BULLISH_ENGULFING:
            case STRATEGY_BEARISH_ENGULFING:
            case STRATEGY_BULLISH_HARAMI:
            case STRATEGY_BEARISH_HARAMI:
                if (params.length < 1) {
                    throw new IllegalArgumentException("吞没形态策略需要至少1个参数: 确认周期");
                }
                paramMap.put("confirmationPeriod", Integer.parseInt(params[0].trim()));
                break;

            case STRATEGY_THREE_WHITE_SOLDIERS:
            case STRATEGY_THREE_BLACK_CROWS:
                if (params.length < 1) {
                    throw new IllegalArgumentException("三兵/三乌鸦策略需要至少1个参数: 确认周期");
                }
                paramMap.put("confirmationPeriod", Integer.parseInt(params[0].trim()));
                break;

            // 组合策略
            case STRATEGY_DUAL_THRUST:
                if (params.length < 3) {
                    throw new IllegalArgumentException("双推策略需要至少3个参数: " + DUAL_THRUST_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("k1", Double.parseDouble(params[1].trim()));
                paramMap.put("k2", Double.parseDouble(params[2].trim()));
                break;

            case STRATEGY_TURTLE_TRADING:
                if (params.length < 4) {
                    throw new IllegalArgumentException("海龟交易策略需要至少4个参数: " + TURTLE_TRADING_PARAMS_DESC);
                }
                paramMap.put("entryPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("exitPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("atrPeriod", Integer.parseInt(params[2].trim()));
                paramMap.put("atrMultiplier", Double.parseDouble(params[3].trim()));
                break;

            case STRATEGY_MEAN_REVERSION:
                if (params.length < 2) {
                    throw new IllegalArgumentException("均值回归策略需要至少2个参数: " + MEAN_REVERSION_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("deviation", Double.parseDouble(params[1].trim()));
                break;

            case STRATEGY_TREND_FOLLOWING:
                if (params.length < 4) {
                    throw new IllegalArgumentException("趋势跟踪策略需要至少4个参数: " + TREND_FOLLOWING_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("adxPeriod", Integer.parseInt(params[2].trim()));
                paramMap.put("adxThreshold", Integer.parseInt(params[3].trim()));
                break;

            case STRATEGY_BREAKOUT:
                if (params.length < 2) {
                    throw new IllegalArgumentException("突破策略需要至少2个参数: " + BREAKOUT_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("confirmationPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_GOLDEN_CROSS:
            case STRATEGY_DEATH_CROSS:
                if (params.length < 2) {
                    throw new IllegalArgumentException("金叉/死叉策略需要至少2个参数: 短期均线周期,长期均线周期");
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_DUAL_MA_WITH_RSI:
                if (params.length < 4) {
                    throw new IllegalArgumentException("双均线RSI策略需要至少4个参数: " + DUAL_MA_WITH_RSI_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("rsiPeriod", Integer.parseInt(params[2].trim()));
                paramMap.put("rsiThreshold", Integer.parseInt(params[3].trim()));
                break;

            case STRATEGY_MACD_WITH_BOLLINGER:
                if (params.length < 5) {
                    throw new IllegalArgumentException("MACD与布林带组合策略需要至少5个参数: " + MACD_WITH_BOLLINGER_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("signalPeriod", Integer.parseInt(params[2].trim()));
                paramMap.put("bollingerPeriod", Integer.parseInt(params[3].trim()));
                paramMap.put("bollingerDeviation", Double.parseDouble(params[4].trim()));
                break;

            case STRATEGY_TRIX:
                if (params.length < 2) {
                    throw new IllegalArgumentException("TRIX策略需要至少2个参数: " + TRIX_PARAMS_DESC);
                }
                paramMap.put("trixPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("signalPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_AWESOME_OSCILLATOR:
                if (params.length < 2) {
                    throw new IllegalArgumentException("Awesome Oscillator策略需要至少2个参数: " + AWESOME_OSCILLATOR_PARAMS_DESC);
                }
                paramMap.put("shortPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("longPeriod", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_ICHIMOKU_CLOUD_BREAKOUT:
                if (params.length < 3) {
                    throw new IllegalArgumentException("一目均衡表云突破策略需要至少3个参数: " + ICHIMOKU_CLOUD_BREAKOUT_PARAMS_DESC);
                }
                paramMap.put("conversionPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("basePeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("laggingSpan", Integer.parseInt(params[2].trim()));
                break;

            case STRATEGY_DMI:
                if (params.length < 2) {
                    throw new IllegalArgumentException("DMI策略需要至少2个参数: " + DMI_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("adxThreshold", Integer.parseInt(params[1].trim()));
                break;

            case STRATEGY_SUPERTREND:
                if (params.length < 2) {
                    throw new IllegalArgumentException("SuperTrend策略需要至少2个参数: " + SUPERTREND_PARAMS_DESC);
                }
                paramMap.put("atrPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("multiplier", Double.parseDouble(params[1].trim()));
                break;

            case STRATEGY_ATR:
                if (params.length < 2) {
                    throw new IllegalArgumentException("ATR策略需要至少2个参数: " + ATR_PARAMS_DESC);
                }
                paramMap.put("period", Integer.parseInt(params[0].trim()));
                paramMap.put("multiplier", Double.parseDouble(params[1].trim()));
                break;

            case STRATEGY_KDJ:
                if (params.length < 5) {
                    throw new IllegalArgumentException("KDJ策略需要至少5个参数: " + KDJ_PARAMS_DESC);
                }
                paramMap.put("kPeriod", Integer.parseInt(params[0].trim()));
                paramMap.put("dPeriod", Integer.parseInt(params[1].trim()));
                paramMap.put("jPeriod", Integer.parseInt(params[2].trim()));
                paramMap.put("oversold", Integer.parseInt(params[3].trim()));
                paramMap.put("overbought", Integer.parseInt(params[4].trim()));
                break;

            default:
                throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }

        return paramMap;
    }

    /**
     * 获取策略参数描述
     *
     * @param strategyType 策略类型
     * @return 参数描述
     */
    public static String getStrategyParamsDescription(String strategyType) {
        switch (strategyType) {
            // 移动平均线策略
            case STRATEGY_SMA:
                return SMA_PARAMS_DESC;
            case STRATEGY_EMA:
                return EMA_PARAMS_DESC;
            case STRATEGY_TRIPLE_EMA:
                return TRIPLE_EMA_PARAMS_DESC;
            case STRATEGY_WMA:
                return WMA_PARAMS_DESC;
            case STRATEGY_HMA:
                return HMA_PARAMS_DESC;
            case STRATEGY_KAMA:
                return KAMA_PARAMS_DESC;
            case STRATEGY_ZLEMA:
                return ZLEMA_PARAMS_DESC;
            case STRATEGY_DEMA:
                return DEMA_PARAMS_DESC;
            case STRATEGY_TEMA:
                return TEMA_PARAMS_DESC;
            case STRATEGY_VWAP:
                return VWAP_PARAMS_DESC;

            // 震荡指标策略
            case STRATEGY_RSI:
                return RSI_PARAMS_DESC;
            case STRATEGY_STOCHASTIC:
                return STOCHASTIC_PARAMS_DESC;
            case STRATEGY_STOCHASTIC_RSI:
                return STOCHASTIC_RSI_PARAMS_DESC;
            case STRATEGY_WILLIAMS_R:
                return WILLIAMS_R_PARAMS_DESC;
            case STRATEGY_CCI:
                return CCI_PARAMS_DESC;
            case STRATEGY_CMO:
                return CMO_PARAMS_DESC;
            case STRATEGY_ROC:
                return ROC_PARAMS_DESC;
            case STRATEGY_MACD:
                return MACD_PARAMS_DESC;
            case STRATEGY_PPO:
                return PPO_PARAMS_DESC;
            case STRATEGY_DPO:
                return DPO_PARAMS_DESC;

            // 趋势指标策略
            case STRATEGY_ADX:
                return ADX_PARAMS_DESC;
            case STRATEGY_AROON:
                return AROON_PARAMS_DESC;
            case STRATEGY_ICHIMOKU:
                return ICHIMOKU_PARAMS_DESC;
            case STRATEGY_PARABOLIC_SAR:
                return PARABOLIC_SAR_PARAMS_DESC;
            case STRATEGY_DMA:
                return DMA_PARAMS_DESC;

            // 波动指标策略
            case STRATEGY_BOLLINGER_BANDS:
                return BOLLINGER_PARAMS_DESC;
            case STRATEGY_KELTNER_CHANNEL:
                return KELTNER_CHANNEL_PARAMS_DESC;
            case STRATEGY_CHANDELIER_EXIT:
                return CHANDELIER_EXIT_PARAMS_DESC;
            case STRATEGY_ULCER_INDEX:
                return ULCER_INDEX_PARAMS_DESC;

            // 成交量指标策略
            case STRATEGY_OBV:
                return OBV_PARAMS_DESC;
            case STRATEGY_MASS_INDEX:
                return MASS_INDEX_PARAMS_DESC;

            // 蜡烛图形态策略
            case STRATEGY_DOJI:
                return DOJI_PARAMS_DESC;
            case STRATEGY_BULLISH_ENGULFING:
                return BULLISH_ENGULFING_PARAMS_DESC;
            case STRATEGY_BEARISH_ENGULFING:
                return BEARISH_ENGULFING_PARAMS_DESC;
            case STRATEGY_BULLISH_HARAMI:
                return BULLISH_HARAMI_PARAMS_DESC;
            case STRATEGY_BEARISH_HARAMI:
                return BEARISH_HARAMI_PARAMS_DESC;
            case STRATEGY_THREE_WHITE_SOLDIERS:
                return THREE_WHITE_SOLDIERS_PARAMS_DESC;
            case STRATEGY_THREE_BLACK_CROWS:
                return THREE_BLACK_CROWS_PARAMS_DESC;

            // 组合策略
            case STRATEGY_DUAL_THRUST:
                return DUAL_THRUST_PARAMS_DESC;
            case STRATEGY_TURTLE_TRADING:
                return TURTLE_TRADING_PARAMS_DESC;
            case STRATEGY_MEAN_REVERSION:
                return MEAN_REVERSION_PARAMS_DESC;
            case STRATEGY_TREND_FOLLOWING:
                return TREND_FOLLOWING_PARAMS_DESC;
            case STRATEGY_BREAKOUT:
                return BREAKOUT_PARAMS_DESC;
            case STRATEGY_GOLDEN_CROSS:
                return GOLDEN_CROSS_PARAMS_DESC;
            case STRATEGY_DEATH_CROSS:
                return DEATH_CROSS_PARAMS_DESC;

            // 新增震荡指标策略
            case STRATEGY_TRIX:
                return TRIX_PARAMS_DESC;
            case STRATEGY_AWESOME_OSCILLATOR:
                return AWESOME_OSCILLATOR_PARAMS_DESC;

            // 新增趋势指标策略
            case STRATEGY_ICHIMOKU_CLOUD_BREAKOUT:
                return ICHIMOKU_CLOUD_BREAKOUT_PARAMS_DESC;
            case STRATEGY_DMI:
                return DMI_PARAMS_DESC;
            case STRATEGY_SUPERTREND:
                return SUPERTREND_PARAMS_DESC;

            // 新增波动指标策略
            case STRATEGY_ATR:
                return ATR_PARAMS_DESC;
            case STRATEGY_KDJ:
                return KDJ_PARAMS_DESC;

            // 新增组合策略
            case STRATEGY_DUAL_MA_WITH_RSI:
                return DUAL_MA_WITH_RSI_PARAMS_DESC;
            case STRATEGY_MACD_WITH_BOLLINGER:
                return MACD_WITH_BOLLINGER_PARAMS_DESC;

            default:
                return "未知策略类型";
        }
    }

    /**
     * 验证策略参数
     *
     * @param strategyType 策略类型
     * @param params       参数字符串
     * @return 是否有效
     */
    public static boolean validateStrategyParams(String strategyType, String params) {
        if (params == null || params.trim().isEmpty()) {
            return false;
        }

        try {
            parseParams(strategyType, params);
            return true;
        } catch (Exception e) {
            log.warn("策略参数验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建SMA交叉策略
     */
    private static Strategy createSMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createBollingerBandsStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 20);
        double multiplier = (double) params.getOrDefault("deviation", 2.0);

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
    private static Strategy createMACDStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 12);
        int longPeriod = (int) params.getOrDefault("longPeriod", 26);
        int signalPeriod = (int) params.getOrDefault("signalPeriod", 9);

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
    private static Strategy createRSIStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 14);
        int oversold = (int) params.getOrDefault("oversold", 30);
        int overbought = (int) params.getOrDefault("overbought", 70);

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
    private static Strategy createStochasticStrategy(BarSeries series, Map<String, Object> params) {
        int kPeriod = (int) params.getOrDefault("kPeriod", 14);
        int kSmooth = (int) params.getOrDefault("kSmooth", 3);
        int dSmooth = (int) params.getOrDefault("dSmooth", 3);
        int oversold = (int) params.getOrDefault("oversold", 20);
        int overbought = (int) params.getOrDefault("overbought", 80);

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
    private static Strategy createADXStrategy(BarSeries series, Map<String, Object> params) {
        int adxPeriod = (int) params.getOrDefault("adxPeriod", 14);
        int diPeriod = (int) params.getOrDefault("diPeriod", 14);
        int threshold = (int) params.getOrDefault("threshold", 25);

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
    private static Strategy createCCIStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 20);
        int oversold = (int) params.getOrDefault("oversold", -100);
        int overbought = (int) params.getOrDefault("overbought", 100);

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
    private static Strategy createWilliamsRStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 14);
        int oversold = (int) params.getOrDefault("oversold", -80);
        int overbought = (int) params.getOrDefault("overbought", -20);

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
    private static Strategy createTripleEMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 5);
        int middlePeriod = (int) params.getOrDefault("middlePeriod", 10);
        int longPeriod = (int) params.getOrDefault("longPeriod", 20);

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
    private static Strategy createIchimokuStrategy(BarSeries series, Map<String, Object> params) {
        int conversionPeriod = (int) params.getOrDefault("conversionPeriod", 9);
        int basePeriod = (int) params.getOrDefault("basePeriod", 26);
        int laggingSpan = (int) params.getOrDefault("laggingSpan", 52);

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
    private static Strategy createEMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createWMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createHMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createKAMAStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 10);
        int fastEMA = (int) params.getOrDefault("fastEMA", 2);
        int slowEMA = (int) params.getOrDefault("slowEMA", 30);

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
    private static Strategy createZLEMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createDEMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createTEMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);

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
    private static Strategy createStochasticRSIStrategy(BarSeries series, Map<String, Object> params) {
        int rsiPeriod = (int) params.getOrDefault("rsiPeriod", 14);
        int stochasticPeriod = (int) params.getOrDefault("stochasticPeriod", 14);
        int kPeriod = (int) params.getOrDefault("kPeriod", 3);
        int dPeriod = (int) params.getOrDefault("dPeriod", 3);
        int overbought = (int) params.getOrDefault("overbought", 80);
        int oversold = (int) params.getOrDefault("oversold", 20);

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
    private static Strategy createCMOStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 14);
        int overbought = (int) params.getOrDefault("overbought", 50);
        int oversold = (int) params.getOrDefault("oversold", -50);

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
    private static Strategy createROCStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 12);
        double threshold = (double) params.getOrDefault("threshold", 0.0);

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
    private static Strategy createPPOStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 12);
        int longPeriod = (int) params.getOrDefault("longPeriod", 26);
        int signalPeriod = (int) params.getOrDefault("signalPeriod", 9);

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
    private static Strategy createDPOStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 20);

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
    private static Strategy createAroonStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 25);

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
    private static Strategy createDMAStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 10);
        int longPeriod = (int) params.getOrDefault("longPeriod", 50);
        int signalPeriod = (int) params.getOrDefault("signalPeriod", 10);

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
    private static Strategy createUlcerIndexStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 14);
        double threshold = (double) params.getOrDefault("threshold", 5.0);

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
    private static Strategy createOBVStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 20);

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
    private static Strategy createMassIndexStrategy(BarSeries series, Map<String, Object> params) {
        int emaPeriod = (int) params.getOrDefault("emaPeriod", 9);
        int massIndexPeriod = (int) params.getOrDefault("massIndexPeriod", 25);
        double threshold = (double) params.getOrDefault("threshold", 27.0);

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
    private static Strategy createDojiStrategy(BarSeries series, Map<String, Object> params) {
        double tolerance = (double) params.getOrDefault("tolerance", 0.05);

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
    private static Strategy createBullishEngulfingStrategy(BarSeries series, Map<String, Object> params) {
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
    private static Strategy createBearishEngulfingStrategy(BarSeries series, Map<String, Object> params) {
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
    private static Strategy createBullishHaramiStrategy(BarSeries series, Map<String, Object> params) {
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
    private static Strategy createBearishHaramiStrategy(BarSeries series, Map<String, Object> params) {
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
    private static Strategy createThreeWhiteSoldiersStrategy(BarSeries series, Map<String, Object> params) {
        ThreeWhiteSoldiersIndicator threeWhiteSoldiers = new ThreeWhiteSoldiersIndicator(series, 5, DoubleNum.valueOf(0.3));
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
    private static Strategy createThreeBlackCrowsStrategy(BarSeries series, Map<String, Object> params) {
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
    private static Strategy createDoublePushStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 5);
        int longPeriod = (int) params.getOrDefault("longPeriod", 20);

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
    private static Strategy createTurtleTradingStrategy(BarSeries series, Map<String, Object> params) {
        int entryPeriod = (int) params.getOrDefault("entryPeriod", 20);
        int exitPeriod = (int) params.getOrDefault("exitPeriod", 10);

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
    private static Strategy createTrendFollowingStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 26);
        int signalPeriod = (int) params.getOrDefault("signalPeriod", 9);

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
    private static Strategy createBreakoutStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 20);

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
    private static Strategy createGoldenCrossStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 26);

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
    private static Strategy createDeathCrossStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 26);

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
    private static Strategy createTRIXStrategy(BarSeries series, Map<String, Object> params) {
        int period = (int) params.getOrDefault("period", 15);
        int signalPeriod = (int) params.getOrDefault("signalPeriod", 9);

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
    private static Strategy createDualMAWithRSIStrategy(BarSeries series, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 9);
        int longPeriod = (int) params.getOrDefault("longPeriod", 21);
        int rsiPeriod = (int) params.getOrDefault("rsiPeriod", 14);
        int rsiThreshold = (int) params.getOrDefault("rsiThreshold", 50);

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
    private static Strategy createParabolicSARStrategy(BarSeries series, Map<String, Object> params) {
        double step = (double) params.getOrDefault("step", 0.02);
        double max = (double) params.getOrDefault("max", 0.2);

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
    private static Strategy createChandelierExitStrategy(BarSeries series, Map<String, Object> params) {
        // 确保period参数至少为1，避免TimePeriod为null的错误
        int period = Math.max(1, (int) params.getOrDefault("period", 22));
        double multiplier = (double) params.getOrDefault("multiplier", 3.0);

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
}
