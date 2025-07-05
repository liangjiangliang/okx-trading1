package com.okx.trading.adapter;

import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.util.DateTimeUtil;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * K线数据与Ta4j之间的适配器
 * 用于解决CandlestickEntity字段命名不一致的问题
 */
@Component
public class CandlestickBarSeriesAdapter {

    /**
     * 将CandlestickEntity列表转换为Ta4j的BarSeries
     *
     * @param candlesticks K线数据列表
     * @param name         序列名称
     * @return Ta4j的BarSeries
     */
    public BarSeries convertToBarSeries(List<CandlestickEntity> candlesticks, String name) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            return new BaseBarSeriesBuilder().withName(name).build();
        }

        List<Bar> bars = candlesticks.stream()
                .map(candlestick -> convertToBar(candlestick, Duration.ofMinutes(1)))
                .collect(Collectors.toList());

        return new BaseBarSeriesBuilder().withName(name).withBars(bars).build();
    }

    /**
     * 将CandlestickEntity转换为Ta4j的Bar
     *
     * @param candlestick K线数据
     * @param duration    时间周期
     * @return Ta4j的Bar
     */
    public Bar convertToBar(CandlestickEntity candlestick, Duration duration) {
        // 使用毫秒时间戳创建ZonedDateTime
        ZonedDateTime endTime = DateTimeUtil.timestampToZonedDateTime(candlestick.getTime());

        // 使用Ta4j 0.18版本的BaseBar构造函数
        return new BaseBar(
                duration,
                endTime.toInstant(),
                DecimalNum.valueOf(candlestick.getOpen()),
                DecimalNum.valueOf(candlestick.getHigh()),
                DecimalNum.valueOf(candlestick.getLow()),
                DecimalNum.valueOf(candlestick.getClose()),
                DecimalNum.valueOf(candlestick.getVolume()),
                DecimalNum.valueOf(BigDecimal.ZERO), // 默认成交额为0
                0L // 交易次数，默认为0
        );
    }
}
