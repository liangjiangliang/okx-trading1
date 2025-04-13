package com.okx.trading.util;

import com.okx.trading.model.entity.CandlestickEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 回测数据生成器测试类
 */
class BacktestDataGeneratorTest {

    @Test
    void testGenerateCandlestickDataUptrend() {
        // 上升趋势数据测试
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        BigDecimal startPrice = new BigDecimal("40000");
        
        List<CandlestickEntity> candlesticks = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "1h", startTime, endTime,
                BacktestDataGenerator.TrendType.UPTREND, startPrice);
        
        // 验证数据生成
        assertNotNull(candlesticks);
        assertFalse(candlesticks.isEmpty());
        
        // 计算小时数并验证生成的数据点数量（可能会有少许偏差）
        long expectedDataPoints = startTime.until(endTime, java.time.temporal.ChronoUnit.HOURS) + 1;
        assertTrue(Math.abs(expectedDataPoints - candlesticks.size()) <= 3, 
                "预期数据点数量应接近 " + expectedDataPoints + "，实际是 " + candlesticks.size());
        
        // 验证是否是上升趋势
        BigDecimal firstPrice = candlesticks.get(0).getClose();
        BigDecimal lastPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        BigDecimal priceChange = lastPrice.subtract(firstPrice);
        
        // 上升趋势应该整体呈现价格上涨
        assertTrue(priceChange.compareTo(BigDecimal.ZERO) > 0, 
                "上升趋势应该呈现价格上涨，但价格从 " + firstPrice + " 变为 " + lastPrice);
        
        // 验证每个K线的数据格式正确性
        for (CandlestickEntity candlestick : candlesticks) {
            // 符号和间隔
            assertEquals("BTC-USDT", candlestick.getSymbol());
            assertEquals("1h", candlestick.getIntervalVal());
            
            // 时间关系正确
            assertTrue(candlestick.getCloseTime().isAfter(candlestick.getOpenTime()));
            assertTrue(candlestick.getOpenTime().isAfter(startTime.minusHours(1)) ||
                       candlestick.getOpenTime().equals(startTime));
            assertTrue(candlestick.getCloseTime().isBefore(endTime.plusHours(2)));
            
            // 价格关系正确
            assertTrue(candlestick.getHigh().compareTo(candlestick.getLow()) >= 0);
            assertTrue(candlestick.getHigh().compareTo(candlestick.getOpen()) >= 0);
            assertTrue(candlestick.getHigh().compareTo(candlestick.getClose()) >= 0);
            assertTrue(candlestick.getLow().compareTo(candlestick.getOpen()) <= 0);
            assertTrue(candlestick.getLow().compareTo(candlestick.getClose()) <= 0);
            
            // 成交量和交易次数合理
            assertTrue(candlestick.getVolume().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(candlestick.getTrades() > 0);
        }
    }
    
    @Test
    void testGenerateCandlestickDataDowntrend() {
        // 下降趋势数据测试
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        BigDecimal startPrice = new BigDecimal("40000");
        
        List<CandlestickEntity> candlesticks = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "1h", startTime, endTime,
                BacktestDataGenerator.TrendType.DOWNTREND, startPrice);
        
        // 验证是否是下降趋势
        BigDecimal firstPrice = candlesticks.get(0).getClose();
        BigDecimal lastPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        BigDecimal priceChange = lastPrice.subtract(firstPrice);
        
        // 下降趋势应该整体呈现价格下跌
        assertTrue(priceChange.compareTo(BigDecimal.ZERO) < 0, 
                "下降趋势应该呈现价格下跌，但价格从 " + firstPrice + " 变为 " + lastPrice);
    }
    
    @Test
    void testGenerateCandlestickDataSideways() {
        // 横盘趋势数据测试
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        BigDecimal startPrice = new BigDecimal("40000");
        
        List<CandlestickEntity> candlesticks = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "1h", startTime, endTime,
                BacktestDataGenerator.TrendType.SIDEWAYS, startPrice);
        
        // 验证是否是横盘趋势 (价格变化不超过起始价格的10%)
        BigDecimal firstPrice = candlesticks.get(0).getClose();
        BigDecimal lastPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        BigDecimal priceChangePercent = lastPrice.subtract(firstPrice)
                .divide(firstPrice, 4, RoundingMode.HALF_UP)
                .abs();
        
        // 横盘趋势应该价格变化不大
        assertTrue(priceChangePercent.compareTo(new BigDecimal("0.1")) < 0, 
                "横盘趋势价格变化应该小于10%，但实际变化为 " + 
                priceChangePercent.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
    }
    
    @Test
    void testGenerateCandlestickDataVolatile() {
        // 波动剧烈趋势数据测试
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        BigDecimal startPrice = new BigDecimal("40000");
        
        List<CandlestickEntity> candlesticks = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "1h", startTime, endTime,
                BacktestDataGenerator.TrendType.VOLATILE, startPrice);
        
        // 计算波动性指标：高低点差值的平均值占起始价格的百分比
        BigDecimal volatilitySum = BigDecimal.ZERO;
        for (CandlestickEntity candlestick : candlesticks) {
            BigDecimal highLowDiff = candlestick.getHigh().subtract(candlestick.getLow());
            BigDecimal volatility = highLowDiff.divide(candlestick.getOpen(), 4, RoundingMode.HALF_UP);
            volatilitySum = volatilitySum.add(volatility);
        }
        
        BigDecimal avgVolatility = volatilitySum.divide(new BigDecimal(candlesticks.size()), 4, RoundingMode.HALF_UP);
        
        // 波动剧烈应该有较高的平均波动率
        assertTrue(avgVolatility.compareTo(new BigDecimal("0.01")) > 0,
                "波动剧烈趋势应该有较高的平均波动率（>1%），但实际为 " + 
                avgVolatility.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
    }
    
    @Test
    void testDifferentTimeIntervals() {
        // 测试不同时间间隔生成的数据点数量
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        BigDecimal startPrice = new BigDecimal("40000");
        
        // 1小时间隔
        List<CandlestickEntity> hourlyData = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "1h", startTime, endTime,
                BacktestDataGenerator.TrendType.UPTREND, startPrice);
        
        // 15分钟间隔
        List<CandlestickEntity> fifteenMinData = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "15m", startTime, endTime,
                BacktestDataGenerator.TrendType.UPTREND, startPrice);
        
        // 1天间隔
        List<CandlestickEntity> dailyData = BacktestDataGenerator.generateCandlestickData(
                "BTC-USDT", "1d", startTime, endTime,
                BacktestDataGenerator.TrendType.UPTREND, startPrice);
        
        // 验证数据点数量与间隔相关
        assertTrue(hourlyData.size() < fifteenMinData.size(),
                "15分钟间隔的数据点数量应该多于1小时间隔");
        assertTrue(dailyData.size() < hourlyData.size(),
                "1天间隔的数据点数量应该少于1小时间隔");
        
        // 验证近似的数据点比例
        double hourToFifteenRatio = (double) hourlyData.size() / fifteenMinData.size();
        assertTrue(Math.abs(hourToFifteenRatio - 0.25) < 0.1,
                "1小时数据与15分钟数据的比例应接近1:4");
    }
} 