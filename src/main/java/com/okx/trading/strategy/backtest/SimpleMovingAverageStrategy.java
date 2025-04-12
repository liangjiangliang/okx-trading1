package com.okx.trading.strategy.backtest;

import com.okx.trading.model.entity.CandlestickEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单移动平均线交叉策略
 * 当短期均线上穿长期均线时买入，下穿时卖出
 */
@Slf4j
@Getter
public class SimpleMovingAverageStrategy extends BacktestFramework {

    /**
     * 短期均线周期
     */
    private final int shortPeriod;

    /**
     * 长期均线周期
     */
    private final int longPeriod;

    /**
     * 每次交易比例 (0-1)
     */
    private final BigDecimal tradingRatio;

    /**
     * 构造函数
     *
     * @param initialBalance 初始资金
     * @param feeRate        手续费率
     * @param shortPeriod    短期均线周期
     * @param longPeriod     长期均线周期
     * @param tradingRatio   每次交易比例
     */
    public SimpleMovingAverageStrategy(BigDecimal initialBalance, BigDecimal feeRate,
                                      int shortPeriod, int longPeriod, BigDecimal tradingRatio) {
        super("简单移动平均线交叉策略", initialBalance, feeRate);
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.tradingRatio = tradingRatio;
    }

    @Override
    public void runBacktest(List<CandlestickEntity> candlesticks) {
        if (candlesticks == null || candlesticks.size() < longPeriod) {
            log.error("数据不足，无法进行回测，至少需要{}条数据", longPeriod);
            return;
        }

        log.info("开始回测 {} | 初始资金: {} | 短期均线: {} | 长期均线: {} | 交易比例: {}",
                strategyName, initialBalance, shortPeriod, longPeriod, tradingRatio);

        // 计算移动平均
        List<BigDecimal> closePrices = new ArrayList<>();
        List<BigDecimal> shortMAs = new ArrayList<>();
        List<BigDecimal> longMAs = new ArrayList<>();

        boolean inPosition = false;  // 是否持仓

        for (int i = 0; i < candlesticks.size(); i++) {
            CandlestickEntity candle = candlesticks.get(i);
            BigDecimal closePrice = candle.getClose();
            closePrices.add(closePrice);

            // 记录当前账户状态
            recordBalance(candle.getOpenTime(), closePrice);

            // 当收集到足够的数据时，开始计算均线
            if (i >= longPeriod - 1) {
                // 计算短期均线
                BigDecimal shortMA = calculateSMA(closePrices, i, shortPeriod);
                shortMAs.add(shortMA);

                // 计算长期均线
                BigDecimal longMA = calculateSMA(closePrices, i, longPeriod);
                longMAs.add(longMA);

                // 交易信号判断
                if (shortMAs.size() > 1 && longMAs.size() > 1) {
                    boolean currentCross = shortMA.compareTo(longMA) > 0;
                    boolean previousCross = shortMAs.get(shortMAs.size() - 2).compareTo(longMAs.get(longMAs.size() - 2)) > 0;

                    // 短期均线上穿长期均线 - 买入信号
                    if (currentCross && !previousCross && !inPosition) {
                        // 使用指定比例的现金买入
                        BigDecimal buyAmount = cash.multiply(tradingRatio).divide(closePrice, 8, RoundingMode.DOWN);
                        buy(candle.getOpenTime(), closePrice, buyAmount, 
                            String.format("短期均线(%.2f)上穿长期均线(%.2f)", shortMA, longMA));
                        inPosition = true;
                    }
                    // 短期均线下穿长期均线 - 卖出信号
                    else if (!currentCross && previousCross && inPosition) {
                        // 卖出全部持仓
                        sell(candle.getOpenTime(), closePrice, position,
                            String.format("短期均线(%.2f)下穿长期均线(%.2f)", shortMA, longMA));
                        inPosition = false;
                    }
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
     * 计算简单移动平均线
     *
     * @param prices 价格列表
     * @param index  当前索引
     * @param period 周期
     * @return 移动平均线值
     */
    private BigDecimal calculateSMA(List<BigDecimal> prices, int index, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(index - i));
        }
        return sum.divide(new BigDecimal(period), 8, RoundingMode.HALF_UP);
    }
} 