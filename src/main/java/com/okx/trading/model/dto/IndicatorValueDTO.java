package com.okx.trading.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 技术指标值数据传输对象
 */
public class IndicatorValueDTO {
    
    /**
     * 交易对
     */
    private String symbol;
    
    /**
     * 时间间隔
     */
    private String interval;
    
    /**
     * 指标类型
     */
    private String indicatorType;
    
    /**
     * 计算时间
     */
    private LocalDateTime calculationTime;
    
    /**
     * K线时间
     */
    private LocalDateTime klineTime;
    
    /**
     * 指标值映射
     * 不同指标有不同的值，例如：
     * - MACD: {"macd": 10.5, "signal": 9.8, "histogram": 0.7}
     * - RSI: {"value": 65.3}
     * - 布林带: {"upper": 32100.5, "middle": 31500.2, "lower": 30900.7}
     */
    private Map<String, BigDecimal> values = new HashMap<>();
    
    /**
     * 指标参数描述
     */
    private String paramDescription;
    
    /**
     * 是否有效
     */
    private boolean valid = true;
    
    /**
     * 错误信息
     */
    private String errorMessage;

    public IndicatorValueDTO() {
    }
    
    public IndicatorValueDTO(String symbol, String interval, String indicatorType) {
        this.symbol = symbol;
        this.interval = interval;
        this.indicatorType = indicatorType;
        this.calculationTime = LocalDateTime.now();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getIndicatorType() {
        return indicatorType;
    }

    public void setIndicatorType(String indicatorType) {
        this.indicatorType = indicatorType;
    }

    public LocalDateTime getCalculationTime() {
        return calculationTime;
    }

    public void setCalculationTime(LocalDateTime calculationTime) {
        this.calculationTime = calculationTime;
    }

    public LocalDateTime getKlineTime() {
        return klineTime;
    }

    public void setKlineTime(LocalDateTime klineTime) {
        this.klineTime = klineTime;
    }

    public Map<String, BigDecimal> getValues() {
        return values;
    }

    public void setValues(Map<String, BigDecimal> values) {
        this.values = values;
    }
    
    /**
     * 添加一个指标值
     * 
     * @param key 指标值键
     * @param value 指标值
     * @return 当前对象，用于链式调用
     */
    public IndicatorValueDTO addValue(String key, BigDecimal value) {
        this.values.put(key, value);
        return this;
    }
    
    /**
     * 添加一个指标值
     * 
     * @param key 指标值键
     * @param value 指标值
     * @return 当前对象，用于链式调用
     */
    public IndicatorValueDTO addValue(String key, double value) {
        this.values.put(key, BigDecimal.valueOf(value));
        return this;
    }

    public String getParamDescription() {
        return paramDescription;
    }

    public void setParamDescription(String paramDescription) {
        this.paramDescription = paramDescription;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.valid = false;
    }
} 