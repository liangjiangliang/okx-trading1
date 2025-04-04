package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.service.RedisCacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 价格数据控制器
 * 提供获取币种实时价格的接口
 */
@Api(tags = "价格数据")
@Slf4j
@Validated
@RestController
@RequestMapping("/price")
@RequiredArgsConstructor
public class PriceController {

    private final RedisCacheService redisCacheService;

    /**
     * 获取所有币种的实时价格
     *
     * @return 所有币种的实时价格Map
     */
    @ApiOperation(value = "获取所有币种实时价格", notes = "从Redis缓存中获取所有币种的最新价格")
    @GetMapping("/all")
    public ApiResponse<Map<String, BigDecimal>> getAllPrices() {
        log.info("获取所有币种实时价格");
        Map<String, BigDecimal> prices = redisCacheService.getAllCoinPrices();
        return ApiResponse.success(prices);
    }

    /**
     * 获取指定币种的实时价格
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 实时价格
     */
    @ApiOperation(value = "获取指定币种实时价格", notes = "从Redis缓存中获取指定币种的最新价格")
    @ApiImplicitParam(name = "symbol", value = "交易对 (如BTC-USDT)", required = true, dataType = "String", paramType = "query")
    @GetMapping("/single")
    public ApiResponse<BigDecimal> getPrice(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {
        log.info("获取币种 {} 实时价格", symbol);
        BigDecimal price = redisCacheService.getCoinPrice(symbol);
        return ApiResponse.success(price);
    }
    
    /**
     * 获取所有订阅的币种
     *
     * @return 订阅的币种列表
     */
    @ApiOperation(value = "获取订阅币种列表", notes = "获取当前系统订阅的所有币种")
    @GetMapping("/subscribed")
    public ApiResponse<Set<String>> getSubscribedCoins() {
        log.info("获取订阅币种列表");
        Set<String> coins = redisCacheService.getSubscribedCoins();
        return ApiResponse.success(coins);
    }
    
    /**
     * 添加订阅币种
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 添加结果
     */
    @ApiOperation(value = "添加订阅币种", notes = "添加币种到订阅列表")
    @ApiImplicitParam(name = "symbol", value = "交易对 (如BTC-USDT)", required = true, dataType = "String", paramType = "query")
    @PostMapping("/subscribe")
    public ApiResponse<Boolean> subscribeCoin(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {
        log.info("添加订阅币种: {}", symbol);
        boolean success = redisCacheService.addSubscribedCoin(symbol);
        return ApiResponse.success(success);
    }
    
    /**
     * 取消订阅币种
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 取消结果
     */
    @ApiOperation(value = "取消订阅币种", notes = "从订阅列表中移除币种")
    @ApiImplicitParam(name = "symbol", value = "交易对 (如BTC-USDT)", required = true, dataType = "String", paramType = "query")
    @PostMapping("/unsubscribe")
    public ApiResponse<Boolean> unsubscribeCoin(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {
        log.info("取消订阅币种: {}", symbol);
        boolean success = redisCacheService.removeSubscribedCoin(symbol);
        return ApiResponse.success(success);
    }
} 