package com.okx.trading.strategy;

import com.okx.trading.backtest.BacktestFramework;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.util.TechnicalIndicatorUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 布林带交易策略
 * 基于布林带的突破交易策略，包含以下几种信号：
 * 1. 价格突破上轨 - 超卖信号，考虑卖出
 * 2. 价格突破下轨 - 超买信号，考虑买入
 * 3. 价格由下向上穿越中轨 - 上升趋势确认，考虑买入
 * 4. 价格由上向下穿越中轨 - 下降趋势确认，考虑卖出
 */
@Slf4j
@Getter
public class BollingerBandsStrategy extends BacktestFramework{

    /**
     * 布林带周期
     */
    private final int period;

    /**
     * 布林带标准差倍数
     */
    private final double multiplier;

    /**
     * 每次交易比例 (0-1)
     */
    private final BigDecimal tradingRatio;

    /**
     * 止损百分比
     */
    private final BigDecimal stopLossPercentage;

    /**
     * 止盈百分比
     */
    private final BigDecimal takeProfitPercentage;

    /**
     * 交易模式枚举
     */
    public enum TradingMode {
        /**
         * 突破模式 - 价格突破上下轨时产生信号
         */
        BREAKOUT,

        /**
         * 反转模式 - 价格触及上下轨后反转时产生信号
         */
        REVERSAL,

        /**
         * 挤压模式 - 布林带收窄后扩张时产生信号
         */
        SQUEEZE
    }

    /**
     * 交易模式
     */
    private final TradingMode tradingMode;

    /**
     * 是否使用均线过滤
     */
    private final boolean useSMAFilter;

    /**
     * 均线周期
     */
    private final int smaFilterPeriod;

    /**
     * 构造函数
     *
     * @param initialBalance       初始资金
     * @param feeRate              手续费率
     * @param period               布林带周期
     * @param multiplier           布林带标准差倍数
     * @param tradingRatio         每次交易比例
     * @param stopLossPercentage   止损百分比
     * @param takeProfitPercentage 止盈百分比
     * @param tradingMode          交易模式
     * @param useSMAFilter         是否使用均线过滤
     * @param smaFilterPeriod      均线过滤周期
     */
    public BollingerBandsStrategy(BigDecimal initialBalance, BigDecimal feeRate,
                                  int period, double multiplier, BigDecimal tradingRatio,
                                  BigDecimal stopLossPercentage, BigDecimal takeProfitPercentage,
                                  TradingMode tradingMode, boolean useSMAFilter, int smaFilterPeriod) {
        super("布林带策略", initialBalance, feeRate);
        this.period = period;
        this.multiplier = multiplier;
        this.tradingRatio = tradingRatio;
        this.stopLossPercentage = stopLossPercentage;
        this.takeProfitPercentage = takeProfitPercentage;
        this.tradingMode = tradingMode;
        this.useSMAFilter = useSMAFilter;
        this.smaFilterPeriod = smaFilterPeriod;
    }

    /**
     * 简化版构造函数，使用默认参数
     *
     * @param initialBalance 初始资金
     * @param feeRate        手续费率
     * @param period         布林带周期
     * @param multiplier     布林带标准差倍数
     * @param tradingRatio   每次交易比例
     */
    public BollingerBandsStrategy(BigDecimal initialBalance, BigDecimal feeRate,
                                 int period, double multiplier, BigDecimal tradingRatio) {
        this(initialBalance, feeRate, period, multiplier, tradingRatio,
                new BigDecimal("0.05"), new BigDecimal("0.10"),
                TradingMode.BREAKOUT, false, 50);
    }

