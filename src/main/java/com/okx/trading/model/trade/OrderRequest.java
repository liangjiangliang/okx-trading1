package com.okx.trading.model.trade;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    /**
     * 交易对，如BTC-USDT
     */
    @NotBlank(message = "交易对不能为空")
    private String symbol;
    
    /**
     * 订单类型：
     * LIMIT - 限价单
     * MARKET - 市价单
     */
    @NotBlank(message = "订单类型不能为空")
    private String type;
    
    /**
     * 交易方向：
     * BUY - 买入
     * SELL - 卖出
     */
    @NotBlank(message = "交易方向不能为空")
    private String side;
    
    /**
     * 价格，对于限价单，该字段必须
     */
    private BigDecimal price;
    
    /**
     * 数量
     * 与金额、买入比例、卖出比例至少指定一个
     * 如果同时提供多个参数，优先级为: quantity > amount > buyRatio/sellRatio
     */
    @DecimalMin(value = "0.00000001", message = "数量必须大于0")
    private BigDecimal quantity;
    
    /**
     * 金额
     * 与数量、买入比例、卖出比例至少指定一个
     * 买入时表示使用多少计价货币（如USDT）
     * 卖出时表示卖出多少交易货币的等值金额
     */
    @DecimalMin(value = "0.00000001", message = "金额必须大于0")
    private BigDecimal amount;
    
    /**
     * 买入比例
     * 与数量、金额、卖出比例至少指定一个
     * 表示用账户可用余额的多少比例来买入，取值范围0.01-1
     * 仅在买入(BUY)时有效
     */
    @DecimalMin(value = "0.01", message = "买入比例必须大于等于0.01")
    @DecimalMax(value = "1", message = "买入比例必须小于等于1")
    private BigDecimal buyRatio;
    
    /**
     * 卖出比例
     * 与数量、金额、买入比例至少指定一个
     * 表示卖出当前持仓的多少比例，取值范围0.01-1
     * 仅在卖出(SELL)时有效
     */
    @DecimalMin(value = "0.01", message = "卖出比例必须大于等于0.01")
    @DecimalMax(value = "1", message = "卖出比例必须小于等于1")
    private BigDecimal sellRatio;
    
    /**
     * 客户端订单ID，用于客户端跟踪订单
     */
    private String clientOrderId;
    
    /**
     * 杠杆倍数，仅对合约订单有效
     */
    private Integer leverage;
    
    /**
     * 订单有效期类型：
     * GTC - 成交为止
     * IOC - 立即成交并取消剩余
     * FOK - 全部成交或立即取消
     */
    private String timeInForce;
    
    /**
     * 是否被动委托
     */
    private Boolean postOnly;
    
    /**
     * 是否为模拟交易
     */
    private Boolean simulated;
}