package com.okx.trading.ta4j;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import com.okx.trading.model.entity.CandlestickEntity;

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
            mockCandlesticks.add(mock(CandlestickEntity.class));
        }
        
        // 创建一个模拟的BarSeries
        mockBarSeries = createMockBarSeries();
        
        // 设置当converter.convert被调用时返回mockBarSeries
        when(converter.convert(anyList(), anyString())).thenReturn(mockBarSeries);
    }

    @Test
    public void testBacktest_WithValidStrategy_ShouldReturnMetrics() {
        // 准备
        Strategy strategy = createMockSMAStrategy(mockBarSeries);
        BigDecimal initialAmount = new BigDecimal("10000");

        // 执行
        Map<String, Object> result = ta4jBacktestService.backtest(mockCandlesticks, strategy, initialAmount);

        // 验证
        assertNotNull(result);
        assertTrue(result.containsKey("totalProfit"));
        assertTrue(result.containsKey("totalReturn"));
        assertTrue(result.containsKey("maxDrawdown"));
        assertTrue(result.containsKey("sharpeRatio"));
        assertTrue(result.containsKey("tradeCount"));
        assertTrue(result.containsKey("symbol"));
        assertTrue(result.containsKey("interval"));
        assertTrue(result.containsKey("dataPoints"));
        assertTrue(result.containsKey("startTime"));
        assertTrue(result.containsKey("endTime"));
        
        // 验证converter被调用
        verify(converter).convert(eq(mockCandlesticks), anyString());
    }

    @Test
    public void testBacktest_WithEmptyCandlesticks_ShouldThrowException() {
        // 准备
        Strategy strategy = createMockSMAStrategy(mockBarSeries);
        BigDecimal initialAmount = new BigDecimal("10000");

        // 执行和验证
        assertThrows(IllegalArgumentException.class, () -> {
            ta4jBacktestService.backtest(new ArrayList<>(), strategy, initialAmount);
        });
    }

    @Test
    public void testCreateBollingerBandsStrategy_ShouldCreateValidStrategy() {
        // 执行
        Strategy strategy = ta4jBacktestService.createBollingerBandsStrategy(mockBarSeries, 20, 2.0);

        // 验证
        assertNotNull(strategy);
        assertTrue(strategy instanceof BaseStrategy);
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
            BigDecimal close = high.add(low).divide(new BigDecimal("2"));
            BigDecimal volume = new BigDecimal(Math.random() * 10 + 1).multiply(new BigDecimal("10"));
            
            // 创建Bar
            Bar bar = BaseBar.builder()
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

    /**
     * 创建模拟的SMA策略
     * 
     * @param series Bar序列
     * @return 策略对象
     */
    private Strategy createMockSMAStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        SMAIndicator longSma = new SMAIndicator(closePrice, 20);
        
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma);
        
        return new BaseStrategy(entryRule, exitRule);
    }
} 