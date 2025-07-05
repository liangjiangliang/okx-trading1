package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.trade.OrderRequest;
import com.okx.trading.service.OkxApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/**
 * 交易控制器
 * 提供订单相关功能
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/trade")
public class TradeController {

    private final OkxApiService okxApiService;
    
    @Autowired
    public TradeController(OkxApiService okxApiService) {
        this.okxApiService = okxApiService;
    }

    /**
     * 获取订单列表
     *
     * @param symbol 交易对，如BTC-USDT
     * @param status 订单状态，如NEW, PARTIALLY_FILLED, FILLED, CANCELED
     * @param limit  获取数据条数，最大为100
     * @return 订单列表
     */
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
    @PostMapping("/spot-orders")
    public ApiResponse<Order> createSpotOrder(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
           @RequestParam String type,
            @NotBlank(message = "交易方向不能为空") @RequestParam String side,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) @DecimalMin(value = "0.00000001", message = "数量必须大于0") BigDecimal quantity,
            @RequestParam(required = false) @DecimalMin(value = "0.00000001", message = "金额必须大于0") BigDecimal amount,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "买入比例必须大于等于0.01") @DecimalMax(value = "1", message = "买入比例必须小于等于1") BigDecimal buyRatio,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "卖出比例必须大于等于0.01") @DecimalMax(value = "1", message = "卖出比例必须小于等于1") BigDecimal sellRatio,
            @RequestParam(required = false) String clientOrderId,
            @RequestParam(required = false) Boolean postOnly,
            @RequestParam(required = false) Boolean simulated,
            @RequestParam(required = false) Long startegyId) {

        log.info("创建现货订单, symbol: {}, type: {}, side: {}, price: {}, quantity: {}, amount: {}, buyRatio: {}, sellRatio: {}, clientOrderId: {},  postOnly: {}, simulated: {}",
                symbol, type, side, price, quantity, amount, buyRatio, sellRatio, clientOrderId, postOnly, simulated);

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
                .timeInForce("")
                .simulated(simulated)
                .strategyId(startegyId)
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
            @RequestParam(required = false) Boolean postOnly,
            @RequestParam(required = false) Boolean simulated) {

        log.info("创建合约订单, symbol: {}, type: {}, side: {}, price: {}, quantity: {}, amount: {}, buyRatio: {}, sellRatio: {}, clientOrderId: {}, leverage: {}, postOnly: {}, simulated: {}",
                symbol, type, side, price, quantity, amount, buyRatio, sellRatio, clientOrderId, leverage, postOnly, simulated);

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
                .timeInForce("")
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
    @DeleteMapping("/orders")
    public ApiResponse<Boolean> cancelOrder(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "订单ID不能为空") @RequestParam String orderId) {

        log.info("取消订单, symbol: {}, orderId: {}", symbol, orderId);

        boolean success = okxApiService.cancelOrder(symbol, orderId);

        return ApiResponse.success(success);
    }
}