    @Override
    public void runBacktest(List<CandlestickEntity> candlesticks) {
        if (candlesticks == null || candlesticks.size() < period) {
            log.error("数据不足，无法进行回测，至少需要{}条数据", period);
            return;
        }

        log.info("开始回测 {} | 初始资金: {} | 布林带周期: {} | 标准差倍数: {} | 交易比例: {} | 交易模式: {}",
                strategyName, initialBalance, period, multiplier, tradingRatio, tradingMode);

        // 提取价格数据
        List<BigDecimal> closePrices = new ArrayList<>();
        List<BigDecimal> highPrices = new ArrayList<>();
        List<BigDecimal> lowPrices = new ArrayList<>();

        // 交易状态变量
        boolean inLongPosition = false;  // 是否持有多头仓位
        BigDecimal entryPrice = BigDecimal.ZERO;  // 入场价格
        BigDecimal stopLossPrice = BigDecimal.ZERO;  // 止损价格
        BigDecimal takeProfitPrice = BigDecimal.ZERO;  // 止盈价格

        // 布林带挤压检测变量
        BigDecimal previousBandWidth = BigDecimal.ZERO;
        boolean isSqueezed = false;
        int squeezeCounter = 0;

        // 主要回测循环
        for (int i = 0; i < candlesticks.size(); i++) {
            CandlestickEntity candle = candlesticks.get(i);
            BigDecimal closePrice = candle.getClose();
            BigDecimal highPrice = candle.getHigh();
            BigDecimal lowPrice = candle.getLow();

            closePrices.add(closePrice);
            highPrices.add(highPrice);
            lowPrices.add(lowPrice);

            // 记录当前账户状态
            recordBalance(candle.getOpenTime(), closePrice);

            // 检查止盈止损
            if (inLongPosition && i > 0) {
                // 止损检查
                if (lowPrice.compareTo(stopLossPrice) <= 0) {
                    sell(candle.getOpenTime(), stopLossPrice, position,
                        String.format("触发止损: 价格 %.2f <= 止损价 %.2f", lowPrice, stopLossPrice));
                    inLongPosition = false;
                    continue;
                }

                // 止盈检查
                if (highPrice.compareTo(takeProfitPrice) >= 0) {
                    sell(candle.getOpenTime(), takeProfitPrice, position,
                        String.format("触发止盈: 价格 %.2f >= 止盈价 %.2f", highPrice, takeProfitPrice));
                    inLongPosition = false;
                    continue;
                }
            }

            // 当收集到足够的数据时，开始计算布林带指标
            if (i >= period - 1) {
                // 计算布林带
                TechnicalIndicatorUtil.BollingerBands bands =
                    TechnicalIndicatorUtil.calculateBollingerBands(
                        closePrices.subList(0, i + 1), period, multiplier, 8);

                // 获取当前布林带值
                BigDecimal middle = bands.getMiddle().get(i);
                BigDecimal upper = bands.getUpper().get(i);
                BigDecimal lower = bands.getLower().get(i);

                // 如果使用均线过滤，计算长期均线
                BigDecimal longSMA = null;
                if (useSMAFilter && i >= smaFilterPeriod - 1) {
                    longSMA = TechnicalIndicatorUtil.calculateSMA(
                        closePrices.subList(i - smaFilterPeriod + 1, i + 1), 8);
                }

                // 计算布林带宽度 (upper - lower) / middle
                BigDecimal bandWidth = upper.subtract(lower).divide(middle, 8, RoundingMode.HALF_UP);

                // 挤压检测 - 连续3个周期带宽缩小
                if (i > period + 2) {
                    if (bandWidth.compareTo(previousBandWidth) < 0) {
                        squeezeCounter++;
                    } else {
                        squeezeCounter = 0;
                    }

                    isSqueezed = squeezeCounter >= 3;
                }
                previousBandWidth = bandWidth;

                // 根据交易模式生成不同的交易信号
                switch (tradingMode) {
                    case BREAKOUT:
                        handleBreakoutStrategy(candle, closePrice, inLongPosition, middle, upper, lower, longSMA);
                        break;
                    case REVERSAL:
                        handleReversalStrategy(candle, closePrice, inLongPosition, middle, upper, lower, longSMA);
                        break;
                    case SQUEEZE:
                        handleSqueezeStrategy(candle, closePrice, inLongPosition, isSqueezed, bandWidth, middle, upper, lower, longSMA);
                        break;
                }

                // 如果当前为持仓状态，更新入场价格、止损和止盈
                if (position.compareTo(BigDecimal.ZERO) > 0 && !inLongPosition) {
                    inLongPosition = true;
                    entryPrice = closePrice;
                    stopLossPrice = entryPrice.multiply(BigDecimal.ONE.subtract(stopLossPercentage));
                    takeProfitPrice = entryPrice.multiply(BigDecimal.ONE.add(takeProfitPercentage));
                }
            }
        }

        // 回测结束，如果还有持仓则平仓
        if (position.compareTo(BigDecimal.ZERO) > 0) {
            CandlestickEntity lastCandle = candlesticks.get(candlesticks.size() - 1);
            sell(lastCandle.getOpenTime(), lastCandle.getClose(), position, "回测结束平仓");
        }

        // 计算策略性能
        calculatePerformance();
    }

