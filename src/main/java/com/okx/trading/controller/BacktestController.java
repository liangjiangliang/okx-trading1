package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.strategy.backtest.BacktestFramework;
import com.okx.trading.strategy.backtest.SimpleMovingAverageStrategy;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 回测控制器
 */
@Api(tags = "策略回测")
@Slf4j
@Validated
@RestController
@RequestMapping("/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final HistoricalDataService historicalDataService;

    /**
     * 执行简单均线交叉策略回测
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1H, 4H, 1D
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @param initialBalance 初始资金
     * @param shortPeriod  短期均线周期
     * @param longPeriod   长期均线周期
     * @param tradingRatio 交易比例 (0-1)
     * @return 回测结果摘要
     */
    @ApiOperation(value = "执行简单均线交叉策略回测", notes = "使用历史K线数据进行策略回测")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔", required = true, dataType = "String", example = "1H", paramType = "query",
                    allowableValues = "1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2018-01-01 00:00:00", paramType = "query"),
            @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2025-04-01 00:00:00", paramType = "query"),
            @ApiImplicitParam(name = "initialBalance", value = "初始资金", required = false, dataType = "BigDecimal", example = "10000", paramType = "query"),
            @ApiImplicitParam(name = "shortPeriod", value = "短期均线周期", required = false, dataType = "Integer", example = "5", paramType = "query"),
            @ApiImplicitParam(name = "longPeriod", value = "长期均线周期", required = false, dataType = "Integer", example = "20", paramType = "query"),
            @ApiImplicitParam(name = "tradingRatio", value = "交易比例 (0-1)", required = false, dataType = "BigDecimal", example = "0.8", paramType = "query")
    })
    @GetMapping("/sma-crossover")
    public ApiResponse<Map<String, Object>> runSmaCrossoverBacktest(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
            @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr,
            @RequestParam(required = false, defaultValue = "10000") BigDecimal initialBalance,
            @RequestParam(required = false, defaultValue = "5") @Min(value = 2, message = "短期均线周期必须大于1") Integer shortPeriod,
            @RequestParam(required = false, defaultValue = "20") @Min(value = 3, message = "长期均线周期必须大于2") Integer longPeriod,
            @RequestParam(required = false, defaultValue = "0.8") BigDecimal tradingRatio) {

        log.info("开始均线交叉策略回测: symbol={}, interval={}, startTime={}, endTime={}, initialBalance={}, shortPeriod={}, longPeriod={}, tradingRatio={}",
                symbol, interval, startTimeStr, endTimeStr, initialBalance, shortPeriod, longPeriod, tradingRatio);

        try {
            // 将字符串时间转换为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);

            if (candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定时间范围内的历史数据");
            }

            log.info("获取到{}条历史K线数据，开始执行回测", candlesticks.size());

            // 创建并执行策略
            BigDecimal feeRate = new BigDecimal("0.002"); // 双向0.2%手续费
            SimpleMovingAverageStrategy strategy = new SimpleMovingAverageStrategy(
                    initialBalance, feeRate, shortPeriod, longPeriod, tradingRatio);
            strategy.runBacktest(candlesticks);

            // 计算回测结果
            Map<String, Object> result = calculateBacktestResult(strategy, symbol, interval, startTimeStr, endTimeStr, candlesticks.size());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("回测过程中发生错误: {}", e.getMessage(), e);
            return ApiResponse.error(500, "回测失败: " + e.getMessage());
        }
    }

    /**
     * 计算回测结果
     *
     * @param strategy 回测策略
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param startTimeStr 开始时间
     * @param endTimeStr 结束时间
     * @param dataPointsCount 数据点数量
     * @return 回测结果
     */
    private Map<String, Object> calculateBacktestResult(SimpleMovingAverageStrategy strategy,
                                                       String symbol, String interval,
                                                       String startTimeStr, String endTimeStr,
                                                       int dataPointsCount) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 获取最终余额和初始余额
        List<BacktestFramework.BalanceRecord> balanceRecords = strategy.getBalanceRecords();
        BigDecimal initialBalance = strategy.getInitialBalance();
        BacktestFramework.BalanceRecord finalBalance = balanceRecords.get(balanceRecords.size() - 1);

        // 计算总收益和收益率
        BigDecimal totalReturn = finalBalance.getTotalBalance().subtract(initialBalance);
        BigDecimal returnRate = totalReturn.divide(initialBalance, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // 获取交易记录
        List<BacktestFramework.TradeRecord> tradeRecords = strategy.getTradeRecords();

        // 计算交易统计信息
        int totalTrades = tradeRecords.size();
        long buyTrades = tradeRecords.stream()
                .filter(trade -> "买入".equals(trade.getType()))
                .count();
        long sellTrades = tradeRecords.stream()
                .filter(trade -> "卖出".equals(trade.getType()))
                .count();

        // 计算胜率
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        int profitableTrades = 0;
        int losingTrades = 0;

        List<Map<String, Object>> tradesDetails = new ArrayList<>();
        BigDecimal buyValue = null;

        for (BacktestFramework.TradeRecord trade : tradeRecords) {
            Map<String, Object> tradeDetail = new HashMap<>();
            tradeDetail.put("time", trade.getTime().format(formatter));
            tradeDetail.put("type", trade.getType());
            tradeDetail.put("price", trade.getPrice());
            tradeDetail.put("amount", trade.getAmount());
            tradeDetail.put("value", trade.getValue());
            tradeDetail.put("fee", trade.getFee());
            tradeDetail.put("reason", trade.getReason());

            if ("买入".equals(trade.getType())) {
                buyValue = trade.getValue();
            } else if ("卖出".equals(trade.getType()) && buyValue != null) {
                BigDecimal profit = trade.getValue().subtract(buyValue);
                tradeDetail.put("profit", profit);

                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    totalProfit = totalProfit.add(profit);
                    profitableTrades++;
                } else {
                    totalLoss = totalLoss.add(profit.abs());
                    losingTrades++;
                }

                buyValue = null;
            }

            tradesDetails.add(tradeDetail);
        }

        BigDecimal winRate = BigDecimal.ZERO;
        if (profitableTrades + losingTrades > 0) {
            winRate = new BigDecimal(profitableTrades)
                    .divide(new BigDecimal(profitableTrades + losingTrades), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // 构建结果
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("interval", interval);
        result.put("startTime", startTimeStr);
        result.put("endTime", endTimeStr);
        result.put("dataPoints", dataPointsCount);

        Map<String, Object> performanceStats = new HashMap<>();
        performanceStats.put("initialBalance", initialBalance);
        performanceStats.put("finalBalance", finalBalance.getTotalBalance());
        performanceStats.put("totalReturn", totalReturn);
        performanceStats.put("returnRate", returnRate + "%");
        performanceStats.put("trades", totalTrades);
        performanceStats.put("buyTrades", buyTrades);
        performanceStats.put("sellTrades", sellTrades);
        performanceStats.put("winRate", winRate + "%");

        Map<String, Object> finalPosition = new HashMap<>();
        finalPosition.put("units", finalBalance.getPosition());
        finalPosition.put("value", finalBalance.getPositionValue());
        finalPosition.put("cash", finalBalance.getCash());

        result.put("performance", performanceStats);
        result.put("finalPosition", finalPosition);
        result.put("trades", tradesDetails);

        // 价格序列和资产序列
        List<Map<String, Object>> balanceHistory = balanceRecords.stream()
                .map(record -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime().format(formatter));
                    entry.put("totalBalance", record.getTotalBalance());
                    entry.put("cash", record.getCash());
                    entry.put("positionValue", record.getPositionValue());
                    entry.put("price", record.getPrice());
                    return entry;
                })
                .collect(Collectors.toList());

        // 只返回部分余额历史记录，避免数据量过大
        List<Map<String, Object>> sampledBalanceHistory = sampleBalanceHistory(balanceHistory, 100);
        result.put("balanceHistory", sampledBalanceHistory);

        return result;
    }

    /**
     * 采样账户历史数据，限制数据点数量
     *
     * @param balanceHistory 完整的账户历史数据
     * @param maxSamples 最大采样点数
     * @return 采样后的数据
     */
    private List<Map<String, Object>> sampleBalanceHistory(List<Map<String, Object>> balanceHistory, int maxSamples) {
        if (balanceHistory.size() <= maxSamples) {
            return balanceHistory;
        }

        // 采样，确保包含第一个和最后一个点
        List<Map<String, Object>> sampledHistory = new ArrayList<>();
        sampledHistory.add(balanceHistory.get(0));  // 第一个点

        double step = (double) (balanceHistory.size() - 2) / (maxSamples - 2);
        for (int i = 1; i < maxSamples - 1; i++) {
            int index = (int) Math.floor(i * step) + 1;
            sampledHistory.add(balanceHistory.get(index));
        }

        sampledHistory.add(balanceHistory.get(balanceHistory.size() - 1));  // 最后一个点

        return sampledHistory;
    }
}
