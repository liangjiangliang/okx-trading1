package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.BacktestTradeService;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.backtest.BacktestFramework;
import com.okx.trading.strategy.BollingerBandsStrategy;
import com.okx.trading.strategy.SimpleMovingAverageStrategy;
import com.okx.trading.ta4j.Ta4jBacktestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回测控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/backtest")
@Api(tags = "回测控制器", description = "提供策略回测相关接口")
@RequiredArgsConstructor
public class BacktestController {

    private final HistoricalDataService historicalDataService;
    private final Ta4jBacktestService ta4jBacktestService;
    private final BacktestTradeService backtestTradeService;

    /**
     * 简单移动平均线策略回测
     *
     * @param symbol       交易对
     * @param interval     K线周期
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param shortPeriod  短期均线周期
     * @param longPeriod   长期均线周期
     * @param initialBalance 初始资金
     * @param feeRate      手续费率
     * @param tradingRatio 交易比例
     * @return 回测结果
     */
    @GetMapping("/sma")
    @ApiOperation("简单移动平均线策略回测")
    public ApiResponse<?> testSimpleMovingAverageStrategy(
            @ApiParam(value = "交易对", defaultValue = "BTC-USDT",required = true) @RequestParam String symbol,
            @ApiParam(value = "K线周期",defaultValue ="1H", required = true) @RequestParam String interval,
            @ApiParam(value = "开始时间",defaultValue = "2018-01-01 00:00:00", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间",defaultValue = "2025-04-01 00:00:00", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "短期均线周期", defaultValue = "5") @RequestParam(defaultValue = "5") int shortPeriod,
            @ApiParam(value = "长期均线周期", defaultValue = "20") @RequestParam(defaultValue = "20") int longPeriod,
            @ApiParam(value = "初始资金", defaultValue = "10000") @RequestParam(defaultValue = "10000") BigDecimal initialBalance,
            @ApiParam(value = "手续费率", defaultValue = "0.002") @RequestParam(defaultValue = "0.002") BigDecimal feeRate,
            @ApiParam(value = "交易比例", defaultValue = "1.0") @RequestParam(defaultValue = "1.0") BigDecimal tradingRatio) {

        log.info("开始简单移动平均线策略回测, symbol: {}, interval: {}, startTime: {}, endTime: {}, shortPeriod: {}, longPeriod: {}, initialBalance: {}, feeRate: {}, tradingRatio: {}",
                symbol, interval, startTime, endTime, shortPeriod, longPeriod, initialBalance, feeRate, tradingRatio);

        try {
            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(
                    symbol, interval, startTime, endTime);

            if (candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定时间范围内的K线数据");
            }

            // 创建策略实例
            SimpleMovingAverageStrategy strategy = new SimpleMovingAverageStrategy(
                    initialBalance, feeRate, shortPeriod, longPeriod, tradingRatio);

            // 运行回测
            strategy.runBacktest(candlesticks);

            // 构建返回结果
            Map<String, Object> result = buildBacktestResult(strategy, candlesticks, "简单移动平均线策略");

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("回测过程中发生错误", e);
            return ApiResponse.error(500, "回测过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 布林带策略回测
     *
     * @param symbol          交易对
     * @param interval        K线周期
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @param period          布林带周期
     * @param multiplier      标准差倍数
     * @param initialBalance  初始资金
     * @param feeRate         手续费率
     * @param tradingRatio    交易比例
     * @param stopLoss        止损百分比
     * @param takeProfit      止盈百分比
     * @param tradingMode     交易模式
     * @param useSMAFilter    是否使用均线过滤
     * @param smaFilterPeriod 均线过滤周期
     * @return 回测结果
     */
    @GetMapping("/bollinger")
    @ApiOperation("布林带策略回测")
    public ApiResponse<?> testBollingerBandsStrategy(
            @ApiParam(value = "交易对", required = true) @RequestParam String symbol,
            @ApiParam(value = "K线周期", required = true) @RequestParam String interval,
            @ApiParam(value = "开始时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "布林带周期", defaultValue = "20") @RequestParam(defaultValue = "20") int period,
            @ApiParam(value = "标准差倍数", defaultValue = "2.0") @RequestParam(defaultValue = "2.0") double multiplier,
            @ApiParam(value = "初始资金", defaultValue = "10000") @RequestParam(defaultValue = "10000") BigDecimal initialBalance,
            @ApiParam(value = "手续费率", defaultValue = "0.002") @RequestParam(defaultValue = "0.002") BigDecimal feeRate,
            @ApiParam(value = "交易比例", defaultValue = "1.0") @RequestParam(defaultValue = "1.0") BigDecimal tradingRatio,
            @ApiParam(value = "止损百分比", defaultValue = "0.05") @RequestParam(defaultValue = "0.05") BigDecimal stopLoss,
            @ApiParam(value = "止盈百分比", defaultValue = "0.1") @RequestParam(defaultValue = "0.1") BigDecimal takeProfit,
            @ApiParam(value = "交易模式", defaultValue = "BREAKOUT", allowableValues = "BREAKOUT,REVERSAL,SQUEEZE")
            @RequestParam(defaultValue = "BREAKOUT") String tradingMode,
            @ApiParam(value = "是否使用均线过滤", defaultValue = "false") @RequestParam(defaultValue = "false") boolean useSMAFilter,
            @ApiParam(value = "均线过滤周期", defaultValue = "50") @RequestParam(defaultValue = "50") int smaFilterPeriod) {

        log.info("开始布林带策略回测, symbol: {}, interval: {}, startTime: {}, endTime: {}, period: {}, multiplier: {}, initialBalance: {}, feeRate: {}, tradingRatio: {}, stopLoss: {}, takeProfit: {}, tradingMode: {}, useSMAFilter: {}, smaFilterPeriod: {}",
                symbol, interval, startTime, endTime, period, multiplier, initialBalance, feeRate, tradingRatio, stopLoss, takeProfit, tradingMode, useSMAFilter, smaFilterPeriod);

        try {
            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(
                    symbol, interval, startTime, endTime);

            if (candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定时间范围内的K线数据");
            }

            // 解析交易模式
            BollingerBandsStrategy.TradingMode mode;
            try {
                mode = BollingerBandsStrategy.TradingMode.valueOf(tradingMode);
            } catch (IllegalArgumentException e) {
                return ApiResponse.error(400, "无效的交易模式: " + tradingMode);
            }

            // 创建策略实例
            BollingerBandsStrategy strategy = new BollingerBandsStrategy(
                    initialBalance, feeRate, period, multiplier, tradingRatio,
                    stopLoss, takeProfit, mode, useSMAFilter, smaFilterPeriod);

            // 运行回测
            strategy.runBacktest(candlesticks);

            // 构建返回结果
            Map<String, Object> result = buildBacktestResult(strategy, candlesticks, "布林带策略(" + tradingMode + ")");

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("回测过程中发生错误", e);
            return ApiResponse.error(500, "回测过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 构建回测结果
     *
     * @param strategy     回测策略
     * @param candlesticks K线数据
     * @param strategyName 策略名称
     * @return 回测结果
     */
    private Map<String, Object> buildBacktestResult(BacktestFramework strategy, List<CandlestickEntity> candlesticks, String strategyName) {
        Map<String, Object> result = new HashMap<>();

        // 基本信息
        result.put("strategyName", strategyName);
        result.put("initialBalance", strategy.getInitialBalance());
        result.put("finalBalance", strategy.getCash().add(strategy.getPositionValue()));
        result.put("totalProfit", strategy.getCash().add(strategy.getPositionValue()).subtract(strategy.getInitialBalance()));
        result.put("profitPercentage", strategy.getCash().add(strategy.getPositionValue())
                .subtract(strategy.getInitialBalance())
                .multiply(new BigDecimal("100"))
                .divide(strategy.getInitialBalance(), 2, BigDecimal.ROUND_HALF_UP));

        // 交易统计
        List<BacktestFramework.TradeRecord> trades = strategy.getTradeRecords();
        result.put("totalTrades", trades.size());

        int winTrades = 0;
        int lossTrades = 0;
        BigDecimal totalWinAmount = BigDecimal.ZERO;
        BigDecimal totalLossAmount = BigDecimal.ZERO;

        for (int i = 0; i < trades.size(); i += 2) {
            if (i + 1 < trades.size()) {
                BacktestFramework.TradeRecord buyTrade = trades.get(i);
                BacktestFramework.TradeRecord sellTrade = trades.get(i + 1);

                if ("买入".equals(buyTrade.getType()) && "卖出".equals(sellTrade.getType())) {
                    BigDecimal buyValue = buyTrade.getAmount().multiply(buyTrade.getPrice());
                    BigDecimal sellValue = sellTrade.getAmount().multiply(sellTrade.getPrice());
                    BigDecimal profit = sellValue.subtract(buyValue);

                    if (profit.compareTo(BigDecimal.ZERO) > 0) {
                        winTrades++;
                        totalWinAmount = totalWinAmount.add(profit);
                    } else {
                        lossTrades++;
                        totalLossAmount = totalLossAmount.add(profit.abs());
                    }
                }
            }
        }

        result.put("winTrades", winTrades);
        result.put("lossTrades", lossTrades);
        result.put("winRate", trades.size() > 0 ? (double)winTrades / ((winTrades + lossTrades) * 1.0) : 0);
        result.put("totalWinAmount", totalWinAmount);
        result.put("totalLossAmount", totalLossAmount);
        result.put("profitFactor", totalLossAmount.compareTo(BigDecimal.ZERO) > 0 ?
                                   totalWinAmount.divide(totalLossAmount, 2, BigDecimal.ROUND_HALF_UP) :
                                   BigDecimal.ZERO);

        // 交易记录和余额变化
        result.put("trades", trades);
        result.put("balanceHistory", strategy.getBalanceRecords());

        // 市场数据
        result.put("dataPoints", candlesticks.size());
        result.put("startDate", candlesticks.get(0).getOpenTime());
        result.put("endDate", candlesticks.get(candlesticks.size() - 1).getCloseTime());

        return result;
    }

    @GetMapping("/ta4j")
    @ApiOperation(value = "基于Ta4j的策略回测", notes = "使用Ta4j库进行策略回测")
    public ApiResponse<BacktestResultDTO> backtestWithTa4j(
            @ApiParam(value = "交易对", required = true) @RequestParam String symbol,
            @ApiParam(value = "时间间隔", required = true) @RequestParam String interval,
            @ApiParam(value = "开始时间", required = true) 
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) 
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "策略类型", required = true) @RequestParam String strategyType,
            @ApiParam(value = "策略参数", required = true) @RequestParam String strategyParams,
            @ApiParam(value = "初始资金", required = true) @RequestParam BigDecimal initialAmount,
            @ApiParam(value = "是否保存结果", required = false, defaultValue = "false") @RequestParam(defaultValue = "false") boolean saveResult) {

        log.info("开始执行回测，交易对: {}, 间隔: {}, 时间范围: {} - {}, 策略: {}, 参数: {}, 初始资金: {}",
                symbol, interval, startTime, endTime, strategyType, strategyParams, initialAmount);

        try {
            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);
            if (candlesticks == null || candlesticks.isEmpty()) {
                return ApiResponse.error("未找到指定条件的历史数据");
            }

            // 执行回测
            BacktestResultDTO result = ta4jBacktestService.backtest(candlesticks, strategyType, initialAmount, strategyParams);

            // 如果需要保存结果到数据库
            if (saveResult && result.isSuccess()) {
                String backtestId = backtestTradeService.saveBacktestTrades(result, strategyParams);
                result.setParameterDescription(result.getParameterDescription() + " (BacktestID: " + backtestId + ")");
            }

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("回测过程中发生错误: {}", e.getMessage(), e);
            return ApiResponse.error("回测过程中发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/backtest-history")
    @ApiOperation(value = "获取回测历史记录", notes = "获取所有已保存的回测历史ID")
    public ApiResponse<List<String>> getBacktestHistory() {
        try {
            List<String> backtestIds = backtestTradeService.getAllBacktestIds();
            return ApiResponse.success(backtestIds);
        } catch (Exception e) {
            log.error("获取回测历史记录出错: {}", e.getMessage(), e);
            return ApiResponse.error("获取回测历史记录出错: " + e.getMessage());
        }
    }

    @GetMapping("/backtest-detail/{backtestId}")
    @ApiOperation(value = "获取回测详情", notes = "获取指定回测ID的详细交易记录")
    public ApiResponse<List<BacktestTradeEntity>> getBacktestDetail(
            @ApiParam(value = "回测ID", required = true) @PathVariable String backtestId) {
        try {
            List<BacktestTradeEntity> trades = backtestTradeService.getTradesByBacktestId(backtestId);
            if (trades.isEmpty()) {
                return ApiResponse.error("未找到指定回测ID的交易记录");
            }
            return ApiResponse.success(trades);
        } catch (Exception e) {
            log.error("获取回测详情出错: {}", e.getMessage(), e);
            return ApiResponse.error("获取回测详情出错: " + e.getMessage());
        }
    }

    @DeleteMapping("/backtest/{backtestId}")
    @ApiOperation(value = "删除回测记录", notes = "删除指定回测ID的所有交易记录")
    public ApiResponse<String> deleteBacktestRecord(
            @ApiParam(value = "回测ID", required = true) @PathVariable String backtestId) {
        try {
            backtestTradeService.deleteBacktestRecords(backtestId);
            return ApiResponse.success( "成功删除回测记录");
        } catch (Exception e) {
            log.error("删除回测记录出错: {}", e.getMessage(), e);
            return ApiResponse.error("删除回测记录出错: " + e.getMessage());
        }
    }
}
