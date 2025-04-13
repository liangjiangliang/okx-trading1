package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.model.entity.CandlestickEntity;
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
            @ApiParam(value = "交易对", required = true) @RequestParam String symbol,
            @ApiParam(value = "时间间隔", required = true) @RequestParam String interval,
            @ApiParam(value = "开始时间", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "策略类型", required = true) @RequestParam String strategyType,
            @ApiParam(value = "策略参数", required = true) @RequestParam String strategyParams,
            @ApiParam(value = "初始资金", required = true) @RequestParam BigDecimal initialAmount,
            @ApiParam(value = "是否保存结果", required = false, defaultValue = "false") @RequestParam(defaultValue = "false") boolean saveResult) {
        
        log.info("开始执行Ta4j回测，交易对: {}, 间隔: {}, 时间范围: {} - {}, 策略: {}, 参数: {}, 初始资金: {}",
                symbol, interval, startTime, endTime, strategyType, strategyParams, initialAmount);
        
        try {
            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);
            if (candlesticks == null || candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定条件的历史数据");
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
            return ApiResponse.error(500, "回测过程中发生错误: " + e.getMessage());
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
            @ApiParam(value = "回测ID", required = true) @PathVariable String backtestId) {
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
            @ApiParam(value = "回测ID", required = true) @PathVariable String backtestId) {
        try {
            backtestTradeService.deleteBacktestRecords(backtestId);
            return ApiResponse.success("成功删除回测记录", null);
        } catch (Exception e) {
            log.error("删除回测记录出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "删除回测记录出错: " + e.getMessage());
        }
    }
} 