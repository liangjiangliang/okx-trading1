package com.okx.trading.constant;


import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

public class IndicatorInfo{
    // 支持的指标类型常量
    public static final String INDICATOR_SMA = "SMA";
    public static final String INDICATOR_EMA = "EMA";
    public static final String INDICATOR_BOLLINGER_BANDS = "BOLLINGER";
    public static final String INDICATOR_MACD = "MACD";
    public static final String INDICATOR_RSI = "RSI";
    public static final String INDICATOR_STOCHASTIC = "STOCHASTIC";
    public static final String INDICATOR_WILLIAMS_R = "WILLIAMS_R";
    public static final String INDICATOR_CCI = "CCI";
    public static final String INDICATOR_ATR = "ATR";
    public static final String INDICATOR_PARABOLIC_SAR = "PARABOLIC_SAR";

    // 指标参数说明
    public static final String SMA_PARAMS_DESC = "14";
    public static final String EMA_PARAMS_DESC = "12";
    public static final String BOLLINGER_PARAMS_DESC = "20,2.0";
    public static final String MACD_PARAMS_DESC = "12,26,9";
    public static final String RSI_PARAMS_DESC = "14";
    public static final String STOCHASTIC_PARAMS_DESC = "14,3,3";
    public static final String WILLIAMS_R_PARAMS_DESC = "14";
    public static final String CCI_PARAMS_DESC = "20";
    public static final String ATR_PARAMS_DESC = "14";
    public static final String PARABOLIC_SAR_PARAMS_DESC = "0.02,0.2";

    public static final Map<String,String> indicatorParamMap = new HashMap<>();

    @PostConstruct
    public void init(){
        indicatorParamMap.put(INDICATOR_BOLLINGER_BANDS, BOLLINGER_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_MACD, MACD_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_RSI, RSI_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_SMA, SMA_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_EMA, EMA_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_STOCHASTIC, STOCHASTIC_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_WILLIAMS_R, WILLIAMS_R_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_CCI, CCI_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_ATR, ATR_PARAMS_DESC);
        indicatorParamMap.put(INDICATOR_PARABOLIC_SAR, PARABOLIC_SAR_PARAMS_DESC);
    }
}