    /**
     * 处理突破策略交易信号
     */
    private void handleBreakoutStrategy(CandlestickEntity candle, BigDecimal closePrice,
                                        boolean inLongPosition, BigDecimal middle,
                                        BigDecimal upper, BigDecimal lower, BigDecimal longSMA) {
        // 获取前一个收盘价（如果可用）
        BigDecimal previousClose = null;
        if (tradeRecords.size() > 0) {
            previousClose = tradeRecords.get(tradeRecords.size() - 1).getPrice();
        }

        // 突破下轨 - 买入信号
        if (!inLongPosition && closePrice.compareTo(lower) <= 0) {
            // 均线过滤条件：如果启用均线过滤，则价格必须在均线之上
            if (!useSMAFilter || longSMA == null || closePrice.compareTo(longSMA) >= 0) {
                BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                buy(candle.getOpenTime(), closePrice, buyAmount,
                    String.format("价格(%.2f)突破下轨(%.2f)，产生买入信号", closePrice, lower));
            }
        }
        // 突破上轨 - 卖出信号
        else if (inLongPosition && closePrice.compareTo(upper) >= 0) {
            sell(candle.getOpenTime(), closePrice, position,
                String.format("价格(%.2f)突破上轨(%.2f)，产生卖出信号", closePrice, upper));
        }
        // 价格从下向上穿越中轨 - 买入信号
        else if (!inLongPosition && previousClose != null &&
                 previousClose.compareTo(middle) < 0 && closePrice.compareTo(middle) >= 0) {
            // 均线过滤条件
            if (!useSMAFilter || longSMA == null || closePrice.compareTo(longSMA) >= 0) {
                BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                buy(candle.getOpenTime(), closePrice, buyAmount,
                    String.format("价格从下向上穿越中轨(%.2f)，产生买入信号", middle));
            }
        }
        // 价格从上向下穿越中轨 - 卖出信号
        else if (inLongPosition && previousClose != null &&
                 previousClose.compareTo(middle) > 0 && closePrice.compareTo(middle) <= 0) {
            sell(candle.getOpenTime(), closePrice, position,
                String.format("价格从上向下穿越中轨(%.2f)，产生卖出信号", middle));
        }
    }

