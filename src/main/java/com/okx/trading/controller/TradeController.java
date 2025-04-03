package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.OkxApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 交易控制器
 * 提供订单相关功能
 */
@Api(tags = "交易功能")
@Slf4j
@Validated
@RestController
@RequestMapping("/trade")
@RequiredArgsConstructor
public class TradeController {

    private final OkxApiService okxApiService;

    /**
     * 获取订单列表
     *
     * @param symbol 交易对，如BTC-USDT
     * @param status 订单状态，如NEW, PARTIALLY_FILLED, FILLED, CANCELED
     * @param limit  获取数据条数，最大为100
     * @return 订单列表
     */
    @ApiOperation("获取订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "status", value = "订单状态", required = false, dataType = "String", example = "NEW", paramType = "query",
                    allowableValues = "NEW,PARTIALLY_FILLED,FILLED,CANCELED,CANCELING", allowMultiple = false),
            @ApiImplicitParam(name = "limit", value = "获取数据条数，最大为100", required = false, dataType = "Integer", example = "10", paramType = "query")
    })
    @GetMapping("/orders")
    public ApiResponse<List<Order>> getOrders(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Min(value = 1, message = "数据条数必须大于0") Integer limit) {

        log.info("获取订单列表, symbol: {}, status: {}, limit: {}", symbol, status, limit);

        List<Order> orders = okxApiService.getOrders(symbol, status, limit);

        return ApiResponse.success(orders);
    }

    /**
     * 创建现货订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    @ApiOperation(value = "创建现货订单", notes = "有四种下单方式：\n" +
            "1. 指定quantity(数量)下单\n" +
            "2. 指定amount(金额)下单\n" +
            "3. 指定buyRatio(账户可用余额比例)买入，取值范围0.01-1\n" +
            "4. 指定sellRatio(持仓比例)卖出，取值范围0.01-1\n" +
            "5. type有LIMIT限价单,MARKET市价单\n" +
            "6. 交易方向side有buy和sell\n " +
            "7. 订单有效期timeInForce GTC-成交为止, IOC-立即成交并取消剩余, FOK-全部成交或立即取消\n " +
            "如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio")
    @PostMapping("/spot-orders")
    public ApiResponse<Order> createSpotOrder(@ApiParam(value = "订单请求参数", required = true) @Valid @RequestBody OrderRequest orderRequest) {
        log.info("创建现货订单, request: {}", orderRequest);

        Order order = okxApiService.createSpotOrder(orderRequest);

        return ApiResponse.success(order);
    }

    /**
     * 创建合约订单
     *
     * @param orderRequest 订单请求参数
     * @return 创建的订单
     */
    @ApiOperation(value = "创建合约订单", notes = "有四种下单方式：\n" +
            "1. 指定quantity(数量)下单\n" +
            "2. 指定amount(金额)下单\n" +
            "3. 指定buyRatio(账户可用余额比例)买入，取值范围0.01-1\n" +
            "4. 指定sellRatio(持仓比例)卖出，取值范围0.01-1\n" +
            "5. type有LIMIT限价单,MARKET市价单\n" +
            "6. 交易方向side有buy和sell\n " +
            "7. 订单有效期timeInForce GTC-成交为止, IOC-立即成交并取消剩余, FOK-全部成交或立即取消\n " +
            "如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio")
    @PostMapping("/futures-orders")
    public ApiResponse<Order> createFuturesOrder(@ApiParam(value = "订单请求参数", required = true) @Valid @RequestBody OrderRequest orderRequest) {
        log.info("创建合约订单, request: {}", orderRequest);

        Order order = okxApiService.createFuturesOrder(orderRequest);

        return ApiResponse.success(order);
    }

    /**
     * 取消订单
     *
     * @param symbol  交易对，如BTC-USDT
     * @param orderId 订单ID
     * @return 是否成功
     */
    @ApiOperation("取消订单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "orderId", value = "订单ID", required = true, dataType = "String", example = "123456789", paramType = "query")
    })
    @DeleteMapping("/orders")
    public ApiResponse<Boolean> cancelOrder(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "订单ID不能为空") @RequestParam String orderId) {

        log.info("取消订单, symbol: {}, orderId: {}", symbol, orderId);

        boolean success = okxApiService.cancelOrder(symbol, orderId);

        return ApiResponse.success(success);
    }
}
