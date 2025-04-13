package com.okx.trading.util;

import com.okx.trading.model.entity.CandlestickEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 回测测试数据生成器
 * 用于生成模拟K线数据进行策略回测
 */
public class BacktestDataGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(BacktestDataGenerator.class);
    private static final Random random = new Random();
    
    /**
     * 根据趋势类型生成模拟K线数据
     *
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param trendType 趋势类型 (UPTREND, DOWNTREND, SIDEWAYS, VOLATILE)
     * @param startPrice 起始价格
     * @return 生成的K线数据列表
     */
    public static List<CandlestickEntity> generateCandlestickData(
            String symbol, 
            String interval, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            TrendType trendType, 
            BigDecimal startPrice) {
        
        log.info("开始生成模拟K线数据，交易对: {}, 间隔: {}, 趋势类型: {}, 起始价格: {}", symbol, interval, trendType, startPrice);
        
        List<CandlestickEntity> candlesticks = new ArrayList<>();
        
        // 确定时间间隔单位和数量
        long intervalMinutes = parseIntervalToMinutes(interval);
        LocalDateTime currentTime = startTime;
        
        // 初始价格
        BigDecimal currentPrice = startPrice;
        
        // 趋势参数
        TrendParameters params = getTrendParameters(trendType);
        
        // 生成K线数据
        while (currentTime.isBefore(endTime) || currentTime.isEqual(endTime)) {
            // 生成本周期的价格变动
            BigDecimal priceChange = calculatePriceChange(currentPrice, params);
            
            // 生成开、高、低、收价格
            BigDecimal open = currentPrice;
            BigDecimal close = currentPrice.add(priceChange);
            
            // 确保价格不为负
            if (close.compareTo(BigDecimal.ZERO) <= 0) {
                close = new BigDecimal("0.01");
            }
            
            // 基于开盘价和收盘价计算高点和低点
            BigDecimal high, low;
            
            if (open.compareTo(close) > 0) {
                // 开盘价高于收盘价
                high = open.multiply(BigDecimal.ONE.add(new BigDecimal(random.nextDouble() * 0.03)));
                low = close.multiply(BigDecimal.ONE.subtract(new BigDecimal(random.nextDouble() * 0.03)));
            } else {
                // 收盘价高于或等于开盘价
                high = close.multiply(BigDecimal.ONE.add(new BigDecimal(random.nextDouble() * 0.03)));
                low = open.multiply(BigDecimal.ONE.subtract(new BigDecimal(random.nextDouble() * 0.03)));
            }
            
            // 确保低点不为负
            if (low.compareTo(BigDecimal.ZERO) <= 0) {
                low = new BigDecimal("0.01");
            }
            
            // 计算成交量
            BigDecimal volume = calculateVolume(params, currentPrice);
            
            // 创建K线实体
            CandlestickEntity candlestick = new CandlestickEntity();
            candlestick.setSymbol(symbol);
            candlestick.setIntervalVal(interval);
            candlestick.setOpenTime(currentTime);
            candlestick.setCloseTime(currentTime.plus(intervalMinutes, ChronoUnit.MINUTES));
            candlestick.setOpen(open.setScale(2, RoundingMode.HALF_UP));
            candlestick.setHigh(high.setScale(2, RoundingMode.HALF_UP));
            candlestick.setLow(low.setScale(2, RoundingMode.HALF_UP));
            candlestick.setClose(close.setScale(2, RoundingMode.HALF_UP));
            candlestick.setVolume(volume.setScale(2, RoundingMode.HALF_UP));
            candlestick.setQuoteVolume(volume.multiply(close).setScale(2, RoundingMode.HALF_UP));
            candlestick.setTrades(Long.valueOf(random.nextInt(1000) + 100));
            candlestick.setFetchTime(LocalDateTime.now());
            
            candlesticks.add(candlestick);
            
            // 为下一个周期更新当前价格和时间
            currentPrice = close;
            currentTime = currentTime.plus(intervalMinutes, ChronoUnit.MINUTES);
        }
        
        log.info("成功生成 {} 条模拟K线数据，价格区间: {} - {}", 
                candlesticks.size(), 
                candlesticks.get(0).getClose(), 
                candlesticks.get(candlesticks.size() - 1).getClose());
        
        return candlesticks;
    }
    
    /**
     * 将时间间隔字符串解析为分钟数
     *
     * @param interval 时间间隔字符串，例如：1m, 5m, 15m, 1h, 4h, 1d, 1w
     * @return 间隔的分钟数
     */
    private static long parseIntervalToMinutes(String interval) {
        String value = interval.substring(0, interval.length() - 1);
        String unit = interval.substring(interval.length() - 1);
        
        int intValue = Integer.parseInt(value);
        
        switch (unit.toLowerCase()) {
            case "m": return intValue;
            case "h": return intValue * 60;
            case "d": return intValue * 60 * 24;
            case "w": return intValue * 60 * 24 * 7;
            default: return 60; // 默认1小时
        }
    }
    
    /**
     * 计算价格变动
     *
     * @param currentPrice 当前价格
     * @param params 趋势参数
     * @return 价格变动值
     */
    private static BigDecimal calculatePriceChange(BigDecimal currentPrice, TrendParameters params) {
        // 基准波动率（根据当前价格的百分比）
        BigDecimal baseVolatility = currentPrice.multiply(params.getBaseVolatilityPercent());
        
        // 随机波动
        double randomFactor = (random.nextDouble() * 2 - 1) * params.getRandomVolatilityMultiplier();
        
        // 趋势方向
        double trendFactor = params.getTrendDirection() * params.getTrendStrength();
        
        // 合并随机波动和趋势
        double totalFactors = randomFactor + trendFactor;
        
        // 计算价格变动
        return baseVolatility.multiply(new BigDecimal(totalFactors));
    }
    
    /**
     * 计算成交量
     *
     * @param params 趋势参数
     * @param currentPrice 当前价格
     * @return 成交量
     */
    private static BigDecimal calculateVolume(TrendParameters params, BigDecimal currentPrice) {
        // 基础成交量
        BigDecimal baseVolume = new BigDecimal(random.nextDouble() * 100 + 50);
        
        // 根据趋势调整成交量
        double volumeMultiplier = 1.0 + Math.abs(params.getTrendDirection() * params.getTrendStrength() * 0.5);
        
        // 加入一些随机波动
        double randomFactor = 0.5 + (random.nextDouble() * 1.5);
        
        // 计算最终成交量
        return baseVolume.multiply(new BigDecimal(volumeMultiplier * randomFactor));
    }
    
    /**
     * 获取趋势参数
     *
     * @param trendType 趋势类型
     * @return 趋势参数
     */
    private static TrendParameters getTrendParameters(TrendType trendType) {
        switch (trendType) {
            case UPTREND:
                return new TrendParameters(
                        new BigDecimal("0.01"),   // 基础波动率为价格的1%
                        0.7,                       // 趋势强度 (0-1)
                        1.0,                       // 趋势方向 (正数表示上升)
                        1.0                        // 随机波动倍数
                );
                
            case DOWNTREND:
                return new TrendParameters(
                        new BigDecimal("0.01"),   // 基础波动率为价格的1%
                        0.7,                       // 趋势强度 (0-1)
                        -1.0,                      // 趋势方向 (负数表示下降)
                        1.0                        // 随机波动倍数
                );
                
            case SIDEWAYS:
                return new TrendParameters(
                        new BigDecimal("0.005"),  // 基础波动率为价格的0.5%
                        0.1,                       // 趋势强度很小
                        0.0,                       // 趋势方向中性
                        1.0                        // 随机波动倍数
                );
                
            case VOLATILE:
                return new TrendParameters(
                        new BigDecimal("0.02"),   // 基础波动率为价格的2%
                        0.3,                       // 中等趋势强度
                        0.0,                       // 趋势方向中性
                        2.0                        // 更大的随机波动
                );
                
            default:
                return new TrendParameters(
                        new BigDecimal("0.01"),
                        0.0,
                        0.0,
                        1.0
                );
        }
    }
    
    /**
     * 趋势类型枚举
     */
    public enum TrendType {
        UPTREND,    // 上升趋势
        DOWNTREND,  // 下降趋势
        SIDEWAYS,   // 横盘趋势
        VOLATILE    // 波动剧烈
    }
    
    /**
     * 趋势参数类
     */
    private static class TrendParameters {
        private final BigDecimal baseVolatilityPercent;  // 基础波动百分比
        private final double trendStrength;              // 趋势强度 (0-1)
        private final double trendDirection;             // 趋势方向 (正:上升, 负:下降, 0:中性)
        private final double randomVolatilityMultiplier; // 随机波动倍数
        
        public TrendParameters(BigDecimal baseVolatilityPercent, double trendStrength, 
                              double trendDirection, double randomVolatilityMultiplier) {
            this.baseVolatilityPercent = baseVolatilityPercent;
            this.trendStrength = trendStrength;
            this.trendDirection = trendDirection;
            this.randomVolatilityMultiplier = randomVolatilityMultiplier;
        }
        
        public BigDecimal getBaseVolatilityPercent() {
            return baseVolatilityPercent;
        }
        
        public double getTrendStrength() {
            return trendStrength;
        }
        
        public double getTrendDirection() {
            return trendDirection;
        }
        
        public double getRandomVolatilityMultiplier() {
            return randomVolatilityMultiplier;
        }
    }
    
    /**
     * 测试方法：生成并打印一些示例数据
     */
    public static void main(String[] args) {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        List<CandlestickEntity> uptrend = generateCandlestickData(
                "BTC-USDT", "1h", start, end, TrendType.UPTREND, new BigDecimal("40000"));
        
        System.out.println("======= 上升趋势数据示例 =======");
        System.out.println("数据点数: " + uptrend.size());
        System.out.println("起始价格: " + uptrend.get(0).getClose());
        System.out.println("结束价格: " + uptrend.get(uptrend.size() - 1).getClose());
        System.out.println("增长幅度: " + 
                uptrend.get(uptrend.size() - 1).getClose()
                        .subtract(uptrend.get(0).getClose())
                        .divide(uptrend.get(0).getClose(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP) + "%");
        
        List<CandlestickEntity> downtrend = generateCandlestickData(
                "BTC-USDT", "1h", start, end, TrendType.DOWNTREND, new BigDecimal("40000"));
        
        System.out.println("\n======= 下降趋势数据示例 =======");
        System.out.println("数据点数: " + downtrend.size());
        System.out.println("起始价格: " + downtrend.get(0).getClose());
        System.out.println("结束价格: " + downtrend.get(downtrend.size() - 1).getClose());
        System.out.println("下跌幅度: " + 
                downtrend.get(downtrend.size() - 1).getClose()
                        .subtract(downtrend.get(0).getClose())
                        .divide(downtrend.get(0).getClose(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP) + "%");
    }
} 