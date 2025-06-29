package com.okx.trading.strategy;

import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.statistics.*;
import org.ta4j.core.indicators.volume.*;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.keltner.*;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;
import org.ta4j.core.num.DecimalNum;

/**
 * 高级策略工厂 - 第三批
 * 包含动量反转、成交量分析、波动率统计、复合指标等策略
 * 参考 StrategyFactory1 的设计模式，每个方法直接返回策略对象
 */
public class StrategyFactory3 {

    /**
     * 策略51: RSI反转策略
     * 基于RSI超买超卖信号进行反转交易
     */
    public static Strategy createRSIReversalStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        // 买入信号：RSI < 30 (超卖)
        Rule buyRule = new UnderIndicatorRule(rsi, DecimalNum.valueOf(30));

        // 卖出信号：RSI > 70 (超买)
        Rule sellRule = new OverIndicatorRule(rsi, DecimalNum.valueOf(70));

        return new BaseStrategy("RSI反转策略", buyRule, sellRule);
    }

    /**
     * 策略52: Williams %R 反转策略
     * 基于Williams %R指标的反转信号
     */
    public static Strategy createWilliamsRReversalStrategy(BarSeries series) {
        WilliamsRIndicator williamsR = new WilliamsRIndicator(series, 14);

        // 买入信号：Williams %R < -80 (超卖)
        Rule buyRule = new UnderIndicatorRule(williamsR, DecimalNum.valueOf(-80));

        // 卖出信号：Williams %R > -20 (超买)
        Rule sellRule = new OverIndicatorRule(williamsR, DecimalNum.valueOf(-20));

        return new BaseStrategy("Williams R反转策略", buyRule, sellRule);
    }

    /**
     * 策略53: 动量振荡器策略
     * 基于价格动量的多空信号
     */
    public static Strategy createMomentumOscillatorStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 动量指标 = 当前价格 / N期前价格
        class MomentumIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public MomentumIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period) {
                    return DecimalNum.valueOf(100);
                }
                return closePrice.getValue(index).dividedBy(closePrice.getValue(index - period)).multipliedBy(DecimalNum.valueOf(100));
            }
        }

        MomentumIndicator momentum = new MomentumIndicator(closePrice, 10, series);
        SMAIndicator momentumSMA = new SMAIndicator(momentum, 5);

        // 买入信号：动量上穿100且动量MA确认
        Rule buyRule = new CrossedUpIndicatorRule(momentum, DecimalNum.valueOf(100))
            .and(new OverIndicatorRule(momentum, momentumSMA));

        // 卖出信号：动量下穿100且动量MA确认
        Rule sellRule = new CrossedDownIndicatorRule(momentum, DecimalNum.valueOf(100))
            .and(new UnderIndicatorRule(momentum, momentumSMA));

        return new BaseStrategy("动量振荡器策略", buyRule, sellRule);
    }

    /**
     * 策略54: ROC背离策略
     * 基于变化率(ROC)与价格背离的信号
     */
    public static Strategy createROCDivergenceStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ROCIndicator roc = new ROCIndicator(closePrice, 12);
        SMAIndicator rocMA = new SMAIndicator(roc, 5);

        // 买入信号：ROC从负值区域上涨且突破其移动平均线
        Rule buyRule = new OverIndicatorRule(roc, DecimalNum.valueOf(0))
            .and(new CrossedUpIndicatorRule(roc, rocMA));

        // 卖出信号：ROC从正值区域下跌且跌破其移动平均线
        Rule sellRule = new UnderIndicatorRule(roc, DecimalNum.valueOf(0))
            .and(new CrossedDownIndicatorRule(roc, rocMA));

        return new BaseStrategy("ROC背离策略", buyRule, sellRule);
    }

    /**
     * 策略55: TRIX信号策略
     * 基于三重指数平滑移动平均线的信号
     */
    public static Strategy createTRIXSignalStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // TRIX = (三重EMA的变化率) * 10000
        EMAIndicator ema1 = new EMAIndicator(closePrice, 14);
        EMAIndicator ema2 = new EMAIndicator(ema1, 14);
        EMAIndicator ema3 = new EMAIndicator(ema2, 14);

        class TRIXIndicator extends CachedIndicator<Num> {
            private final EMAIndicator ema3;
            private final Num multiplier;

            public TRIXIndicator(EMAIndicator ema3, BarSeries series) {
                super(series);
                this.ema3 = ema3;
                this.multiplier = DecimalNum.valueOf(10000);
            }

            @Override
            protected Num calculate(int index) {
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num currentEma = ema3.getValue(index);
                Num previousEma = ema3.getValue(index - 1);
                return currentEma.minus(previousEma).dividedBy(previousEma).multipliedBy(multiplier);
            }
        }

        TRIXIndicator trix = new TRIXIndicator(ema3, series);
        SMAIndicator trixSignal = new SMAIndicator(trix, 9);

        // 买入信号：TRIX上穿其信号线
        Rule buyRule = new CrossedUpIndicatorRule(trix, trixSignal);

        // 卖出信号：TRIX下穿其信号线
        Rule sellRule = new CrossedDownIndicatorRule(trix, trixSignal);

        return new BaseStrategy("TRIX信号策略", buyRule, sellRule);
    }

    /**
     * 策略56: 抛物线SAR反转策略
     * 基于抛物线SAR指标的趋势反转信号
     */
    public static Strategy createParabolicSARReversalStrategy(BarSeries series) {
        ParabolicSarIndicator psar = new ParabolicSarIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 买入信号：价格上穿SAR
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, psar);

        // 卖出信号：价格下穿SAR
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, psar);

        return new BaseStrategy("抛物线SAR反转策略", buyRule, sellRule);
    }

    /**
     * 策略57: ATR突破策略
     * 基于平均真实波幅(ATR)的突破交易
     */
    public static Strategy createATRBreakoutStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ATRIndicator atr = new ATRIndicator(series, 14);
        SMAIndicator closeMA = new SMAIndicator(closePrice, 20);

        // ATR带宽突破
        class ATRUpperBand extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final ATRIndicator atr;
            private final Num multiplier;

            public ATRUpperBand(SMAIndicator sma, ATRIndicator atr, double multiplier, BarSeries series) {
                super(series);
                this.sma = sma;
                this.atr = atr;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return sma.getValue(index).plus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        class ATRLowerBand extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final ATRIndicator atr;
            private final Num multiplier;

            public ATRLowerBand(SMAIndicator sma, ATRIndicator atr, double multiplier, BarSeries series) {
                super(series);
                this.sma = sma;
                this.atr = atr;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return sma.getValue(index).minus(atr.getValue(index).multipliedBy(multiplier));
            }
        }

        ATRUpperBand upperBand = new ATRUpperBand(closeMA, atr, 2.0, series);
        ATRLowerBand lowerBand = new ATRLowerBand(closeMA, atr, 2.0, series);

        // 买入信号：价格突破ATR上轨
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, upperBand);

        // 卖出信号：价格跌破ATR下轨
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, lowerBand);

        return new BaseStrategy("ATR突破策略", buyRule, sellRule);
    }

    /**
     * 策略58: 唐奇安通道突破策略
     * 基于唐奇安通道的突破交易系统
     */
    public static Strategy createDonchianBreakoutStrategy(BarSeries series) {
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 唐奇安通道上轨 = N期最高价
        class DonchianUpper extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final int period;

            public DonchianUpper(HighPriceIndicator highPrice, int period, BarSeries series) {
                super(series);
                this.highPrice = highPrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num highest = highPrice.getValue(startIndex);
                for (int i = startIndex + 1; i <= index; i++) {
                    Num current = highPrice.getValue(i);
                    if (current.isGreaterThan(highest)) {
                        highest = current;
                    }
                }
                return highest;
            }
        }

        // 唐奇安通道下轨 = N期最低价
        class DonchianLower extends CachedIndicator<Num> {
            private final LowPriceIndicator lowPrice;
            private final int period;

            public DonchianLower(LowPriceIndicator lowPrice, int period, BarSeries series) {
                super(series);
                this.lowPrice = lowPrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num lowest = lowPrice.getValue(startIndex);
                for (int i = startIndex + 1; i <= index; i++) {
                    Num current = lowPrice.getValue(i);
                    if (current.isLessThan(lowest)) {
                        lowest = current;
                    }
                }
                return lowest;
            }
        }

        DonchianUpper donchianUpper = new DonchianUpper(highPrice, 15, series);
        DonchianLower donchianLower = new DonchianLower(lowPrice, 15, series);

        // 添加成交量确认
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, 10);


        // 买入信号：价格突破唐奇安上轨且成交量放大
        // 创建成交量阈值指标
        TransformIndicator volumeThreshold = TransformIndicator.multiply(avgVolume, 1.1);
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, donchianUpper)
                .and(new OverIndicatorRule(volume, volumeThreshold));

        // 卖出信号：价格跌破唐奇安下轨
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, donchianLower);

        return new BaseStrategy("唐奇安通道突破策略", buyRule, sellRule);
    }

    /**
     * 策略59: 肯特纳通道突破策略
     * 基于肯特纳通道的突破交易
     */
    public static Strategy createKeltnerBreakoutStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        KeltnerChannelUpperIndicator keltnerUpper = new KeltnerChannelUpperIndicator(new KeltnerChannelMiddleIndicator(series, 20), Double.valueOf(2), 10);
        KeltnerChannelLowerIndicator keltnerLower = new KeltnerChannelLowerIndicator(new KeltnerChannelMiddleIndicator(series, 20), Double.valueOf(2), 10);

        // 买入信号：价格突破肯特纳上轨
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, keltnerUpper);

        // 卖出信号：价格跌破肯特纳下轨
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, keltnerLower);

        return new BaseStrategy("肯特纳通道突破策略", buyRule, sellRule);
    }

    /**
     * 策略60: 价格通道策略
     * 基于价格通道的边界反弹交易
     */
    public static Strategy createPriceChannelStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);

        // 价格通道上轨
        class UpperChannel extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final StandardDeviationIndicator stdDev;
            private final Num multiplier;

            public UpperChannel(SMAIndicator sma, StandardDeviationIndicator stdDev, double multiplier, BarSeries series) {
                super(series);
                this.sma = sma;
                this.stdDev = stdDev;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return sma.getValue(index).plus(stdDev.getValue(index).multipliedBy(multiplier));
            }
        }

        // 价格通道下轨
        class LowerChannel extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final StandardDeviationIndicator stdDev;
            private final Num multiplier;

            public LowerChannel(SMAIndicator sma, StandardDeviationIndicator stdDev, double multiplier, BarSeries series) {
                super(series);
                this.sma = sma;
                this.stdDev = stdDev;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return sma.getValue(index).minus(stdDev.getValue(index).multipliedBy(multiplier));
            }
        }

        UpperChannel upperChannel = new UpperChannel(sma20, stdDev, 1.5, series);
        LowerChannel lowerChannel = new LowerChannel(sma20, stdDev, 1.5, series);

        // 买入信号：价格上穿上轨
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, upperChannel);

        // 卖出信号：价格下穿下轨
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, lowerChannel);

        return new BaseStrategy("价格通道策略", buyRule, sellRule);
    }

    /**
     * 策略61: 成交量加权移动平均交叉策略
     * 基于VWMA的交叉信号
     */
    public static Strategy createVWMACrossoverStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 成交量加权移动平均线
        class VWMAIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final VolumeIndicator volume;
            private final int period;

            public VWMAIndicator(ClosePriceIndicator closePrice, VolumeIndicator volume, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.volume = volume;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num totalVolumePrice = DecimalNum.valueOf(0);
                Num totalVolume = DecimalNum.valueOf(0);

                for (int i = startIndex; i <= index; i++) {
                    Num price = closePrice.getValue(i);
                    Num vol = volume.getValue(i);
                    totalVolumePrice = totalVolumePrice.plus(price.multipliedBy(vol));
                    totalVolume = totalVolume.plus(vol);
                }

                if (totalVolume.isZero()) {
                    return closePrice.getValue(index);
                }
                return totalVolumePrice.dividedBy(totalVolume);
            }
        }

        VWMAIndicator vwma12 = new VWMAIndicator(closePrice, volume, 12, series);
        VWMAIndicator vwma26 = new VWMAIndicator(closePrice, volume, 26, series);

        // 买入信号：短期VWMA上穿长期VWMA
        Rule buyRule = new CrossedUpIndicatorRule(vwma12, vwma26);

        // 卖出信号：短期VWMA下穿长期VWMA
        Rule sellRule = new CrossedDownIndicatorRule(vwma12, vwma26);

        return new BaseStrategy("VWMA交叉策略", buyRule, sellRule);
    }

    /**
     * 策略62: 累积/分布线背离策略
     * 基于A/D线与价格背离的信号
     */
    public static Strategy createAccumulationDistributionDivergenceStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // A/D线指标
        class ADLineIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final LowPriceIndicator lowPrice;
            private final ClosePriceIndicator closePrice;
            private final VolumeIndicator volume;

            public ADLineIndicator(BarSeries series) {
                super(series);
                this.highPrice = new HighPriceIndicator(series);
                this.lowPrice = new LowPriceIndicator(series);
                this.closePrice = new ClosePriceIndicator(series);
                this.volume = new VolumeIndicator(series);
            }

            @Override
            protected Num calculate(int index) {
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }

                Num high = highPrice.getValue(index);
                Num low = lowPrice.getValue(index);
                Num close = closePrice.getValue(index);
                Num vol = volume.getValue(index);

                Num clv;
                if (high.isEqual(low)) {
                    clv = DecimalNum.valueOf(0);
                } else {
                    clv = close.minus(low).minus(high.minus(close)).dividedBy(high.minus(low));
                }

                return getValue(index - 1).plus(clv.multipliedBy(vol));
            }
        }

        ADLineIndicator adLine = new ADLineIndicator(series);
        SMAIndicator adMA = new SMAIndicator(adLine, 10);
        SMAIndicator priceMA = new SMAIndicator(closePrice, 10);

        // 买入信号：A/D线向上突破其移动平均线，且价格也上涨
        Rule buyRule = new CrossedUpIndicatorRule(adLine, adMA)
            .and(new OverIndicatorRule(closePrice, priceMA));

        // 卖出信号：A/D线向下跌破其移动平均线，且价格也下跌
        Rule sellRule = new CrossedDownIndicatorRule(adLine, adMA)
            .and(new UnderIndicatorRule(closePrice, priceMA));

        return new BaseStrategy("A/D线背离策略", buyRule, sellRule);
    }

    /**
     * 策略63: OBV背离策略
     * 基于能量潮(OBV)与价格背离的信号
     */
    public static Strategy createOBVDivergenceStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OnBalanceVolumeIndicator obv = new OnBalanceVolumeIndicator(series);
        SMAIndicator obvMA = new SMAIndicator(obv, 10);
        SMAIndicator priceMA = new SMAIndicator(closePrice, 10);

        // 买入信号：OBV向上突破其移动平均线
        Rule buyRule = new CrossedUpIndicatorRule(obv, obvMA)
            .and(new OverIndicatorRule(closePrice, priceMA));

        // 卖出信号：OBV向下跌破其移动平均线
        Rule sellRule = new CrossedDownIndicatorRule(obv, obvMA)
            .and(new UnderIndicatorRule(closePrice, priceMA));

        return new BaseStrategy("OBV背离策略", buyRule, sellRule);
    }

    /**
     * 策略64: 价量确认策略
     * 价格突破必须有成交量确认
     */
    public static Strategy createPriceVolumeConfirmationStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator priceMA = new SMAIndicator(closePrice, 20);
        SMAIndicator volumeMA = new SMAIndicator(volume, 20);

        // 成交量阈值指标
        class VolumeThresholdIndicator extends CachedIndicator<Num> {
            private final SMAIndicator volumeMA;
            private final Num multiplier;

            public VolumeThresholdIndicator(SMAIndicator volumeMA, double multiplier, BarSeries series) {
                super(series);
                this.volumeMA = volumeMA;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return volumeMA.getValue(index).multipliedBy(multiplier);
            }
        }

        VolumeThresholdIndicator volumeThreshold = new VolumeThresholdIndicator(volumeMA, 1.2, series);

        // 买入信号：价格突破均线且成交量放大
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, priceMA)
            .and(new OverIndicatorRule(volume, volumeThreshold));

        // 卖出信号：价格跌破均线且成交量放大
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, priceMA)
            .and(new OverIndicatorRule(volume, volumeThreshold));

        return new BaseStrategy("价量确认策略", buyRule, sellRule);
    }

    /**
     * 策略65: 成交量振荡器信号策略
     * 基于成交量振荡器的信号
     */
    public static Strategy createVolumeOscillatorSignalStrategy(BarSeries series) {
        VolumeIndicator volume = new VolumeIndicator(series);

        // 成交量振荡器 = (短期成交量MA - 长期成交量MA) / 长期成交量MA * 100
        class VolumeOscillator extends CachedIndicator<Num> {
            private final SMAIndicator shortMA;
            private final SMAIndicator longMA;
            private final Num hundred;

            public VolumeOscillator(SMAIndicator shortMA, SMAIndicator longMA, BarSeries series) {
                super(series);
                this.shortMA = shortMA;
                this.longMA = longMA;
                this.hundred = DecimalNum.valueOf(100);
            }

            @Override
            protected Num calculate(int index) {
                Num shortValue = shortMA.getValue(index);
                Num longValue = longMA.getValue(index);
                if (longValue.isZero()) {
                    return DecimalNum.valueOf(0);
                }
                return shortValue.minus(longValue).dividedBy(longValue).multipliedBy(hundred);
            }
        }

        SMAIndicator volumeMA12 = new SMAIndicator(volume, 12);
        SMAIndicator volumeMA26 = new SMAIndicator(volume, 26);
        VolumeOscillator volOsc = new VolumeOscillator(volumeMA12, volumeMA26, series);

        // 买入信号：成交量振荡器从负值区域上穿0轴
        Rule buyRule = new CrossedUpIndicatorRule(volOsc, DecimalNum.valueOf(0));

        // 卖出信号：成交量振荡器从正值区域下穿0轴
        Rule sellRule = new CrossedDownIndicatorRule(volOsc, DecimalNum.valueOf(0));

        return new BaseStrategy("成交量振荡器策略", buyRule, sellRule);
    }

    /**
     * 策略66: 正成交量指数信号策略
     * 基于PVI的信号
     */
    public static Strategy createPositiveVolumeIndexSignalStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 正成交量指数
        class PVIIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final VolumeIndicator volume;

            public PVIIndicator(ClosePriceIndicator closePrice, VolumeIndicator volume, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.volume = volume;
            }

            @Override
            protected Num calculate(int index) {
                if (index == 0) {
                    return DecimalNum.valueOf(1000);
                }

                Num currentVolume = volume.getValue(index);
                Num previousVolume = volume.getValue(index - 1);
                Num previousPVI = getValue(index - 1);

                if (currentVolume.isGreaterThan(previousVolume)) {
                    Num priceChange = closePrice.getValue(index).dividedBy(closePrice.getValue(index - 1));
                    return previousPVI.multipliedBy(priceChange);
                } else {
                    return previousPVI;
                }
            }
        }

        PVIIndicator pvi = new PVIIndicator(closePrice, volume, series);
        SMAIndicator pviMA = new SMAIndicator(pvi, 255);

        // 买入信号：PVI上穿其长期移动平均线
        Rule buyRule = new CrossedUpIndicatorRule(pvi, pviMA);

        // 卖出信号：PVI下穿其长期移动平均线
        Rule sellRule = new CrossedDownIndicatorRule(pvi, pviMA);

        return new BaseStrategy("正成交量指数策略", buyRule, sellRule);
    }

    /**
     * 策略67: 负成交量指数信号策略
     * 基于NVI的信号
     */
    public static Strategy createNegativeVolumeIndexSignalStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 负成交量指数
        class NVIIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final VolumeIndicator volume;

            public NVIIndicator(ClosePriceIndicator closePrice, VolumeIndicator volume, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.volume = volume;
            }

            @Override
            protected Num calculate(int index) {
                if (index == 0) {
                    return DecimalNum.valueOf(1000);
                }

                Num currentVolume = volume.getValue(index);
                Num previousVolume = volume.getValue(index - 1);
                Num previousNVI = getValue(index - 1);

                if (currentVolume.isLessThan(previousVolume)) {
                    Num priceChange = closePrice.getValue(index).dividedBy(closePrice.getValue(index - 1));
                    return previousNVI.multipliedBy(priceChange);
                } else {
                    return previousNVI;
                }
            }
        }

        NVIIndicator nvi = new NVIIndicator(closePrice, volume, series);
        SMAIndicator nviMA = new SMAIndicator(nvi, 255);

        // 买入信号：NVI上穿其长期移动平均线
        Rule buyRule = new CrossedUpIndicatorRule(nvi, nviMA);

        // 卖出信号：NVI下穿其长期移动平均线
        Rule sellRule = new CrossedDownIndicatorRule(nvi, nviMA);

        return new BaseStrategy("负成交量指数策略", buyRule, sellRule);
    }

    /**
     * 策略68: 成交量RSI策略
     * 基于成交量RSI的超买超卖信号
     */
    public static Strategy createVolumeRSIStrategy(BarSeries series) {
        VolumeIndicator volume = new VolumeIndicator(series);
        RSIIndicator volumeRSI = new RSIIndicator(volume, 14);

        // 买入信号：成交量RSI从超卖区域(30以下)回升
        Rule buyRule = new CrossedUpIndicatorRule(volumeRSI, DecimalNum.valueOf(30));

        // 卖出信号：成交量RSI从超买区域(70以上)回落
        Rule sellRule = new CrossedDownIndicatorRule(volumeRSI, DecimalNum.valueOf(70));

        return new BaseStrategy("成交量RSI策略", buyRule, sellRule);
    }

    /**
     * 策略69: 成交量加权RSI信号策略
     * 结合价格RSI和成交量RSI的综合信号
     */
    public static Strategy createVolumeWeightedRSISignalStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        RSIIndicator priceRSI = new RSIIndicator(closePrice, 14);
        RSIIndicator volumeRSI = new RSIIndicator(volume, 14);

        // 买入信号：价格RSI和成交量RSI都从超卖区域回升
        Rule buyRule = new CrossedUpIndicatorRule(priceRSI, DecimalNum.valueOf(30))
            .and(new OverIndicatorRule(volumeRSI, DecimalNum.valueOf(50)));

        // 卖出信号：价格RSI和成交量RSI都从超买区域回落
        Rule sellRule = new CrossedDownIndicatorRule(priceRSI, DecimalNum.valueOf(70))
            .and(new UnderIndicatorRule(volumeRSI, DecimalNum.valueOf(50)));

        return new BaseStrategy("成交量加权RSI策略", buyRule, sellRule);
    }

    /**
     * 策略70: 成交量突破确认策略
     * 价格突破必须有异常成交量确认
     */
    public static Strategy createVolumeBreakoutConfirmationStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // 20日最高价
        class HighestHighIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final int period;

            public HighestHighIndicator(HighPriceIndicator highPrice, int period, BarSeries series) {
                super(series);
                this.highPrice = highPrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num highest = highPrice.getValue(startIndex);
                for (int i = startIndex + 1; i <= index; i++) {
                    Num current = highPrice.getValue(i);
                    if (current.isGreaterThan(highest)) {
                        highest = current;
                    }
                }
                return highest;
            }
        }

        // 20日最低价
        class LowestLowIndicator extends CachedIndicator<Num> {
            private final LowPriceIndicator lowPrice;
            private final int period;

            public LowestLowIndicator(LowPriceIndicator lowPrice, int period, BarSeries series) {
                super(series);
                this.lowPrice = lowPrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num lowest = lowPrice.getValue(startIndex);
                for (int i = startIndex + 1; i <= index; i++) {
                    Num current = lowPrice.getValue(i);
                    if (current.isLessThan(lowest)) {
                        lowest = current;
                    }
                }
                return lowest;
            }
        }

        HighestHighIndicator highest20 = new HighestHighIndicator(highPrice, 20, series);
        LowestLowIndicator lowest20 = new LowestLowIndicator(lowPrice, 20, series);
        SMAIndicator volumeMA = new SMAIndicator(volume, 20);

        // 成交量阈值指标
        class VolumeThresholdIndicator extends CachedIndicator<Num> {
            private final SMAIndicator volumeMA;
            private final Num multiplier;

            public VolumeThresholdIndicator(SMAIndicator volumeMA, double multiplier, BarSeries series) {
                super(series);
                this.volumeMA = volumeMA;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return volumeMA.getValue(index).multipliedBy(multiplier);
            }
        }

        VolumeThresholdIndicator volumeThreshold = new VolumeThresholdIndicator(volumeMA, 1.5, series);

        // 买入信号：突破20日最高价且成交量放大1.5倍以上
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, highest20)
            .and(new OverIndicatorRule(volume, volumeThreshold));

        // 卖出信号：跌破20日最低价且成交量放大1.5倍以上
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, lowest20)
            .and(new OverIndicatorRule(volume, volumeThreshold));

        return new BaseStrategy("成交量突破确认策略", buyRule, sellRule);
    }

    // 波动性统计分析策略 (71-80)
    /**
     * 策略71: 历史波动率策略
     */
    public static Strategy createHistoricalVolatilityStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 历史波动率指标
        class HistoricalVolatilityIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public HistoricalVolatilityIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period) {
                    return DecimalNum.valueOf(0);
                }

                // 计算对数收益率的标准差
                Num sumSquaredReturns = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    if (i > 0) {
                        Num logReturn = closePrice.getValue(i).dividedBy(closePrice.getValue(i - 1)).log();
                        sumSquaredReturns = sumSquaredReturns.plus(logReturn.pow(2));
                    }
                }
                return sumSquaredReturns.dividedBy(DecimalNum.valueOf(period)).sqrt();
            }
        }

        HistoricalVolatilityIndicator hv = new HistoricalVolatilityIndicator(closePrice, 20, series);
        SMAIndicator hvMA = new SMAIndicator(hv, 10);

        // 买入信号：波动率低于均值
        Rule buyRule = new UnderIndicatorRule(hv, hvMA);

        // 卖出信号：波动率高于均值
        Rule sellRule = new OverIndicatorRule(hv, hvMA);

        return new BaseStrategy("历史波动率策略", buyRule, sellRule);
    }

    /**
     * 策略72: 标准差通道策略
     */
    public static Strategy createStandardDeviationChannelStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);

        // 上轨 = SMA + 2 * StdDev
        class UpperBandIndicator extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final StandardDeviationIndicator stdDev;
            private final Num multiplier;

            public UpperBandIndicator(SMAIndicator sma, StandardDeviationIndicator stdDev, double multiplier, BarSeries series) {
                super(series);
                this.sma = sma;
                this.stdDev = stdDev;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return sma.getValue(index).plus(stdDev.getValue(index).multipliedBy(multiplier));
            }
        }

        // 下轨 = SMA - 2 * StdDev
        class LowerBandIndicator extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final StandardDeviationIndicator stdDev;
            private final Num multiplier;

            public LowerBandIndicator(SMAIndicator sma, StandardDeviationIndicator stdDev, double multiplier, BarSeries series) {
                super(series);
                this.sma = sma;
                this.stdDev = stdDev;
                this.multiplier = DecimalNum.valueOf(multiplier);
            }

            @Override
            protected Num calculate(int index) {
                return sma.getValue(index).minus(stdDev.getValue(index).multipliedBy(multiplier));
            }
        }

        UpperBandIndicator upperBand = new UpperBandIndicator(sma, stdDev, 2.0, series);
        LowerBandIndicator lowerBand = new LowerBandIndicator(sma, stdDev, 2.0, series);

        // 买入信号：价格触及下轨
        Rule buyRule = new UnderIndicatorRule(closePrice, lowerBand);

        // 卖出信号：价格触及上轨
        Rule sellRule = new OverIndicatorRule(closePrice, upperBand);

        return new BaseStrategy("标准差通道策略", buyRule, sellRule);
    }

    /**
     * 策略73: 变异系数策略
     */
    public static Strategy createCoefficientOfVariationStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);

        // 变异系数 = 标准差 / 均值
        class CoefficientOfVariationIndicator extends CachedIndicator<Num> {
            private final SMAIndicator sma;
            private final StandardDeviationIndicator stdDev;

            public CoefficientOfVariationIndicator(SMAIndicator sma, StandardDeviationIndicator stdDev, BarSeries series) {
                super(series);
                this.sma = sma;
                this.stdDev = stdDev;
            }

            @Override
            protected Num calculate(int index) {
                Num mean = sma.getValue(index);
                if (mean.isZero()) {
                    return DecimalNum.valueOf(0);
                }
                return stdDev.getValue(index).dividedBy(mean);
            }
        }

        CoefficientOfVariationIndicator cv = new CoefficientOfVariationIndicator(sma, stdDev, series);

        // 买入信号：变异系数低于0.05
        Rule buyRule = new UnderIndicatorRule(cv, DecimalNum.valueOf(0.05));

        // 卖出信号：变异系数高于0.15
        Rule sellRule = new OverIndicatorRule(cv, DecimalNum.valueOf(0.15));

        return new BaseStrategy("变异系数策略", buyRule, sellRule);
    }

    /**
     * 策略74: 偏度策略 - 真正的偏度统计计算
     */
    public static Strategy createSkewnessStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 真正的偏度计算指标
        class SkewnessIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public SkewnessIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return DecimalNum.valueOf(0);
                }

                // 计算均值
                Num sum = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    sum = sum.plus(closePrice.getValue(i));
                }
                Num mean = sum.dividedBy(DecimalNum.valueOf(period));

                // 计算标准差
                Num variance = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    Num diff = closePrice.getValue(i).minus(mean);
                    variance = variance.plus(diff.multipliedBy(diff));
                }
                variance = variance.dividedBy(DecimalNum.valueOf(period));
                Num stdDev = DecimalNum.valueOf(Math.sqrt(variance.doubleValue()));

                if (stdDev.isZero()) {
                    return DecimalNum.valueOf(0);
                }

                // 计算偏度
                Num skewness = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    Num diff = closePrice.getValue(i).minus(mean);
                    Num standardized = diff.dividedBy(stdDev);
                    skewness = skewness.plus(standardized.multipliedBy(standardized).multipliedBy(standardized));
                }
                skewness = skewness.dividedBy(DecimalNum.valueOf(period));

                return skewness;
            }
        }

        SkewnessIndicator skewness = new SkewnessIndicator(closePrice, 20, series);

        // 正偏度买入，负偏度卖出（降低阈值）
        Rule buyRule = new OverIndicatorRule(skewness, DecimalNum.valueOf(0.1)); // 降低阈值（原来0.5）
        Rule sellRule = new UnderIndicatorRule(skewness, DecimalNum.valueOf(-0.1)); // 降低阈值（原来-0.5）

        return new BaseStrategy("偏度策略", buyRule, sellRule);
    }

    /**
     * 策略75: 峰度策略 - 真正的峰度统计计算
     */
    public static Strategy createKurtosisStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 真正的峰度计算指标
        class KurtosisIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public KurtosisIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return DecimalNum.valueOf(3); // 正态分布的峰度为3
                }

                // 计算均值
                Num sum = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    sum = sum.plus(closePrice.getValue(i));
                }
                Num mean = sum.dividedBy(DecimalNum.valueOf(period));

                // 计算标准差
                Num variance = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    Num diff = closePrice.getValue(i).minus(mean);
                    variance = variance.plus(diff.multipliedBy(diff));
                }
                variance = variance.dividedBy(DecimalNum.valueOf(period));
                Num stdDev = DecimalNum.valueOf(Math.sqrt(variance.doubleValue()));

                if (stdDev.isZero()) {
                    return DecimalNum.valueOf(3);
                }

                // 计算峰度
                Num kurtosis = DecimalNum.valueOf(0);
                for (int i = index - period + 1; i <= index; i++) {
                    Num diff = closePrice.getValue(i).minus(mean);
                    Num standardized = diff.dividedBy(stdDev);
                    Num fourthPower = standardized.multipliedBy(standardized).multipliedBy(standardized).multipliedBy(standardized);
                    kurtosis = kurtosis.plus(fourthPower);
                }
                kurtosis = kurtosis.dividedBy(DecimalNum.valueOf(period));

                return kurtosis;
            }
        }

        KurtosisIndicator kurtosis = new KurtosisIndicator(closePrice, 20, series);

        // 高峰度（厚尾）买入，低峰度（薄尾）卖出（降低阈值）
        Rule buyRule = new OverIndicatorRule(kurtosis, DecimalNum.valueOf(2.5)); // 降低阈值（原来4）
        Rule sellRule = new UnderIndicatorRule(kurtosis, DecimalNum.valueOf(1)); // 降低阈值（原来2）

        return new BaseStrategy("峰度策略", buyRule, sellRule);
    }

    /**
     * 策略76: Z-Score策略
     */
    public static Strategy createZScoreStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);

        // Z-Score = (价格 - 均值) / 标准差
        class ZScoreIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final SMAIndicator sma;
            private final StandardDeviationIndicator stdDev;

            public ZScoreIndicator(ClosePriceIndicator closePrice, SMAIndicator sma, StandardDeviationIndicator stdDev, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.sma = sma;
                this.stdDev = stdDev;
            }

            @Override
            protected Num calculate(int index) {
                Num price = closePrice.getValue(index);
                Num mean = sma.getValue(index);
                Num std = stdDev.getValue(index);

                if (std.isZero()) {
                    return DecimalNum.valueOf(0);
                }
                return price.minus(mean).dividedBy(std);
            }
        }

        ZScoreIndicator zscore = new ZScoreIndicator(closePrice, sma, stdDev, series);

        // Z分数超买超卖（降低阈值，更容易触发）
        Rule buyRule = new UnderIndicatorRule(zscore, DecimalNum.valueOf(-1.5)); // 降低阈值（原来-2）
        Rule sellRule = new OverIndicatorRule(zscore, DecimalNum.valueOf(1.5)); // 降低阈值（原来2）

        return new BaseStrategy("Z-Score策略", buyRule, sellRule);
    }

    /**
     * 策略77: 百分位策略 - 真正的百分位计算
     */
    public static Strategy createPercentileStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 百分位指标
        class PercentileIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;
            private final double percentile;

            public PercentileIndicator(ClosePriceIndicator closePrice, int period, double percentile, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
                this.percentile = percentile;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return closePrice.getValue(index);
                }

                // 收集周期内的价格数据
                double[] prices = new double[period];
                for (int i = 0; i < period; i++) {
                    prices[i] = closePrice.getValue(index - period + 1 + i).doubleValue();
                }

                // 排序
                java.util.Arrays.sort(prices);

                // 计算百分位数
                int rankIndex = (int) Math.ceil(percentile * period / 100.0) - 1;
                rankIndex = Math.max(0, Math.min(rankIndex, period - 1));

                return DecimalNum.valueOf(prices[rankIndex]);
            }
        }

        PercentileIndicator percentile25 = new PercentileIndicator(closePrice, 20, 25.0, series);
        PercentileIndicator percentile75 = new PercentileIndicator(closePrice, 20, 75.0, series);

        // 价格低于25%分位数时买入，高于75%分位数时卖出
        Rule buyRule = new UnderIndicatorRule(closePrice, percentile25);
        Rule sellRule = new OverIndicatorRule(closePrice, percentile75);

        return new BaseStrategy("百分位策略", buyRule, sellRule);
    }

    /**
     * 策略78: 线性回归策略 - 真正的线性回归计算
     */
    public static Strategy createLinearRegressionStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 线性回归指标
        class LinearRegressionIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public LinearRegressionIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return closePrice.getValue(index);
                }

                // 线性回归计算
                double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
                int n = period;

                for (int i = 0; i < period; i++) {
                    double x = i + 1; // 时间序列
                    double y = closePrice.getValue(index - period + 1 + i).doubleValue();
                    sumX += x;
                    sumY += y;
                    sumXY += x * y;
                    sumX2 += x * x;
                }

                // 计算斜率和截距
                double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
                double intercept = (sumY - slope * sumX) / n;

                // 预测当前点的回归值
                double predictedValue = slope * period + intercept;

                return DecimalNum.valueOf(predictedValue);
            }
        }

        LinearRegressionIndicator regression = new LinearRegressionIndicator(closePrice, 20, series);

        // 改进的交易逻辑：使用更灵活的条件
        // 创建回归线的上下阈值
        TransformIndicator upperThreshold = TransformIndicator.multiply(regression, 1.01);
        TransformIndicator lowerThreshold = TransformIndicator.multiply(regression, 0.99);
        
        // 买入信号：价格相对于回归线有足够的向上偏差
        Rule buyRule = new OverIndicatorRule(closePrice, upperThreshold); // 高于回归线1%

        // 卖出信号：价格回归到回归线或下破
        Rule sellRule = new UnderIndicatorRule(closePrice, lowerThreshold); // 低于回归线1%

        return new BaseStrategy("线性回归策略", buyRule, sellRule);
    }

    /**
     * 策略79: 线性回归斜率策略 - 真正的斜率计算
     */
    public static Strategy createLinearRegressionSlopeStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 线性回归斜率指标
        class LinearRegressionSlopeIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public LinearRegressionSlopeIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return DecimalNum.valueOf(0);
                }

                // 线性回归斜率计算
                double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
                int n = period;

                for (int i = 0; i < period; i++) {
                    double x = i + 1; // 时间序列
                    double y = closePrice.getValue(index - period + 1 + i).doubleValue();
                    sumX += x;
                    sumY += y;
                    sumXY += x * y;
                    sumX2 += x * x;
                }

                // 计算斜率
                double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

                return DecimalNum.valueOf(slope);
            }
        }

        LinearRegressionSlopeIndicator slope = new LinearRegressionSlopeIndicator(closePrice, 20, series);

        // 改进的斜率策略：使用更敏感的阈值
        // 买入信号：斜率显著为正（上升趋势）
        Rule buyRule = new OverIndicatorRule(slope, series.numOf(0.1)); // 斜率 > 0.1

        // 卖出信号：斜率转为负或接近零（趋势减弱）
        Rule sellRule = new UnderIndicatorRule(slope, series.numOf(-0.05)); // 斜率 < -0.05

        return new BaseStrategy("线性回归斜率策略", buyRule, sellRule);
    }

    /**
     * 策略80: R平方策略 - 真正的R平方统计计算
     */
    public static Strategy createRSquaredStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // R平方指标
        class RSquaredIndicator extends CachedIndicator<Num> {
            private final ClosePriceIndicator closePrice;
            private final int period;

            public RSquaredIndicator(ClosePriceIndicator closePrice, int period, BarSeries series) {
                super(series);
                this.closePrice = closePrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                if (index < period - 1) {
                    return DecimalNum.valueOf(0);
                }

                // 计算R平方
                double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
                int n = period;

                for (int i = 0; i < period; i++) {
                    double x = i + 1; // 时间序列
                    double y = closePrice.getValue(index - period + 1 + i).doubleValue();
                    sumX += x;
                    sumY += y;
                    sumXY += x * y;
                    sumX2 += x * x;
                    sumY2 += y * y;
                }

                // 计算相关系数
                double numerator = n * sumXY - sumX * sumY;
                double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

                if (denominator == 0) {
                    return DecimalNum.valueOf(0);
                }

                double correlation = numerator / denominator;
                double rSquared = correlation * correlation;

                return DecimalNum.valueOf(rSquared);
            }
        }

        RSquaredIndicator rSquared = new RSquaredIndicator(closePrice, 20, series);

        // R平方高说明趋势性强，R平方低说明随机性强（降低阈值）
        Rule buyRule = new OverIndicatorRule(rSquared, DecimalNum.valueOf(0.6)); // 降低阈值（原来0.8）
        Rule sellRule = new UnderIndicatorRule(rSquared, DecimalNum.valueOf(0.2)); // 降低阈值（原来0.3）

        return new BaseStrategy("R平方策略", buyRule, sellRule);
    }

    // 复合指标策略 (81-90)
    public static Strategy createMultipleMAConfirmationStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);

        // 买入信号：短期MA > 中期MA > 长期MA
        Rule buyRule = new OverIndicatorRule(sma10, sma20)
            .and(new OverIndicatorRule(sma20, sma50));

        // 卖出信号：短期MA < 中期MA < 长期MA
        Rule sellRule = new UnderIndicatorRule(sma10, sma20)
            .and(new UnderIndicatorRule(sma20, sma50));

        return new BaseStrategy("多重MA确认策略", buyRule, sellRule);
    }

    public static Strategy createRSIMACDConfirmationStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);

        // 买入信号：RSI > 50 且 MACD > Signal
        Rule buyRule = new OverIndicatorRule(rsi, DecimalNum.valueOf(50))
            .and(new OverIndicatorRule(macd, macdSignal));

        // 卖出信号：RSI < 50 且 MACD < Signal
        Rule sellRule = new UnderIndicatorRule(rsi, DecimalNum.valueOf(50))
            .and(new UnderIndicatorRule(macd, macdSignal));

        return new BaseStrategy("RSI-MACD确认策略", buyRule, sellRule);
    }

    public static Strategy createBollingerRSIComboStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator(closePrice, 20)), new StandardDeviationIndicator(closePrice, 20), DecimalNum.valueOf(2));
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator(closePrice, 20)), new StandardDeviationIndicator(closePrice, 20), DecimalNum.valueOf(2));

        // 买入信号：价格触及布林下轨且RSI超卖
        Rule buyRule = new UnderIndicatorRule(closePrice, bbLower)
            .and(new UnderIndicatorRule(rsi, DecimalNum.valueOf(30)));

        // 卖出信号：价格触及布林上轨且RSI超买
        Rule sellRule = new OverIndicatorRule(closePrice, bbUpper)
            .and(new OverIndicatorRule(rsi, DecimalNum.valueOf(70)));

        return new BaseStrategy("布林-RSI组合策略", buyRule, sellRule);
    }

    public static Strategy createTripleIndicatorConfirmationStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 真正的三重指标确认：RSI + MACD + 成交量确认
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volumeMA = new SMAIndicator(volume, 20);

        // 买入信号：三个指标都确认看涨
        // 1. RSI > 50 (动量看涨)
        // 2. MACD > Signal (趋势看涨)
        // 3. 成交量 > 均量 (成交量确认)
        Rule buyRule = new OverIndicatorRule(rsi, DecimalNum.valueOf(50))
            .and(new OverIndicatorRule(macd, macdSignal))
            .and(new OverIndicatorRule(volume, volumeMA));

        // 卖出信号：任意两个指标看跌即卖出
        Rule sellRule = new UnderIndicatorRule(rsi, DecimalNum.valueOf(50))
            .and(new UnderIndicatorRule(macd, macdSignal))
            .or(new UnderIndicatorRule(volume, volumeMA));

        return new BaseStrategy("三重指标确认策略", buyRule, sellRule);
    }

    public static Strategy createMomentumBreakoutStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ROCIndicator roc = new ROCIndicator(closePrice, 10);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 买入信号：动量突破且价格突破均线
        Rule buyRule = new OverIndicatorRule(roc, DecimalNum.valueOf(5))
            .and(new OverIndicatorRule(closePrice, sma));

        // 卖出信号：动量下降且价格跌破均线
        Rule sellRule = new UnderIndicatorRule(roc, DecimalNum.valueOf(-5))
            .and(new UnderIndicatorRule(closePrice, sma));

        return new BaseStrategy("动量突破策略", buyRule, sellRule);
    }

    public static Strategy createVolatilityBreakoutSystemStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ATRIndicator atr = new ATRIndicator(series, 14);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, 10);

        // 买入信号：价格突破均线且波动率适中（降低ATR阈值）
        // 创建成交量阈值指标
        TransformIndicator volumeThreshold2 = TransformIndicator.multiply(avgVolume, 1.1);
        
        Rule buyRule = new OverIndicatorRule(closePrice, sma)
            .and(new OverIndicatorRule(atr, DecimalNum.valueOf(0.005))) // 大幅降低阈值（原来0.01）
            .and(new OverIndicatorRule(volume, volumeThreshold2));

        // 卖出信号：价格跌破均线或止盈2%
        Rule sellRule = new UnderIndicatorRule(closePrice, sma)
            .or(new OverIndicatorRule(closePrice, closePrice.getValue(1).multipliedBy(DecimalNum.valueOf(1.02))));

        return new BaseStrategy("波动性突破系统策略", buyRule, sellRule);
    }

    public static Strategy createTrendStrengthStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ADXIndicator adx = new ADXIndicator(series, 14);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 买入信号：趋势强度高且价格上涨
        Rule buyRule = new OverIndicatorRule(adx, DecimalNum.valueOf(25))
            .and(new OverIndicatorRule(closePrice, sma));

        // 卖出信号：趋势强度弱或价格下跌
        Rule sellRule = new UnderIndicatorRule(adx, DecimalNum.valueOf(20))
            .or(new UnderIndicatorRule(closePrice, sma));

        return new BaseStrategy("趋势强度策略", buyRule, sellRule);
    }

    public static Strategy createSupportResistanceBreakoutStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // 20日最高价作为阻力位
        class ResistanceIndicator extends CachedIndicator<Num> {
            private final HighPriceIndicator highPrice;
            private final int period;

            public ResistanceIndicator(HighPriceIndicator highPrice, int period, BarSeries series) {
                super(series);
                this.highPrice = highPrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num highest = highPrice.getValue(startIndex);
                for (int i = startIndex + 1; i <= index; i++) {
                    Num current = highPrice.getValue(i);
                    if (current.isGreaterThan(highest)) {
                        highest = current;
                    }
                }
                return highest;
            }
        }

        // 20日最低价作为支撑位
        class SupportIndicator extends CachedIndicator<Num> {
            private final LowPriceIndicator lowPrice;
            private final int period;

            public SupportIndicator(LowPriceIndicator lowPrice, int period, BarSeries series) {
                super(series);
                this.lowPrice = lowPrice;
                this.period = period;
            }

            @Override
            protected Num calculate(int index) {
                int startIndex = Math.max(0, index - period + 1);
                Num lowest = lowPrice.getValue(startIndex);
                for (int i = startIndex + 1; i <= index; i++) {
                    Num current = lowPrice.getValue(i);
                    if (current.isLessThan(lowest)) {
                        lowest = current;
                    }
                }
                return lowest;
            }
        }

        ResistanceIndicator resistance = new ResistanceIndicator(highPrice, 20, series);
        SupportIndicator support = new SupportIndicator(lowPrice, 20, series);

        // 买入信号：突破阻力位
        Rule buyRule = new CrossedUpIndicatorRule(closePrice, resistance);

        // 卖出信号：跌破支撑位
        Rule sellRule = new CrossedDownIndicatorRule(closePrice, support);

        return new BaseStrategy("支撑阻力突破策略", buyRule, sellRule);
    }

    public static Strategy createPricePatternRecognitionStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);

        // 买入信号：金叉
        Rule buyRule = new CrossedUpIndicatorRule(sma10, sma20);

        // 卖出信号：死叉
        Rule sellRule = new CrossedDownIndicatorRule(sma10, sma20);

        return new BaseStrategy("价格形态识别策略", buyRule, sellRule);
    }

    public static Strategy createComprehensiveScoringStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);

        // 多指标确认策略：需要多个指标同时确认才进行交易
        Rule buyRule = new OverIndicatorRule(rsi, DecimalNum.valueOf(40))
            .and(new OverIndicatorRule(macd, macdSignal))
            .and(new OverIndicatorRule(closePrice, sma));

        Rule sellRule = new UnderIndicatorRule(rsi, DecimalNum.valueOf(60))
            .and(new UnderIndicatorRule(macd, macdSignal))
            .and(new UnderIndicatorRule(closePrice, sma));

        return new BaseStrategy("多指标确认策略", buyRule, sellRule);
    }
}
