package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.service.BacktestTradeService;
import com.okx.trading.service.HistoricalDataService;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Ta4j回测控制器
 * 专门用于Ta4j库的回测及结果存储
 */
@Slf4j
@RestController
@RequestMapping("/api/backtest/ta4j")
@Api(tags = "Ta4j回测控制器", description = "提供基于Ta4j库的策略回测及结果存储接口")
@RequiredArgsConstructor
public class Ta4jBacktestController {

    private final HistoricalDataService historicalDataService;
    private final Ta4jBacktestService ta4jBacktestService;
    private final BacktestTradeService backtestTradeService;

    @GetMapping("/run")
    @ApiOperation(value = "执行Ta4j策略回测", notes = "使用Ta4j库进行策略回测，可选保存结果")
    public ApiResponse<BacktestResultDTO> runBacktest(
            @ApiParam(value = "交易对", defaultValue = "BTC-USDT", required = true, type = "string") @RequestParam String symbol,
            @ApiParam(value = "时间间隔",defaultValue = "1h", required = true, type = "string") @RequestParam String interval,
            @ApiParam(value = "开始时间 (格式: yyyy-MM-dd HH:mm:ss)",
                defaultValue = "2018-01-01 00:00:00", 
                example = "2018-01-01 00:00:00",
                required = true,
                type = "string") 
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间 (格式: yyyy-MM-dd HH:mm:ss)",
                defaultValue = "2023-04-01 00:00:00", 
                example = "2023-04-01 00:00:00",
                required = true,
                type = "string") 
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "策略类型 (SMA: 简单移动平均线策略, BOLLINGER: 布林带策略)",
                   required = true,
                   allowableValues = "SMA,BOLLINGER",
                   example = "BOLLINGER",
                   type = "string")
            @RequestParam String strategyType,
            @ApiParam(value = "策略参数 (以逗号分隔的数字)\n" +
                         "- SMA策略参数: 短期均线周期,长期均线周期 (例如：5,20)\n" +
                         "- BOLLINGER策略参数: 周期,标准差倍数 (例如：20,2.0)",
                   required = true,
                   example = "5,20",
                   type = "string")
            @RequestParam String strategyParams,
            @ApiParam(value = "初始资金",
                   defaultValue = "100000", 
                   required = true,
                   type = "number",
                   format = "decimal")
            @RequestParam BigDecimal initialAmount,
            @ApiParam(value = "是否保存结果", 
                   required = true, 
                   defaultValue = "true",
                   type = "boolean") 
            @RequestParam(defaultValue = "true") boolean saveResult) {

        log.info("开始执行Ta4j回测，交易对: {}, 间隔: {}, 时间范围: {} - {}, 策略: {}, 参数: {}, 初始资金: {}",
                symbol, interval, startTime, endTime, strategyType, strategyParams, initialAmount);

        try {
            // 验证策略类型
            if (!strategyType.equals(Ta4jBacktestService.STRATEGY_SMA) &&
                !strategyType.equals(Ta4jBacktestService.STRATEGY_BOLLINGER_BANDS)) {
                return ApiResponse.error(400, "无效的策略类型: " + strategyType +
                        "，支持的策略类型: SMA, BOLLINGER");
            }

            // 验证策略参数
            if (!Ta4jBacktestService.validateStrategyParams(strategyType, strategyParams)) {
                return ApiResponse.error(400, "无效的策略参数: " + strategyParams +
                        "，正确格式: " + Ta4jBacktestService.getStrategyParamsDescription(strategyType));
            }

            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);
            if (candlesticks == null || candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定条件的历史数据");
            }

            // 执行回测
            BacktestResultDTO result = ta4jBacktestService.backtest(candlesticks, strategyType, initialAmount, strategyParams);

            // 如果需要保存结果到数据库
            if (saveResult && result.isSuccess()) {
                // 保存交易明细
                String backtestId = backtestTradeService.saveBacktestTrades(result, strategyParams);
                
                // 保存汇总信息
                backtestTradeService.saveBacktestSummary(result, strategyParams, symbol, interval, startTime, endTime, backtestId);
                
                result.setParameterDescription(result.getParameterDescription() + " (BacktestID: " + backtestId + ")");
            }

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("回测过程中发生错误: {}", e.getMessage(), e);
            return ApiResponse.error(500, "回测过程中发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/strategies")
    @ApiOperation(value = "获取支持的策略类型和参数说明", notes = "返回系统支持的所有策略类型和对应的参数说明")
    public ApiResponse<Map<String, Map<String, String>>> getStrategies() {
        try {
            Map<String, Map<String, String>> strategies = new HashMap<>();
            
            // SMA策略
            Map<String, String> smaInfo = new HashMap<>();
            smaInfo.put("name", "简单移动平均线策略");
            smaInfo.put("description", "基于短期和长期移动平均线的交叉信号产生买卖信号");
            smaInfo.put("params", Ta4jBacktestService.SMA_PARAMS_DESC);
            strategies.put(Ta4jBacktestService.STRATEGY_SMA, smaInfo);
            
            // 布林带策略
            Map<String, String> bollingerInfo = new HashMap<>();
            bollingerInfo.put("name", "布林带策略");
            bollingerInfo.put("description", "基于价格突破布林带上下轨或回归中轨产生买卖信号");
            bollingerInfo.put("params", Ta4jBacktestService.BOLLINGER_PARAMS_DESC);
            strategies.put(Ta4jBacktestService.STRATEGY_BOLLINGER_BANDS, bollingerInfo);
            
            return ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("获取策略信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取策略信息出错: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    @ApiOperation(value = "获取回测历史记录", notes = "获取所有已保存的回测历史ID")
    public ApiResponse<List<String>> getBacktestHistory() {
        try {
            List<String> backtestIds = backtestTradeService.getAllBacktestIds();
            return ApiResponse.success(backtestIds);
        } catch (Exception e) {
            log.error("获取回测历史记录出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测历史记录出错: " + e.getMessage());
        }
    }

    @GetMapping("/detail/{backtestId}")
    @ApiOperation(value = "获取回测详情", notes = "获取指定回测ID的详细交易记录")
    public ApiResponse<List<BacktestTradeEntity>> getBacktestDetail(
            @ApiParam(value = "回测ID", required = true, type = "string") @PathVariable String backtestId) {
        try {
            List<BacktestTradeEntity> trades = backtestTradeService.getTradesByBacktestId(backtestId);
            if (trades.isEmpty()) {
                return ApiResponse.error(404, "未找到指定回测ID的交易记录");
            }
            return ApiResponse.success(trades);
        } catch (Exception e) {
            log.error("获取回测详情出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测详情出错: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{backtestId}")
    @ApiOperation(value = "删除回测记录", notes = "删除指定回测ID的所有交易记录")
    public ApiResponse<Void> deleteBacktestRecord(
            @ApiParam(value = "回测ID", required = true, type = "string") @PathVariable String backtestId) {
        try {
            backtestTradeService.deleteBacktestRecords(backtestId);
            return ApiResponse.success("成功删除回测记录", null);
        } catch (Exception e) {
            log.error("删除回测记录出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "删除回测记录出错: " + e.getMessage());
        }
    }

    @GetMapping("/summaries")
    @ApiOperation(value = "获取所有回测汇总信息", notes = "获取所有已保存的回测汇总信息")
    public ApiResponse<List<BacktestSummaryEntity>> getAllBacktestSummaries() {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getAllBacktestSummaries();
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测汇总信息出错: " + e.getMessage());
        }
    }
    
    @GetMapping("/summary/{backtestId}")
    @ApiOperation(value = "获取回测汇总信息", notes = "根据回测ID获取回测汇总信息")
    public ApiResponse<BacktestSummaryEntity> getBacktestSummary(
            @ApiParam(value = "回测ID", required = true, type = "string") @PathVariable String backtestId) {
        try {
            Optional<BacktestSummaryEntity> summary = backtestTradeService.getBacktestSummaryById(backtestId);
            if (summary.isPresent()) {
                return ApiResponse.success(summary.get());
            } else {
                return ApiResponse.error(404, "未找到指定回测ID的汇总信息");
            }
        } catch (Exception e) {
            log.error("获取回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测汇总信息出错: " + e.getMessage());
        }
    }
    
    @GetMapping("/summaries/strategy/{strategyName}")
    @ApiOperation(value = "根据策略名称获取回测汇总信息", notes = "获取特定策略的所有回测汇总信息")
    public ApiResponse<List<BacktestSummaryEntity>> getBacktestSummariesByStrategy(
            @ApiParam(value = "策略名称", required = true, type = "string") @PathVariable String strategyName) {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getBacktestSummariesByStrategy(strategyName);
            if (summaries.isEmpty()) {
                return ApiResponse.error(404, "未找到该策略的回测汇总信息");
            }
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取策略回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取策略回测汇总信息出错: " + e.getMessage());
        }
    }
    
    @GetMapping("/summaries/symbol/{symbol}")
    @ApiOperation(value = "根据交易对获取回测汇总信息", notes = "获取特定交易对的所有回测汇总信息")
    public ApiResponse<List<BacktestSummaryEntity>> getBacktestSummariesBySymbol(
            @ApiParam(value = "交易对", required = true, type = "string") @PathVariable String symbol) {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getBacktestSummariesBySymbol(symbol);
            if (summaries.isEmpty()) {
                return ApiResponse.error(404, "未找到该交易对的回测汇总信息");
            }
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取交易对回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取交易对回测汇总信息出错: " + e.getMessage());
        }
    }
    
    @GetMapping("/summaries/best")
    @ApiOperation(value = "获取最佳表现的回测", notes = "根据策略名称和交易对获取表现最好的回测")
    public ApiResponse<List<BacktestSummaryEntity>> getBestPerformingBacktests(
            @ApiParam(value = "策略名称", required = true, type = "string") @RequestParam String strategyName,
            @ApiParam(value = "交易对", required = true, type = "string") @RequestParam String symbol) {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getBestPerformingBacktests(strategyName, symbol);
            if (summaries.isEmpty()) {
                return ApiResponse.error(404, "未找到符合条件的回测汇总信息");
            }
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取最佳表现回测信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取最佳表现回测信息出错: " + e.getMessage());
        }
    }
}
