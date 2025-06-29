package com.okx.trading.strategy;

import com.okx.trading.model.entity.CandlestickEntity;
import org.junit.Test;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * BacktestMetricsCalculator 测试类
 * 验证不同K线周期下年化指标的正确性
 */
public class BacktestMetricsCalculatorTest {

    /**
     * 测试不同K线周期的年化因子检测
     */
    @Test
    public void testAnnualizationFactorDetection() {
        // 测试1分钟K线
        BarSeries series1m = createTestBarSeries("1m", 1000);
        BacktestMetricsCalculator calculator1m = new BacktestMetricsCalculator(
            series1m, createSimpleStrategy(series1m), new BigDecimal("100000"), 
            "TEST", "1分钟测试", new BigDecimal("0.001"), "1m", createBenchmarkData()
        );
        
        // 测试1小时K线
        BarSeries series1h = createTestBarSeries("1H", 1000);
        BacktestMetricsCalculator calculator1h = new BacktestMetricsCalculator(
            series1h, createSimpleStrategy(series1h), new BigDecimal("100000"), 
            "TEST", "1小时测试", new BigDecimal("0.001"), "1H", createBenchmarkData()
        );
        
        // 测试1天K线
        BarSeries series1d = createTestBarSeries("1D", 365);
        BacktestMetricsCalculator calculator1d = new BacktestMetricsCalculator(
            series1d, createSimpleStrategy(series1d), new BigDecimal("100000"), 
            "TEST", "1天测试", new BigDecimal("0.001"), "1D", createBenchmarkData()
        );
        
        // 验证结果不为null
        assertNotNull("1分钟K线计算结果不应为null", calculator1m.getResult());
        assertNotNull("1小时K线计算结果不应为null", calculator1h.getResult());
        assertNotNull("1天K线计算结果不应为null", calculator1d.getResult());
        
        // 验证所有结果都成功
        assertTrue("1分钟K线计算应该成功", calculator1m.getResult().isSuccess());
        assertTrue("1小时K线计算应该成功", calculator1h.getResult().isSuccess());
        assertTrue("1天K线计算应该成功", calculator1d.getResult().isSuccess());
        
        // 验证年化指标被正确计算
        assertNotNull("1分钟K线夏普比率不应为null", calculator1m.getResult().getSharpeRatio());
        assertNotNull("1小时K线夏普比率不应为null", calculator1h.getResult().getSharpeRatio());
        assertNotNull("1天K线夏普比率不应为null", calculator1d.getResult().getSharpeRatio());
        
        assertNotNull("1分钟K线Treynor比率不应为null", calculator1m.getResult().getTreynorRatio());
        assertNotNull("1小时K线Treynor比率不应为null", calculator1h.getResult().getTreynorRatio());
        assertNotNull("1天K线Treynor比率不应为null", calculator1d.getResult().getTreynorRatio());
    }

    /**
     * 测试年化收益率计算的一致性
     */
    @Test
    public void testAnnualizedReturnConsistency() {
        // 创建相同收益率但不同周期的数据
        BarSeries series1h = createTestBarSeries("1H", 8760); // 1年的小时数据
        BarSeries series1d = createTestBarSeries("1D", 365);  // 1年的日数据
        
        BacktestMetricsCalculator calculator1h = new BacktestMetricsCalculator(
            series1h, createSimpleStrategy(series1h), new BigDecimal("100000"), 
            "TEST", "1小时测试", new BigDecimal("0.001"), "1H", createBenchmarkData()
        );
        
        BacktestMetricsCalculator calculator1d = new BacktestMetricsCalculator(
            series1d, createSimpleStrategy(series1d), new BigDecimal("100000"), 
            "TEST", "1天测试", new BigDecimal("0.001"), "1D", createBenchmarkData()
        );
        
        // 验证年化收益率的数量级是合理的
        BigDecimal annualizedReturn1h = calculator1h.getResult().getAnnualizedReturn();
        BigDecimal annualizedReturn1d = calculator1d.getResult().getAnnualizedReturn();
        
        assertNotNull("1小时年化收益率不应为null", annualizedReturn1h);
        assertNotNull("1天年化收益率不应为null", annualizedReturn1d);
        
        // 年化收益率应该在合理范围内 (-100% 到 1000%)
        assertTrue("1小时年化收益率应该在合理范围内", 
                   annualizedReturn1h.compareTo(new BigDecimal("-1.0")) >= 0 && 
                   annualizedReturn1h.compareTo(new BigDecimal("10.0")) <= 0);
        
        assertTrue("1天年化收益率应该在合理范围内", 
                   annualizedReturn1d.compareTo(new BigDecimal("-1.0")) >= 0 && 
                   annualizedReturn1d.compareTo(new BigDecimal("10.0")) <= 0);
    }

