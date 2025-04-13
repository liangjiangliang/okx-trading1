package com.okx.trading.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 技术指标工具类
 * 用于计算各种技术分析指标
 */
public class TechnicalIndicatorUtil {

    /**
     * 计算布林带(Bollinger Bands)
     *
     * @param prices      价格列表
     * @param period      周期(通常为20)
     * @param multiplier  标准差倍数(通常为2)
     * @param scale       小数点精度
     * @return 布林带结果，包含中轨(SMA)、上轨(Upper)和下轨(Lower)
     */
    public static BollingerBands calculateBollingerBands(List<BigDecimal> prices, int period, double multiplier, int scale) {
        if (prices == null || prices.isEmpty() || prices.size() < period) {
            throw new IllegalArgumentException("价格数据不足，无法计算布林带，至少需要" + period + "个数据点");
        }

        List<BigDecimal> smaList = new ArrayList<>();
        List<BigDecimal> upperList = new ArrayList<>();
        List<BigDecimal> lowerList = new ArrayList<>();
        
        // 对于前period-1个数据点，无法计算完整的布林带，填充空值
        for (int i = 0; i < period - 1; i++) {
            smaList.add(null);
            upperList.add(null);
            lowerList.add(null);
        }
        
        // 计算每个数据点的布林带值
        for (int i = period - 1; i < prices.size(); i++) {
            // 提取当前窗口的价格数据
            List<BigDecimal> window = prices.subList(i - period + 1, i + 1);
            
            // 计算简单移动平均线(SMA)
            BigDecimal sma = calculateSMA(window, scale);
            smaList.add(sma);
            
            // 计算标准差
            BigDecimal stdDev = calculateStandardDeviation(window, sma, scale);
            
            // 计算上轨和下轨
            BigDecimal multiplierDecimal = BigDecimal.valueOf(multiplier);
            BigDecimal upper = sma.add(stdDev.multiply(multiplierDecimal)).setScale(scale, RoundingMode.HALF_UP);
            BigDecimal lower = sma.subtract(stdDev.multiply(multiplierDecimal)).setScale(scale, RoundingMode.HALF_UP);
            
            upperList.add(upper);
            lowerList.add(lower);
        }
        
        return new BollingerBands(smaList, upperList, lowerList);
    }
    
    /**
     * 计算相对强弱指标(RSI - Relative Strength Index)
     *
     * @param prices    价格列表
     * @param period    周期(通常为14)
     * @param scale     小数点精度
     * @return RSI值列表
     */
    public static List<BigDecimal> calculateRSI(List<BigDecimal> prices, int period, int scale) {
        if (prices == null || prices.isEmpty() || prices.size() <= period) {
            throw new IllegalArgumentException("价格数据不足，无法计算RSI，至少需要" + (period + 1) + "个数据点");
        }

        List<BigDecimal> rsiValues = new ArrayList<>();
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        
        // 第一个数据点没有变化，从第二个点开始计算
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }
        
        // 对于前period个数据点，无法计算RSI，填充空值
        for (int i = 0; i < period; i++) {
            rsiValues.add(null);
        }
        
        // 计算首个RSI值
        BigDecimal avgGain = calculateAverage(gains.subList(0, period), scale);
        BigDecimal avgLoss = calculateAverage(losses.subList(0, period), scale);
        
        BigDecimal rs = avgLoss.compareTo(BigDecimal.ZERO) == 0 ? 
                        BigDecimal.valueOf(100) : 
                        avgGain.divide(avgLoss, scale + 2, RoundingMode.HALF_UP);
        BigDecimal rsi = calculateRSIFromRS(rs, scale);
        rsiValues.add(rsi);
        
        // 计算剩余RSI值，使用平滑移动平均法
        for (int i = period; i < gains.size(); i++) {
            // 更新平均增益和平均损失
            avgGain = (avgGain.multiply(BigDecimal.valueOf(period - 1))
                      .add(gains.get(i)))
                      .divide(BigDecimal.valueOf(period), scale + 2, RoundingMode.HALF_UP);
                      
            avgLoss = (avgLoss.multiply(BigDecimal.valueOf(period - 1))
                      .add(losses.get(i)))
                      .divide(BigDecimal.valueOf(period), scale + 2, RoundingMode.HALF_UP);
            
            // 计算相对强度(RS)和RSI
            rs = avgLoss.compareTo(BigDecimal.ZERO) == 0 ? 
                BigDecimal.valueOf(100) : 
                avgGain.divide(avgLoss, scale + 2, RoundingMode.HALF_UP);
            rsi = calculateRSIFromRS(rs, scale);
            rsiValues.add(rsi);
        }
        
