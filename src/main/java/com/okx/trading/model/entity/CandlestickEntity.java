package com.okx.trading.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K线数据实体类
 * 用于存储K线数据到MySQL
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "candlestick_history")
public class CandlestickEntity {

    /**
     * 复合主键ID (symbol + interval + openTime)
     */
    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    /**
     * 交易对，如BTC-USDT
     */
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /**
     * K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     */
    @Column(name = "interval_val", nullable = false, length = 10)
    private String intervalVal;

    /**
     * 开盘时间
     */
    @Column(name = "open_time", nullable = false)
    private LocalDateTime openTime;

    /**
     * 收盘时间
     */
    @Column(name = "close_time")
    private LocalDateTime closeTime;

    /**
     * 开盘价
     */
    @Column(name = "open", precision = 30, scale = 15)
    private BigDecimal open;

    /**
     * 最高价
     */
    @Column(name = "high", precision = 30, scale = 15)
    private BigDecimal high;

    /**
     * 最低价
     */
    @Column(name = "low", precision = 30, scale = 15)
    private BigDecimal low;

    /**
     * 收盘价
     */
    @Column(name = "close", precision = 30, scale = 15)
    private BigDecimal close;

    /**
     * 成交量
     */
    @Column(name = "volume", precision = 30, scale = 15)
    private BigDecimal volume;

    /**
     * 成交额
     */
    @Column(name = "quote_volume", precision = 30, scale = 15)
    private BigDecimal quoteVolume;

    /**
     * 成交笔数
     */
    @Column(name = "trades")
    private Long trades;

    /**
     * 数据获取时间
     */
    @Column(name = "fetch_time")
    private LocalDateTime fetchTime;

    /**
     * 创建复合ID
     * @param symbol 交易对
     * @param interval 时间间隔
     * @param openTime 开盘时间
     * @return 复合ID
     */
    public static String createId(String symbol, String interval, LocalDateTime openTime) {
        return symbol + "_" + interval + "_" + openTime.toString();
    }
}