    /**
     * 创建测试用的BarSeries
     */
    private BarSeries createTestBarSeries(String interval, int barCount) {
        BarSeries series = new BaseBarSeries("TEST-USDT");
        LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        
        double price = 100.0;
        for (int i = 0; i < barCount; i++) {
            // 模拟价格波动
            price += (Math.random() - 0.5) * 2; // ±1的随机波动
            price = Math.max(price, 50.0); // 最低价格50
            
            LocalDateTime barTime = getNextBarTime(startTime, interval, i);
            
            Bar bar = BaseBar.builder()
                .timePeriod(java.time.Duration.ofMinutes(getIntervalMinutes(interval)))
                .endTime(barTime.atZone(ZoneId.systemDefault()))
                .openPrice(DecimalNum.valueOf(price))
                .highPrice(DecimalNum.valueOf(price + Math.random()))
                .lowPrice(DecimalNum.valueOf(price - Math.random()))
                .closePrice(DecimalNum.valueOf(price))
                .volume(DecimalNum.valueOf(1000))
                .build();
            
            series.addBar(bar);
        }
        
        return series;
    }

    /**
     * 获取下一个Bar的时间
     */
    private LocalDateTime getNextBarTime(LocalDateTime startTime, String interval, int index) {
        switch (interval) {
            case "1m":
                return startTime.plusMinutes(index);
            case "1H":
                return startTime.plusHours(index);
            case "1D":
                return startTime.plusDays(index);
            default:
                return startTime.plusMinutes(index);
        }
    }

    /**
     * 获取间隔分钟数
     */
    private int getIntervalMinutes(String interval) {
        switch (interval) {
            case "1m": return 1;
            case "1H": return 60;
            case "1D": return 1440;
            default: return 1;
        }
    }

    /**
     * 创建简单的买入持有策略
     */
    private TradingRecord createSimpleStrategy(BarSeries series) {
        TradingRecord record = new BaseTradingRecord();
        
        // 在第10个bar买入，在最后一个bar卖出
        if (series.getBarCount() > 20) {
            record.enter(10, DecimalNum.valueOf(series.getBar(10).getClosePrice().doubleValue()), DecimalNum.valueOf(100));
            record.exit(series.getBarCount() - 1, DecimalNum.valueOf(series.getBar(series.getBarCount() - 1).getClosePrice().doubleValue()), DecimalNum.valueOf(100));
        }
        
        return record;
    }

    /**
     * 创建基准数据
     */
    private List<CandlestickEntity> createBenchmarkData() {
        List<CandlestickEntity> benchmark = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        
        double price = 100.0;
        for (int i = 0; i < 365; i++) {
            CandlestickEntity candle = new CandlestickEntity();
            candle.setOpen(new BigDecimal(price));
            candle.setHigh(new BigDecimal(price + 1));
            candle.setLow(new BigDecimal(price - 1));
            candle.setClose(new BigDecimal(price));
            candle.setVolume(new BigDecimal("1000"));
            candle.setOpenTime(startTime.plusDays(i));
            candle.setCloseTime(startTime.plusDays(i + 1));
            benchmark.add(candle);
            
            price += (Math.random() - 0.5) * 0.5; // 基准更稳定的波动
        }
        
        return benchmark;
    }
} 