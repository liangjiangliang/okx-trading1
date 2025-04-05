package com.okx.trading.model.trade;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 订单请求模型
 * 用于创建交易订单的请求参数
 */
@ApiModel(value = "OrderRequest", description = "订单请求参数")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /**
     * 交易对，如BTC-USDT
     */
    @ApiModelProperty(value = "交易对", required = true, example = "BTC-USDT", position = 1)
    @NotBlank(message = "交易对不能为空")
    private String symbol;

    /**
     * 订单类型：
     * LIMIT - 限价单
     * MARKET - 市价单
     */
    @ApiModelProperty(value = "订单类型", required = true, example = "LIMIT", allowableValues = "LIMIT,MARKET", position = 2)
    @NotBlank(message = "订单类型不能为空")
    private String type;

    /**
     * 交易方向：
     * BUY - 买入
     * SELL - 卖出
     */
    @ApiModelProperty(value = "交易方向", required = true, example = "BUY", allowableValues = "BUY,SELL", position = 3)
    @NotBlank(message = "交易方向不能为空")
    private String side;

    /**
     * 价格，对于限价单，该字段必须
     */
    @ApiModelProperty(value = "价格(对限价单必填)", example = "50000", notes = "LIMIT类型订单必填，MARKET类型订单可不填", position = 4)
    private BigDecimal price;

    /**
     * 数量
     * 与金额、买入比例、卖出比例至少指定一个
     * 如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio
     */
    @ApiModelProperty(value = "数量", example = "0.1", notes = "quantity/amount/buyRatio/sellRatio至少填一个，优先级：quantity > amount > buyRatio/sellRatio", position = 5)
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal quantity;

    /**
     * 金额
     * 与数量、买入比例、卖出比例至少指定一个
     * 买入时表示使用多少计价货币（如USDT）
     * 卖出时表示卖出多少交易货币的等值金额
     */
    @ApiModelProperty(value = "金额", example = "5000", notes = "买入时表示使用计价货币金额(如5000USDT)，卖出时表示要卖出的标的资产数量", position = 6)
    @DecimalMin(value = "0.00000001", message = "金额必须大于0")
    private BigDecimal amount;

    /**
     * 买入比例
     * 与数量、金额、卖出比例至少指定一个
     * 表示用账户可用余额的多少比例来买入，取值范围0.01-1
     * 仅在买入(BUY)时有效
     */
    @ApiModelProperty(value = "买入比例", example = "0.5", notes = "用账户可用余额的比例买入，取值范围0.01-1，仅在BUY时有效", position = 7)
    @DecimalMin(value = "0.01", message = "买入比例必须大于等于0.01")
    @DecimalMax(value = "1", message = "买入比例必须小于等于1")
    private BigDecimal buyRatio;

    /**
     * 卖出比例
     * 与数量、金额、买入比例至少指定一个
     * 表示卖出当前持仓的多少比例，取值范围0.01-1
     * 仅在卖出(SELL)时有效
     */
    @ApiModelProperty(value = "卖出比例", example = "0.5", notes = "卖出当前持仓的比例，取值范围0.01-1，仅在SELL时有效", position = 8)
    @DecimalMin(value = "0.01", message = "卖出比例必须大于等于0.01")
    @DecimalMax(value = "1", message = "卖出比例必须小于等于1")
    private BigDecimal sellRatio;

    /**
     * 客户端订单ID，用于客户端跟踪订单
     */
    @ApiModelProperty(value = "客户端订单ID", example = "client_order_12345", notes = "可自定义，用于客户端跟踪订单", position = 9)
    private String clientOrderId;

    /**
     * 杠杆倍数，仅对合约订单有效
     */
    @ApiModelProperty(value = "杠杆倍数", example = "5", notes = "仅对合约订单有效", position = 10)
    private Integer leverage;

    /**
     * 订单有效期类型：
     * GTC - 成交为止
     * IOC - 立即成交并取消剩余
     * FOK - 全部成交或立即取消
     */
    @ApiModelProperty(value = "订单有效期类型", example = "GTC", allowableValues = "GTC,IOC,FOK", notes = "GTC-成交为止, IOC-立即成交并取消剩余, FOK-全部成交或立即取消", position = 11)
    private String timeInForce;

//    /**
//     * 是否被动委托
//     */
//    @ApiModelProperty(value = "是否被动委托", example = "false", position = 12)
//    private Boolean postOnly;
//
    /**
     * 是否为模拟交易
     */
    @ApiModelProperty(value = "是否为模拟交易", example = "false", position = 13)
    private Boolean simulated;
}
