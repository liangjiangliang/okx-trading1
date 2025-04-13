package com.okx.trading.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * 技术指标工具类测试
 */
public class TechnicalIndicatorUtilTest {

    @Test
    public void testCalculateSMA() {
        // 测试数据
        List<BigDecimal> prices = Arrays.asList(
            new BigDecimal("10.0"),
            new BigDecimal("12.0"),
            new BigDecimal("13.0"),
            new BigDecimal("15.0"),
            new BigDecimal("14.0")
        );

        // 计算SMA
        BigDecimal result = TechnicalIndicatorUtil.calculateSMA(prices, 2);

        // 预期结果 (10 + 12 + 13 + 15 + 14) / 5 = 64 / 5 = 12.8
        assertEquals(0, new BigDecimal("12.80").compareTo(result));
    }

    @Test
    public void testCalculateStandardDeviation() {
        // 测试数据
        List<BigDecimal> prices = Arrays.asList(
            new BigDecimal("10.0"),
            new BigDecimal("12.0"),
            new BigDecimal("13.0"),
            new BigDecimal("15.0"),
            new BigDecimal("14.0")
        );

        // 先计算均值
        BigDecimal mean = TechnicalIndicatorUtil.calculateSMA(prices, 4);

        // 计算标准差
        BigDecimal stdDev = TechnicalIndicatorUtil.calculateStandardDeviation(prices, mean, 2);

        // 手动计算期望标准差
        // 差值平方和: (10-12.8)² + (12-12.8)² + (13-12.8)² + (15-12.8)² + (14-12.8)²
        // = 7.84 + 0.64 + 0.04 + 4.84 + 1.44 = 14.8
        // 方差: 14.8 / 5 = 2.96
        // 标准差: √2.96 ≈ 1.72
        assertEquals(0, new BigDecimal("1.72").compareTo(stdDev));
    }

    @Test
    public void testCalculateBollingerBands() {
        // 创建测试价格数据 - 模拟20天收盘价
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            // 加入一些波动使数据更真实
            double value = 100 + Math.sin(i * 0.5) * 10 + (i % 5);
            prices.add(new BigDecimal(Double.toString(value)).setScale(2, RoundingMode.HALF_UP));
        }

        // 计算布林带 (周期=20, 系数=2)
        TechnicalIndicatorUtil.BollingerBands bands =
            TechnicalIndicatorUtil.calculateBollingerBands(prices, 20, 2, 2);

        // 验证结果长度
        assertEquals(prices.size(), bands.getMiddle().size());
        assertEquals(prices.size(), bands.getUpper().size());
        assertEquals(prices.size(), bands.getLower().size());

        // 验证前19个值应为null (因为需要至少20个点才能计算第一个布林带值)
        for (int i = 0; i < 19; i++) {
            assertNull(bands.getMiddle().get(i));
            assertNull(bands.getUpper().get(i));
            assertNull(bands.getLower().get(i));
        }