        return rsiValues;
    }
    
    /**
     * 根据相对强度(RS)计算RSI值
     *
     * @param rs    相对强度值
     * @param scale 小数点精度
     * @return RSI值
     */
    private static BigDecimal calculateRSIFromRS(BigDecimal rs, int scale) {
        BigDecimal hundred = BigDecimal.valueOf(100);
        return hundred.subtract(
            hundred.divide(BigDecimal.ONE.add(rs), scale, RoundingMode.HALF_UP)
        ).setScale(scale, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算列表平均值
     *
     * @param values 数值列表
     * @param scale  小数点精度
     * @return 平均值
     */
    private static BigDecimal calculateAverage(List<BigDecimal> values, int scale) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        
        return sum.divide(BigDecimal.valueOf(values.size()), scale, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算简单移动平均线(SMA)
     *
     * @param prices 价格列表
     * @param scale  小数点精度
     * @return 简单移动平均线值
     */
    public static BigDecimal calculateSMA(List<BigDecimal> prices, int scale) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("价格数据不能为空");
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            sum = sum.add(price);
        }
        
        return sum.divide(BigDecimal.valueOf(prices.size()), scale, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算标准差
     *
     * @param prices 价格列表
     * @param mean   均值
     * @param scale  小数点精度
     * @return 标准差
     */
    public static BigDecimal calculateStandardDeviation(List<BigDecimal> prices, BigDecimal mean, int scale) {
        if (prices == null || prices.isEmpty() || mean == null) {
            throw new IllegalArgumentException("价格数据或均值不能为空");
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            BigDecimal deviation = price.subtract(mean);
            sum = sum.add(deviation.multiply(deviation));
        }
        
        BigDecimal variance = sum.divide(BigDecimal.valueOf(prices.size()), scale + 10, RoundingMode.HALF_UP);
        return sqrt(variance, scale);
    }
    
    /**
     * 计算平方根（牛顿迭代法）
     *
     * @param value 需要计算平方根的值
     * @param scale 小数点精度
     * @return 平方根
     */
    public static BigDecimal sqrt(BigDecimal value, int scale) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of a negative number");
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // 初始猜测值
        BigDecimal x = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
        
        // 设置更高的精度用于计算
        int workingScale = scale + 10;
        
        // 牛顿迭代法
        BigDecimal TWO = new BigDecimal("2");
        BigDecimal epsilon = BigDecimal.ONE.movePointLeft(workingScale);
        
        while (true) {
            BigDecimal nextX = x.add(value.divide(x, workingScale, RoundingMode.HALF_UP))
                               .divide(TWO, workingScale, RoundingMode.HALF_UP);
            BigDecimal delta = nextX.subtract(x).abs();
            if (delta.compareTo(epsilon) <= 0) {
                break;
            }
            x = nextX;
        }
        
        return x.setScale(scale, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算MACD指标(Moving Average Convergence Divergence)
     *
     * @param prices       价格列表
     * @param fastPeriod   快速EMA周期(通常为12)
     * @param slowPeriod   慢速EMA周期(通常为26)
     * @param signalPeriod 信号线EMA周期(通常为9)
     * @param scale        小数点精度
     * @return MACD指标结果，包含MACD线、信号线和直方图值
     */
    public static MACD calculateMACD(List<BigDecimal> prices, int fastPeriod, int slowPeriod, int signalPeriod, int scale) {
        if (prices == null || prices.isEmpty() || prices.size() < Math.max(fastPeriod, slowPeriod) + signalPeriod) {
            throw new IllegalArgumentException("价格数据不足，无法计算MACD，至少需要" + (Math.max(fastPeriod, slowPeriod) + signalPeriod) + "个数据点");
        }

        // 计算快速和慢速指数移动平均线(EMA)
        List<BigDecimal> fastEMA = calculateEMA(prices, fastPeriod, scale);
        List<BigDecimal> slowEMA = calculateEMA(prices, slowPeriod, scale);

        // 计算MACD线(快速EMA - 慢速EMA)
        List<BigDecimal> macdLine = new ArrayList<>();
        // 对齐数据，前slowPeriod-1个点无法计算完整MACD
        for (int i = 0; i < slowPeriod - 1; i++) {
            macdLine.add(null);
        }

        for (int i = slowPeriod - 1; i < prices.size(); i++) {
            int fastIndex = i - (slowPeriod - fastPeriod);
            if (fastIndex >= 0) {
                BigDecimal macd = fastEMA.get(fastIndex + fastPeriod - 1).subtract(slowEMA.get(i));
                macdLine.add(macd.setScale(scale, RoundingMode.HALF_UP));
            } else {
                macdLine.add(null);
            }
        }

        // 计算信号线(MACD的EMA)
        List<BigDecimal> validMacdForSignal = new ArrayList<>();
        for (BigDecimal value : macdLine) {
            if (value != null) {
                validMacdForSignal.add(value);
            }
        }
        
        List<BigDecimal> signalLine = calculateEMA(validMacdForSignal, signalPeriod, scale);
        
        // 完整的信号线，包含空值
        List<BigDecimal> fullSignalLine = new ArrayList<>();
        // 对齐数据，前slowPeriod-1+signalPeriod-1个点无法计算完整信号线
        for (int i = 0; i < slowPeriod + signalPeriod - 2; i++) {
            fullSignalLine.add(null);
        }
        
        fullSignalLine.addAll(signalLine);

        // 计算柱状图(MACD线 - 信号线)
        List<BigDecimal> histogram = new ArrayList<>();
        
        for (int i = 0; i < prices.size(); i++) {
            if (i < slowPeriod + signalPeriod - 2 || macdLine.get(i) == null || fullSignalLine.get(i) == null) {
                histogram.add(null);
            } else {
                BigDecimal diff = macdLine.get(i).subtract(fullSignalLine.get(i));
                histogram.add(diff.setScale(scale, RoundingMode.HALF_UP));
            }
        }

        return new MACD(macdLine, fullSignalLine, histogram);
    }

    /**
     * 计算指数移动平均线(EMA)
     *
     * @param prices 价格列表
     * @param period EMA周期
     * @param scale  小数点精度
     * @return EMA值列表
     */
    public static List<BigDecimal> calculateEMA(List<BigDecimal> prices, int period, int scale) {
        if (prices == null || prices.isEmpty() || prices.size() < period) {
            throw new IllegalArgumentException("价格数据不足，无法计算EMA，至少需要" + period + "个数据点");
        }

        List<BigDecimal> emaValues = new ArrayList<>();
        
        // 对于前period-1个数据点，无法计算EMA，填充空值
        for (int i = 0; i < period - 1; i++) {
            emaValues.add(null);
        }

        // 第一个EMA值等于前period个价格的SMA
        BigDecimal firstEma = calculateSMA(prices.subList(0, period), scale);
        emaValues.add(firstEma);

        // 平滑因子: 2/(period+1)
        BigDecimal multiplier = new BigDecimal(2).divide(new BigDecimal(period + 1), scale + 10, RoundingMode.HALF_UP);
        
        // 计算剩余的EMA值
        for (int i = period; i < prices.size(); i++) {
            // EMA = 当前价格 * 平滑因子 + 前一个EMA值 * (1 - 平滑因子)
            BigDecimal currentPrice = prices.get(i);
            BigDecimal previousEma = emaValues.get(i - 1);
            
            BigDecimal ema = currentPrice.multiply(multiplier).add(
                previousEma.multiply(BigDecimal.ONE.subtract(multiplier))
            ).setScale(scale, RoundingMode.HALF_UP);
            
            emaValues.add(ema);
        }

        return emaValues;
    }
    
    /**
     * 布林带结果类
     */
    public static class BollingerBands {
        private final List<BigDecimal> middle; // 中轨(SMA)
        private final List<BigDecimal> upper;  // 上轨
        private final List<BigDecimal> lower;  // 下轨
        
        public BollingerBands(List<BigDecimal> middle, List<BigDecimal> upper, List<BigDecimal> lower) {
            this.middle = middle;
            this.upper = upper;
            this.lower = lower;
        }
        
        public List<BigDecimal> getMiddle() {
            return middle;
        }
        
        public List<BigDecimal> getUpper() {
            return upper;
        }
        
        public List<BigDecimal> getLower() {
            return lower;
        }
        
        /**
         * 获取指定索引位置的布林带值
         *
         * @param index 索引位置
         * @return 布林带值(中轨、上轨、下轨)
         */
        public BollingerBandValue getValueAt(int index) {
            if (index < 0 || index >= middle.size()) {
                throw new IndexOutOfBoundsException("索引超出范围: " + index);
            }
            
            return new BollingerBandValue(
                middle.get(index),
                upper.get(index),
                lower.get(index)
            );
        }
    }
    
    /**
     * 单个位置的布林带值
     */
    public static class BollingerBandValue {
        private final BigDecimal middle; // 中轨(SMA)
        private final BigDecimal upper;  // 上轨
        private final BigDecimal lower;  // 下轨
        
        public BollingerBandValue(BigDecimal middle, BigDecimal upper, BigDecimal lower) {
            this.middle = middle;
            this.upper = upper;
            this.lower = lower;
        }
        
        public BigDecimal getMiddle() {
            return middle;
        }
        
        public BigDecimal getUpper() {
            return upper;
        }
        
        public BigDecimal getLower() {
            return lower;
        }
        
        @Override
        public String toString() {
            return "BollingerBand{" +
                   "middle=" + middle +
                   ", upper=" + upper +
                   ", lower=" + lower +
                   '}';
        }
    }

    /**
     * MACD指标结果类
     */
    public static class MACD {
        private final List<BigDecimal> macdLine;    // MACD线
        private final List<BigDecimal> signalLine;  // 信号线
        private final List<BigDecimal> histogram;   // 柱状图(差值)
        
        public MACD(List<BigDecimal> macdLine, List<BigDecimal> signalLine, List<BigDecimal> histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
        
        public List<BigDecimal> getMacdLine() {
            return macdLine;
        }
        
        public List<BigDecimal> getSignalLine() {
            return signalLine;
        }
        
        public List<BigDecimal> getHistogram() {
            return histogram;
        }
        
        /**
         * 获取指定索引位置的MACD值
         *
         * @param index 索引位置
         * @return MACD值(MACD线、信号线、柱状图)
         */
        public MACDValue getValueAt(int index) {
            if (index < 0 || index >= macdLine.size()) {
                throw new IndexOutOfBoundsException("索引超出范围: " + index);
            }
            
            return new MACDValue(
                macdLine.get(index),
                signalLine.get(index),
                histogram.get(index)
            );
        }
    }
    
    /**
     * 单个位置的MACD值
     */
    public static class MACDValue {
        private final BigDecimal macdLine;    // MACD线
        private final BigDecimal signalLine;  // 信号线
        private final BigDecimal histogram;   // 柱状图(差值)
        
        public MACDValue(BigDecimal macdLine, BigDecimal signalLine, BigDecimal histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
        
        public BigDecimal getMacdLine() {
            return macdLine;
        }
        
        public BigDecimal getSignalLine() {
            return signalLine;
        }
        
        public BigDecimal getHistogram() {
            return histogram;
        }
        
        @Override
        public String toString() {
            return "MACDValue{" +
                   "macdLine=" + macdLine +
                   ", signalLine=" + signalLine +
                   ", histogram=" + histogram +
                   '}';
        }
    }

    /**
     * KDJ指标数据
     */
    public static class KDJ {
        private final List<BigDecimal> kValues;
        private final List<BigDecimal> dValues;
        private final List<BigDecimal> jValues;

        public KDJ(List<BigDecimal> kValues, List<BigDecimal> dValues, List<BigDecimal> jValues) {
            this.kValues = kValues;
            this.dValues = dValues;
            this.jValues = jValues;
        }

        public List<BigDecimal> getKValues() {
            return kValues;
        }

        public List<BigDecimal> getDValues() {
            return dValues;
        }

        public List<BigDecimal> getJValues() {
            return jValues;
        }

        /**
         * 获取指定位置的KDJ值
         *
         * @param index 索引位置
         * @return KDJ值
         * @throws IndexOutOfBoundsException 如果索引超出范围
         */
        public KDJValue getValueAt(int index) {
            if (index < 0 || index >= kValues.size()) {
                throw new IndexOutOfBoundsException("索引超出范围: " + index);
            }
            return new KDJValue(kValues.get(index), dValues.get(index), jValues.get(index));
        }
    }

    /**
     * 单个KDJ值
     */
    public static class KDJValue {
        private final BigDecimal k;
        private final BigDecimal d;
        private final BigDecimal j;

        public KDJValue(BigDecimal k, BigDecimal d, BigDecimal j) {
            this.k = k;
            this.d = d;
            this.j = j;
        }

        public BigDecimal getK() {
            return k;
        }

        public BigDecimal getD() {
            return d;
        }

        public BigDecimal getJ() {
            return j;
        }
    }

    /**
     * 计算KDJ指标
     * KDJ指标也称随机指标，是一种相当新颖、实用的技术分析工具。
     *
     * @param highs    高点价格列表
     * @param lows     低点价格列表
     * @param closes   收盘价格列表
     * @param period   计算周期（通常为9）
     * @param kWeight  K值权重（通常为2/3）
     * @param dWeight  D值权重（通常为2/3）
     * @param scale    精度
     * @return KDJ指标值
     * @throws IllegalArgumentException 如果输入数据不足以计算或参数无效
     */
    public static KDJ calculateKDJ(List<BigDecimal> highs, List<BigDecimal> lows, List<BigDecimal> closes,
                                  int period, BigDecimal kWeight, BigDecimal dWeight, int scale) {
        // 参数验证
        if (highs == null || lows == null || closes == null) {
            throw new IllegalArgumentException("价格数据不能为null");
        }
        
        if (highs.size() != lows.size() || highs.size() != closes.size()) {
            throw new IllegalArgumentException("高点、低点和收盘价数据长度必须相同");
        }
        
        if (highs.size() < period) {
            throw new IllegalArgumentException("价格数据点数不足，至少需要 " + period + " 个数据点");
        }
        
        if (period <= 0) {
            throw new IllegalArgumentException("周期必须为正数");
        }
        
        if (kWeight.compareTo(BigDecimal.ZERO) <= 0 || kWeight.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("K值权重必须在0到1之间");
        }
        
        if (dWeight.compareTo(BigDecimal.ZERO) <= 0 || dWeight.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("D值权重必须在0到1之间");
        }
        
        // 创建RSV、K、D、J值列表
        int size = highs.size();
        List<BigDecimal> rsvValues = new ArrayList<>(size);
        List<BigDecimal> kValues = new ArrayList<>(size);
        List<BigDecimal> dValues = new ArrayList<>(size);
        List<BigDecimal> jValues = new ArrayList<>(size);
        
        // 计算1-K和1-D的权重
        BigDecimal oneMinusKWeight = BigDecimal.ONE.subtract(kWeight);
        BigDecimal oneMinusDWeight = BigDecimal.ONE.subtract(dWeight);
        
        // 前period-1个点的值设为null（因为数据不足以计算）
        for (int i = 0; i < period - 1; i++) {
            rsvValues.add(null);
            kValues.add(null);
            dValues.add(null);
            jValues.add(null);
        }
        
        // 初始K和D值，通常设为50
        BigDecimal lastK = new BigDecimal("50");
        BigDecimal lastD = new BigDecimal("50");
        
        // 计算每个点的KDJ值
        for (int i = period - 1; i < size; i++) {
            // 寻找周期内的最高价和最低价
            BigDecimal highestHigh = highs.get(i);
            BigDecimal lowestLow = lows.get(i);
            
            for (int j = i - (period - 1); j < i; j++) {
                if (highs.get(j).compareTo(highestHigh) > 0) {
                    highestHigh = highs.get(j);
                }
                if (lows.get(j).compareTo(lowestLow) < 0) {
                    lowestLow = lows.get(j);
                }
            }
            
            // 计算RSV值：(收盘价-周期内最低价)/(周期内最高价-周期内最低价)*100
            BigDecimal rsv;
            BigDecimal range = highestHigh.subtract(lowestLow);
            
            if (range.compareTo(BigDecimal.ZERO) == 0) {
                // 如果范围为0，RSV设为50
                rsv = new BigDecimal("50");
            } else {
                rsv = closes.get(i).subtract(lowestLow)
                        .multiply(new BigDecimal("100"))
                        .divide(range, scale, RoundingMode.HALF_UP);
            }
            
            rsvValues.add(rsv);
            
            // 计算K值：K = 前一日K值 * (1-kWeight) + 当日RSV * kWeight
            BigDecimal k = lastK.multiply(oneMinusKWeight).add(rsv.multiply(kWeight))
                           .setScale(scale, RoundingMode.HALF_UP);
            kValues.add(k);
            
            // 计算D值：D = 前一日D值 * (1-dWeight) + 当日K值 * dWeight
            BigDecimal d = lastD.multiply(oneMinusDWeight).add(k.multiply(dWeight))
                           .setScale(scale, RoundingMode.HALF_UP);
            dValues.add(d);
            
            // 计算J值：J = 3*K - 2*D
            BigDecimal j = k.multiply(new BigDecimal("3")).subtract(d.multiply(new BigDecimal("2")))
                           .setScale(scale, RoundingMode.HALF_UP);
            jValues.add(j);
            
            // 更新上一个K和D值
            lastK = k;
            lastD = d;
        }
        
        return new KDJ(kValues, dValues, jValues);
    }

    /**
     * 计算真实波幅(True Range)
     *
     * @param high      当前周期最高价
     * @param low       当前周期最低价
     * @param prevClose 前一周期收盘价
     * @return 真实波幅值
     */
    private static BigDecimal calculateTrueRange(BigDecimal high, BigDecimal low, BigDecimal prevClose) {
        if (prevClose == null) {
            // 如果没有前一周期收盘价，直接返回当前周期的高低差
            return high.subtract(low);
        }

        // 真实波幅是以下三个值中的最大值:
        // 1. 当前周期最高价 - 当前周期最低价
        // 2. |当前周期最高价 - 前一周期收盘价|
        // 3. |当前周期最低价 - 前一周期收盘价|
        BigDecimal highLowDiff = high.subtract(low);
        BigDecimal highPrevDiff = high.subtract(prevClose).abs();
        BigDecimal lowPrevDiff = low.subtract(prevClose).abs();

        return highLowDiff.max(highPrevDiff).max(lowPrevDiff);
    }

    /**
     * 计算平均真实波幅(ATR - Average True Range)
     *
     * @param highs    最高价列表
     * @param lows     最低价列表
     * @param closes   收盘价列表
     * @param period   ATR周期(通常为14)
     * @param scale    小数点精度
     * @return ATR值列表
     */
    public static List<BigDecimal> calculateATR(List<BigDecimal> highs, List<BigDecimal> lows, List<BigDecimal> closes, int period, int scale) {
        if (highs == null || lows == null || closes == null) {
            throw new IllegalArgumentException("价格数据不能为空");
        }
        
        if (highs.size() != lows.size() || highs.size() != closes.size()) {
            throw new IllegalArgumentException("最高价、最低价和收盘价数据长度必须一致");
        }
        
        if (highs.size() <= period) {
            throw new IllegalArgumentException("价格数据不足，无法计算ATR，至少需要" + (period + 1) + "个数据点");
        }
        
        if (period <= 0) {
            throw new IllegalArgumentException("周期必须大于0");
        }

        int size = highs.size();
        List<BigDecimal> trValues = new ArrayList<>();
        List<BigDecimal> atrValues = new ArrayList<>();
        
        // 计算每个周期的真实波幅(TR)
        BigDecimal prevClose = null;
        for (int i = 0; i < size; i++) {
            BigDecimal tr = calculateTrueRange(highs.get(i), lows.get(i), prevClose);
            trValues.add(tr);
            prevClose = closes.get(i);
        }
        
        // 对于前period-1个数据点，无法计算ATR，填充空值
        for (int i = 0; i < period - 1; i++) {
            atrValues.add(null);
        }
        
        // 第一个ATR值是前period个TR的简单平均
        BigDecimal firstAtr = calculateSMA(trValues.subList(0, period), scale);
        atrValues.add(firstAtr);
        
        // 使用Wilder平滑法计算剩余ATR值: ATR = [(period-1) * 前一ATR + 当前TR] / period
        for (int i = period; i < size; i++) {
            BigDecimal tr = trValues.get(i);
            BigDecimal prevAtr = atrValues.get(i - 1);
            
            BigDecimal atr = prevAtr.multiply(new BigDecimal(period - 1))
                             .add(tr)
                             .divide(new BigDecimal(period), scale, RoundingMode.HALF_UP);
            
            atrValues.add(atr);
        }
        
        return atrValues;
    }

    /**
     * ATR结果类
     */
    public static class ATRResult {
        private final List<BigDecimal> atrValues;
        
        public ATRResult(List<BigDecimal> atrValues) {
            this.atrValues = atrValues;
        }
        
        public List<BigDecimal> getAtrValues() {
            return atrValues;
        }
        
        /**
         * 获取指定索引位置的ATR值
         *
         * @param index 索引位置
         * @return ATR值
         * @throws IndexOutOfBoundsException 如果索引越界
         */
        public BigDecimal getValueAt(int index) {
            if (index < 0 || index >= atrValues.size()) {
                throw new IndexOutOfBoundsException("索引超出范围: " + index);
            }
            return atrValues.get(index);
        }
        
        /**
         * 获取最新的ATR值
         *
         * @return 最新的ATR值，如果没有值则返回null
         */
        public BigDecimal getLatestValue() {
            if (atrValues.isEmpty()) {
                return null;
            }
            return atrValues.get(atrValues.size() - 1);
        }
    }
} 