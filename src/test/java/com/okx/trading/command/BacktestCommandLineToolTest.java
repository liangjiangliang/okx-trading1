package com.okx.trading.command;

import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.service.BacktestTradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 回测命令行工具测试类
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
public class BacktestCommandLineToolTest {

    @Mock
    private BacktestTradeService backtestTradeService;

    @InjectMocks
    private BacktestCommandLineTool commandLineTool;

    private BacktestSummaryEntity sampleSummary;
    private List<BacktestTradeEntity> sampleTrades;

    @BeforeEach
    void setUp() {
        // 创建示例回测汇总数据
        sampleSummary = new BacktestSummaryEntity();
        sampleSummary.setId(1L);
        sampleSummary.setBacktestId("BT12345");
        sampleSummary.setStrategyName("SMA");
        sampleSummary.setStrategyParams("5,20");
        sampleSummary.setSymbol("BTC-USDT");
        sampleSummary.setIntervalVal("1h");
        sampleSummary.setStartTime(LocalDateTime.now().minusDays(30));
        sampleSummary.setEndTime(LocalDateTime.now());
        sampleSummary.setInitialAmount(new BigDecimal("10000"));
        sampleSummary.setFinalAmount(new BigDecimal("12500"));
        sampleSummary.setTotalProfit(new BigDecimal("2500"));
        sampleSummary.setTotalReturn(new BigDecimal("0.25"));
        sampleSummary.setNumberOfTrades(20);
        sampleSummary.setProfitableTrades(14);
        sampleSummary.setUnprofitableTrades(6);
        sampleSummary.setWinRate(new BigDecimal("0.7"));
        sampleSummary.setMaxDrawdown(new BigDecimal("0.05"));
        sampleSummary.setSharpeRatio(new BigDecimal("1.5"));
        sampleSummary.setCreateTime(LocalDateTime.now());

        // 创建示例交易记录
        sampleTrades = new ArrayList<>();
        BacktestTradeEntity trade1 = new BacktestTradeEntity();
        trade1.setId(1L);
        trade1.setBacktestId("BT12345");
        trade1.setStrategyName("SMA");
        trade1.setStrategyParams("5,20");
        trade1.setSymbol("BTC-USDT");
        trade1.setIndex(1);
        trade1.setType("BUY");
        trade1.setEntryTime(LocalDateTime.now().minusDays(25));
        trade1.setEntryPrice(new BigDecimal("30000"));
        trade1.setExitTime(LocalDateTime.now().minusDays(20));
        trade1.setExitPrice(new BigDecimal("32000"));
        trade1.setProfit(new BigDecimal("2000"));
        trade1.setProfitPercentage(new BigDecimal("0.0666"));
        trade1.setClosed(true);

        BacktestTradeEntity trade2 = new BacktestTradeEntity();
        trade2.setId(2L);
        trade2.setBacktestId("BT12345");
        trade2.setStrategyName("SMA");
        trade2.setStrategyParams("5,20");
        trade2.setSymbol("BTC-USDT");
        trade2.setIndex(2);
        trade2.setType("BUY");
        trade2.setEntryTime(LocalDateTime.now().minusDays(15));
        trade2.setEntryPrice(new BigDecimal("31000"));
        trade2.setExitTime(LocalDateTime.now().minusDays(10));
        trade2.setExitPrice(new BigDecimal("30500"));
        trade2.setProfit(new BigDecimal("-500"));
        trade2.setProfitPercentage(new BigDecimal("-0.0161"));
        trade2.setClosed(true);

        sampleTrades.add(trade1);
        sampleTrades.add(trade2);
    }

    @Test
    void testRunWithoutCommand(CapturedOutput output) throws Exception {
        // 执行无命令的情况
        commandLineTool.run(new String[]{});

        // 验证输出包含帮助信息
        assertThat(output.getOut()).contains("回测命令行工具使用帮助");
        assertThat(output.getOut()).contains("列出最近回测");
        assertThat(output.getOut()).contains("打印特定回测");
        assertThat(output.getOut()).contains("导出回测到CSV");
    }

