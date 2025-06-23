package com.okx.trading.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.Trade;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TradingMetrics 测试类
 * 测试新增的风险指标计算是否合理
 */
public class TradingMetricsTest {

    private BarSeries series;
    private TradingRecord tradingRecord;
    private BigDecimal initialAmount;
    private BigDecimal finalAmount;
    private BigDecimal totalPnL;
    private BigDecimal totalFees;
    private BigDecimal riskFreeRate;

    @BeforeEach
    void setUp() {
        // 创建测试用的价格序列
        series = new BaseBarSeries("TEST");
        ZonedDateTime now = ZonedDateTime.now();
        
        // 添加一些测试数据（模拟价格变化）
        for (int i = 0; i < 100; i++) {
            double price = 100.0 + (Math.sin(i * 0.1) * 10) + (Math.random() - 0.5) * 5;
            series.addBar(now.plusDays(i), price, price + 1, price - 1, price, 1000);
        }

        // 创建测试交易记录
        tradingRecord = new BaseTradingRecord();
        tradingRecord.enter(10, DecimalNum.valueOf(100), DecimalNum.valueOf(1));
        tradingRecord.exit(20, DecimalNum.valueOf(105), DecimalNum.valueOf(1));
        tradingRecord.enter(30, DecimalNum.valueOf(103), DecimalNum.valueOf(1));
        tradingRecord.exit(40, DecimalNum.valueOf(98), DecimalNum.valueOf(1));

        // 设置测试参数
        initialAmount = BigDecimal.valueOf(10000);
        finalAmount = BigDecimal.valueOf(10200);
        totalPnL = BigDecimal.valueOf(200);
        totalFees = BigDecimal.valueOf(20);
        riskFreeRate = BigDecimal.valueOf(0.03); // 3%年化无风险利率
    }

    @Test
    void testNewRiskMetrics() {
        // 创建TradingMetrics实例
        TradingMetrics metrics = new TradingMetrics(
            series, tradingRecord, initialAmount, finalAmount, 
            totalPnL, totalFees, riskFreeRate
        );

        // 测试信息比率
        BigDecimal informationRatio = metrics.getInformationRatio();
        assertNotNull(informationRatio, "信息比率不应为null");
        System.out.println("信息比率: " + informationRatio);

        // 测试VaR
        BigDecimal var95 = metrics.getVar95();
        BigDecimal var99 = metrics.getVar99();
        assertNotNull(var95, "95% VaR不应为null");
        assertNotNull(var99, "99% VaR不应为null");
        assertTrue(var99.compareTo(var95) >= 0, "99% VaR应该大于或等于95% VaR");
        System.out.println("95% VaR: " + var95);
        System.out.println("99% VaR: " + var99);

        // 测试CVaR
        BigDecimal cvar95 = metrics.getCvar95();
        BigDecimal cvar99 = metrics.getCvar99();
        assertNotNull(cvar95, "95% CVaR不应为null");
        assertNotNull(cvar99, "99% CVaR不应为null");
        assertTrue(cvar99.compareTo(cvar95) >= 0, "99% CVaR应该大于或等于95% CVaR");
        System.out.println("95% CVaR: " + cvar95);
        System.out.println("99% CVaR: " + cvar99);

        // 测试下行波动率
        BigDecimal downsideVolatility = metrics.getDownsideVolatility();
        assertNotNull(downsideVolatility, "下行波动率不应为null");
        assertTrue(downsideVolatility.compareTo(BigDecimal.ZERO) >= 0, "下行波动率应该非负");
        System.out.println("下行波动率: " + downsideVolatility);

        // 测试RoMaD
        BigDecimal romad = metrics.getRomad();
        assertNotNull(romad, "RoMaD不应为null");
        System.out.println("RoMaD: " + romad);

        // 测试其他指标是否仍然正常
        System.out.println("夏普比率: " + metrics.getSharpeRatio());
        System.out.println("索提诺比率: " + metrics.getSortinoRatio());
        System.out.println("最大回撤: " + metrics.getMaxDrawdown());
        System.out.println("波动率: " + metrics.getVolatility());
        System.out.println("胜率: " + metrics.getWinRate() + "%");
    }

    @Test
    void testMetricsLogicalConsistency() {
        TradingMetrics metrics = new TradingMetrics(
            series, tradingRecord, initialAmount, finalAmount, 
            totalPnL, totalFees, riskFreeRate
        );

        // 验证逻辑一致性
        
        // CVaR应该大于或等于对应的VaR
        assertTrue(metrics.getCvar95().compareTo(metrics.getVar95()) >= 0, 
                  "CVaR95应该大于或等于VaR95");
        assertTrue(metrics.getCvar99().compareTo(metrics.getVar99()) >= 0, 
                  "CVaR99应该大于或等于VaR99");

        // 下行波动率应该是非负的（在某些情况下可能大于总波动率）
        assertTrue(metrics.getDownsideVolatility().compareTo(BigDecimal.ZERO) >= 0, 
                  "下行波动率应该非负");
        
        // 打印波动率比较信息
        System.out.println("总波动率: " + metrics.getVolatility());
        System.out.println("下行波动率: " + metrics.getDownsideVolatility());

        // 所有比率类指标都应该是有限的数值
        assertFalse(metrics.getSharpeRatio().toString().contains("Infinity"), 
                   "夏普比率应该是有限数值");
        assertFalse(metrics.getInformationRatio().toString().contains("Infinity"), 
                   "信息比率应该是有限数值");
    }

    @Test
    void testEdgeCases() {
        // 测试边界情况：无交易记录
        TradingRecord emptyRecord = new BaseTradingRecord();
        TradingMetrics emptyMetrics = new TradingMetrics(
            series, emptyRecord, initialAmount, initialAmount, 
            BigDecimal.ZERO, BigDecimal.ZERO, riskFreeRate
        );

        // 所有指标都应该有合理的默认值
        assertNotNull(emptyMetrics.getVar95());
        assertNotNull(emptyMetrics.getCvar95());
        assertNotNull(emptyMetrics.getDownsideVolatility());
        assertNotNull(emptyMetrics.getRomad());
        assertNotNull(emptyMetrics.getInformationRatio());

        System.out.println("=== 边界情况测试 ===");
        System.out.println("无交易VaR95: " + emptyMetrics.getVar95());
        System.out.println("无交易CVaR95: " + emptyMetrics.getCvar95());
        System.out.println("无交易下行波动率: " + emptyMetrics.getDownsideVolatility());
        System.out.println("无交易RoMaD: " + emptyMetrics.getRomad());
    }
} 