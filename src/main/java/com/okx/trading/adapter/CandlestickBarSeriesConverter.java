package com.okx.trading.adapter;

import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.BarBuilder;
import org.ta4j.core.BarBuilderFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

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
     *
     * @param candlesticks 蜡烛图数据列表
     * @param seriesName   数据系列名称
     * @return Ta4j的BarSeries
     */
    public BarSeries convert(List<CandlestickEntity> candlesticks, String seriesName) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            log.warn("传入的蜡烛图数据为空，无法转换为BarSeries");
            // 创建空的BarSeries，适配Ta4j 0.18版本
            List<Bar> emptyBars = new ArrayList<>();
            return new BaseBarSeriesBuilder().withName(seriesName).withBars(emptyBars).build();

        }

        // 按时间排序
        List<CandlestickEntity> sortedCandles = candlesticks.stream().distinct()
                .sorted(Comparator.comparing(CandlestickEntity::getOpenTime))
                .collect(Collectors.toList());

        // 确定K线时间间隔
        Duration barDuration = determineBarDuration(sortedCandles);

        // 转换每个蜡烛图数据为Bar对象
        List<Bar> bars = new ArrayList<>();
        for (CandlestickEntity candle : sortedCandles) {
            try {
                Bar bar = convertToBar(candle, barDuration);
                bars.add(bar);
            } catch (Exception e) {
                log.error("转换蜡烛图数据时发生错误: {}", e.getMessage(), e);
            }
        }

        // 创建BarSeries，适配Ta4j 0.18版本
        return new BaseBarSeriesBuilder().withName(seriesName).withBars(bars).build();
    }

    /**
     * 根据一组蜡烛图数据确定时间间隔
     *
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
     *
     * @param candle   蜡烛图数据
     * @param duration 时间间隔
     * @return Ta4j的Bar对象
     */
    private Bar convertToBar(CandlestickEntity candle, Duration duration) {
        if (candle.getCloseTime() == null) {
            log.warn("K线数据的关闭时间为null，使用开盘时间作为替代");
            ZonedDateTime endTime = candle.getOpenTime().atZone(ZoneId.systemDefault());

            // 获取价格和成交量数据
            Num openPrice = CandlestickAdapter.getOpen(candle);
            Num highPrice = CandlestickAdapter.getHigh(candle);
            Num lowPrice = CandlestickAdapter.getLow(candle);
            Num closePrice = CandlestickAdapter.getClose(candle);
            BigDecimal volume = candle.getVolume();

            return new BaseBar(
                    duration,
                    endTime.toInstant(),
                    openPrice,
                    highPrice,
                    lowPrice,
                    closePrice,
                    DecimalNum.valueOf(volume != null ? volume : BigDecimal.ZERO),
                    DecimalNum.valueOf(BigDecimal.ZERO),
                    0
            );
        }

        // 使用Java 21兼容的方式创建ZonedDateTime
        ZonedDateTime endTime = candle.getCloseTime().atZone(ZoneId.systemDefault());

        // 获取价格和成交量数据
        Num openPrice = CandlestickAdapter.getOpen(candle);
        Num highPrice = CandlestickAdapter.getHigh(candle);
        Num lowPrice = CandlestickAdapter.getLow(candle);
        Num closePrice = CandlestickAdapter.getClose(candle);
        BigDecimal volume = candle.getVolume();

        // 适配Ta4j 0.18版本，不再使用builder模式
        return new BaseBar(
                duration,
                endTime.toInstant(),
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                DecimalNum.valueOf(volume != null ? volume : BigDecimal.ZERO),
                DecimalNum.valueOf(BigDecimal.ZERO),// 默认成交额为0,
                0
        );
    }

    /**
     * 根据交易对和时间间隔创建BarSeries名称
     *
     * @param symbol   交易对
     * @param interval 时间间隔
     * @return BarSeries名称
     */
    public static String createSeriesName(String symbol, String interval) {
        return symbol + "_" + interval;
    }

    /**
     * 将CandlestickEntity列表转换为Ta4j的BarSeries
     *
     * @param candlesticks K线数据列表
     * @param name         序列名称
     * @return Ta4j的BarSeries
     */
    public BarSeries convertToBarSeries(List<CandlestickEntity> candlesticks, String name) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            // 使用Ta4j 0.18版本的BaseBarSeriesBuilder
            return new BaseBarSeriesBuilder().withName(name).build();
        }

        // 按时间戳排序（从旧到新）
        List<CandlestickEntity> sortedCandlesticks = candlesticks.stream()
                .sorted(Comparator.comparing(CandlestickEntity::getTime))
                .collect(Collectors.toList());

        // 计算时间间隔（使用第一个和第二个K线之间的时间差）
        Duration interval = Duration.ofMillis(1);
        if (sortedCandlesticks.size() > 1) {
            long diff = sortedCandlesticks.get(1).getTime() - sortedCandlesticks.get(0).getTime();
            interval = Duration.ofMillis(diff);
        }

        // 创建Bar列表
        List<Bar> bars = new ArrayList<>();

        // 添加Bars
        for (CandlestickEntity candlestick : sortedCandlesticks) {
            // 获取时间戳并转换为毫秒
            long timestamp = candlestick.getTime();

            // 从时间戳创建ZonedDateTime
            ZonedDateTime endTime = DateTimeUtil.timestampToZonedDateTime(timestamp);

            // 使用Ta4j 0.18版本的BaseBar构造函数
            Bar bar = new BaseBar(
                    interval,
                    endTime.toInstant(),
                    DecimalNum.valueOf(candlestick.getOpen()),
                    DecimalNum.valueOf(candlestick.getHigh()),
                    DecimalNum.valueOf(candlestick.getLow()),
                    DecimalNum.valueOf(candlestick.getClose()),
                    DecimalNum.valueOf(candlestick.getVolume()),
                    DecimalNum.valueOf(BigDecimal.ZERO), // 默认成交额为0
                    0L // 交易次数，默认为0
            );
            bars.add(bar);
        }

        // 创建BarSeries，使用Ta4j 0.18版本的BaseBarSeriesBuilder
        return new BaseBarSeriesBuilder().withName(name).withBars(bars).build();
    }
}
