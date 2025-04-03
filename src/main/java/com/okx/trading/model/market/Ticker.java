package com.okx.trading.model.market;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行情数据模型
 * 用于存储交易对的实时行情数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticker {
    
    /**
     * 交易对，如BTC-USDT
     */
    private String symbol;
    
    /**
     * 最新价格
     */
    private BigDecimal lastPrice;
    
    /**
     * 24小时价格变动
     */
    private BigDecimal priceChange;
    
    /**
     * 24小时价格变动百分比
     */
    private BigDecimal priceChangePercent;
    
    /**
     * 24小时最高价
     */
    private BigDecimal highPrice;
    
    /**
     * 24小时最低价
     */
    private BigDecimal lowPrice;
    
    /**
     * 24小时成交量
     */
    private BigDecimal volume;
    
    /**
     * 24小时成交额
     */
    private BigDecimal quoteVolume;
    
    /**
     * 买一价
     */
    private BigDecimal bidPrice;
    
    /**
     * 买一量
     */
    private BigDecimal bidQty;
    
    /**
     * 卖一价
     */
    private BigDecimal askPrice;
    
    /**
     * 卖一量
     */
    private BigDecimal askQty;
    
    /**
     * 最新成交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
} 