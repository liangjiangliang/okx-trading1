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
    public static final String STRATEGY_TRIMA = "TRIMA"; // 三角移动平均线
    public static final String STRATEGY_T3 = "T3"; // 三重指数移动平均线
    public static final String STRATEGY_MAMA = "MAMA"; // MESA自适应移动平均线
    public static final String STRATEGY_VIDYA = "VIDYA"; // 可变指数动态平均线
    public static final String STRATEGY_WILDERS = "WILDERS"; // 威尔德平滑

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
    public static final String STRATEGY_FISHER = "FISHER"; // Fisher变换
    public static final String STRATEGY_FOSC = "FOSC"; // 预测振荡器
    public static final String STRATEGY_EOM = "EOM"; // 移动便利性
    public static final String STRATEGY_CHOP = "CHOP"; // 震荡指数
    public static final String STRATEGY_KVO = "KVO"; // 克林格交易量振荡器
    public static final String STRATEGY_RVGI = "RVGI"; // 相对活力指数
    public static final String STRATEGY_STC = "STC"; // 沙夫趋势周期

    // 趋势指标策略
    public static final String STRATEGY_ADX = "ADX";
    public static final String STRATEGY_AROON = "AROON";
    public static final String STRATEGY_ICHIMOKU = "ICHIMOKU";
    public static final String STRATEGY_ICHIMOKU_CLOUD_BREAKOUT = "ICHIMOKU_CLOUD_BREAKOUT";
    public static final String STRATEGY_PARABOLIC_SAR = "PARABOLIC_SAR";
    public static final String STRATEGY_DMA = "DMA";
    public static final String STRATEGY_DMI = "DMI";
    public static final String STRATEGY_SUPERTREND = "SUPERTREND";
    public static final String STRATEGY_VORTEX = "VORTEX"; // 涡流指标
    public static final String STRATEGY_QSTICK = "QSTICK"; // Q棒指标
    public static final String STRATEGY_WILLIAMS_ALLIGATOR = "WILLIAMS_ALLIGATOR"; // 威廉姆斯鳄鱼指标
    public static final String STRATEGY_HT_TRENDLINE = "HT_TRENDLINE"; // 希尔伯特变换-瞬时趋势线

    // 波动指标策略
    public static final String STRATEGY_BOLLINGER_BANDS = "BOLLINGER";
    public static final String STRATEGY_KELTNER_CHANNEL = "KELTNER";
    public static final String STRATEGY_CHANDELIER_EXIT = "CHANDELIER_EXIT";
    public static final String STRATEGY_ULCER_INDEX = "ULCER_INDEX";
    public static final String STRATEGY_ATR = "ATR";
    public static final String STRATEGY_KDJ = "KDJ";
    public static final String STRATEGY_NATR = "NATR"; // 归一化平均真实范围
    public static final String STRATEGY_MASS = "MASS"; // 质量指数
    public static final String STRATEGY_STDDEV = "STDDEV"; // 标准差
    public static final String STRATEGY_SQUEEZE = "SQUEEZE"; // 挤压动量指标
    public static final String STRATEGY_BBW = "BBW"; // 布林带宽度
    public static final String STRATEGY_VOLATILITY = "VOLATILITY"; // 年化历史波动率
    public static final String STRATEGY_DONCHIAN_CHANNELS = "DONCHIAN_CHANNELS"; // 唐奇安通道

    // 成交量指标策略
    public static final String STRATEGY_OBV = "OBV";
    public static final String STRATEGY_MASS_INDEX = "MASS_INDEX";
    public static final String STRATEGY_AD = "AD"; // 累积/派发线
    public static final String STRATEGY_ADOSC = "ADOSC"; // 累积/派发振荡器
    public static final String STRATEGY_NVI = "NVI"; // 负成交量指数
    public static final String STRATEGY_PVI = "PVI"; // 正成交量指数
    public static final String STRATEGY_VWMA = "VWMA"; // 成交量加权移动平均线
    public static final String STRATEGY_VOSC = "VOSC"; // 成交量振荡器
    public static final String STRATEGY_MARKETFI = "MARKETFI"; // 市场便利指数

    // 蜡烛图形态策略
    public static final String STRATEGY_DOJI = "DOJI";
    public static final String STRATEGY_BULLISH_ENGULFING = "BULLISH_ENGULFING";
    public static final String STRATEGY_BEARISH_ENGULFING = "BEARISH_ENGULFING";
    public static final String STRATEGY_BULLISH_HARAMI = "BULLISH_HARAMI";
    public static final String STRATEGY_BEARISH_HARAMI = "BEARISH_HARAMI";
    public static final String STRATEGY_THREE_WHITE_SOLDIERS = "THREE_WHITE_SOLDIERS";
    public static final String STRATEGY_THREE_BLACK_CROWS = "THREE_BLACK_CROWS";
    public static final String STRATEGY_HANGING_MAN = "HANGING_MAN";
    public static final String STRATEGY_HAMMER = "HAMMER"; // 锤子线
    public static final String STRATEGY_INVERTED_HAMMER = "INVERTED_HAMMER"; // 倒锤子线
    public static final String STRATEGY_SHOOTING_STAR = "SHOOTING_STAR"; // 流星线
    public static final String STRATEGY_MORNING_STAR = "MORNING_STAR"; // 晨星
    public static final String STRATEGY_EVENING_STAR = "EVENING_STAR"; // 暮星
    public static final String STRATEGY_PIERCING = "PIERCING"; // 刺透形态
    public static final String STRATEGY_DARK_CLOUD_COVER = "DARK_CLOUD_COVER"; // 乌云盖顶
    public static final String STRATEGY_MARUBOZU = "MARUBOZU"; // 光头光脚阳线/阴线

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
    
    // 统计函数策略
    public static final String STRATEGY_BETA = "BETA"; // Beta系数
    public static final String STRATEGY_CORREL = "CORREL"; // 皮尔逊相关系数
    public static final String STRATEGY_LINEARREG = "LINEARREG"; // 线性回归
    public static final String STRATEGY_LINEARREG_ANGLE = "LINEARREG_ANGLE"; // 线性回归角度
    public static final String STRATEGY_LINEARREG_INTERCEPT = "LINEARREG_INTERCEPT"; // 线性回归截距
    public static final String STRATEGY_LINEARREG_SLOPE = "LINEARREG_SLOPE"; // 线性回归斜率
    public static final String STRATEGY_TSF = "TSF"; // 时间序列预测
    public static final String STRATEGY_VAR = "VAR"; // 方差

    // 希尔伯特变换策略
    public static final String STRATEGY_HT_DCPERIOD = "HT_DCPERIOD"; // 希尔伯特变换-主导周期
    public static final String STRATEGY_HT_DCPHASE = "HT_DCPHASE"; // 希尔伯特变换-主导相位
    public static final String STRATEGY_HT_PHASOR = "HT_PHASOR"; // 希尔伯特变换-相量分量
    public static final String STRATEGY_HT_SINE = "HT_SINE"; // 希尔伯特变换-正弦波
    public static final String STRATEGY_HT_TRENDMODE = "HT_TRENDMODE"; // 希尔伯特变换-趋势与周期模式
    public static final String STRATEGY_MSW = "MSW"; // MESA正弦波

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
    public static final String TRIMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：5,20)";
    public static final String T3_PARAMS_DESC = "周期,成交量因子 (例如：5,0.7)";
    public static final String MAMA_PARAMS_DESC = "快速限制,慢速限制 (例如：0.5,0.05)";
    public static final String VIDYA_PARAMS_DESC = "短期周期,长期周期,alpha (例如：9,12,0.2)";
    public static final String WILDERS_PARAMS_DESC = "周期 (例如：14)";

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
    public static final String FISHER_PARAMS_DESC = "周期 (例如：10)";
    public static final String FOSC_PARAMS_DESC = "周期 (例如：14)";
    public static final String EOM_PARAMS_DESC = "周期,除数 (例如：14,100000000)";
    public static final String CHOP_PARAMS_DESC = "周期 (例如：14)";
    public static final String KVO_PARAMS_DESC = "短周期,长周期,信号周期 (例如：34,55,13)";
    public static final String RVGI_PARAMS_DESC = "周期,信号周期 (例如：10,4)";
    public static final String STC_PARAMS_DESC = "快周期,慢周期,信号周期,K平滑,D平滑 (例如：23,50,10,3,3)";

    // 趋势指标策略参数
    public static final String ADX_PARAMS_DESC = "ADX周期,DI周期,阈值 (例如：14,14,25)";
    public static final String AROON_PARAMS_DESC = "周期,阈值 (例如：25,70)";
    public static final String ICHIMOKU_PARAMS_DESC = "转换线周期,基准线周期,延迟跨度 (例如：9,26,52)";
    public static final String ICHIMOKU_CLOUD_BREAKOUT_PARAMS_DESC = "转换线周期,基准线周期,延迟跨度 (例如：9,26,52)";
    public static final String PARABOLIC_SAR_PARAMS_DESC = "步长,最大步长 (例如：0.02,0.2)";
    public static final String DMA_PARAMS_DESC = "短期均线周期,长期均线周期 (例如：10,50)";
    public static final String DMI_PARAMS_DESC = "周期,ADX阈值 (例如：14,20)";
    public static final String SUPERTREND_PARAMS_DESC = "ATR周期,乘数 (例如：10,3.0)";
    public static final String VORTEX_PARAMS_DESC = "周期 (例如：14)";
    public static final String QSTICK_PARAMS_DESC = "周期 (例如：14)";
    public static final String WILLIAMS_ALLIGATOR_PARAMS_DESC = "下颚周期,牙齿周期,嘴唇周期,下颚偏移,牙齿偏移,嘴唇偏移 (例如：13,8,5,8,5,3)";
    public static final String HT_TRENDLINE_PARAMS_DESC = "周期 (例如：14)";

    // 波动指标策略参数
    public static final String BOLLINGER_PARAMS_DESC = "周期,标准差倍数 (例如：20,2.0)";
    public static final String KELTNER_CHANNEL_PARAMS_DESC = "EMA周期,ATR周期,乘数 (例如：20,10,2.0)";
    public static final String CHANDELIER_EXIT_PARAMS_DESC = "周期,乘数 (例如：22,3.0)";
    public static final String ULCER_INDEX_PARAMS_DESC = "周期,阈值 (例如：14,5.0)";
    public static final String ATR_PARAMS_DESC = "周期,乘数 (例如：14,2.0)";
    public static final String KDJ_PARAMS_DESC = "K周期,D周期,J周期,超卖阈值,超买阈值 (例如：9,3,3,20,80)";
    public static final String NATR_PARAMS_DESC = "周期 (例如：14)";
    public static final String MASS_PARAMS_DESC = "EMA周期,累积周期,阈值 (例如：9,25,27)";
    public static final String STDDEV_PARAMS_DESC = "周期,标准差倍数 (例如：20,2.0)";
    public static final String SQUEEZE_PARAMS_DESC = "BB周期,KC周期,BB倍数,KC倍数 (例如：20,20,2.0,1.5)";
    public static final String BBW_PARAMS_DESC = "周期,标准差倍数 (例如：20,2.0)";
    public static final String VOLATILITY_PARAMS_DESC = "周期 (例如：20)";
    public static final String DONCHIAN_CHANNELS_PARAMS_DESC = "周期 (例如：20)";

    // 成交量指标策略参数
    public static final String OBV_PARAMS_DESC = "短期OBV周期,长期OBV周期 (例如：5,20)";
    public static final String MASS_INDEX_PARAMS_DESC = "EMA周期,累积周期,阈值 (例如：9,25,27)";
    public static final String AD_PARAMS_DESC = "短期周期,长期周期 (例如：3,10)";
    public static final String ADOSC_PARAMS_DESC = "快速周期,慢速周期 (例如：3,10)";
    public static final String NVI_PARAMS_DESC = "短期周期,长期周期 (例如：1,255)";
    public static final String PVI_PARAMS_DESC = "短期周期,长期周期 (例如：1,255)";
    public static final String VWMA_PARAMS_DESC = "周期 (例如：20)";
    public static final String VOSC_PARAMS_DESC = "短周期,长周期 (例如：5,10)";
    public static final String MARKETFI_PARAMS_DESC = "周期 (例如：14)";

    // 蜡烛图形态策略参数
    public static final String DOJI_PARAMS_DESC = "影线比例阈值 (例如：0.1)";
    public static final String BULLISH_ENGULFING_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String BEARISH_ENGULFING_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String BULLISH_HARAMI_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String BEARISH_HARAMI_PARAMS_DESC = "确认周期 (例如：1)";
    public static final String THREE_WHITE_SOLDIERS_PARAMS_DESC = "确认周期 (例如：3)";
    public static final String THREE_BLACK_CROWS_PARAMS_DESC = "确认周期 (例如：3)";
    public static final String HANGING_MAN_PARAMS_DESC = "上影线与实体比例阈值,下影线与实体比例阈值 (例如：0.1,2.0)";
    public static final String HAMMER_PARAMS_DESC = "影线比例阈值 (例如：2.0)";
    public static final String INVERTED_HAMMER_PARAMS_DESC = "影线比例阈值 (例如：2.0)";
    public static final String SHOOTING_STAR_PARAMS_DESC = "影线比例阈值 (例如：2.0)";
    public static final String MORNING_STAR_PARAMS_DESC = "渗透率 (例如：3.0)";
    public static final String EVENING_STAR_PARAMS_DESC = "渗透率 (例如：3.0)";
    public static final String PIERCING_PARAMS_DESC = "渗透率 (例如：50.0)";
    public static final String DARK_CLOUD_COVER_PARAMS_DESC = "渗透率 (例如：50.0)";
    public static final String MARUBOZU_PARAMS_DESC = "影线比例阈值 (例如：0.1)";

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
    
    // 统计函数策略参数
    public static final String BETA_PARAMS_DESC = "周期 (例如：5)";
    public static final String CORREL_PARAMS_DESC = "周期 (例如：30)";
    public static final String LINEARREG_PARAMS_DESC = "周期 (例如：14)";
    public static final String LINEARREG_ANGLE_PARAMS_DESC = "周期 (例如：14)";
    public static final String LINEARREG_INTERCEPT_PARAMS_DESC = "周期 (例如：14)";
    public static final String LINEARREG_SLOPE_PARAMS_DESC = "周期 (例如：14)";
    public static final String TSF_PARAMS_DESC = "周期 (例如：14)";
    public static final String VAR_PARAMS_DESC = "周期 (例如：5)";
    
    // 希尔伯特变换策略参数
    public static final String HT_DCPERIOD_PARAMS_DESC = "周期 (例如：14)";
    public static final String HT_DCPHASE_PARAMS_DESC = "周期 (例如：14)";
    public static final String HT_PHASOR_PARAMS_DESC = "周期 (例如：14)";
    public static final String HT_SINE_PARAMS_DESC = "周期 (例如：14)";
    public static final String HT_TRENDMODE_PARAMS_DESC = "周期 (例如：14)";
    public static final String MSW_PARAMS_DESC = "周期 (例如：14)";
}
