package com.okx.trading.model.trade;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单模型
 * 用于存储交易订单信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 客户端订单ID
     */
    private String clientOrderId;

    /**
     * 交易对，如BTC-USDT
     */
    private String symbol;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 原始数量
     */
    private BigDecimal origQty;

    /**
     * 已执行数量
     */
    private BigDecimal executedQty;

    /**
     * 成交金额
     */
    private BigDecimal cummulativeQuoteQty;

    /**
     * 订单状态：
     * NEW - 新建
     * PARTIALLY_FILLED - 部分成交
     * FILLED - 全部成交
     * CANCELED - 已取消
     * REJECTED - 已拒绝
     * EXPIRED - 已过期
     */
    private String status;

    /**
     * 订单类型：
     * LIMIT - 限价单
     * MARKET - 市价单
     */
    private String type;

    /**
     * 交易方向：
     * BUY - 买入
     * SELL - 卖出
     */
    private String side;

    /**
     * 止盈价格
     */
    private BigDecimal stopPrice;

    /**
     * 触发价格
     */
    private BigDecimal triggerPrice;

    /**
     * 订单有效期类型
     */
    private String timeInForce;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否为模拟交易
     */
    private Boolean simulated;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 手续费币种
     */
    private String feeCurrency;

    private String sMsg;

    private int sCode;
}