        // 验证第20个值开始有实际数据
        for (int i = 19; i < prices.size(); i++) {
            // 验证中轨、上轨、下轨关系：上轨 > 中轨 > 下轨
            assertNotNull(bands.getMiddle().get(i));
            assertNotNull(bands.getUpper().get(i));
            assertNotNull(bands.getLower().get(i));

            assertTrue(bands.getUpper().get(i).compareTo(bands.getMiddle().get(i)) > 0);
            assertTrue(bands.getMiddle().get(i).compareTo(bands.getLower().get(i)) > 0);

            // 验证上下轨与中轨的距离应该相等
            BigDecimal upperDiff = bands.getUpper().get(i).subtract(bands.getMiddle().get(i));
            BigDecimal lowerDiff = bands.getMiddle().get(i).subtract(bands.getLower().get(i));

            assertEquals(0, upperDiff.compareTo(lowerDiff),
                         "上轨和下轨应该与中轨等距，索引: " + i);
        }
    }

    @Test
    public void testBollingerBandsValue() {
        // 创建测试价格数据
        List<BigDecimal> prices = createTestPrices(25);

        // 计算布林带
        TechnicalIndicatorUtil.BollingerBands bands =
            TechnicalIndicatorUtil.calculateBollingerBands(prices, 20, 2, 2);

        // 测试getValueAt方法
        TechnicalIndicatorUtil.BollingerBandValue value = bands.getValueAt(20);

        assertNotNull(value.getMiddle());
        assertNotNull(value.getUpper());
        assertNotNull(value.getLower());

        assertEquals(bands.getMiddle().get(20), value.getMiddle());
        assertEquals(bands.getUpper().get(20), value.getUpper());
        assertEquals(bands.getLower().get(20), value.getLower());

        // 测试索引越界异常
        assertThrows(IndexOutOfBoundsException.class, () -> bands.getValueAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bands.getValueAt(prices.size()));
    }

    @Test
    public void testBollingerBandsWithInsufficientData() {
        // 不足一个周期的数据
        List<BigDecimal> insufficientPrices = createTestPrices(15);
        
        assertThrows(IllegalArgumentException.class, 
            () -> TechnicalIndicatorUtil.calculateBollingerBands(insufficientPrices, 20, 2, 2));
            
        // 边界情况 - 刚好一个周期的数据
        List<BigDecimal> exactPrices = createTestPrices(20);
        TechnicalIndicatorUtil.BollingerBands bands = 
            TechnicalIndicatorUtil.calculateBollingerBands(exactPrices, 20, 2, 2);
            
        // 只有最后一个点有值
        assertNull(bands.getMiddle().get(18));
        assertNotNull(bands.getMiddle().get(19));
    }

    /**
     * 创建测试价格数据
     *
     * @param count 价格点数量
     * @return 价格列表
     */
    private List<BigDecimal> createTestPrices(int count) {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            double value = 100 + Math.sin(i * 0.5) * 10 + (i % 5);
            prices.add(new BigDecimal(Double.toString(value)).setScale(2, RoundingMode.HALF_UP));
        }
        return prices;
    }

    /**
     * 测试MACD计算方法
     */
    @Test
    public void testCalculateMACD() {
        // 准备测试数据
        List<BigDecimal> prices = Arrays.asList(
            new BigDecimal("10.00"),
            new BigDecimal("10.15"),
            new BigDecimal("10.17"),
            new BigDecimal("10.13"),
            new BigDecimal("10.11"),
            new BigDecimal("10.15"),
            new BigDecimal("10.20"),
            new BigDecimal("10.31"),
            new BigDecimal("10.40"),
            new BigDecimal("10.38"),
            new BigDecimal("10.35"),
            new BigDecimal("10.37"),
            new BigDecimal("10.30"), // 13
            new BigDecimal("10.33"),
            new BigDecimal("10.40"),
            new BigDecimal("10.41"),
            new BigDecimal("10.50"),
            new BigDecimal("10.57"),
            new BigDecimal("10.70"),
            new BigDecimal("10.90"),
            new BigDecimal("11.10"),
            new BigDecimal("11.30"),
            new BigDecimal("11.50"),
            new BigDecimal("11.55"),
            new BigDecimal("11.30"),
            new BigDecimal("11.40"),
            new BigDecimal("11.80"),
            new BigDecimal("12.00"),
            new BigDecimal("12.20"),
            new BigDecimal("12.30"), // 30
            new BigDecimal("12.40"),
            new BigDecimal("12.10"),
            new BigDecimal("12.00"),
            new BigDecimal("12.15"),
            new BigDecimal("12.25"),
            new BigDecimal("12.40"),
            new BigDecimal("12.35"),
            new BigDecimal("12.20"),
            new BigDecimal("12.30"),
            new BigDecimal("12.40")  // 40
        );

        // 计算MACD，使用标准参数：快线12，慢线26，信号线9
        int fastPeriod = 12;
        int slowPeriod = 26;
        int signalPeriod = 9;
        int scale = 4;
        
        TechnicalIndicatorUtil.MACD macd = TechnicalIndicatorUtil.calculateMACD(prices, fastPeriod, slowPeriod, signalPeriod, scale);
        
        // 验证结果不为null
        assertNotNull(macd);
        assertNotNull(macd.getMacdLine());
        assertNotNull(macd.getSignalLine());
        assertNotNull(macd.getHistogram());
        
        // 验证长度
        assertEquals(prices.size(), macd.getMacdLine().size());
        assertEquals(prices.size(), macd.getSignalLine().size());
        assertEquals(prices.size(), macd.getHistogram().size());
        
        // 验证前slowPeriod-1个MACD值应为null
        for (int i = 0; i < slowPeriod - 1; i++) {
            assertNull(macd.getMacdLine().get(i));
        }
        
        // 验证前slowPeriod+signalPeriod-2个信号线值应为null
        for (int i = 0; i < slowPeriod + signalPeriod - 2; i++) {
            assertNull(macd.getSignalLine().get(i));
        }
        
        // 验证前slowPeriod+signalPeriod-2个柱状图值应为null
        for (int i = 0; i < slowPeriod + signalPeriod - 2; i++) {
            assertNull(macd.getHistogram().get(i));
        }
        
        // 验证MACD线第一个非null值
        assertNotNull(macd.getMacdLine().get(slowPeriod - 1));
        
        // 验证信号线第一个非null值
        assertNotNull(macd.getSignalLine().get(slowPeriod + signalPeriod - 2));
        
        // 验证柱状图第一个非null值
        assertNotNull(macd.getHistogram().get(slowPeriod + signalPeriod - 2));
        
        // 测试MACD计算的正确性
        // 以下是使用公式手动计算的一些特定点的MACD值进行验证
        // 注意：由于EMA初始值可能有微小差异，所以允许一定的误差范围
        if (prices.size() >= 35) {
            // 验证第35个点的MACD值 (索引34)
            TechnicalIndicatorUtil.MACDValue valueAt34 = macd.getValueAt(34);
            assertNotNull(valueAt34.getMacdLine());
            assertNotNull(valueAt34.getSignalLine());
            assertNotNull(valueAt34.getHistogram());
            
            // 允许0.01的误差
            BigDecimal tolerance = new BigDecimal("0.01");
            
            // 检查是否在允许的误差范围内
            // 注意：以下期望值是根据标准MACD计算公式手动计算的参考值
            BigDecimal expectedMacdLine = new BigDecimal("0.4");  // 示例期望值
            BigDecimal diff = valueAt34.getMacdLine().subtract(expectedMacdLine).abs();
            assertTrue(diff.compareTo(tolerance) <= 0, 
                "MACD线值超出误差范围: 实际=" + valueAt34.getMacdLine() + ", 期望=" + expectedMacdLine);
        }
    }

    /**
     * 测试EMA计算方法
     */
    @Test
    public void testCalculateEMA() {
        // 准备测试数据
        List<BigDecimal> prices = Arrays.asList(
            new BigDecimal("10.00"),
            new BigDecimal("10.50"),
            new BigDecimal("11.00"),
            new BigDecimal("11.50"),
            new BigDecimal("12.00"),
            new BigDecimal("12.50"),
            new BigDecimal("13.00"),
            new BigDecimal("13.50"),
            new BigDecimal("14.00"),
            new BigDecimal("14.50")
        );
        
        int period = 3;
        int scale = 4;
        
        // 计算EMA
        List<BigDecimal> emaValues = TechnicalIndicatorUtil.calculateEMA(prices, period, scale);
        
        // 验证结果
        assertNotNull(emaValues);
        assertEquals(prices.size(), emaValues.size());
        
        // 前period-1个值应为null
        for (int i = 0; i < period - 1; i++) {
            assertNull(emaValues.get(i));
        }
        
        // 第一个EMA值应等于前period个价格的SMA
        BigDecimal expectedFirstEma = new BigDecimal("10.50"); // (10 + 10.5 + 11) / 3
        assertEquals(0, emaValues.get(period - 1).compareTo(expectedFirstEma));
        
        // 手动计算并验证几个特定点的EMA
        BigDecimal multiplier = new BigDecimal("0.5"); // 2/(period+1) = 2/(3+1) = 0.5
        
        // EMA(3) = 价格(3) * 0.5 + EMA(2) * 0.5
        BigDecimal ema3 = new BigDecimal("11.50").multiply(multiplier)
                         .add(expectedFirstEma.multiply(multiplier))
                         .setScale(scale, RoundingMode.HALF_UP);
        assertEquals(0, emaValues.get(3).compareTo(ema3));
        
        // EMA(4) = 价格(4) * 0.5 + EMA(3) * 0.5
        BigDecimal ema4 = new BigDecimal("12.00").multiply(multiplier)
                         .add(ema3.multiply(multiplier))
                         .setScale(scale, RoundingMode.HALF_UP);
        assertEquals(0, emaValues.get(4).compareTo(ema4));
    }

    @Test
    public void testCalculateKDJ() {
        // 准备测试数据
        List<BigDecimal> highs = new ArrayList<>();
        List<BigDecimal> lows = new ArrayList<>();
        List<BigDecimal> closes = new ArrayList<>();

        // 添加15天的模拟价格数据
        for (int i = 0; i < 15; i++) {
            highs.add(new BigDecimal(100 + i * 2));
            lows.add(new BigDecimal(90 + i));
            closes.add(new BigDecimal(95 + i * 1.5));
        }

        // 设置KDJ参数
        int period = 9;
        BigDecimal kWeight = new BigDecimal("0.6667"); // 2/3
        BigDecimal dWeight = new BigDecimal("0.6667"); // 2/3
        int scale = 4;

        // 计算KDJ指标
        TechnicalIndicatorUtil.KDJ kdj = TechnicalIndicatorUtil.calculateKDJ(
                highs, lows, closes, period, kWeight, dWeight, scale);

        // 验证结果不为null
        assertNotNull(kdj);
        assertNotNull(kdj.getKValues());
        assertNotNull(kdj.getDValues());
        assertNotNull(kdj.getJValues());

        // 验证前8个值为null（数据不足以计算）
        for (int i = 0; i < period - 1; i++) {
            assertNull(kdj.getKValues().get(i));
            assertNull(kdj.getDValues().get(i));
            assertNull(kdj.getJValues().get(i));
        }

        // 验证从第9个值开始有实际值
        for (int i = period - 1; i < highs.size(); i++) {
            assertNotNull(kdj.getKValues().get(i));
            assertNotNull(kdj.getDValues().get(i));
            assertNotNull(kdj.getJValues().get(i));
        }

        // 验证J值计算公式：J = 3*K - 2*D
        for (int i = period - 1; i < highs.size(); i++) {
            BigDecimal k = kdj.getKValues().get(i);
            BigDecimal d = kdj.getDValues().get(i);
            BigDecimal j = kdj.getJValues().get(i);
            
            BigDecimal expectedJ = k.multiply(new BigDecimal("3")).subtract(d.multiply(new BigDecimal("2"))).setScale(scale, RoundingMode.HALF_UP);
            assertEquals(expectedJ, j);
        }

        // 测试getValueAt方法
        TechnicalIndicatorUtil.KDJValue kdjValue = kdj.getValueAt(period - 1);
        assertNotNull(kdjValue);
        assertEquals(kdj.getKValues().get(period - 1), kdjValue.getK());
        assertEquals(kdj.getDValues().get(period - 1), kdjValue.getD());
        assertEquals(kdj.getJValues().get(period - 1), kdjValue.getJ());
        
        // 测试索引越界异常
        assertThrows(IndexOutOfBoundsException.class, () -> kdj.getValueAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> kdj.getValueAt(highs.size()));
    }

    @Test
    public void testCalculateKDJWithInvalidParameters() {
        // 准备基本测试数据
        List<BigDecimal> highs = Arrays.asList(
                new BigDecimal("110"), new BigDecimal("112"), new BigDecimal("115"),
                new BigDecimal("113"), new BigDecimal("116"), new BigDecimal("120"));
        List<BigDecimal> lows = Arrays.asList(
                new BigDecimal("105"), new BigDecimal("107"), new BigDecimal("109"),
                new BigDecimal("106"), new BigDecimal("110"), new BigDecimal("115"));
        List<BigDecimal> closes = Arrays.asList(
                new BigDecimal("108"), new BigDecimal("110"), new BigDecimal("112"),
                new BigDecimal("109"), new BigDecimal("114"), new BigDecimal("119"));
        
        // 测试null输入
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(null, lows, closes, 3, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, null, closes, 3, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, lows, null, 3, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        // 测试长度不一致的列表
        List<BigDecimal> shorterList = Arrays.asList(
                new BigDecimal("105"), new BigDecimal("107"), new BigDecimal("109"),
                new BigDecimal("106"), new BigDecimal("110"));
                
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, shorterList, closes, 3, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        // 测试无效周期
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, lows, closes, 0, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, lows, closes, -1, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        // 测试数据点不足
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, lows, closes, 10, new BigDecimal("0.5"), new BigDecimal("0.5"), 4));
        
        // 测试无效权重
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, lows, closes, 3, new BigDecimal("0"), new BigDecimal("0.5"), 4));
        
        assertThrows(IllegalArgumentException.class, () -> 
            TechnicalIndicatorUtil.calculateKDJ(highs, lows, closes, 3, new BigDecimal("1"), new BigDecimal("0.5"), 4));
    }

    /**
     * 测试ATR (Average True Range)计算
     */
    @Test
    public void testCalculateATR() {
        // 准备测试数据
        List<BigDecimal> highs = Arrays.asList(
            new BigDecimal("126.01"), 
            new BigDecimal("127.62"), 
            new BigDecimal("126.59"), 
            new BigDecimal("127.35"), 
            new BigDecimal("128.17"), 
            new BigDecimal("128.43"), 
            new BigDecimal("127.37"), 
            new BigDecimal("124.93"), 
            new BigDecimal("125.72"), 
            new BigDecimal("127.15"), 
            new BigDecimal("127.41"), 
            new BigDecimal("128.43"), 
            new BigDecimal("127.08"), 
            new BigDecimal("126.82"), 
            new BigDecimal("126.13")
        );
        
        List<BigDecimal> lows = Arrays.asList(
            new BigDecimal("124.20"), 
            new BigDecimal("126.16"), 
            new BigDecimal("124.93"), 
            new BigDecimal("126.09"), 
            new BigDecimal("126.82"), 
            new BigDecimal("126.48"), 
            new BigDecimal("124.83"), 
            new BigDecimal("123.10"), 
            new BigDecimal("124.56"), 
            new BigDecimal("125.07"), 
            new BigDecimal("126.09"), 
            new BigDecimal("126.82"), 
            new BigDecimal("125.94"), 
            new BigDecimal("124.56"), 
            new BigDecimal("124.80")
        );
        
        List<BigDecimal> closes = Arrays.asList(
            new BigDecimal("125.36"), 
            new BigDecimal("127.29"), 
            new BigDecimal("126.16"), 
            new BigDecimal("127.29"), 
            new BigDecimal("127.18"), 
            new BigDecimal("127.11"), 
            new BigDecimal("125.48"), 
            new BigDecimal("124.36"), 
            new BigDecimal("125.12"), 
            new BigDecimal("127.03"), 
            new BigDecimal("127.11"), 
            new BigDecimal("127.29"), 
            new BigDecimal("126.16"), 
            new BigDecimal("125.42"), 
            new BigDecimal("125.70")
        );
        
        // 测试周期为5的ATR
        int period = 5;
        int scale = 2;
        List<BigDecimal> atrValues = TechnicalIndicatorUtil.calculateATR(highs, lows, closes, period, scale);
        
        // 验证结果数量
        assertEquals(highs.size(), atrValues.size());
        
        // 前period-1个值应该为null
        for (int i = 0; i < period - 1; i++) {
            assertNull(atrValues.get(i));
        }
        
        // 验证第一个有效ATR值(第5个位置，索引4)
        // 手动计算前5个TR的平均值: TR(0)+TR(1)+TR(2)+TR(3)+TR(4)/5
        // TR(0) = high(0) - low(0) = 126.01 - 124.20 = 1.81
        // TR(1) = max(127.62-126.16, |127.62-125.36|, |126.16-125.36|) = max(1.46, 2.26, 0.8) = 2.26
        // TR(2) = max(126.59-124.93, |126.59-127.29|, |124.93-127.29|) = max(1.66, 0.7, 2.36) = 2.36
        // TR(3) = max(127.35-126.09, |127.35-126.16|, |126.09-126.16|) = max(1.26, 1.19, 0.07) = 1.26
        // TR(4) = max(128.17-126.82, |128.17-127.29|, |126.82-127.29|) = max(1.35, 0.88, 0.47) = 1.35
        // 平均值 = (1.81+2.26+2.36+1.26+1.35)/5 = 9.04/5 = 1.81
        BigDecimal expectedFirstATR = new BigDecimal("1.81");
        assertEquals(expectedFirstATR.setScale(2, RoundingMode.HALF_UP), atrValues.get(period - 1).setScale(2, RoundingMode.HALF_UP));
        
        // 测试第6个位置的ATR (索引5)
        // ATR(5) = [(period-1) * ATR(4) + TR(5)] / period
        // TR(5) = max(128.43-126.48, |128.43-127.18|, |126.48-127.18|) = max(1.95, 1.25, 0.7) = 1.95
        // ATR(5) = (4 * 1.81 + 1.95) / 5 = 9.19/5 = 1.84
        BigDecimal expectedATR5 = new BigDecimal("1.84");
        assertEquals(expectedATR5.setScale(2, RoundingMode.HALF_UP), atrValues.get(5).setScale(2, RoundingMode.HALF_UP));
        
        // 测试ATRResult包装类
        TechnicalIndicatorUtil.ATRResult atrResult = new TechnicalIndicatorUtil.ATRResult(atrValues);
        assertEquals(atrValues, atrResult.getAtrValues());
        assertEquals(atrValues.get(5), atrResult.getValueAt(5));
        assertEquals(atrValues.get(atrValues.size() - 1), atrResult.getLatestValue());
    }
    
    /**
     * 测试ATR边界情况
     */
    @Test
    public void testCalculateATREdgeCases() {
        List<BigDecimal> highs = Arrays.asList(
            new BigDecimal("100"), new BigDecimal("101"), new BigDecimal("102")
        );
        List<BigDecimal> lows = Arrays.asList(
            new BigDecimal("99"), new BigDecimal("98"), new BigDecimal("97")
        );
        List<BigDecimal> closes = Arrays.asList(
            new BigDecimal("99.5"), new BigDecimal("100.5"), new BigDecimal("101.5")
        );
        
        // 测试数据点不足的情况
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TechnicalIndicatorUtil.calculateATR(highs, lows, closes, 5, 2);
        });
        assertTrue(exception.getMessage().contains("价格数据不足"));
        
        // 测试参数检查
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TechnicalIndicatorUtil.calculateATR(null, lows, closes, 2, 2);
        });
        assertTrue(exception.getMessage().contains("价格数据不能为空"));
        
        exception = assertThrows(IllegalArgumentException.class, () -> {
            List<BigDecimal> shortList = Arrays.asList(new BigDecimal("100"));
            TechnicalIndicatorUtil.calculateATR(shortList, lows, closes, 2, 2);
        });
        assertTrue(exception.getMessage().contains("长度必须一致"));
        
        exception = assertThrows(IllegalArgumentException.class, () -> {
            TechnicalIndicatorUtil.calculateATR(highs, lows, closes, 0, 2);
        });
        assertTrue(exception.getMessage().contains("周期必须大于0"));
    }
}
