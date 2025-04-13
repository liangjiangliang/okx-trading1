package com.okx.trading.ta4j;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ta4j回测服务测试类
 */
@ExtendWith(MockitoExtension.class)
public class Ta4jBacktestServiceTest {

    @InjectMocks
    private Ta4jBacktestService ta4jBacktestService;

    @Mock
    private CandlestickBarSeriesConverter converter;

    private List<CandlestickEntity> mockCandlesticks;
    private BarSeries mockBarSeries;

    @BeforeEach
    public void setUp() {
        // 创建模拟数据
        mockCandlesticks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            CandlestickEntity candlestick = mock(CandlestickEntity.class);
            // 设置必要的mock行为
            when(CandlestickAdapter.getSymbol(candlestick)).thenReturn("BTC-USDT");
            when(CandlestickAdapter.getIntervalVal(candlestick)).thenReturn("1h");
            mockCandlesticks.add(candlestick);
        }

        // 创建一个模拟的BarSeries
        mockBarSeries = createMockBarSeries();

        // 设置当converter.convert被调用时返回mockBarSeries
        when(converter.convert(anyList(), anyString())).thenReturn(mockBarSeries);
    }

    @Test
    public void testBacktest_WithSMAStrategy_ShouldReturnResultDTO() {
        // 准备
        BigDecimal initialAmount = new BigDecimal("10000");
        String params = "5,20"; // 短期SMA=5, 长期SMA=20

        // 执行
        BacktestResultDTO result = ta4jBacktestService.backtest(
                mockCandlesticks, 
                Ta4jBacktestService.STRATEGY_SMA, 
                initialAmount, 
                params);

        // 验证
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(initialAmount, result.getInitialAmount());
        assertNotNull(result.getTotalProfit());
        assertNotNull(result.getTotalReturn());
        assertNotNull(result.getWinRate());
        assertNotNull(result.getTrades());
        assertEquals(Ta4jBacktestService.STRATEGY_SMA, result.getStrategyName());

        // 验证converter被调用
        verify(converter).convert(eq(mockCandlesticks), anyString());
    }

    @Test
    public void testBacktest_WithBollingerBandsStrategy_ShouldReturnResultDTO() {
        // 准备
        BigDecimal initialAmount = new BigDecimal("10000");
        String params = "20,2.0"; // period=20, multiplier=2.0

        // 执行
        BacktestResultDTO result = ta4jBacktestService.backtest(
                mockCandlesticks, 
                Ta4jBacktestService.STRATEGY_BOLLINGER_BANDS, 
                initialAmount, 
                params);

        // 验证
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(initialAmount, result.getInitialAmount());
        assertNotNull(result.getTotalProfit());
        assertNotNull(result.getTotalReturn());
        assertNotNull(result.getWinRate());
        assertNotNull(result.getTrades());
        assertEquals(Ta4jBacktestService.STRATEGY_BOLLINGER_BANDS, result.getStrategyName());

        // 验证converter被调用
        verify(converter).convert(eq(mockCandlesticks), anyString());
    }

    @Test
    public void testBacktest_WithEmptyCandlesticks_ShouldReturnErrorResultDTO() {
        // 准备
        BigDecimal initialAmount = new BigDecimal("10000");
        String params = "5,20";

        // 执行
        BacktestResultDTO result = ta4jBacktestService.backtest(
                new ArrayList<>(), 
                Ta4jBacktestService.STRATEGY_SMA, 
                initialAmount, 
                params);

        // 验证
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    public void testBacktest_WithInvalidStrategyType_ShouldReturnErrorResultDTO() {
        // 准备
        BigDecimal initialAmount = new BigDecimal("10000");
        String params = "5,20";

        // 执行
        BacktestResultDTO result = ta4jBacktestService.backtest(
                mockCandlesticks, 
                "INVALID_STRATEGY", 
                initialAmount, 
                params);

        // 验证
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    /**
     * 创建模拟的BarSeries数据
     *
     * @return 模拟的BarSeries
     */
    private BarSeries createMockBarSeries() {
        BaseBarSeries series = new BaseBarSeries("BTC-USDT_1H");

        ZonedDateTime startTime = ZonedDateTime.now().minusDays(50);
        BigDecimal basePrice = new BigDecimal("30000.00");

        for (int i = 0; i < 50; i++) {
            double variation = Math.sin(i * 0.1) * 500 + (Math.random() - 0.5) * 200;
            BigDecimal currentPrice = basePrice.add(new BigDecimal(variation));

            // 设置OHLC
            BigDecimal open = currentPrice;
            BigDecimal high = open.add(new BigDecimal(Math.random() * 100));
            BigDecimal low = open.subtract(new BigDecimal(Math.random() * 100));
            BigDecimal close = high.add(low).divide(new BigDecimal("2"), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal volume = new BigDecimal(Math.random() * 10 + 1).multiply(new BigDecimal("10"));

            // 创建Bar
            Bar bar = BaseBar.builder()
                .timePeriod(Duration.ofHours(1))
                .openPrice(DecimalNum.valueOf(open))
                .highPrice(DecimalNum.valueOf(high))
                .lowPrice(DecimalNum.valueOf(low))
                .closePrice(DecimalNum.valueOf(close))
                .volume(DecimalNum.valueOf(volume))
                .endTime(startTime.plusHours(i))
                .build();

            series.addBar(bar);
        }

        return series;
    }
}
