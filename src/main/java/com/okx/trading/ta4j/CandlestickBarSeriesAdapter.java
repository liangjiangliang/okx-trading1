package com.okx.trading.ta4j;

import com.okx.trading.model.entity.CandlestickEntity;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
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
     * @param name 序列名称
     * @return Ta4j的BarSeries
     */
    public BarSeries convert(List<CandlestickEntity> candlesticks, String name) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            return new BaseBarSeries(name);
        }

        // 按开盘时间排序
        List<CandlestickEntity> sortedCandles = candlesticks.stream()
                .sorted((c1, c2) -> c1.getOpenTime().compareTo(c2.getOpenTime()))
                .collect(Collectors.toList());

        BaseBarSeries series = new BaseBarSeries(name);

        for (CandlestickEntity candle : sortedCandles) {
            // 检查价格是否为空
            if (candle == null || candle.getOpen() == null || 
                candle.getHigh() == null || 
                candle.getLow() == null || 
                candle.getClose() == null) {
                continue;
            }

            // 获取时间
            ZonedDateTime dateTime = ZonedDateTime.of(
                candle.getOpenTime(), 
                ZoneId.systemDefault()
            );

            // 创建价格对象
            Num openPrice = DecimalNum.valueOf(candle.getOpen());
            Num highPrice = DecimalNum.valueOf(candle.getHigh());
            Num lowPrice = DecimalNum.valueOf(candle.getLow());
            Num closePrice = DecimalNum.valueOf(candle.getClose());
            
            // 处理交易量，如果为空则使用0
            Num volume = candle.getVolume() != null ? 
                DecimalNum.valueOf(candle.getVolume()) : 
                DecimalNum.valueOf(BigDecimal.ZERO);

            // 创建Bar对象
            Bar bar = BaseBar.builder()
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(volume)
                .endTime(dateTime)
                .build();

            series.addBar(bar);
        }

        return series;
    }
} 