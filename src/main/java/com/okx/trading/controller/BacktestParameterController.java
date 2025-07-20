package com.okx.trading.controller;

import com.okx.trading.config.BacktestParameterConfig;
import com.okx.trading.model.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 回测参数配置控制器
 */
@RestController
@RequestMapping("/api/backtest/parameters")
@Slf4j
@RequiredArgsConstructor
public class BacktestParameterController {

    private final BacktestParameterConfig backtestParameterConfig;

    /**
     * 获取当前回测参数
     */
    @GetMapping
    public ApiResponse<Map<String, BigDecimal>> getParameters() {
        Map<String, BigDecimal> parameters = new HashMap<>();
        parameters.put("stopLossPercent", backtestParameterConfig.getStopLossPercent());
        parameters.put("trailingProfitPercent", backtestParameterConfig.getTrailingProfitPercent());
        return ApiResponse.success(parameters);
    }

    /**
     * 更新止损百分比
     */
    @GetMapping("/stop-loss")
    public ApiResponse<BigDecimal> updateStopLossPercent(@RequestParam BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("止损百分比必须大于等于0");
        }
        backtestParameterConfig.updateStopLossPercent(percent);
        return ApiResponse.success(percent);
    }

    /**
     * 更新移动止盈百分比
     */
    @GetMapping("/trailing-profit")
    public ApiResponse<BigDecimal> updateTrailingProfitPercent(@RequestParam BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("移动止盈百分比必须大于等于0");
        }
        backtestParameterConfig.updateTrailingProfitPercent(percent);
        return ApiResponse.success(percent);
    }

    /**
     * 重置所有参数为默认值
     */
    @PostMapping("/reset")
    public ApiResponse<Map<String, BigDecimal>> resetParameters() {
        backtestParameterConfig.resetToDefaults();
        return getParameters();
    }
}
