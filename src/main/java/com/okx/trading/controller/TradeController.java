package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.OkxApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
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
     * @param symbol 交易对，如BTC-USDT
     * @param type 订单类型：LIMIT - 限价单, MARKET - 市价单
     * @param side 交易方向：BUY - 买入, SELL - 卖出
     * @param price 价格，对于限价单，该字段必须
     * @param quantity 数量
     * @param amount 金额
     * @param buyRatio 买入比例
     * @param sellRatio 卖出比例
     * @param clientOrderId 客户端订单ID
     * @param timeInForce 订单有效期类型
     * @param postOnly 是否被动委托
     * @param simulated 是否为模拟交易
     * @return 创建的订单
     */
    @ApiOperation(value = "创建现货订单", notes = "有四种下单方式：\n" +
            "1. 指定quantity(数量)下单\n" +
            "2. 指定amount(金额)下单\n" +
            "3. 指定buyRatio(账户可用余额比例)买入，取值范围0.01-1\n" +
            "4. 指定sellRatio(持仓比例)卖出，取值范围0.01-1\n" +
            "如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT"),
            @ApiImplicitParam(name = "type", value = "订单类型", required = true, dataType = "String", example = "MARKET", allowableValues = "LIMIT,MARKET"),
            @ApiImplicitParam(name = "side", value = "交易方向", required = true, dataType = "String", example = "BUY", allowableValues = "BUY,SELL"),
            @ApiImplicitParam(name = "price", value = "价格(对限价单必填)", required = false, dataType = "BigDecimal", example = "100"),
            @ApiImplicitParam(name = "quantity", value = "数量", required = false, dataType = "BigDecimal", example = "0.1"),
            @ApiImplicitParam(name = "amount", value = "金额", required = false, dataType = "BigDecimal", example = "5000"),
            @ApiImplicitParam(name = "buyRatio", value = "买入比例", required = false, dataType = "BigDecimal", example = "0.5"),
            @ApiImplicitParam(name = "sellRatio", value = "卖出比例", required = false, dataType = "BigDecimal", example = "0.5"),
            @ApiImplicitParam(name = "clientOrderId", value = "客户端订单ID", required = false, dataType = "String", example = ""),
            @ApiImplicitParam(name = "timeInForce", value = "订单有效期类型", required = false, dataType = "String", example = "GTC", allowableValues = "GTC,IOC,FOK"),
            @ApiImplicitParam(name = "postOnly", value = "是否被动委托", required = false, dataType = "Boolean", example = "false"),
            @ApiImplicitParam(name = "simulated", value = "是否为模拟交易", required = false, dataType = "Boolean", example = "false")
    })
    @PostMapping("/spot-orders")
    public ApiResponse<Order> createSpotOrder(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "订单类型不能为空") @RequestParam String type,
            @NotBlank(message = "交易方向不能为空") @RequestParam String side,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) @DecimalMin(value = "0.00000001", message = "数量必须大于0") BigDecimal quantity,
            @RequestParam(required = false) @DecimalMin(value = "0.00000001", message = "金额必须大于0") BigDecimal amount,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "买入比例必须大于等于0.01") @DecimalMax(value = "1", message = "买入比例必须小于等于1") BigDecimal buyRatio,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "卖出比例必须大于等于0.01") @DecimalMax(value = "1", message = "卖出比例必须小于等于1") BigDecimal sellRatio,
            @RequestParam(required = false) String clientOrderId,
            @RequestParam(required = false) String timeInForce,
            @RequestParam(required = false) Boolean postOnly,
            @RequestParam(required = false) Boolean simulated) {

        log.info("创建现货订单, symbol: {}, type: {}, side: {}, price: {}, quantity: {}, amount: {}, buyRatio: {}, sellRatio: {}, clientOrderId: {}, timeInForce: {}, postOnly: {}, simulated: {}",
                symbol, type, side, price, quantity, amount, buyRatio, sellRatio, clientOrderId, timeInForce, postOnly, simulated);

        // 构建订单请求对象
        OrderRequest orderRequest = OrderRequest.builder()
                .symbol(symbol)
                .type(type)
                .side(side)
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .buyRatio(buyRatio)
                .sellRatio(sellRatio)
                .clientOrderId(clientOrderId)
                .timeInForce(timeInForce)
                .postOnly(postOnly)
                .simulated(simulated)
                .build();

        Order order = okxApiService.createSpotOrder(orderRequest);

        return ApiResponse.success(order);
    }

    /**
     * 创建合约订单
     *
     * @param symbol 交易对，如BTC-USDT
     * @param type 订单类型：LIMIT - 限价单, MARKET - 市价单
     * @param side 交易方向：BUY - 买入, SELL - 卖出
     * @param price 价格，对于限价单，该字段必须
     * @param quantity 数量
     * @param amount 金额
     * @param buyRatio 买入比例
     * @param sellRatio 卖出比例
     * @param clientOrderId 客户端订单ID
     * @param leverage 杠杆倍数
     * @param timeInForce 订单有效期类型
     * @param postOnly 是否被动委托
     * @param simulated 是否为模拟交易
     * @return 创建的订单
     */
    @ApiOperation(value = "创建合约订单", notes = "有四种下单方式：\n" +
            "1. 指定quantity(数量)下单\n" +
            "2. 指定amount(金额)下单\n" +
            "3. 指定buyRatio(账户可用余额比例)买入，取值范围0.01-1\n" +
            "4. 指定sellRatio(持仓比例)卖出，取值范围0.01-1\n" +
            "如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT-SWAP"),
            @ApiImplicitParam(name = "type", value = "订单类型", required = true, dataType = "String", example = "LIMIT", allowableValues = "LIMIT,MARKET"),
            @ApiImplicitParam(name = "side", value = "交易方向", required = true, dataType = "String", example = "BUY", allowableValues = "BUY,SELL"),
            @ApiImplicitParam(name = "price", value = "价格(对限价单必填)", required = false, dataType = "BigDecimal", example = "50000"),
            @ApiImplicitParam(name = "quantity", value = "数量", required = false, dataType = "BigDecimal", example = "0.1"),
            @ApiImplicitParam(name = "amount", value = "金额", required = false, dataType = "BigDecimal", example = "5000"),
            @ApiImplicitParam(name = "buyRatio", value = "买入比例", required = false, dataType = "BigDecimal", example = "0.5"),
            @ApiImplicitParam(name = "sellRatio", value = "卖出比例", required = false, dataType = "BigDecimal", example = "0.5"),
            @ApiImplicitParam(name = "clientOrderId", value = "客户端订单ID", required = false, dataType = "String", example = "client_order_12345"),
            @ApiImplicitParam(name = "leverage", value = "杠杆倍数", required = false, dataType = "Integer", example = "5"),
            @ApiImplicitParam(name = "timeInForce", value = "订单有效期类型", required = false, dataType = "String", example = "GTC", allowableValues = "GTC,IOC,FOK"),
            @ApiImplicitParam(name = "postOnly", value = "是否被动委托", required = false, dataType = "Boolean", example = "false"),
            @ApiImplicitParam(name = "simulated", value = "是否为模拟交易", required = false, dataType = "Boolean", example = "false")
    })
    @PostMapping("/futures-orders")
    public ApiResponse<Order> createFuturesOrder(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "订单类型不能为空") @RequestParam String type,
            @NotBlank(message = "交易方向不能为空") @RequestParam String side,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) @DecimalMin(value = "0.00000001", message = "数量必须大于0") BigDecimal quantity,
            @RequestParam(required = false) @DecimalMin(value = "0.00000001", message = "金额必须大于0") BigDecimal amount,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "买入比例必须大于等于0.01") @DecimalMax(value = "1", message = "买入比例必须小于等于1") BigDecimal buyRatio,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "卖出比例必须大于等于0.01") @DecimalMax(value = "1", message = "卖出比例必须小于等于1") BigDecimal sellRatio,
            @RequestParam(required = false) String clientOrderId,
            @RequestParam(required = false) Integer leverage,
            @RequestParam(required = false) String timeInForce,
            @RequestParam(required = false) Boolean postOnly,
            @RequestParam(required = false) Boolean simulated) {

        log.info("创建合约订单, symbol: {}, type: {}, side: {}, price: {}, quantity: {}, amount: {}, buyRatio: {}, sellRatio: {}, clientOrderId: {}, leverage: {}, timeInForce: {}, postOnly: {}, simulated: {}",
                symbol, type, side, price, quantity, amount, buyRatio, sellRatio, clientOrderId, leverage, timeInForce, postOnly, simulated);

        // 构建订单请求对象
        OrderRequest orderRequest = OrderRequest.builder()
                .symbol(symbol)
                .type(type)
                .side(side)
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .buyRatio(buyRatio)
                .sellRatio(sellRatio)
                .clientOrderId(clientOrderId)
                .leverage(leverage)
                .timeInForce(timeInForce)
                .postOnly(postOnly)
                .simulated(simulated)
                .build();

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
