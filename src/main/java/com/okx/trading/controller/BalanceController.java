package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 余额控制器
 * 提供获取账户余额的API接口
 */
@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
@Tag(name = "余额API", description = "获取账户余额信息")
public class BalanceController {

    private final BalanceService balanceService;

    /**
     * 获取所有币种余额
     *
     * @return 所有币种余额信息
     */
    @GetMapping
    @Operation(summary = "获取所有币种余额", description = "返回所有币种的可用余额信息")
    public ApiResponse<Map<String, BigDecimal>> getAllBalances() {
        Map<String, BigDecimal> balances = balanceService.getAllBalances();
        return ApiResponse.success(balances);
    }

    /**
     * 获取指定币种余额
     *
     * @param asset 币种代码，如BTC、ETH等
     * @return 指定币种的可用余额
     */
    @GetMapping("/{asset}")
    @Operation(summary = "获取指定币种余额", description = "返回指定币种的可用余额")
    public ApiResponse<BigDecimal> getBalance(@PathVariable String asset) {
        BigDecimal balance = balanceService.getBalance(asset);
        if (balance != null) {
            return ApiResponse.success(balance);
        } else {
            return ApiResponse.fail("未找到该币种余额信息");
        }
    }

    /**
     * 刷新余额信息
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新余额信息", description = "强制刷新所有币种余额信息")
    public ApiResponse<String> refreshBalances() {
        boolean success = balanceService.refreshBalances();
        if (success) {
            return ApiResponse.success("余额信息刷新成功");
        } else {
            return ApiResponse.fail("余额信息刷新失败");
        }
    }
} 