package com.okx.trading.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.okx.trading.model.dto.IndicatorValueDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.service.IndicatorCalculationService;
import com.okx.trading.service.KlineCacheService;
import com.okx.trading.service.TechnicalIndicatorService;
import com.okx.trading.ta4j.CandlestickBarSeriesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.WilliamsRIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.util.*;
import java.util.stream.Collectors;

import static com.okx.trading.constant.IndicatorInfo.*;

/**
 * 技术指标服务实现类
 * 负责计算各种技术指标值
 */
@Service
public class TechnicalIndicatorServiceImpl implements TechnicalIndicatorService{

    private static final Logger log = LoggerFactory.getLogger(TechnicalIndicatorServiceImpl.class);


    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private KlineCacheService klineCacheService;

    @Autowired
    private CandlestickBarSeriesConverter barSeriesConverter;

    @Autowired
    private IndicatorCalculationService indicatorCalculationService;

    @Override
    public IndicatorValueDTO calculateLastIndicator(String symbol, String interval, String indicatorType, String params){
        // 获取K线数据
        List<CandlestickEntity> klines = klineCacheService.getKlineData(symbol, interval, MIN_KLINE_COUNT);

        // 检查K线数据是否足够
        if(klines == null || klines.size() < MIN_KLINE_COUNT){
            IndicatorValueDTO result = new IndicatorValueDTO(symbol, interval, indicatorType);
            result.setErrorMessage("没有足够的K线数据进行计算, 需要至少" + MIN_KLINE_COUNT + "根K线");
            return result;
        }

        return calculateIndicator(klines, indicatorType, params);
    }

    @Override
    public Map<String,IndicatorValueDTO> calculateMultipleIndicators(String symbol, String interval, Map<String,String> indicators){
        // 获取K线数据
        log.debug("计算技术指标: {} {}", symbol, interval);
        String sourceKey = SOURCE_KLINE_PREFIX + symbol + ":" + interval;

        // 获取所有K线数据
        Set<Object> klineSet = redisTemplate.opsForZSet().reverseRange(sourceKey, 0, MIN_KLINE_COUNT);
        if(klineSet == null || klineSet.isEmpty()){
            log.warn("未找到K线数据: {}", sourceKey);
            return new HashMap<>();
        }

        List<Candlestick> candlestickList = klineSet.stream().map(x -> JSONObject.parseObject((String)x, Candlestick.class)).collect(Collectors.toList());
        Collections.sort(candlestickList);

        if(candlestickList.size() > MIN_KLINE_COUNT){
            candlestickList = candlestickList.subList(candlestickList.size() - 1 - MIN_KLINE_COUNT, candlestickList.size() - 1);
        }

        indicatorCalculationService.checkKlineContinuityAndFill(candlestickList);

        // 转换为List并按时间排序
        List<CandlestickEntity> klines = candlestickList.stream()
            .map(JSONObject :: toJSONString)
            .map(obj -> JSONObject.parseObject((String)obj, CandlestickEntity.class))
            .sorted()
            .collect(Collectors.toList());

        Map<String,IndicatorValueDTO> result = new HashMap<>();

        // 检查K线数据是否足够
        if(klines == null || klines.size() < MIN_KLINE_COUNT){
            for(String indicatorType: indicators.keySet()){
                IndicatorValueDTO dto = new IndicatorValueDTO(symbol, interval, indicatorType);
                dto.setErrorMessage("没有足够的K线数据进行计算, 需要至少" + MIN_KLINE_COUNT + "根K线");
                result.put(indicatorType, dto);
            }
            return result;
        }

        // 计算每个指标的值
        for(Map.Entry<String,String> entry: indicators.entrySet()){
            String indicatorType = entry.getKey();
            String params = entry.getValue();

            try{
                IndicatorValueDTO dto = calculateIndicator(klines, indicatorType, params);
                result.put(indicatorType, dto);
            }catch(Exception e){
                log.error("计算指标 {} 出错: {}", indicatorType, e.getMessage(), e);
                IndicatorValueDTO dto = new IndicatorValueDTO(symbol, interval, indicatorType);
                dto.setErrorMessage("计算指标出错: " + e.getMessage());
                result.put(indicatorType, dto);
            }
        }

        return result;
    }

