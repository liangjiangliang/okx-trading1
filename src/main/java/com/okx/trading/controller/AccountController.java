package com.okx.trading.controller;

import com.okx.trading.model.account.AccountBalance;
import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.service.OkxApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户信息控制器
 * 提供账户余额等接口
 */
@Api(tags = "账户信息")
@Slf4j
@Validated
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final OkxApiService okxApiService;

    /**
     * 获取账户余额
     *
     * @return 账户余额信息
     */
    @ApiOperation("获取账户余额")
    @GetMapping("/balance")
    public ApiResponse<AccountBalance> getAccountBalance() {
        log.info("获取账户余额");
        
        AccountBalance accountBalance = okxApiService.getAccountBalance();
        
        return ApiResponse.success(accountBalance);
    }

    /**
     * 获取模拟账户余额
     *
     * @return 模拟账户余额信息
     */
    @ApiOperation("获取模拟账户余额")
    @GetMapping("/simulated-balance")
    public ApiResponse<AccountBalance> getSimulatedAccountBalance() {
        log.info("获取模拟账户余额");
        
        AccountBalance accountBalance = okxApiService.getSimulatedAccountBalance();
        
        return ApiResponse.success(accountBalance);
    }
} 