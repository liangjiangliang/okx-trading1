package com.okx.trading.constant;


import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class IndicatorInfo {

    public static final String SOURCE_KLINE_PREFIX = "coin-rt-kline:";
    public static final String TARGET_INDICATOR_PREFIX = "coin-rt-indicator:";
    public static final String INDICATOR_SUBSCRIPTION_KEY = "kline:subscriptions";
    // 最少需要的K线数量
    public static final int MIN_KLINE_COUNT = 50;
    public static final Map<String, String> indicatorParamMap = new HashMap<>();

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
    public static final String STRATEGY_HANGING_MAN = "HANGING_MAN";

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
    public static final String HANGING_MAN_PARAMS_DESC = "上影线与实体比例阈值,下影线与实体比例阈值 (例如：0.1,2.0)";

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
}