    @Test
    void testListRecentBacktests(CapturedOutput output) throws Exception {
        // 模拟服务返回数据
        when(backtestTradeService.getAllBacktestSummaries()).thenReturn(Arrays.asList(sampleSummary));

        // 执行列表命令
        commandLineTool.run(new String[]{"--backtest.command=list"});

        // 验证输出包含回测信息
        assertThat(output.getOut()).contains("找到 1 条回测记录");
        assertThat(output.getOut()).contains("BT12345");
        assertThat(output.getOut()).contains("SMA");
        assertThat(output.getOut()).contains("BTC-USDT");
        assertThat(output.getOut()).contains("25.00%");
        assertThat(output.getOut()).contains("70.00%");

        // 验证服务方法调用
        verify(backtestTradeService, times(1)).getAllBacktestSummaries();
    }

    @Test
    void testPrintBacktestResult(CapturedOutput output) throws Exception {
        // 模拟服务返回数据
        when(backtestTradeService.getBacktestSummaryById("BT12345")).thenReturn(Optional.of(sampleSummary));
        when(backtestTradeService.getTradesByBacktestId("BT12345")).thenReturn(sampleTrades);

        // 执行打印命令
        commandLineTool.run(new String[]{"--backtest.command=print", "--backtest.id=BT12345"});

        // 验证输出包含回测详细信息
        assertThat(output.getOut()).contains("查询回测ID为 BT12345 的结果");
        
        // 验证服务方法调用
        verify(backtestTradeService, times(1)).getBacktestSummaryById("BT12345");
        verify(backtestTradeService, times(1)).getTradesByBacktestId("BT12345");
    }

    @Test
    void testPrintBacktestResultNotFound(CapturedOutput output) throws Exception {
        // 模拟服务返回空数据
        when(backtestTradeService.getBacktestSummaryById("NOTFOUND")).thenReturn(Optional.empty());

        // 执行打印命令
        commandLineTool.run(new String[]{"--backtest.command=print", "--backtest.id=NOTFOUND"});

        // 验证输出包含错误信息
        assertThat(output.getOut()).contains("未找到ID为 NOTFOUND 的回测记录");
        
        // 验证服务方法调用
        verify(backtestTradeService, times(1)).getBacktestSummaryById("NOTFOUND");
        verify(backtestTradeService, never()).getTradesByBacktestId(anyString());
    }

    @Test
    void testFindBestBacktests(CapturedOutput output) throws Exception {
        // 模拟服务返回数据
        when(backtestTradeService.getBestPerformingBacktests("SMA", "BTC-USDT")).thenReturn(Arrays.asList(sampleSummary));

        // 执行最佳回测命令
        commandLineTool.run(new String[]{"--backtest.command=best", "--backtest.strategy=SMA", "--backtest.symbol=BTC-USDT"});

        // 验证输出包含最佳回测信息
        assertThat(output.getOut()).contains("最佳表现回测结果");
        assertThat(output.getOut()).contains("BT12345");
        assertThat(output.getOut()).contains("5,20");
        assertThat(output.getOut()).contains("25.00%");
        assertThat(output.getOut()).contains("1.50");
        
        // 验证服务方法调用
        verify(backtestTradeService, times(1)).getBestPerformingBacktests("SMA", "BTC-USDT");
    }

    @Test
    void testExportBacktestToCsv(CapturedOutput output) throws Exception {
        // 模拟服务返回数据
        when(backtestTradeService.getBacktestSummaryById("BT12345")).thenReturn(Optional.of(sampleSummary));
        when(backtestTradeService.getTradesByBacktestId("BT12345")).thenReturn(sampleTrades);

        // 使用临时目录作为导出路径
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/backtest_export_test.csv";

        // 执行导出命令
        commandLineTool.run(new String[]{
            "--backtest.command=export", 
            "--backtest.id=BT12345", 
            "--backtest.export.path=" + tempFilePath
        });

        // 验证输出包含导出成功信息
        assertThat(output.getOut()).contains("导出回测ID为 BT12345 的结果");
        assertThat(output.getOut()).contains("成功导出回测结果到");
        
        // 验证服务方法调用
        verify(backtestTradeService, times(1)).getBacktestSummaryById("BT12345");
        verify(backtestTradeService, times(1)).getTradesByBacktestId("BT12345");
    }
} 