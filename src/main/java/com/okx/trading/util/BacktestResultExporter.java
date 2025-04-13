package com.okx.trading.util;

import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.repository.BacktestSummaryRepository;
import com.okx.trading.repository.BacktestTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 回测结果导出工具类
 * 用于查询和导出回测结果
 */
@Component
public class BacktestResultExporter {

    private static final Logger log = LoggerFactory.getLogger(BacktestResultExporter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BacktestSummaryRepository backtestSummaryRepository;

    @Autowired
    private BacktestTradeRepository backtestTradeRepository;

    /**
     * 打印指定回测ID的回测结果汇总
     *
     * @param backtestId 回测ID
     */
    public void printBacktestSummary(String backtestId) {
        if (backtestId == null || backtestId.isEmpty()) {
            log.warn("回测ID为空，无法查询回测结果");
            return;
        }

        Optional<BacktestSummaryEntity> summaryOpt = backtestSummaryRepository.findByBacktestId(backtestId);
        if (!summaryOpt.isPresent()) {
            log.warn("未找到回测ID为 {} 的回测汇总记录", backtestId);
            return;
        }

        BacktestSummaryEntity summary = summaryOpt.get();
        BacktestResultPrinter.printSummaryEntity(summary);
    }

    /**
     * 打印指定回测ID的交易明细
     *
     * @param backtestId 回测ID
     */
    public void printBacktestTrades(String backtestId) {
        if (backtestId == null || backtestId.isEmpty()) {
            log.warn("回测ID为空，无法查询交易记录");
            return;
        }

        List<BacktestTradeEntity> trades = backtestTradeRepository.findByBacktestIdOrderByIndexAsc(backtestId);
        if (trades.isEmpty()) {
            log.warn("未找到回测ID为 {} 的交易记录", backtestId);
            return;
        }

        // 打印交易记录表头
        StringBuilder sb = new StringBuilder();
        String separator = "================================================================";

        sb.append("\n").append(separator).append("\n");
        sb.append("====== 回测ID: ").append(backtestId).append(" 的交易明细记录 ======\n");
        sb.append(separator).append("\n");

        // 表头
        sb.append(String.format("%-4s | %-10s | %-19s | %-12s | %-19s | %-12s | %-12s | %-9s | %s\n",
                "序号", "类型", "入场时间", "入场价格", "出场时间", "出场价格", "盈亏金额", "盈亏比例", "状态"));

        sb.append("----------------------------------------------------------------\n");

        // 内容
        for (BacktestTradeEntity trade : trades) {
            String entryTime = trade.getEntryTime() != null ? trade.getEntryTime().format(DATE_FORMATTER) : "-";
            String exitTime = trade.getExitTime() != null ? trade.getExitTime().format(DATE_FORMATTER) : "-";

            String profitFormatted = trade.getProfit() != null ? String.format("%,.2f", trade.getProfit()) : "-";
            String profitPercentageFormatted = trade.getProfitPercentage() != null ?
                    String.format("%.2f%%", trade.getProfitPercentage().multiply(new BigDecimal("100"))) : "-";

            sb.append(String.format("%-4d | %-10s | %-19s | %-12s | %-19s | %-12s | %-12s | %-9s | %s\n",
                    trade.getIndex(),
                    trade.getType(),
                    entryTime,
                    trade.getEntryPrice(),
                    exitTime,
                    trade.getExitPrice(),
                    profitFormatted,
                    profitPercentageFormatted,
                    trade.getClosed() ? "已平仓" : "持仓中"));
        }

        sb.append(separator).append("\n");

        log.info(sb.toString());
    }

    /**
     * 打印指定回测ID的完整回测结果（汇总和交易明细）
     *
     * @param backtestId 回测ID
     */
    public void printFullBacktestResult(String backtestId) {
        if (backtestId == null || backtestId.isEmpty()) {
            log.warn("回测ID为空，无法查询回测结果");
            return;
        }

        printBacktestSummary(backtestId);
        printBacktestTrades(backtestId);
    }

    /**
     * 打印最近的n条回测汇总记录
     *
     * @param count 记录数量
     */
    public void printRecentBacktestSummaries(int count) {
        List<BacktestSummaryEntity> summaries = backtestSummaryRepository.findAll();

        if (summaries.isEmpty()) {
            log.warn("未找到任何回测汇总记录");
            return;
        }

        // 排序并限制数量
        summaries.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        int resultCount = Math.min(count, summaries.size());
        summaries = summaries.subList(0, resultCount);

        // 打印汇总表头
        StringBuilder sb = new StringBuilder();
        String separator = "================================================================";

        sb.append("\n").append(separator).append("\n");
        sb.append("==================== 最近").append(resultCount).append("条回测汇总记录 ====================\n");
        sb.append(separator).append("\n");

        // 表头
        sb.append(String.format("%-36s | %-15s | %-12s | %-12s | %-8s | %-8s | %-7s | %s\n",
                "回测ID", "策略名称", "交易对", "初始资金", "交易次数", "总收益率", "胜率", "创建时间"));

        sb.append("----------------------------------------------------------------\n");

        // 内容
        for (BacktestSummaryEntity summary : summaries) {
            String totalReturnFormatted = String.format("%.2f%%", summary.getTotalReturn().multiply(new BigDecimal("100")));
            String winRateFormatted = String.format("%.2f%%", summary.getWinRate().multiply(new BigDecimal("100")));
            String createTime = summary.getCreateTime() != null ? summary.getCreateTime().format(DATE_FORMATTER) : "-";

            sb.append(String.format("%-36s | %-15s | %-12s | %-12s | %-8d | %-8s | %-7s | %s\n",
                    summary.getBacktestId(),
                    summary.getStrategyName(),
                    summary.getSymbol(),
                    String.format("%,.2f", summary.getInitialAmount()),
                    summary.getNumberOfTrades(),
                    totalReturnFormatted,
                    winRateFormatted,
                    createTime));
        }

        sb.append(separator).append("\n");

        log.info(sb.toString());
    }

    /**
     * 将回测结果导出到CSV文件
     *
     * @param backtestId 回测ID
     * @param filePath 文件路径
     * @return 是否导出成功
     */
    public boolean exportToCSV(String backtestId, String filePath) {
        if (backtestId == null || backtestId.isEmpty()) {
            log.warn("回测ID为空，无法导出回测结果");
            return false;
        }

        Optional<BacktestSummaryEntity> summaryOpt = backtestSummaryRepository.findByBacktestId(backtestId);
        if (!summaryOpt.isPresent()) {
            log.warn("未找到回测ID为 {} 的回测汇总记录", backtestId);
            return false;
        }

        List<BacktestTradeEntity> trades = backtestTradeRepository.findByBacktestIdOrderByIndexAsc(backtestId);
        if (trades.isEmpty()) {
            log.warn("未找到回测ID为 {} 的交易记录", backtestId);
            return false;
        }

        BacktestSummaryEntity summary = summaryOpt.get();

        try (FileWriter writer = new FileWriter(filePath)) {
            // 写入汇总信息
            writer.write("# 回测汇总信息\n");
            writer.write("回测ID," + summary.getBacktestId() + "\n");
            writer.write("交易对," + summary.getSymbol() + "\n");
            writer.write("时间间隔," + summary.getIntervalVal() + "\n");
            writer.write("策略名称," + summary.getStrategyName() + "\n");
            writer.write("策略参数," + summary.getStrategyParams() + "\n");
            writer.write("回测开始时间," + summary.getStartTime().format(DATE_FORMATTER) + "\n");
            writer.write("回测结束时间," + summary.getEndTime().format(DATE_FORMATTER) + "\n");
            writer.write("初始资金," + summary.getInitialAmount() + "\n");
            writer.write("最终资金," + summary.getFinalAmount() + "\n");
            writer.write("总盈亏," + summary.getTotalProfit() + "\n");
            writer.write("总收益率," + summary.getTotalReturn().multiply(new BigDecimal("100")) + "%\n");
            writer.write("交易次数," + summary.getNumberOfTrades() + "\n");
            writer.write("盈利交易次数," + summary.getProfitableTrades() + "\n");
            writer.write("亏损交易次数," + summary.getUnprofitableTrades() + "\n");
            writer.write("胜率," + summary.getWinRate().multiply(new BigDecimal("100")) + "%\n");
            writer.write("最大回撤," + summary.getMaxDrawdown().multiply(new BigDecimal("100")) + "%\n");
            writer.write("夏普比率," + summary.getSharpeRatio() + "\n\n");

            // 写入交易记录
            writer.write("# 交易记录\n");
            writer.write("序号,类型,入场时间,入场价格,出场时间,出场价格,盈亏金额,盈亏比例,状态\n");

            for (BacktestTradeEntity trade : trades) {
                String entryTime = trade.getEntryTime() != null ? trade.getEntryTime().format(DATE_FORMATTER) : "";
                String exitTime = trade.getExitTime() != null ? trade.getExitTime().format(DATE_FORMATTER) : "";
                String profitPercentage = trade.getProfitPercentage() != null ?
                        trade.getProfitPercentage().multiply(new BigDecimal("100")) + "%" : "";

                writer.write(trade.getIndex() + ",");
                writer.write(trade.getType() + ",");
                writer.write(entryTime + ",");
                writer.write(trade.getEntryPrice() + ",");
                writer.write(exitTime + ",");
                writer.write(trade.getExitPrice() + ",");
                writer.write(trade.getProfit() + ",");
                writer.write(profitPercentage + ",");
                writer.write((trade.getClosed() ? "已平仓" : "持仓中") + "\n");
            }

            log.info("成功导出回测结果到: {}", filePath);
            return true;
        } catch (IOException e) {
            log.error("导出回测结果到CSV文件时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
}
