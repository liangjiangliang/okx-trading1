package com.okx.trading.util;

import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.lang.reflect.Method;

/**
 * Ta4j库升级辅助工具类
 * 用于帮助从Ta4j旧版本升级到0.18版本
 */
public class Ta4jUpgradeHelper {

    /**
     * 创建Bar对象
     * 适配Ta4j 0.18版本的BaseBar构造函数
     *
     * @param duration 时间周期
     * @param endTime 结束时间
     * @param openPrice 开盘价
     * @param highPrice 最高价
     * @param lowPrice 最低价
     * @param closePrice 收盘价
     * @param volume 成交量
     * @return Bar对象
     */
    public static Bar createBar(Duration duration, ZonedDateTime endTime,
                                Num openPrice, Num highPrice, Num lowPrice, Num closePrice, Num volume) {
        // 在Ta4j 0.18版本中，BaseBar构造函数需要额外的交易次数参数
        return new BaseBar(duration, endTime.toInstant(), openPrice, highPrice, lowPrice, closePrice, volume, DecimalNum.valueOf(0), 0);
    }

    /**
     * 创建BarSeries对象
     * 适配Ta4j 0.18版本的BaseBarSeriesBuilder
     *
     * @param name 序列名称
     * @param bars Bar列表
     * @return BarSeries对象
     */
    public static BarSeries createBarSeries(String name, java.util.List<Bar> bars) {
        return new BaseBarSeriesBuilder().withName(name).withBars(bars).build();
    }

    /**
     * 创建空的BarSeries对象
     * 适配Ta4j 0.18版本的BaseBarSeriesBuilder
     *
     * @param name 序列名称
     * @return 空的BarSeries对象
     */
    public static BarSeries createEmptyBarSeries(String name) {
        return new BaseBarSeriesBuilder().withName(name).build();
    }

    /**
     * 将Instant转换为ZonedDateTime
     *
     * @param instant 时间戳
     * @return ZonedDateTime对象
     */
    public static ZonedDateTime toZonedDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.of("UTC+8"));
    }

    /**
     * 将ZonedDateTime转换为Instant
     * 使用安全的方式避免直接调用可能不兼容的toInstant方法
     *
     * @param zonedDateTime 带时区的日期时间
     * @return Instant对象
     */
    public static Instant toInstant(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }

        try {
            // 尝试使用epochSecond和nano构建Instant
            long epochSecond = zonedDateTime.toEpochSecond();
            int nano = zonedDateTime.getNano();
            return Instant.ofEpochSecond(epochSecond, nano);
        } catch (Exception e) {
            // 如果上面的方法失败，尝试使用反射调用toInstant
            try {
                Method toInstantMethod = ZonedDateTime.class.getMethod("toInstant");
                return (Instant) toInstantMethod.invoke(zonedDateTime);
            } catch (Exception ex) {
                // 如果反射也失败，使用时间戳作为备选方案
                try {
                    return Instant.ofEpochMilli(zonedDateTime.toLocalDateTime()
                            .atZone(ZoneId.of("UTC+8")).toInstant().toEpochMilli());
                } catch (Exception exc) {
                    throw new RuntimeException("无法将ZonedDateTime转换为Instant: " + zonedDateTime, exc);
                }
            }
        }
    }

    /**
     * 将LocalDateTime转换为ZonedDateTime
     *
     * @param localDateTime 本地日期时间
     * @return ZonedDateTime对象
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.of("UTC+8"));
    }

    /**
     * 将毫秒时间戳转换为ZonedDateTime
     *
     * @param timestamp 毫秒时间戳
     * @return ZonedDateTime对象
     */
    public static ZonedDateTime timestampToZonedDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC+8"));
    }

    /**
     * 创建Strategy对象
     * 适配Ta4j 0.18版本的BaseStrategy构造函数
     *
     * @param entryRule 入场规则
     * @param exitRule 出场规则
     * @return Strategy对象
     */
    public static Strategy createStrategy(Rule entryRule, Rule exitRule) {
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 创建带名称的Strategy对象
     * 适配Ta4j 0.18版本的BaseStrategy构造函数
     *
     * @param name 策略名称
     * @param entryRule 入场规则
     * @param exitRule 出场规则
     * @return Strategy对象
     */
    public static Strategy createStrategy(String name, Rule entryRule, Rule exitRule) {
        return new BaseStrategy(name, entryRule, exitRule);
    }
}
