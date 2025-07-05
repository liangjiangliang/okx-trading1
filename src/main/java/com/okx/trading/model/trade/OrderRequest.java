package com.okx.trading.model.trade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;

/**
 * 订单请求实体类
 * 用于向OKX API发送订单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单请求实体类")
public class OrderRequest {

    /**
     * 交易对，如BTC-USDT
     */
    @Schema(description = "交易对", requiredMode = Schema.RequiredMode.REQUIRED, example = "BTC-USDT")
    @NotBlank(message = "交易对不能为空")
    private String symbol;

    /**
     * 订单类型：
     * LIMIT - 限价单
     * MARKET - 市价单
     */
    @Schema(description = "订单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LIMIT", allowableValues = {"LIMIT", "MARKET"})
    @NotBlank(message = "订单类型不能为空")
    private String type;

    /**
     * 交易方向：
     * BUY - 买入
     * SELL - 卖出
     */
    @Schema(description = "交易方向", requiredMode = Schema.RequiredMode.REQUIRED, example = "BUY", allowableValues = {"BUY", "SELL"})
    @NotBlank(message = "交易方向不能为空")
    private String side;

    /**
     * 价格，对于限价单，该字段必须
     */
    @Schema(description = "价格(对限价单必填)", example = "50000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private BigDecimal price;

    /**
     * 数量
     * 对于买入订单，表示买入多少个交易货币
     * 对于卖出订单，表示卖出多少个交易货币
     * 如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio
     */
    @Schema(description = "数量", example = "0.1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal quantity;

    /**
     * 金额
     * 买入时表示使用多少计价货币
     * 卖出时表示卖出多少交易货币的等值金额
     */
    @Schema(description = "金额", example = "5000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @DecimalMin(value = "0.00000001", message = "金额必须大于0")
    private BigDecimal amount;

    /**
     * 买入比例
     * 表示使用账户可用余额的比例来买入
     * 取值范围：0.01-1
     * 仅在买入(BUY)时有效
     */
    @Schema(description = "买入比例", example = "0.5", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @DecimalMin(value = "0.01", message = "买入比例必须大于等于0.01")
    @DecimalMax(value = "1", message = "买入比例必须小于等于1")
    private BigDecimal buyRatio;

    /**
     * 卖出比例
     * 表示卖出当前持仓的比例
     * 取值范围：0.01-1
     * 仅在卖出(SELL)时有效
     */
    @Schema(description = "卖出比例", example = "0.5", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @DecimalMin(value = "0.01", message = "卖出比例必须大于等于0.01")
    @DecimalMax(value = "1", message = "卖出比例必须小于等于1")
    private BigDecimal sellRatio;

    /**
     * 客户端订单ID，用于客户端跟踪订单
     */
    @Schema(description = "客户端订单ID", example = "client_order_12345", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String clientOrderId;

    /**
     * 杠杆倍数，仅对合约订单有效
     */
    @Schema(description = "杠杆倍数", example = "5", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer leverage;

    /**
     * 订单有效期类型
     * GTC - 成交为止
     * IOC - 立即成交并取消剩余
     * FOK - 全部成交或立即取消
     */
    @Schema(description = "订单有效期类型", example = "GTC", allowableValues = {"GTC", "IOC", "FOK"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String timeInForce;

    /**
     * 是否为模拟交易
     */
    @Schema(description = "是否为模拟交易", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean simulated;

    @Schema(description = "所属策略id", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long strategyId;
}
