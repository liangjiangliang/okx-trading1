package com.okx.trading.ta4j;

import com.okx.trading.adapter.CandlestickBarSeriesAdapter;
import com.okx.trading.model.entity.CandlestickEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * K线数据转换适配器测试类
 */
public class CandlestickBarSeriesAdapterTest {

    private CandlestickBarSeriesAdapter adapter;
    private List<CandlestickEntity> mockCandlesticks;

    @BeforeEach
    public void setUp() {
        adapter = new CandlestickBarSeriesAdapter();
        mockCandlesticks = createMockCandlesticks();
    }

    @Test
    public void testConvert_WithValidCandlesticks_ShouldReturnValidBarSeries() {
        // 执行
        BarSeries series = adapter.convert(mockCandlesticks, "BTC-USDT_1H");

        // 验证
        assertNotNull(series);
        assertEquals("BTC-USDT_1H", series.getName());
        assertEquals(20, series.getBarCount());

        // 验证价格转换正确
        assertEquals(mockCandlesticks.get(0).getOpen().doubleValue(),
                     series.getBar(0).getOpenPrice().doubleValue(), 0.001);
        assertEquals(mockCandlesticks.get(0).getHigh().doubleValue(),
                     series.getBar(0).getHighPrice().doubleValue(), 0.001);
        assertEquals(mockCandlesticks.get(0).getLow().doubleValue(),
                     series.getBar(0).getLowPrice().doubleValue(), 0.001);
        assertEquals(mockCandlesticks.get(0).getClose().doubleValue(),
                     series.getBar(0).getClosePrice().doubleValue(), 0.001);
        assertEquals(mockCandlesticks.get(0).getVolume().doubleValue(),
                     series.getBar(0).getVolume().doubleValue(), 0.001);
    }

    @Test
    public void testConvert_WithNullCandlesticks_ShouldReturnEmptyBarSeries() {
        // 执行
        BarSeries series = adapter.convert(null, "BTC-USDT_1H");

        // 验证
        assertNotNull(series);
        assertEquals(0, series.getBarCount());
    }

    @Test
    public void testConvert_WithEmptyCandlesticks_ShouldReturnEmptyBarSeries() {
        // 执行
        BarSeries series = adapter.convert(new ArrayList<>(), "BTC-USDT_1H");

        // 验证
        assertNotNull(series);
        assertEquals(0, series.getBarCount());
    }

    @Test
    public void testConvert_WithNullPrices_ShouldSkipInvalidCandles() {
        // 准备 - 创建一个包含空价格的K线
        CandlestickEntity invalidCandle = mock(CandlestickEntity.class);
        when(invalidCandle.getOpenTime()).thenReturn(LocalDateTime.now());
        when(invalidCandle.getOpen()).thenReturn(null);

        List<CandlestickEntity> candlesWithNull = new ArrayList<>(mockCandlesticks);
        candlesWithNull.add(0, invalidCandle); // 添加到列表开头

        // 执行
        BarSeries series = adapter.convert(candlesWithNull, "BTC-USDT_1H");

        // 验证 - 应该跳过无效的K线
        assertEquals(20, series.getBarCount());
    }

    /**
     * 创建模拟的K线数据
     */
    private List<CandlestickEntity> createMockCandlesticks() {
        List<CandlestickEntity> candlesticks = new ArrayList<>();

        // 创建20条模拟K线数据
        LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        BigDecimal basePrice = new BigDecimal("30000.00");

        for (int i = 0; i < 20; i++) {
            // 创建模拟的K线数据
            CandlestickEntity mockCandle = mock(CandlestickEntity.class);

            LocalDateTime openTime = startTime.plusHours(i);

            // 模拟价格波动
            double variation = Math.sin(i * 0.1) * 500 + (Math.random() - 0.5) * 200;
            BigDecimal open = basePrice.add(new BigDecimal(variation));
            BigDecimal high = open.add(new BigDecimal(Math.random() * 100));
            BigDecimal low = open.subtract(new BigDecimal(Math.random() * 100));
            BigDecimal close = high.add(low).divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal volume = new BigDecimal(Math.random() * 100 + 10);

            // 设置mock的返回值
            when(mockCandle.getOpenTime()).thenReturn(openTime);
            when(mockCandle.getOpen()).thenReturn(open);
            when(mockCandle.getHigh()).thenReturn(high);
            when(mockCandle.getLow()).thenReturn(low);
            when(mockCandle.getClose()).thenReturn(close);
            when(mockCandle.getVolume()).thenReturn(volume);

            candlesticks.add(mockCandle);
        }

        return candlesticks;
    }
}
