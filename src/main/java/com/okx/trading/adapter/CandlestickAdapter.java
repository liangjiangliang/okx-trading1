package com.okx.trading.adapter;

import com.okx.trading.model.entity.CandlestickEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 蜡烛图数据适配器，用于解决CandlestickEntity和Ta4j之间的字段名不匹配问题
 * 避免修改原有代码
 */
@Component
public class CandlestickAdapter {

    private static final Logger log = LoggerFactory.getLogger(CandlestickAdapter.class);

    /**
     * 获取开盘价
     * @param candle K线数据
     * @return 开盘价
     */
    public static Num getOpen(CandlestickEntity candle) {
        return DecimalNum.valueOf(candle.getOpen());
    }

    /**
     * 获取最高价
     * @param candle K线数据
     * @return 最高价
     */
    public static Num getHigh(CandlestickEntity candle) {
        return DecimalNum.valueOf(candle.getHigh());
    }

    /**
     * 获取最低价
     * @param candle K线数据
     * @return 最低价
     */
    public static Num getLow(CandlestickEntity candle) {
        return DecimalNum.valueOf(candle.getLow());
    }

    /**
     * 获取收盘价
     * @param candle K线数据
     * @return 收盘价
     */
    public static Num getClose(CandlestickEntity candle) {
        return DecimalNum.valueOf(candle.getClose());
    }

    /**
     * 获取成交量
     * @param candle K线数据
     * @return 成交量
     */
    public static Num getVolume(CandlestickEntity candle) {
        return DecimalNum.valueOf(candle.getVolume());
    }

    /**
     * 获取开盘时间
     * @param candle K线数据
     * @return 开盘时间
     */
    public static ZonedDateTime getOpenTime(CandlestickEntity candle) {
        return candle.getOpenTime().atZone(ZoneId.of("UTC+8"));
    }

    /**
     * 获取K线间隔
     * @param candle K线数据
     * @return K线间隔
     */
    public static String getIntervalVal(CandlestickEntity candle) {
        return candle.getIntervalVal();
    }

    /**
     * 获取交易对
     * @param candle K线数据
     * @return 交易对
     */
    public static String getSymbol(CandlestickEntity candle) {
        return candle.getSymbol();
    }

    /**
     * 设置开盘价
     */
    public static void setOpenPrice(CandlestickEntity candle, BigDecimal price) {
        candle.setOpen(price);
    }

    /**
     * 设置最高价
     */
    public static void setHighPrice(CandlestickEntity candle, BigDecimal price) {
        candle.setHigh(price);
    }

    /**
     * 设置最低价
     */
    public static void setLowPrice(CandlestickEntity candle, BigDecimal price) {
        candle.setLow(price);
    }

    /**
     * 设置收盘价
     */
    public static void setClosePrice(CandlestickEntity candle, BigDecimal price) {
        candle.setClose(price);
    }

    /**
     * 设置间隔值
     */
    public static void setInterval(CandlestickEntity candle, String interval) {
        candle.setIntervalVal(interval);
    }
}
