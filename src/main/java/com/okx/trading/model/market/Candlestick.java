package com.okx.trading.model.market;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K线数据模型
 * 用于存储交易对的K线数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candlestick {
    
    /**
     * 交易对，如BTC-USDT
     */
    private String symbol;
    
    /**
     * K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     */
    private String interval;
    
    /**
     * 开盘时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime openTime;
    
    /**
     * 开盘价
     */
    private BigDecimal open;
    
    /**
     * 最高价
     */
    private BigDecimal high;
    
    /**
     * 最低价
     */
    private BigDecimal low;
    
    /**
     * 收盘价
     */
    private BigDecimal close;
    
    /**
     * 成交量
     */
    private BigDecimal volume;
    
    /**
     * 成交额
     */
    private BigDecimal quoteVolume;
    
    /**
     * 收盘时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;
    
    /**
     * 成交笔数
     */
    private Long trades;
} 