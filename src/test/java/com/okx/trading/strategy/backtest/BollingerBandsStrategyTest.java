package com.okx.trading.strategy.backtest;

import com.okx.trading.backtest.BacktestFramework;
import com.okx.trading.strategy.BollingerBandsStrategy;
import com.okx.trading.model.entity.CandlestickEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 布林带策略测试类
 */
public class BollingerBandsStrategyTest {

    /**
     * 策略实例 - 突破模式
     */
    private BollingerBandsStrategy breakoutStrategy;

    /**
     * 策略实例 - 反转模式
     */
    private BollingerBandsStrategy reversalStrategy;

    /**
     * 策略实例 - 挤压模式
     */
    private BollingerBandsStrategy squeezeStrategy;

    /**
     * 模拟K线数据
     */
    private List<CandlestickEntity> candlesticks;

    /**
     * 测试初始化
     */
    @BeforeEach
    public void setup() {
        // 设置初始参数
        BigDecimal initialBalance = new BigDecimal("10000");
        BigDecimal feeRate = new BigDecimal("0.002");
        int period = 20;
        double multiplier = 2.0;
        BigDecimal tradingRatio = new BigDecimal("0.3");
        BigDecimal stopLoss = new BigDecimal("0.05");
        BigDecimal takeProfit = new BigDecimal("0.1");

        // 创建三种不同模式的策略实例
        breakoutStrategy = new BollingerBandsStrategy(
                initialBalance, feeRate, period, multiplier, tradingRatio,
                stopLoss, takeProfit, BollingerBandsStrategy.TradingMode.BREAKOUT, false, 50);

        reversalStrategy = new BollingerBandsStrategy(
                initialBalance, feeRate, period, multiplier, tradingRatio,
                stopLoss, takeProfit, BollingerBandsStrategy.TradingMode.REVERSAL, false, 50);

        squeezeStrategy = new BollingerBandsStrategy(
                initialBalance, feeRate, period, multiplier, tradingRatio,
                stopLoss, takeProfit, BollingerBandsStrategy.TradingMode.SQUEEZE, false, 50);

        // 创建模拟K线数据
        candlesticks = createMockCandlesticks(100);
    }

    /**
     * 测试策略构造函数
     */
    @Test
    public void testConstructor() {
        // 测试完整构造函数
        BollingerBandsStrategy strategy = new BollingerBandsStrategy(
                new BigDecimal("10000"), new BigDecimal("0.002"), 20, 2.0, new BigDecimal("0.3"),
                new BigDecimal("0.05"), new BigDecimal("0.1"),
                BollingerBandsStrategy.TradingMode.BREAKOUT, true, 50);

        assertEquals(20, strategy.getPeriod());
        assertEquals(2.0, strategy.getMultiplier());
        assertEquals(0, new BigDecimal("0.3").compareTo(strategy.getTradingRatio()));
        assertEquals(0, new BigDecimal("0.05").compareTo(strategy.getStopLossPercentage()));
        assertEquals(0, new BigDecimal("0.1").compareTo(strategy.getTakeProfitPercentage()));
        assertEquals(BollingerBandsStrategy.TradingMode.BREAKOUT, strategy.getTradingMode());
        assertTrue(strategy.isUseSMAFilter());
        assertEquals(50, strategy.getSmaFilterPeriod());

        // 测试简化构造函数
        BollingerBandsStrategy simpleStrategy = new BollingerBandsStrategy(
                new BigDecimal("10000"), new BigDecimal("0.002"), 20, 2.0, new BigDecimal("0.3"));

        assertEquals(20, simpleStrategy.getPeriod());
        assertEquals(2.0, simpleStrategy.getMultiplier());
        assertEquals(0, new BigDecimal("0.3").compareTo(simpleStrategy.getTradingRatio()));
        assertEquals(0, new BigDecimal("0.05").compareTo(simpleStrategy.getStopLossPercentage()));
        assertEquals(0, new BigDecimal("0.1").compareTo(simpleStrategy.getTakeProfitPercentage()));
        assertEquals(BollingerBandsStrategy.TradingMode.BREAKOUT, simpleStrategy.getTradingMode());
        assertFalse(simpleStrategy.isUseSMAFilter());
        assertEquals(50, simpleStrategy.getSmaFilterPeriod());
    }

    /**
     * 测试突破模式策略
     */
    @Test
    public void testBreakoutStrategy() {
        // 运行回测
        breakoutStrategy.runBacktest(candlesticks);

        // 验证回测结果
        assertNotNull(breakoutStrategy.getTradeRecords());
        assertFalse(breakoutStrategy.getTradeRecords().isEmpty());
        assertNotNull(breakoutStrategy.getBalanceRecords());
        assertFalse(breakoutStrategy.getBalanceRecords().isEmpty());
    }