    @Override
    public IndicatorValueDTO calculateIndicator(List<CandlestickEntity> candlesticks, String indicatorType, String params){
        if(candlesticks == null || candlesticks.isEmpty()){
            IndicatorValueDTO result = new IndicatorValueDTO();
            result.setIndicatorType(indicatorType);
            result.setErrorMessage("K线数据不能为空");
            return result;
        }

        CandlestickEntity lastCandle = candlesticks.get(candlesticks.size() - 1);
        String symbol = lastCandle.getSymbol();
        String interval = lastCandle.getIntervalVal();

        IndicatorValueDTO result = new IndicatorValueDTO(symbol, interval, indicatorType);
        result.setKlineTime(lastCandle.getCloseTime());
        result.setParamDescription(params);

        try{
            // 将K线数据转换为Ta4j的BarSeries
            String seriesName = CandlestickBarSeriesConverter.createSeriesName(symbol, interval);
            BarSeries series = barSeriesConverter.convert(candlesticks, seriesName);

            // 检查是否有足够的数据
            if(series.getBarCount() < 10){
                result.setErrorMessage("没有足够的K线数据进行计算");
                return result;
            }

            // 根据不同指标计算值
            switch(indicatorType.toUpperCase()){
                case INDICATOR_SMA:
                    calculateSMA(series, params, result);
                    break;
                case INDICATOR_EMA:
                    calculateEMA(series, params, result);
                    break;
                case INDICATOR_BOLLINGER_BANDS:
                    calculateBollingerBands(series, params, result);
                    break;
                case INDICATOR_MACD:
                    calculateMACD(series, params, result);
                    break;
                case INDICATOR_RSI:
                    calculateRSI(series, params, result);
                    break;
                case INDICATOR_STOCHASTIC:
                    calculateStochastic(series, params, result);
                    break;
                case INDICATOR_WILLIAMS_R:
                    calculateWilliamsR(series, params, result);
                    break;
                case INDICATOR_CCI:
                    calculateCCI(series, params, result);
                    break;
                case INDICATOR_ATR:
                    calculateATR(series, params, result);
                    break;
                case INDICATOR_PARABOLIC_SAR:
                    calculateParabolicSAR(series, params, result);
                    break;
                default:
                    result.setErrorMessage("不支持的指标类型: " + indicatorType);
                    break;
            }
        }catch(Exception e){
            log.error("计算指标 {} 出错: {}", indicatorType, e.getMessage(), e);
            result.setErrorMessage("计算指标出错: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<String> getSupportedIndicators(){
        return Arrays.asList(
            INDICATOR_SMA,
            INDICATOR_EMA,
            INDICATOR_BOLLINGER_BANDS,
            INDICATOR_MACD,
            INDICATOR_RSI,
            INDICATOR_STOCHASTIC,
            INDICATOR_WILLIAMS_R,
            INDICATOR_CCI,
            INDICATOR_ATR,
            INDICATOR_PARABOLIC_SAR
        );
    }

    @Override
    public String getIndicatorParamsDescription(String indicatorType){
        switch(indicatorType.toUpperCase()){
            case INDICATOR_SMA:
                return SMA_PARAMS_DESC;
            case INDICATOR_EMA:
                return EMA_PARAMS_DESC;
            case INDICATOR_BOLLINGER_BANDS:
                return BOLLINGER_PARAMS_DESC;
            case INDICATOR_MACD:
                return MACD_PARAMS_DESC;
            case INDICATOR_RSI:
                return RSI_PARAMS_DESC;
            case INDICATOR_STOCHASTIC:
                return STOCHASTIC_PARAMS_DESC;
            case INDICATOR_WILLIAMS_R:
                return WILLIAMS_R_PARAMS_DESC;
            case INDICATOR_CCI:
                return CCI_PARAMS_DESC;
            case INDICATOR_ATR:
                return ATR_PARAMS_DESC;
            case INDICATOR_PARABOLIC_SAR:
                return PARABOLIC_SAR_PARAMS_DESC;
            default:
                return "未知指标类型";
        }
    }

    /**
     * 计算简单移动平均线(SMA)
     */
    private void calculateSMA(BarSeries series, String params, IndicatorValueDTO result){
        int period = Integer.parseInt(params.trim());

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);

        int lastIndex = series.getEndIndex();
        Num smaValue = sma.getValue(lastIndex);

        result.addValue("value", smaValue.doubleValue());
    }

    /**
     * 计算指数移动平均线(EMA)
     */
    private void calculateEMA(BarSeries series, String params, IndicatorValueDTO result){
        int period = Integer.parseInt(params.trim());

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, period);

        int lastIndex = series.getEndIndex();
        Num emaValue = ema.getValue(lastIndex);

        result.addValue("value", emaValue.doubleValue());
    }

