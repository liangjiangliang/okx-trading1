package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.ta4j.Ta4jBacktestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ta4j回测控制器
 * 提供基于Ta4j库的回测API接口
 */
@Api(tags = "Ta4j回测接口", description = "使用Ta4j库进行策略回测")
@RestController
@RequestMapping("/api/ta4j/backtest")
public class Ta4jBacktestController {

    private static final Logger log = LoggerFactory.getLogger(Ta4jBacktestController.class);

    private final HistoricalDataService historicalDataService;
    private final Ta4jBacktestService ta4jBacktestService;

    @Autowired
    public Ta4jBacktestController(HistoricalDataService historicalDataService, Ta4jBacktestService ta4jBacktestService) {
        this.historicalDataService = historicalDataService;
        this.ta4jBacktestService = ta4jBacktestService;
    }

    /**
     * 执行SMA策略回测
     * 
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param shortPeriod 短期均线周期
     * @param longPeriod 长期均线周期
     * @param initialAmount 初始资金
     * @return 回测结果
     */
    @ApiOperation(value = "SMA策略回测", notes = "使用简单移动平均线策略进行回测")
    @GetMapping("/sma")
    public ApiResponse<BacktestResultDTO> backtestSMA(
            @ApiParam(value = "交易对", defaultValue = "BTC-USDT", required = true) @RequestParam String symbol,
            @ApiParam(value = "K线时间间隔", defaultValue = "1h", required = true) @RequestParam String interval,
            @ApiParam(value = "开始时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "短期均线周期", defaultValue = "5") @RequestParam(defaultValue = "5") int shortPeriod,
            @ApiParam(value = "长期均线周期", defaultValue = "20") @RequestParam(defaultValue = "20") int longPeriod,
            @ApiParam(value = "初始资金", defaultValue = "10000") @RequestParam(defaultValue = "10000") double initialAmount) {
        
        log.info("执行Ta4j SMA策略回测: symbol={}, interval={}, startTime={}, endTime={}, shortPeriod={}, longPeriod={}, initialAmount={}",
                symbol, interval, startTime, endTime, shortPeriod, longPeriod, initialAmount);
        
        try {
            // 获取历史K线数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(
                    symbol, interval, startTime, endTime);
            
            if (candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定时间范围内的K线数据");
            }
            
            // 设置策略参数
            String params = shortPeriod + "," + longPeriod;
            
            // 执行回测
            BacktestResultDTO result = ta4jBacktestService.backtest(
                    candlesticks, Ta4jBacktestService.STRATEGY_SMA, new BigDecimal(initialAmount), params);
            
            if (result == null) {
                return ApiResponse.error(500, "回测执行失败，无法生成结果");
            }
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("执行SMA策略回测失败", e);
            return ApiResponse.error(500, "回测失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行布林带策略回测
     * 
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param period 布林带周期
     * @param deviation 标准差倍数
     * @param initialAmount 初始资金
     * @param signalType 信号类型
     * @return 回测结果
     */
    @ApiOperation(value = "布林带策略回测", notes = "使用布林带策略进行回测")
    @GetMapping("/bollinger")
    public ApiResponse<BacktestResultDTO> backtestBollingerBands(
            @ApiParam(value = "交易对", defaultValue = "BTC-USDT", required = true) @RequestParam String symbol,
            @ApiParam(value = "K线时间间隔", defaultValue = "1h", required = true) @RequestParam String interval,
            @ApiParam(value = "开始时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "布林带周期", defaultValue = "20") @RequestParam(defaultValue = "20") int period,
            @ApiParam(value = "标准差倍数", defaultValue = "2.0") @RequestParam(defaultValue = "2.0") double deviation,
            @ApiParam(value = "初始资金", defaultValue = "10000") @RequestParam(defaultValue = "10000") double initialAmount,
            @ApiParam(value = "信号类型", defaultValue = "BANDS_BREAKOUT", allowableValues = "BANDS_BREAKOUT,MIDDLE_CROSS,MEAN_REVERSION")
            @RequestParam(defaultValue = "BANDS_BREAKOUT") String signalType) {
        
        log.info("执行Ta4j布林带策略回测: symbol={}, interval={}, startTime={}, endTime={}, period={}, deviation={}, initialAmount={}, signalType={}",
                symbol, interval, startTime, endTime, period, deviation, initialAmount, signalType);
        
        try {
            // 获取历史K线数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(
                    symbol, interval, startTime, endTime);
            
            if (candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定时间范围内的K线数据");
            }
            
            // 设置策略参数
            String params = period + "," + deviation;
            
            // 执行回测
            BacktestResultDTO result = ta4jBacktestService.backtest(
                    candlesticks, Ta4jBacktestService.STRATEGY_BOLLINGER_BANDS, new BigDecimal(initialAmount), params);
            
            if (result == null) {
                return ApiResponse.error(500, "回测执行失败，无法生成结果");
            }
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("执行布林带策略回测失败", e);
            return ApiResponse.error(500, "回测失败: " + e.getMessage());
        }
    }
} 