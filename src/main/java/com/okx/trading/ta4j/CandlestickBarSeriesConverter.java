package com.okx.trading.ta4j;

import com.okx.trading.model.entity.CandlestickEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 蜡烛图数据转换器
 * 将CandlestickEntity列表转换为Ta4j的BarSeries
 */
@Component
@Service
public class CandlestickBarSeriesConverter {

    private static final Logger log = LoggerFactory.getLogger(CandlestickBarSeriesConverter.class);

    /**
     * 将蜡烛图数据列表转换为Ta4j的BarSeries
     * @param candlesticks 蜡烛图数据列表
     * @param seriesName 数据系列名称
     * @return Ta4j的BarSeries
     */
    public BarSeries convert(List<CandlestickEntity> candlesticks, String seriesName) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            log.warn("传入的蜡烛图数据为空，无法转换为BarSeries");
            return new BaseBarSeries(seriesName);
        }

        // 按时间排序
        List<CandlestickEntity> sortedCandles = candlesticks.stream()
                .sorted(Comparator.comparing(CandlestickEntity::getOpenTime))
                .collect(Collectors.toList());

        // 创建BarSeries
        BaseBarSeries series = new BaseBarSeries(seriesName);

        // 确定K线时间间隔
        Duration barDuration = determineBarDuration(sortedCandles);

        // 转换每个蜡烛图数据为Bar对象并添加到series中
        for (CandlestickEntity candle : sortedCandles) {
            try {
                Bar bar = convertToBar(candle, barDuration);
                series.addBar(bar);
            } catch (Exception e) {
                log.error("转换蜡烛图数据时发生错误: {}", e.getMessage(), e);
            }
        }

        return series;
    }

    /**
     * 根据一组蜡烛图数据确定时间间隔
     * @param candlesticks 蜡烛图数据列表
     * @return 时间间隔
     */
    private Duration determineBarDuration(List<CandlestickEntity> candlesticks) {
        if (candlesticks.size() < 2) {
            // 默认使用1分钟
            return Duration.ofMinutes(1);
        }

        // 获取第一个和第二个K线的时间差来确定间隔
        Duration diff = Duration.between(
            candlesticks.get(0).getOpenTime(),
            candlesticks.get(1).getOpenTime()
        );

        // 如果计算出的间隔为0或负值，则使用默认值
        if (diff.isZero() || diff.isNegative()) {
            return Duration.ofMinutes(1);
        }

        return diff;
    }

    /**
     * 将单个蜡烛图数据转换为Ta4j的Bar对象
     * @param candle 蜡烛图数据
     * @param duration 时间间隔
     * @return Ta4j的Bar对象
     */
    private Bar convertToBar(CandlestickEntity candle, Duration duration) {
        // 获取ZonedDateTime
        ZonedDateTime dateTime = candle.getOpenTime().atZone(ZoneId.systemDefault());

        // 获取价格和成交量数据
        Num openPrice = CandlestickAdapter.getOpen(candle);
        Num highPrice = CandlestickAdapter.getHigh(candle);
        Num lowPrice = CandlestickAdapter.getLow(candle);
        Num closePrice = CandlestickAdapter.getClose(candle);
        BigDecimal volume = candle.getVolume();

        // 创建并返回Bar对象
        return BaseBar.builder()
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(DecimalNum.valueOf(volume != null ? volume : BigDecimal.ZERO))
                .timePeriod(duration)
                .endTime(dateTime)
                .build();
    }

    /**
     * 根据交易对和时间间隔创建BarSeries名称
     * @param symbol 交易对
     * @param interval 时间间隔
     * @return BarSeries名称
     */
    public static String createSeriesName(String symbol, String interval) {
        return symbol + "_" + interval;
    }
}