    /**
     * 测试反转模式策略
     */
    @Test
    public void testReversalStrategy() {
        // 运行回测
        reversalStrategy.runBacktest(candlesticks);

        // 验证回测结果
        assertNotNull(reversalStrategy.getTradeRecords());
        assertFalse(reversalStrategy.getTradeRecords().isEmpty());
        assertNotNull(reversalStrategy.getBalanceRecords());
        assertFalse(reversalStrategy.getBalanceRecords().isEmpty());
    }

    /**
     * 测试挤压模式策略
     */
    @Test
    public void testSqueezeStrategy() {
        // 运行回测
        squeezeStrategy.runBacktest(candlesticks);

        // 验证回测结果
        assertNotNull(squeezeStrategy.getTradeRecords());
        assertFalse(squeezeStrategy.getTradeRecords().isEmpty());
        assertNotNull(squeezeStrategy.getBalanceRecords());
        assertFalse(squeezeStrategy.getBalanceRecords().isEmpty());
    }

    /**
     * 测试数据不足的情况
     */
    @Test
    public void testInsufficientData() {
        // 创建不足一个周期的数据
        List<CandlestickEntity> insufficientData = createMockCandlesticks(15);

        // 运行回测
        breakoutStrategy.runBacktest(insufficientData);

        // 验证没有交易记录产生
        assertTrue(breakoutStrategy.getTradeRecords().isEmpty());
    }

    /**
     * 测试止盈止损功能
     */
    @Test
    public void testStopLossAndTakeProfit() {
        // 创建特殊的K线数据测试止盈止损
        List<CandlestickEntity> specialData = createStopLossTakeProfitTestData();

        // 运行回测
        breakoutStrategy.runBacktest(specialData);

        // 验证结果中存在止盈或止损的交易
        boolean hasStopLoss = false;
        boolean hasTakeProfit = false;

        for (BacktestFramework.TradeRecord trade : breakoutStrategy.getTradeRecords()) {
            if (trade.getReason() != null) {
                if (trade.getReason().contains("触发止损")) {
                    hasStopLoss = true;
                } else if (trade.getReason().contains("触发止盈")) {
                    hasTakeProfit = true;
                }
            }
        }

        assertTrue(hasStopLoss || hasTakeProfit, "应至少触发一次止盈或止损");
    }

    /**
     * 创建模拟K线数据
     *
     * @param count 数据点数量
     * @return K线数据列表
     */
    private List<CandlestickEntity> createMockCandlesticks(int count) {
        List<CandlestickEntity> result = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 0, 0);

        // 基础价格
        BigDecimal basePrice = new BigDecimal("10000");

        // 创建波动的价格序列
        for (int i = 0; i < count; i++) {
            // 使用正弦函数创建波动
            double amplitude = 500; // 振幅
            double period = 20; // 周期
            double noise = Math.random() * 200 - 100; // 随机噪声

            double priceChange = amplitude * Math.sin(i * 2 * Math.PI / period) + noise;
            BigDecimal currentPrice = basePrice.add(new BigDecimal(priceChange));

            // 确保价格为正
            if (currentPrice.compareTo(BigDecimal.ZERO) < 0) {
                currentPrice = new BigDecimal("100");
            }

            // 计算开盘价（略低于收盘价）
            BigDecimal openPrice = currentPrice.multiply(new BigDecimal("0.99"));

            // 计算最高价和最低价
            BigDecimal highPrice = currentPrice.multiply(new BigDecimal("1.02"));
            BigDecimal lowPrice = openPrice.multiply(new BigDecimal("0.98"));

            // 创建K线对象
            CandlestickEntity candle = new CandlestickEntity();
            candle.setId((long) i);
            candle.setSymbol("BTC-USDT");
            candle.setIntervalVal("1h");
            candle.setOpenTime(startTime.plusHours(i));
            candle.setCloseTime(startTime.plusHours(i + 1));
            candle.setOpen(openPrice);
            candle.setHigh(highPrice);
            candle.setLow(lowPrice);
            candle.setClose(currentPrice);
            candle.setVolume(new BigDecimal(Math.random() * 100 + 10));
            candle.setQuoteVolume(candle.getVolume().multiply(currentPrice));
            candle.setTrades((long) (Math.random() * 1000 + 100));
            candle.setFetchTime(LocalDateTime.now());

            result.add(candle);
        }