    /**
     * 处理反转策略交易信号
     */
    private void handleReversalStrategy(CandlestickEntity candle, BigDecimal closePrice,
                                        boolean inLongPosition, BigDecimal middle,
                                        BigDecimal upper, BigDecimal lower, BigDecimal longSMA) {
        // 获取前一个收盘价（如果可用）
        BigDecimal previousClose = null;
        if (tradeRecords.size() > 0) {
            previousClose = tradeRecords.get(tradeRecords.size() - 1).getPrice();
        }

        // 从下轨反弹 - 买入信号（价格之前触及下轨，现在回升）
        if (!inLongPosition && previousClose != null &&
            previousClose.compareTo(lower) <= 0 && closePrice.compareTo(lower) > 0) {
            // 均线过滤条件
            if (!useSMAFilter || longSMA == null || closePrice.compareTo(longSMA) >= 0) {
                BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                buy(candle.getOpenTime(), closePrice, buyAmount,
                    String.format("价格从下轨(%.2f)反弹，产生买入信号", lower));
            }
        }
        // 从上轨回落 - 卖出信号（价格之前触及上轨，现在回落）
        else if (inLongPosition && previousClose != null &&
                 previousClose.compareTo(upper) >= 0 && closePrice.compareTo(upper) < 0) {
            sell(candle.getOpenTime(), closePrice, position,
                String.format("价格从上轨(%.2f)回落，产生卖出信号", upper));
        }
        // 价格在通道中间区域，从下向上穿越中轨 - 买入信号
        else if (!inLongPosition && previousClose != null &&
                 previousClose.compareTo(middle) < 0 && closePrice.compareTo(middle) >= 0) {
            // 均线过滤条件
            if (!useSMAFilter || longSMA == null || closePrice.compareTo(longSMA) >= 0) {
                BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                buy(candle.getOpenTime(), closePrice, buyAmount,
                    String.format("价格从下向上穿越中轨(%.2f)，产生买入信号", middle));
            }
        }
        // 价格在通道中间区域，从上向下穿越中轨 - 卖出信号
        else if (inLongPosition && previousClose != null &&
                 previousClose.compareTo(middle) > 0 && closePrice.compareTo(middle) <= 0) {
            sell(candle.getOpenTime(), closePrice, position,
                String.format("价格从上向下穿越中轨(%.2f)，产生卖出信号", middle));
        }
    }

    /**
     * 处理挤压策略交易信号
     */
    private void handleSqueezeStrategy(CandlestickEntity candle, BigDecimal closePrice,
                                       boolean inLongPosition, boolean isSqueezed,
                                       BigDecimal bandWidth, BigDecimal middle,
                                       BigDecimal upper, BigDecimal lower, BigDecimal longSMA) {
        // 获取前一个收盘价和前一个布林带宽度（如果可用）
        BigDecimal previousClose = null;
        if (tradeRecords.size() > 0) {
            previousClose = tradeRecords.get(tradeRecords.size() - 1).getPrice();
        }

        // 布林带挤压后向上突破 - 买入信号
        if (!inLongPosition && isSqueezed && previousClose != null &&
            previousClose.compareTo(middle) <= 0 && closePrice.compareTo(middle) > 0) {
            // 均线过滤条件
            if (!useSMAFilter || longSMA == null || closePrice.compareTo(longSMA) >= 0) {
                BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                buy(candle.getOpenTime(), closePrice, buyAmount,
                    String.format("布林带挤压后价格向上突破中轨(%.2f)，产生买入信号", middle));
            }
        }
        // 布林带挤压后向下突破 - 卖出信号
        else if (inLongPosition && isSqueezed && previousClose != null &&
                 previousClose.compareTo(middle) >= 0 && closePrice.compareTo(middle) < 0) {
            sell(candle.getOpenTime(), closePrice, position,
                String.format("布林带挤压后价格向下突破中轨(%.2f)，产生卖出信号", middle));
        }
        // 普通的上下轨突破信号，与BREAKOUT模式相同
        else if (!inLongPosition && closePrice.compareTo(lower) <= 0) {
            // 均线过滤条件
            if (!useSMAFilter || longSMA == null || closePrice.compareTo(longSMA) >= 0) {
                BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                buy(candle.getOpenTime(), closePrice, buyAmount,
                    String.format("价格(%.2f)突破下轨(%.2f)，产生买入信号", closePrice, lower));
            }
        }
        else if (inLongPosition && closePrice.compareTo(upper) >= 0) {
            sell(candle.getOpenTime(), closePrice, position,
                String.format("价格(%.2f)突破上轨(%.2f)，产生卖出信号", closePrice, upper));
        }
    }
}
