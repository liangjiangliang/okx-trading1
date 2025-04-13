package com.okx.trading.command;

import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.service.BacktestTradeService;
import com.okx.trading.util.BacktestResultPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 回测命令行工具类
 * 用于在命令行中查询和导出回测结果
 * 可以通过program arguments激活不同功能
 * 使用方式:
 * --backtest.command=list  列出最近的回测
 * --backtest.command=print --backtest.id=xxx  打印指定ID的回测结果
 * --backtest.command=export --backtest.id=xxx --backtest.export.path=/path/to/file.csv  导出回测结果到CSV
 */
@Component
@ConditionalOnProperty(prefix = "backtest", name = "command")
public class BacktestCommandLineTool implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BacktestCommandLineTool.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private BacktestTradeService backtestTradeService;

    @Override
    public void run(String... args) throws Exception {
        String command = getArgValue(args, "backtest.command");
        
        if (command == null) {
            printHelp();
            return;
        }
        
        switch (command.toLowerCase()) {
            case "list":
                listRecentBacktests();
                break;
            case "print":
                String backtestId = getArgValue(args, "backtest.id");
                if (backtestId != null) {
                    printBacktestResult(backtestId);
                } else {
                    log.error("必须提供backtest.id参数");
                    printHelp();
                }
                break;
            case "export":
                backtestId = getArgValue(args, "backtest.id");
                String exportPath = getArgValue(args, "backtest.export.path");
                
                if (backtestId != null && exportPath != null) {
                    exportBacktestToCSV(backtestId, exportPath);
                } else {
                    log.error("必须提供backtest.id和backtest.export.path参数");
                    printHelp();
                }
                break;
            case "best":
                String strategyName = getArgValue(args, "backtest.strategy");
                String symbol = getArgValue(args, "backtest.symbol");
                
                if (strategyName != null && symbol != null) {
                    findBestBacktests(strategyName, symbol);
                } else {
                    log.error("必须提供backtest.strategy和backtest.symbol参数");
                    printHelp();
                }
                break;
            default:
                log.error("未知命令: {}", command);
                printHelp();
        }
    }
    
    /**
     * 列出最近的回测结果
     */
    private void listRecentBacktests() {
        log.info("获取最近回测结果...");
        List<BacktestSummaryEntity> summaries = backtestTradeService.getAllBacktestSummaries();
        
        if (summaries.isEmpty()) {
            log.info("没有找到回测记录");
            return;
        }
        
        log.info("找到 {} 条回测记录:", summaries.size());
        
        // 打印表头
        System.out.printf("%-20s | %-15s | %-15s | %-10s | %-15s | %-10s | %-10s%n", 
                "回测ID", "策略名称", "交易对", "时间间隔", "总收益率", "胜率", "交易数");
        System.out.println("---------------------------------------------------------------------------------------------");
        
        // 按创建时间降序排序并打印
        summaries.stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .forEach(summary -> {
                    String totalReturnFormatted = summary.getTotalReturn() != null ? 
                            String.format("%.2f%%", summary.getTotalReturn().multiply(new BigDecimal("100"))) : "N/A";
                    String winRateFormatted = summary.getWinRate() != null ? 
                            String.format("%.2f%%", summary.getWinRate().multiply(new BigDecimal("100"))) : "N/A";
                    
                    System.out.printf("%-20s | %-15s | %-15s | %-10s | %-15s | %-10s | %-10d%n",
                            summary.getBacktestId(),
                            summary.getStrategyName(),
                            summary.getSymbol(),
                            summary.getIntervalVal(),
                            totalReturnFormatted,
                            winRateFormatted,
                            summary.getNumberOfTrades());
                });
    }
    
    /**
     * 打印指定回测ID的详细结果
     *
     * @param backtestId 回测ID
     */
    private void printBacktestResult(String backtestId) {
        log.info("查询回测ID为 {} 的结果...", backtestId);
        
        // 获取回测汇总信息
        Optional<BacktestSummaryEntity> summaryOpt = backtestTradeService.getBacktestSummaryById(backtestId);
        if (!summaryOpt.isPresent()) {
            log.error("未找到ID为 {} 的回测记录", backtestId);
            return;
        }
        
        // 获取回测交易详情
        List<BacktestTradeEntity> trades = backtestTradeService.getTradesByBacktestId(backtestId);
        
        // 打印回测汇总
        BacktestResultPrinter.printSummaryEntity(summaryOpt.get());
        
        // 打印交易详情
        if (!trades.isEmpty()) {
            BacktestResultPrinter.printTradeRecords(backtestId, trades);
        } else {
            log.info("该回测没有交易记录");
        }
    }
    
    /**
     * 导出回测结果到CSV文件
     *
     * @param backtestId 回测ID
     * @param filePath CSV文件路径
     */
    private void exportBacktestToCSV(String backtestId, String filePath) {
        log.info("导出回测ID为 {} 的结果到 {}", backtestId, filePath);
        
        // 获取回测汇总信息
        Optional<BacktestSummaryEntity> summaryOpt = backtestTradeService.getBacktestSummaryById(backtestId);
        if (!summaryOpt.isPresent()) {
            log.error("未找到ID为 {} 的回测记录", backtestId);
            return;
        }
        
        // 获取回测交易详情
        List<BacktestTradeEntity> trades = backtestTradeService.getTradesByBacktestId(backtestId);
        
        try {
            File file = new File(filePath);
            
            // 创建目录（如果需要）
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.error("无法创建目录: {}", parentDir.getAbsolutePath());
                    return;
                }
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                BacktestSummaryEntity summary = summaryOpt.get();
                
                // 写入回测汇总信息
                writer.write("回测结果汇总信息\n");
                writer.write("回测ID," + summary.getBacktestId() + "\n");
                writer.write("策略名称," + summary.getStrategyName() + "\n");
                writer.write("策略参数," + summary.getStrategyParams() + "\n");
                writer.write("交易对," + summary.getSymbol() + "\n");
                writer.write("时间间隔," + summary.getIntervalVal() + "\n");
                writer.write("回测时间范围," + summary.getStartTime().format(DATE_FORMATTER) + " 至 " + 
                        summary.getEndTime().format(DATE_FORMATTER) + "\n");
                writer.write("\n");
                
                writer.write("初始资金," + summary.getInitialAmount() + "\n");
                writer.write("最终资金," + summary.getFinalAmount() + "\n");
                writer.write("总盈亏," + summary.getTotalProfit() + "\n");
                writer.write("总收益率," + summary.getTotalReturn().multiply(new BigDecimal("100")) + "%\n");
                writer.write("交易次数," + summary.getNumberOfTrades() + "\n");
                writer.write("盈利交易," + summary.getProfitableTrades() + "\n");
                writer.write("亏损交易," + (summary.getNumberOfTrades() - summary.getProfitableTrades()) + "\n");
                writer.write("胜率," + summary.getWinRate().multiply(new BigDecimal("100")) + "%\n");
                writer.write("最大回撤," + summary.getMaxDrawdown().multiply(new BigDecimal("100")) + "%\n");
                
                if (summary.getSharpeRatio() != null) {
                    writer.write("夏普比率," + summary.getSharpeRatio() + "\n");
                }
                
                writer.write("\n\n");
                
                // 写入交易详情
                if (!trades.isEmpty()) {
                    // CSV表头
                    writer.write("序号,交易类型,入场时间,入场价格,出场时间,出场价格,盈亏金额,盈亏比例,状态\n");
                    
                    // 交易记录
                    for (BacktestTradeEntity trade : trades) {
                        StringBuilder record = new StringBuilder();
                        record.append(trade.getIndex()).append(",");
                        record.append(trade.getType()).append(",");
                        
                        if (trade.getEntryTime() != null) {
                            record.append(trade.getEntryTime().format(DATE_FORMATTER));
                        }
                        record.append(",");
                        
                        record.append(trade.getEntryPrice()).append(",");
                        
                        if (trade.getExitTime() != null) {
                            record.append(trade.getExitTime().format(DATE_FORMATTER));
                        }
                        record.append(",");
                        
                        record.append(trade.getExitPrice()).append(",");
                        record.append(trade.getProfit()).append(",");
                        
                        if (trade.getProfitPercentage() != null) {
                            record.append(trade.getProfitPercentage().multiply(new BigDecimal("100"))).append("%");
                        }
                        record.append(",");
                        
                        record.append(trade.getClosed() ? "已平仓" : "持仓中");
                        record.append("\n");
                        
                        writer.write(record.toString());
                    }
                }
            }
            
            log.info("成功导出回测结果到: {}", file.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("导出CSV文件时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 查找并打印最佳表现的回测结果
     *
     * @param strategyName 策略名称
     * @param symbol 交易对
     */
    private void findBestBacktests(String strategyName, String symbol) {
        log.info("查找策略 {} 在交易对 {} 上的最佳表现回测...", strategyName, symbol);
        
        List<BacktestSummaryEntity> bestResults = backtestTradeService.getBestPerformingBacktests(strategyName, symbol);
        
        if (bestResults.isEmpty()) {
            log.info("没有找到符合条件的回测结果");
            return;
        }
        
        log.info("最佳表现回测结果 (按收益率排序):");
        
        // 打印表头
        System.out.printf("%-20s | %-15s | %-15s | %-10s | %-15s | %-10s | %-10s%n", 
                "回测ID", "策略参数", "总收益率", "胜率", "交易数", "最大回撤", "夏普比率");
        System.out.println("---------------------------------------------------------------------------------------------");
        
        // 打印回测结果
        bestResults.forEach(summary -> {
            String totalReturnFormatted = summary.getTotalReturn() != null ? 
                    String.format("%.2f%%", summary.getTotalReturn().multiply(new BigDecimal("100"))) : "N/A";
            String winRateFormatted = summary.getWinRate() != null ? 
                    String.format("%.2f%%", summary.getWinRate().multiply(new BigDecimal("100"))) : "N/A";
            String maxDrawdownFormatted = summary.getMaxDrawdown() != null ? 
                    String.format("%.2f%%", summary.getMaxDrawdown().multiply(new BigDecimal("100"))) : "N/A";
            String sharpeRatioFormatted = summary.getSharpeRatio() != null ? 
                    String.format("%.2f", summary.getSharpeRatio()) : "N/A";
            
            System.out.printf("%-20s | %-15s | %-15s | %-10s | %-10d | %-10s | %-10s%n",
                    summary.getBacktestId(),
                    summary.getStrategyParams(),
                    totalReturnFormatted,
                    winRateFormatted,
                    summary.getNumberOfTrades(),
                    maxDrawdownFormatted,
                    sharpeRatioFormatted);
        });
    }
    
    /**
     * 从命令行参数获取指定参数的值
     *
     * @param args 命令行参数数组
     * @param key 参数名
     * @return 参数值，如果不存在则返回null
     */
    private String getArgValue(String[] args, String key) {
        for (String arg : args) {
            if (arg.startsWith("--" + key + "=")) {
                return arg.substring(key.length() + 3);
            }
        }
        return null;
    }
    
    /**
     * 打印使用帮助
     */
    private void printHelp() {
        System.out.println("\n回测命令行工具使用帮助:");
        System.out.println("列出最近回测: --backtest.command=list");
        System.out.println("打印特定回测: --backtest.command=print --backtest.id=xxx");
        System.out.println("导出回测到CSV: --backtest.command=export --backtest.id=xxx --backtest.export.path=/path/to/file.csv");
        System.out.println("查找最佳回测: --backtest.command=best --backtest.strategy=SMA --backtest.symbol=BTC-USDT");
    }
} 