        return result;
    }

    /**
     * 创建用于测试止盈止损的特殊K线数据
     *
     * @return 特殊K线数据列表
     */
    private List<CandlestickEntity> createStopLossTakeProfitTestData() {
        List<CandlestickEntity> result = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 0, 0);

        // 第一阶段：稳定上升趋势，引发买入信号
        for (int i = 0; i < 30; i++) {
            BigDecimal price = new BigDecimal("1000").add(new BigDecimal(i * 10));

            CandlestickEntity candle = new CandlestickEntity();
            candle.setId((long) i);
            candle.setSymbol("BTC-USDT");
            candle.setIntervalVal("1h");
            candle.setOpenTime(startTime.plusHours(i));
            candle.setCloseTime(startTime.plusHours(i + 1));
            candle.setOpen(price.subtract(new BigDecimal("5")));
            candle.setHigh(price.add(new BigDecimal("15")));
            candle.setLow(price.subtract(new BigDecimal("15")));
            candle.setClose(price);
            candle.setVolume(new BigDecimal("100"));
            candle.setQuoteVolume(price.multiply(new BigDecimal("100")));
            candle.setTrades(100L);
            candle.setFetchTime(LocalDateTime.now());

            result.add(candle);
        }

        // 第二阶段：价格大幅上涨，触发止盈
        BigDecimal lastPrice = result.get(result.size() - 1).getClose();
        BigDecimal takeProfit = lastPrice.multiply(new BigDecimal("1.15")); // 涨幅超过10%

        for (int i = 0; i < 5; i++) {
            BigDecimal price = lastPrice.add(new BigDecimal(i * 50));

            // 确保最高价达到止盈水平
            BigDecimal highPrice = (i == 2) ? takeProfit : price.add(new BigDecimal("20"));

            CandlestickEntity candle = new CandlestickEntity();
            candle.setId((long) (30 + i));
            candle.setSymbol("BTC-USDT");
            candle.setIntervalVal("1h");
            candle.setOpenTime(startTime.plusHours(30 + i));
            candle.setCloseTime(startTime.plusHours(31 + i));
            candle.setOpen(price.subtract(new BigDecimal("10")));
            candle.setHigh(highPrice);
            candle.setLow(price.subtract(new BigDecimal("10")));
            candle.setClose(price);
            candle.setVolume(new BigDecimal("200"));
            candle.setQuoteVolume(price.multiply(new BigDecimal("200")));
            candle.setTrades(200L);
            candle.setFetchTime(LocalDateTime.now());

            result.add(candle);
        }

        // 第三阶段：价格稳定后再次上涨，引发新的买入信号
        lastPrice = result.get(result.size() - 1).getClose();

        for (int i = 0; i < 30; i++) {
            BigDecimal price = lastPrice.add(new BigDecimal(i * 5));

            CandlestickEntity candle = new CandlestickEntity();
            candle.setId((long) (35 + i));
            candle.setSymbol("BTC-USDT");
            candle.setIntervalVal("1h");
            candle.setOpenTime(startTime.plusHours(35 + i));
            candle.setCloseTime(startTime.plusHours(36 + i));
            candle.setOpen(price.subtract(new BigDecimal("3")));
            candle.setHigh(price.add(new BigDecimal("10")));
            candle.setLow(price.subtract(new BigDecimal("10")));
            candle.setClose(price);
            candle.setVolume(new BigDecimal("100"));
            candle.setQuoteVolume(price.multiply(new BigDecimal("100")));
            candle.setTrades(100L);
            candle.setFetchTime(LocalDateTime.now());

            result.add(candle);
        }

        // 第四阶段：价格大幅下跌，触发止损
        lastPrice = result.get(result.size() - 1).getClose();
        BigDecimal stopLoss = lastPrice.multiply(new BigDecimal("0.93")); // 跌幅超过5%

        for (int i = 0; i < 5; i++) {
            BigDecimal price = lastPrice.subtract(new BigDecimal(i * 40));

            // 确保最低价达到止损水平
            BigDecimal lowPrice = (i == 2) ? stopLoss : price.subtract(new BigDecimal("20"));

            CandlestickEntity candle = new CandlestickEntity();
            candle.setId((long) (65 + i));
            candle.setSymbol("BTC-USDT");
            candle.setIntervalVal("1h");
            candle.setOpenTime(startTime.plusHours(65 + i));
            candle.setCloseTime(startTime.plusHours(66 + i));
            candle.setOpen(price.add(new BigDecimal("10")));
            candle.setHigh(price.add(new BigDecimal("10")));
            candle.setLow(lowPrice);
            candle.setClose(price);
            candle.setVolume(new BigDecimal("300"));
            candle.setQuoteVolume(price.multiply(new BigDecimal("300")));
            candle.setTrades(300L);
            candle.setFetchTime(LocalDateTime.now());

            result.add(candle);
        }

        return result;
    }
}
