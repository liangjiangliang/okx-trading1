package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.service.PriceUpdateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.util.Set;

/**
 * 价格更新控制器
 * 提供管理价格更新服务的接口
 */
@Api(tags = "价格更新服务")
@Slf4j
@Validated
@RestController
@RequestMapping("/price-update")
@RequiredArgsConstructor
public class PriceUpdateController {

    private final PriceUpdateService priceUpdateService;

    /**
     * 启动价格更新线程
     *
     * @return 操作结果
     */
    @ApiOperation(value = "启动价格更新线程", notes = "启动独立线程更新缓存价格")
    @PostMapping("/start")
    public ApiResponse<Void> startPriceUpdateThread() {
        log.info("手动启动价格更新线程");
        priceUpdateService.startPriceUpdateThread();
        return ApiResponse.success();
    }

    /**
     * 停止价格更新线程
     *
     * @return 操作结果
     */
    @ApiOperation(value = "停止价格更新线程", notes = "停止独立线程更新缓存价格")
    @PostMapping("/stop")
    public ApiResponse<Void> stopPriceUpdateThread() {
        log.info("手动停止价格更新线程");
        priceUpdateService.stopPriceUpdateThread();
        return ApiResponse.success();
    }

    /**
     * 获取当前订阅的币种
     *
     * @return 订阅的币种列表
     */
    @ApiOperation(value = "获取订阅币种列表", notes = "获取当前价格更新服务订阅的所有币种")
    @PostMapping("/subscribed")
    public ApiResponse<Set<String>> getSubscribedCoins() {
        log.info("获取价格更新服务订阅的币种列表");
        Set<String> coins = priceUpdateService.getSubscribedCoins();
        return ApiResponse.success(coins);
    }

    /**
     * 添加订阅币种
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 添加结果
     */
    @ApiOperation(value = "添加订阅币种", notes = "添加币种到价格更新服务的订阅列表")
    @ApiImplicitParam(name = "symbol", value = "交易对 (如BTC-USDT)", required = true, dataType = "String", paramType = "query")
    @PostMapping("/subscribe")
    public ApiResponse<Boolean> subscribeCoin(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {
        log.info("向价格更新服务添加订阅币种: {}", symbol);
        boolean success = priceUpdateService.addSubscribedCoin(symbol);
        return ApiResponse.success(success);
    }

    /**
     * 取消订阅币种
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 取消结果
     */
    @ApiOperation(value = "取消订阅币种", notes = "从价格更新服务的订阅列表中移除币种")
    @ApiImplicitParam(name = "symbol", value = "交易对 (如BTC-USDT)", required = true, dataType = "String", paramType = "query")
    @PostMapping("/unsubscribe")
    public ApiResponse<Boolean> unsubscribeCoin(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {
        log.info("从价格更新服务取消订阅币种: {}", symbol);
        boolean success = priceUpdateService.removeSubscribedCoin(symbol);
        return ApiResponse.success(success);
    }
} 