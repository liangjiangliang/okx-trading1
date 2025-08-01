package com.okx.trading.model.market;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * K线数据模型
 * 用于存储交易对的K线数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candlestick implements Comparable<Candlestick>{

    private String channel;
    /**
     * 交易对，如BTC-USDT
     */
    private String symbol;

    /**
     * K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     */
    private String intervalVal;

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
     * 成交量,以张为单位
     */
    private BigDecimal volume;

    /**
     * 成交量,以币为单位
     */
    private BigDecimal volCcy;

    /**
     * 成交额,计价货币 ,usdt usdc
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

    /**
     * 0 代表 K 线未完结，1 代表 K 线已完结
     */
    private int state;

    private Map<String,Map<String,BigDecimal>> indecator;

    @Override
    public String toString(){
        return JSONObject.toJSONString(this);
    }


    @Override
    public int compareTo(@NotNull Candlestick o){
        return this.getOpenTime().compareTo(o.getOpenTime());
    }

    public Candlestick(BigDecimal close) {
        this.close = close;
    }
}
