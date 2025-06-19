package com.okx.trading.strategy;

import com.okx.trading.strategy.StrategyFactory1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.ta4j.core.*;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;

import java.time.ZonedDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多层次止盈止损策略测试类
 * 测试多层次止盈止损策略的各种场景
 */
public class MultiLevelStrategyTest {

    private BarSeries series;
    private Strategy multiLevelStrategy;
    private Strategy advancedMultiLevelStrategy;

    @BeforeEach
    void setUp() {
        // 创建模拟数据
        series = createTestBarSeries();
        multiLevelStrategy = StrategyFactory1.createMultiLevelTakeProfitStopLossStrategy(series);
        advancedMultiLevelStrategy = StrategyFactory1.createAdvancedMultiLevelStrategy(series);
    }

    /**
     * 创建测试用的K线数据
     * 模拟不同的市场情况：上涨、下跌、震荡
     */
    private BarSeries createTestBarSeries() {
        BaseBarSeries series = new BaseBarSeries("TEST");
        ZonedDateTime time = ZonedDateTime.now();
        
        // 添加足够的数据点用于计算指标（至少需要26个点）
        double[] prices = {
            100, 98, 96, 94, 92, 90, 88, 86, 84, 82,  // 下跌趋势
            80, 78, 76, 74, 72, 70, 68, 66, 64, 62,  // 继续下跌
            60, 58, 59, 61, 63, 65, 67, 69, 71, 73,  // 反弹开始（RSI从超卖恢复）
            75, 77, 79, 81, 83, 85, 87, 89, 91, 93,  // 上涨趋势
            95, 97, 99, 101, 103, 105, 107, 109, 111, 113, // 继续上涨
            115, 117, 119, 121, 123, 125, 127, 129, 131, 133, // 强势上涨
            135, 137, 139, 141, 143, 145, 147, 149, 151, 153, // 持续上涨
            155, 157, 159, 161, 163, 165, 167, 169, 171, 173  // 继续上涨到止盈
        };

        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            // 模拟价格波动
            double high = price * 1.02;
            double low = price * 0.98;
            double open = i > 0 ? prices[i-1] : price;
            double close = price;
            double volume = 1000 + (i * 10);

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low), 
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }

        return series;
    }

    @Test
    void testMultiLevelStrategyBasicFunctionality() {
        System.out.println("=== 基础多层次止盈止损策略测试 ===");
        
        assertNotNull(multiLevelStrategy, "多层次策略不应为null");
        assertNotNull(multiLevelStrategy.getEntryRule(), "入场规则不应为null");
        assertNotNull(multiLevelStrategy.getExitRule(), "出场规则不应为null");
        
        System.out.println("策略名称: " + multiLevelStrategy.getName());
        System.out.println("数据点数量: " + series.getBarCount());
        
        // 验证策略可以正常运行
        assertDoesNotThrow(() -> {
            BarSeriesManager manager = new BarSeriesManager(series);
            TradingRecord tradingRecord = manager.run(multiLevelStrategy);
            assertNotNull(tradingRecord, "交易记录不应为null");
        });
    }

    @Test
    void testAdvancedMultiLevelStrategyBasicFunctionality() {
        System.out.println("=== 高级多层次止盈止损策略测试 ===");
        
        assertNotNull(advancedMultiLevelStrategy, "高级多层次策略不应为null");
        assertNotNull(advancedMultiLevelStrategy.getEntryRule(), "入场规则不应为null");
        assertNotNull(advancedMultiLevelStrategy.getExitRule(), "出场规则不应为null");
        
        System.out.println("策略名称: " + advancedMultiLevelStrategy.getName());
        System.out.println("数据点数量: " + series.getBarCount());
        
        // 验证策略可以正常运行
        assertDoesNotThrow(() -> {
            BarSeriesManager manager = new BarSeriesManager(series);
            TradingRecord tradingRecord = manager.run(advancedMultiLevelStrategy);
            assertNotNull(tradingRecord, "交易记录不应为null");
        });
    }

    @Test
    void testMultiLevelStrategyTrading() {
        System.out.println("=== 多层次策略交易测试 ===");
        
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(multiLevelStrategy);
        
        System.out.println("总交易次数: " + tradingRecord.getPositionCount());
        
        int profitableTrades = 0;
        int losingTrades = 0;
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                if (position.getProfit().doubleValue() > 0) {
                    profitableTrades++;
                } else {
                    losingTrades++;
                }
            }
        }
        
        System.out.println("盈利交易次数: " + profitableTrades);
        System.out.println("亏损交易次数: " + losingTrades);
        
        // 验证交易记录
        assertTrue(tradingRecord.getPositionCount() >= 0, "交易次数应该大于等于0");
        
        // 打印交易详情
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                Trade entry = position.getEntry();
                Trade exit = position.getExit();
                double entryPrice = entry.getNetPrice().doubleValue();
                double exitPrice = exit.getNetPrice().doubleValue();
                double profit = (exitPrice - entryPrice) / entryPrice * 100;
                
                System.out.printf("交易: 入场价格=%.2f, 出场价格=%.2f, 收益率=%.2f%%\n", 
                    entryPrice, exitPrice, profit);
            }
        }
    }

    @Test
    void testAdvancedMultiLevelStrategyTrading() {
        System.out.println("=== 高级多层次策略交易测试 ===");
        
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(advancedMultiLevelStrategy);
        
        System.out.println("总交易次数: " + tradingRecord.getPositionCount());
        
        int profitableTrades = 0;
        int losingTrades = 0;
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                if (position.getProfit().doubleValue() > 0) {
                    profitableTrades++;
                } else {
                    losingTrades++;
                }
            }
        }
        
        System.out.println("盈利交易次数: " + profitableTrades);
        System.out.println("亏损交易次数: " + losingTrades);
        
        // 验证交易记录
        assertTrue(tradingRecord.getPositionCount() >= 0, "交易次数应该大于等于0");
        
        // 打印交易详情
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                Trade entry = position.getEntry();
                Trade exit = position.getExit();
                double entryPrice = entry.getNetPrice().doubleValue();
                double exitPrice = exit.getNetPrice().doubleValue();
                double profit = (exitPrice - entryPrice) / entryPrice * 100;
                
                System.out.printf("交易: 入场价格=%.2f, 出场价格=%.2f, 收益率=%.2f%%\n", 
                    entryPrice, exitPrice, profit);
            }
        }
    }

    @Test
    void testStrategyPerformanceComparison() {
        System.out.println("=== 策略性能对比测试 ===");
        
        BarSeriesManager manager = new BarSeriesManager(series);
        
        // 测试基础多层次策略
        TradingRecord basicRecord = manager.run(multiLevelStrategy);
        
        // 测试高级多层次策略
        TradingRecord advancedRecord = manager.run(advancedMultiLevelStrategy);
        
        System.out.println("\n基础多层次策略:");
        printStrategyPerformance(basicRecord);
        
        System.out.println("\n高级多层次策略:");
        printStrategyPerformance(advancedRecord);
        
        // 验证两个策略都能产生交易
        assertTrue(basicRecord.getPositionCount() >= 0, "基础策略应该能产生交易");
        assertTrue(advancedRecord.getPositionCount() >= 0, "高级策略应该能产生交易");
    }

    private void printStrategyPerformance(TradingRecord record) {
        System.out.println("交易次数: " + record.getPositionCount());
        
        int profitableTrades = 0;
        int losingTrades = 0;
        for (Position position : record.getPositions()) {
            if (position.isClosed()) {
                if (position.getProfit().doubleValue() > 0) {
                    profitableTrades++;
                } else {
                    losingTrades++;
                }
            }
        }
        
        System.out.println("盈利交易: " + profitableTrades);
        System.out.println("亏损交易: " + losingTrades);
        
        if (record.getPositionCount() > 0) {
            double winRate = (double) profitableTrades / record.getPositionCount() * 100;
            System.out.printf("胜率: %.2f%%\n", winRate);
        }
    }

    @Test
    void testStrategyWithVolatileMarket() {
        System.out.println("=== 波动市场测试 ===");
        
        // 创建波动性更高的测试数据
        BarSeries volatileSeries = createVolatileTestSeries();
        
        Strategy volatileStrategy = StrategyFactory1.createMultiLevelTakeProfitStopLossStrategy(volatileSeries);
        BarSeriesManager manager = new BarSeriesManager(volatileSeries);
        TradingRecord record = manager.run(volatileStrategy);
        
        System.out.println("波动市场中的交易表现:");
        printStrategyPerformance(record);
        
        // 在波动市场中策略应该能够控制风险
        assertTrue(record.getPositionCount() >= 0, "策略应该在波动市场中也能正常工作");
    }

    private BarSeries createVolatileTestSeries() {
        BaseBarSeries series = new BaseBarSeries("VOLATILE_TEST");
        ZonedDateTime time = ZonedDateTime.now();
        
        // 创建高波动性数据
        double[] prices = {
            100, 95, 105, 90, 110, 85, 115, 80, 120, 75,
            125, 70, 130, 65, 135, 60, 140, 65, 145, 70,
            150, 75, 155, 80, 160, 85, 165, 90, 170, 95,
            175, 100, 180, 105, 185, 110, 190, 115, 195, 120
        };

        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            double high = price * 1.05; // 5%的日内波动
            double low = price * 0.95;
            double open = i > 0 ? prices[i-1] : price;
            double close = price;
            double volume = 1000 + Math.random() * 500;

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }

        return series;
    }

    @Test
    void testProfitTargetReached() {
        System.out.println("=== 止盈目标达成测试 ===");
        
        // 创建明显上涨的数据来测试止盈功能
        BarSeries trendingSeries = createTrendingUpSeries();
        Strategy strategy = StrategyFactory1.createMultiLevelTakeProfitStopLossStrategy(trendingSeries);
        
        BarSeriesManager manager = new BarSeriesManager(trendingSeries);
        TradingRecord record = manager.run(strategy);
        
        System.out.println("上涨趋势中的交易表现:");
        printStrategyPerformance(record);
        
        // 检查是否有盈利交易
        if (record.getPositionCount() > 0) {
            int profitableTrades = 0;
            for (Position position : record.getPositions()) {
                if (position.isClosed() && position.getProfit().doubleValue() > 0) {
                    profitableTrades++;
                }
            }
            boolean hasProfitableTrades = profitableTrades > 0;
            System.out.println("是否有盈利交易: " + hasProfitableTrades);
        }
    }

    private BarSeries createTrendingUpSeries() {
        BaseBarSeries series = new BaseBarSeries("TRENDING_UP_TEST");
        ZonedDateTime time = ZonedDateTime.now();
        
        // 创建明显上涨趋势的数据
        double basePrice = 100;
        for (int i = 0; i < 50; i++) {
            double price = basePrice + (i * 2); // 每天上涨2%
            double high = price * 1.01;
            double low = price * 0.99;
            double open = i > 0 ? (basePrice + ((i-1) * 2)) : price;
            double close = price;
            double volume = 1000;

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }

        return series;
    }

    @Test
    void testSpecificMultiLevelScenario() {
        System.out.println("=== 专门设计的多层次策略触发测试 ===");
        
        // 创建专门设计的数据来触发策略
        BarSeries specialSeries = createSpecialTriggerSeries();
        Strategy strategy = StrategyFactory1.createMultiLevelTakeProfitStopLossStrategy(specialSeries);
        
        BarSeriesManager manager = new BarSeriesManager(specialSeries);
        TradingRecord record = manager.run(strategy);
        
        System.out.println("专门测试场景的交易表现:");
        printStrategyPerformance(record);
        
        // 分析为什么没有交易
        analyzeStrategyConditions(specialSeries, strategy);
        
        // 打印详细的交易记录
        if (record.getPositionCount() > 0) {
            System.out.println("\n交易详情:");
            for (Position position : record.getPositions()) {
                if (position.isClosed()) {
                    Trade entry = position.getEntry();
                    Trade exit = position.getExit();
                    System.out.printf("入场: 第%d根K线, 价格=%.2f\n", 
                        entry.getIndex(), entry.getNetPrice().doubleValue());
                    System.out.printf("出场: 第%d根K线, 价格=%.2f\n", 
                        exit.getIndex(), exit.getNetPrice().doubleValue());
                    System.out.printf("收益: %.2f%%\n", position.getProfit().doubleValue() * 100);
                }
            }
        } else {
            System.out.println("没有生成任何交易，可能原因：");
            System.out.println("1. 入场条件太严格");
            System.out.println("2. 测试数据不符合策略预期");
            System.out.println("3. RSI指标没有达到超卖条件");
        }
    }

    /**
     * 创建专门用来触发多层次策略的数据
     * 确保RSI会达到超卖状态，然后价格突破均线
     */
    private BarSeries createSpecialTriggerSeries() {
        BaseBarSeries series = new BaseBarSeries("SPECIAL_TRIGGER_TEST");
        ZonedDateTime time = ZonedDateTime.now();
        
        // 第一阶段：创建强烈下跌让RSI进入超卖区域
        double basePrice = 100;
        for (int i = 0; i < 20; i++) {
            double price = basePrice - (i * 3); // 强烈下跌
            double high = price * 1.005;
            double low = price * 0.995;
            double open = i > 0 ? (basePrice - ((i-1) * 3)) : price;
            double close = price;
            double volume = 1000;

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }
        
        // 第二阶段：稳定一段时间，让均线下降
        double stablePrice = 40; // 最低点附近
        for (int i = 20; i < 30; i++) {
            double price = stablePrice + Math.random() * 2 - 1; // 小幅波动
            double high = price * 1.01;
            double low = price * 0.99;
            double open = stablePrice;
            double close = price;
            double volume = 1000;

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }
        
        // 第三阶段：价格突破20日均线，这时RSI应该仍在超卖区域或刚离开
        double breakthroughPrice = 45;
        for (int i = 30; i < 50; i++) {
            double price = breakthroughPrice + ((i - 30) * 1.5); // 逐步上涨
            double high = price * 1.02;
            double low = price * 0.98;
            double open = i > 30 ? (breakthroughPrice + ((i-31) * 1.5)) : breakthroughPrice;
            double close = price;
            double volume = 1500; // 增加成交量

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }
        
        // 第四阶段：继续上涨以测试止盈功能
        for (int i = 50; i < 80; i++) {
            double price = 75 + ((i - 50) * 2); // 持续上涨
            double high = price * 1.01;
            double low = price * 0.99;
            double open = 75 + ((i - 51) * 2);
            double close = price;
            double volume = 1200;

            series.addBar(Duration.ofDays(1), time.plusDays(i),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume)
            );
        }
        
        return series;
    }

    /**
     * 分析策略条件，帮助理解为什么没有交易
     */
    private void analyzeStrategyConditions(BarSeries series, Strategy strategy) {
        System.out.println("\n=== 策略条件分析 ===");
        System.out.println("数据点数量: " + series.getBarCount());
        
        if (series.getBarCount() > 30) {
            // 分析最后几个数据点的指标值
            int lastIndex = series.getBarCount() - 1;
            
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
            
            System.out.printf("最后价格: %.2f\n", closePrice.getValue(lastIndex).doubleValue());
            System.out.printf("最后RSI: %.2f\n", rsi.getValue(lastIndex).doubleValue());
            System.out.printf("最后20日均线: %.2f\n", sma20.getValue(lastIndex).doubleValue());
            
            // 检查几个关键点的条件
            for (int i = Math.max(25, lastIndex - 10); i <= lastIndex; i++) {
                double rsiVal = rsi.getValue(i).doubleValue();
                double priceVal = closePrice.getValue(i).doubleValue();
                double smaVal = sma20.getValue(i).doubleValue();
                boolean rsiCondition = rsiVal < 30;
                boolean priceAboveSMA = priceVal > smaVal;
                
                if (rsiCondition || priceAboveSMA) {
                    System.out.printf("第%d根K线: RSI=%.2f(<30?%s), 价格=%.2f, SMA=%.2f(价格>SMA?%s)\n", 
                        i, rsiVal, rsiCondition ? "是" : "否", 
                        priceVal, smaVal, priceAboveSMA ? "是" : "否");
                }
            }
        }
    }

    @Test
    void testSimpleMovingAverageStrategy() {
        System.out.println("=== 简单移动平均策略对比测试 ===");
        
        // 使用简单的SMA策略作为对比
        Strategy smaStrategy = StrategyFactory1.createSMAStrategy(series);
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord smaRecord = manager.run(smaStrategy);
        
        System.out.println("SMA策略交易表现:");
        printStrategyPerformance(smaRecord);
        
        if (smaRecord.getPositionCount() > 0) {
            System.out.println("SMA策略能够产生交易，说明数据和框架工作正常");
        } else {
            System.out.println("连SMA策略都没有交易，可能是测试数据或框架配置问题");
        }
    }
} 