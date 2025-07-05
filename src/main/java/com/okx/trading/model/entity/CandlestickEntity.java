package com.okx.trading.model.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

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
public class CandlestickEntity implements Comparable<CandlestickEntity> {

    /**
     * 自增主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

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
     * 获取开盘时间的时间戳（毫秒）
     * 为了兼容Ta4j 0.18版本的API
     * @return 开盘时间的时间戳（毫秒）
     */
    public long getTimestamp() {
        return getTime();
    }

    /**
     * 获取开盘时间的时间戳（毫秒）
     * 为了兼容Ta4j 0.18版本的API
     * @return 开盘时间的时间戳（毫秒）
     */
    public long getTime() {
        return openTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public int compareTo(@NotNull CandlestickEntity o) {
        return this.getOpenTime().compareTo(o.getOpenTime());
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public static CandlestickEntity fromJSONObject(String text) {
        return JSONObject.parseObject(text, CandlestickEntity.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CandlestickEntity that = (CandlestickEntity) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(intervalVal, that.intervalVal) && Objects.equals(openTime, that.openTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, intervalVal, openTime);
    }
}