    /**
     * 计算布林带(Bollinger Bands)
     */
    private void calculateBollingerBands(BarSeries series, String params, IndicatorValueDTO result){
        String[] paramParts = params.split(",");
        int period = Integer.parseInt(paramParts[0].trim());
        double deviationMultiplier = Double.parseDouble(paramParts[1].trim());

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator deviation = new StandardDeviationIndicator(closePrice, period);

        BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, deviation, series.numOf(deviationMultiplier));
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, deviation, series.numOf(deviationMultiplier));

        int lastIndex = series.getEndIndex();

        result.addValue("upper", upper.getValue(lastIndex).doubleValue());
        result.addValue("middle", middle.getValue(lastIndex).doubleValue());
        result.addValue("lower", lower.getValue(lastIndex).doubleValue());
    }

    /**
     * 计算MACD
     */
    private void calculateMACD(BarSeries series, String params, IndicatorValueDTO result){
        String[] paramParts = params.split(",");
        int shortPeriod = Integer.parseInt(paramParts[0].trim());
        int longPeriod = Integer.parseInt(paramParts[1].trim());
        int signalPeriod = Integer.parseInt(paramParts[2].trim());

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);

        int lastIndex = series.getEndIndex();
        double macdValue = macd.getValue(lastIndex).doubleValue();
        double signalValue = signal.getValue(lastIndex).doubleValue();
        double histogram = macdValue - signalValue;

        result.addValue("macd", macdValue);
        result.addValue("signal", signalValue);
        result.addValue("histogram", histogram);
    }

    /**
     * 计算RSI
     */
    private void calculateRSI(BarSeries series, String params, IndicatorValueDTO result){
        int period = Integer.parseInt(params.trim());

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);

        int lastIndex = series.getEndIndex();
        Num rsiValue = rsi.getValue(lastIndex);

        result.addValue("value", rsiValue.doubleValue());
    }

    /**
     * 计算随机指标(Stochastic Oscillator)
     */
    private void calculateStochastic(BarSeries series, String params, IndicatorValueDTO result){
        String[] paramParts = params.split(",");
        int kPeriod = Integer.parseInt(paramParts[0].trim());
        int kSmooth = Integer.parseInt(paramParts[1].trim());
        int dSmooth = Integer.parseInt(paramParts[2].trim());

        StochasticOscillatorKIndicator stochK = new StochasticOscillatorKIndicator(series, kPeriod);
        SMAIndicator stochKSMA = new SMAIndicator(stochK, kSmooth);
        StochasticOscillatorDIndicator stochD = new StochasticOscillatorDIndicator(stochKSMA);

        int lastIndex = series.getEndIndex();

        result.addValue("k", stochKSMA.getValue(lastIndex).doubleValue());
        result.addValue("d", stochD.getValue(lastIndex).doubleValue());
    }

    /**
     * 计算威廉指标(Williams %R)
     */
    private void calculateWilliamsR(BarSeries series, String params, IndicatorValueDTO result){
        int period = Integer.parseInt(params.trim());

        WilliamsRIndicator williamsR = new WilliamsRIndicator(series, period);

        int lastIndex = series.getEndIndex();
        Num williamsRValue = williamsR.getValue(lastIndex);

        result.addValue("value", williamsRValue.doubleValue());
    }

    /**
     * 计算商品通道指数(CCI)
     */
    private void calculateCCI(BarSeries series, String params, IndicatorValueDTO result){
        int period = Integer.parseInt(params.trim());

        CCIIndicator cci = new CCIIndicator(series, period);

        int lastIndex = series.getEndIndex();
        Num cciValue = cci.getValue(lastIndex);

        result.addValue("value", cciValue.doubleValue());
    }

    /**
     * 计算真实波动幅度均值(ATR)
     */
    private void calculateATR(BarSeries series, String params, IndicatorValueDTO result){
        int period = Integer.parseInt(params.trim());

        ATRIndicator atr = new ATRIndicator(series, period);

        int lastIndex = series.getEndIndex();
        Num atrValue = atr.getValue(lastIndex);

        result.addValue("value", atrValue.doubleValue());
    }

    /**
     * 计算抛物线转向指标(Parabolic SAR)
     */
    private void calculateParabolicSAR(BarSeries series, String params, IndicatorValueDTO result){
        String[] paramParts = params.split(",");
        double step = Double.parseDouble(paramParts[0].trim());
        double max = Double.parseDouble(paramParts[1].trim());

        ParabolicSarIndicator sar = new ParabolicSarIndicator(series, series.numOf(step), series.numOf(max));

        int lastIndex = series.getEndIndex();
        Num sarValue = sar.getValue(lastIndex);

        result.addValue("value", sarValue.doubleValue());
    }
}
