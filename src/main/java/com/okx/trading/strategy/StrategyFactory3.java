package com.okx.trading.strategy;

import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.statistics.*;
import org.ta4j.core.indicators.volume.*;
import org.ta4j.core.rules.*;
import org.ta4j.core.num.DecimalNum;

/**
 * 高级策略集合 - 第二批 (策略 51-90)
 * 包含动量反转、成交量分析、波动率统计、复合指标等40个策略
 */
public class StrategyFactory3 {

    // 静态工厂方法 - 动量反转策略 (51-60)
    public static Strategy createRSIReversalStrategy(BarSeries series) {
        return new RSIReversalStrategy(series);
    }

    public static Strategy createWilliamsRReversalStrategy(BarSeries series) {
        return new WilliamsRStrategy(series);
    }

    public static Strategy createMomentumOscillatorStrategy(BarSeries series) {
        return new MomentumStrategy(series);
    }

    public static Strategy createROCDivergenceStrategy(BarSeries series) {
        return new ROCStrategy(series);
    }

    public static Strategy createTRIXSignalStrategy(BarSeries series) {
        return new TRIXStrategy(series);
    }

    public static Strategy createParabolicSARReversalStrategy(BarSeries series) {
        return new ParabolicSARStrategy(series);
    }

    public static Strategy createATRBreakoutStrategy(BarSeries series) {
        return new ATRBreakoutStrategy(series);
    }

    public static Strategy createDonchianBreakoutStrategy(BarSeries series) {
        return new DonchianChannelStrategy(series);
    }

    public static Strategy createKeltnerBreakoutStrategy(BarSeries series) {
        return new KeltnerChannelStrategy(series);
    }

    public static Strategy createPriceChannelStrategy(BarSeries series) {
        return new PriceChannelStrategy(series);
    }

    // 成交量价格关系策略 (61-70)
    public static Strategy createVWMACrossoverStrategy(BarSeries series) {
        return new VWMAStrategy(series);
    }

    public static Strategy createAccumulationDistributionDivergenceStrategy(BarSeries series) {
        return new ADLineStrategy(series);
    }

    public static Strategy createOBVDivergenceStrategy(BarSeries series) {
        return new OBVStrategy(series);
    }

    public static Strategy createPriceVolumeConfirmationStrategy(BarSeries series) {
        return new PriceVolumeConfirmationStrategy(series);
    }

    public static Strategy createVolumeOscillatorSignalStrategy(BarSeries series) {
        return new VolumeOscillatorStrategy(series);
    }

    public static Strategy createPositiveVolumeIndexSignalStrategy(BarSeries series) {
        return new PVIStrategy(series);
    }

    public static Strategy createNegativeVolumeIndexSignalStrategy(BarSeries series) {
        return new NVIStrategy(series);
    }

    public static Strategy createVolumeRSIStrategy(BarSeries series) {
        return new VRSIStrategy(series);
    }

    public static Strategy createVolumeWeightedRSISignalStrategy(BarSeries series) {
        return new VolumeWeightedRSIStrategy(series);
    }

    public static Strategy createVolumeBreakoutConfirmationStrategy(BarSeries series) {
        return new VolumeBreakoutConfirmationStrategy(series);
    }

    // 波动性统计分析策略 (71-80)
    public static Strategy createHistoricalVolatilityStrategy(BarSeries series) {
        return new HistoricalVolatilityStrategy(series);
    }

    public static Strategy createStandardDeviationChannelStrategy(BarSeries series) {
        return new StandardDeviationChannelStrategy(series);
    }

    public static Strategy createCoefficientOfVariationStrategy(BarSeries series) {
        return new CoefficientOfVariationStrategy(series);
    }

    public static Strategy createSkewnessStrategy(BarSeries series) {
        return new SkewnessStrategy(series);
    }

    public static Strategy createKurtosisStrategy(BarSeries series) {
        return new KurtosisStrategy(series);
    }

    public static Strategy createZScoreStrategy(BarSeries series) {
        return new ZScoreStrategy(series);
    }

    public static Strategy createPercentileStrategy(BarSeries series) {
        return new PercentileStrategy(series);
    }

    public static Strategy createLinearRegressionStrategy(BarSeries series) {
        return new LinearRegressionStrategy(series);
    }

    public static Strategy createLinearRegressionSlopeStrategy(BarSeries series) {
        return new LinearRegressionSlopeStrategy(series);
    }

    public static Strategy createRSquaredStrategy(BarSeries series) {
        return new RSquaredStrategy(series);
    }

    // 复合指标策略 (81-90)
    public static Strategy createMultipleMAConfirmationStrategy(BarSeries series) {
        return new MultipleMAConfirmationStrategy(series);
    }

    public static Strategy createRSIMACDConfirmationStrategy(BarSeries series) {
        return new RSIMACDConfirmationStrategy(series);
    }

    public static Strategy createBollingerRSIComboStrategy(BarSeries series) {
        return new BollingerRSIComboStrategy(series);
    }

    public static Strategy createTripleIndicatorConfirmationStrategy(BarSeries series) {
        return new TripleIndicatorConfirmationStrategy(series);
    }

    public static Strategy createMomentumBreakoutStrategy(BarSeries series) {
        return new MomentumBreakoutStrategy(series);
    }

    public static Strategy createVolatilityBreakoutSystemStrategy(BarSeries series) {
        return new VolatilityBreakoutStrategy(series);
    }

    public static Strategy createTrendStrengthStrategy(BarSeries series) {
        return new TrendStrengthStrategy(series);
    }

    public static Strategy createSupportResistanceBreakoutStrategy(BarSeries series) {
        return new SupportResistanceBreakoutStrategy(series);
    }

    public static Strategy createPricePatternRecognitionStrategy(BarSeries series) {
        return new PricePatternRecognitionStrategy(series);
    }

    public static Strategy createComprehensiveScoringStrategy(BarSeries series) {
        return new ComprehensiveScoringStrategy(series);
    }

    /**
     * 策略51: RSI反转策略
     */
    public static class RSIReversalStrategy extends BaseStrategy {
        public RSIReversalStrategy(BarSeries series) {
            super(
                new UnderIndicatorRule(
                    new RSIIndicator(new ClosePriceIndicator(series), 14),
                    DecimalNum.valueOf(30)
                ),
                new OverIndicatorRule(
                    new RSIIndicator(new ClosePriceIndicator(series), 14),
                    DecimalNum.valueOf(70)
                )
            );
        }
    }

    /**
     * 策略52: 威廉指标策略
     */
    public static class WilliamsRStrategy extends BaseStrategy {
        public WilliamsRStrategy(BarSeries series) {
            super(
                new UnderIndicatorRule(
                    new WilliamsRIndicator(series, 14),
                    DecimalNum.valueOf(-80)
                ),
                new OverIndicatorRule(
                    new WilliamsRIndicator(series, 14),
                    DecimalNum.valueOf(-20)
                )
            );
        }
    }

    /**
     * 策略53: 动量指标策略
     */
    public static class MomentumStrategy extends BaseStrategy {
        public MomentumStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ROCIndicator(new ClosePriceIndicator(series), 10),
                    DecimalNum.valueOf(0)
                ),
                new UnderIndicatorRule(
                    new ROCIndicator(new ClosePriceIndicator(series), 10),
                    DecimalNum.valueOf(0)
                )
            );
        }
    }

    /**
     * 策略54: 变动率策略
     */
    public static class ROCStrategy extends BaseStrategy {
        public ROCStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ROCIndicator(new ClosePriceIndicator(series), 12),
                    DecimalNum.valueOf(0)
                ),
                new UnderIndicatorRule(
                    new ROCIndicator(new ClosePriceIndicator(series), 12),
                    DecimalNum.valueOf(0)
                )
            );
        }
    }

    /**
     * 策略55: TRIX策略
     */
    public static class TRIXStrategy extends BaseStrategy {
        public TRIXStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new EMAIndicator(new EMAIndicator(new EMAIndicator(new ClosePriceIndicator(series), 14), 14), 14),
                    new ClosePriceIndicator(series)
                ),
                new UnderIndicatorRule(
                    new EMAIndicator(new EMAIndicator(new EMAIndicator(new ClosePriceIndicator(series), 14), 14), 14),
                    new ClosePriceIndicator(series)
                )
            );
        }
    }

    /**
     * 策略56: 抛物线SAR策略
     */
    public static class ParabolicSARStrategy extends BaseStrategy {
        public ParabolicSARStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new ParabolicSarIndicator(series, DecimalNum.valueOf(0.02), DecimalNum.valueOf(0.2))
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new ParabolicSarIndicator(series, DecimalNum.valueOf(0.02), DecimalNum.valueOf(0.2))
                )
            );
        }
    }

    /**
     * 策略57: ATR突破策略
     */
    public static class ATRBreakoutStrategy extends BaseStrategy {
        public ATRBreakoutStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 20)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略58: 唐奇安通道策略
     */
    public static class DonchianChannelStrategy extends BaseStrategy {
        public DonchianChannelStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new HighestValueIndicator(new HighPriceIndicator(series), 20)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new LowestValueIndicator(new LowPriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略59: 肯特纳通道策略
     */
    public static class KeltnerChannelStrategy extends BaseStrategy {
        public KeltnerChannelStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new EMAIndicator(new ClosePriceIndicator(series), 20)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new EMAIndicator(new ClosePriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略60: 价格通道策略
     */
    public static class PriceChannelStrategy extends BaseStrategy {
        public PriceChannelStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new HighPriceIndicator(series), 20)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new LowPriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略61: VWMA策略
     */
    public static class VWMAStrategy extends BaseStrategy {
        public VWMAStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new VWAPIndicator(series, 20)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new VWAPIndicator(series, 20)
                )
            );
        }
    }

    /**
     * 策略62: 累积分布线策略
     */
    public static class ADLineStrategy extends BaseStrategy {
        public ADLineStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 14)
                ),
                new UnderIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 14)
                )
            );
        }
    }

    /**
     * 策略63: 能量潮策略
     */
    public static class OBVStrategy extends BaseStrategy {
        public OBVStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 10)
                ),
                new UnderIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 10)
                )
            );
        }
    }

    /**
     * 策略64: 价量确认策略
     */
    public static class PriceVolumeConfirmationStrategy extends BaseStrategy {
        public PriceVolumeConfirmationStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                )
            );
        }
    }

    /**
     * 策略65: 成交量震荡器策略
     */
    public static class VolumeOscillatorStrategy extends BaseStrategy {
        public VolumeOscillatorStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new SMAIndicator(new VolumeIndicator(series), 5),
                    new SMAIndicator(new VolumeIndicator(series), 20)
                ),
                new UnderIndicatorRule(
                    new SMAIndicator(new VolumeIndicator(series), 5),
                    new SMAIndicator(new VolumeIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略66: 正成交量指标策略
     */
    public static class PVIStrategy extends BaseStrategy {
        public PVIStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 255)
                ),
                new UnderIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 255)
                )
            );
        }
    }

    /**
     * 策略67: 负成交量指标策略
     */
    public static class NVIStrategy extends BaseStrategy {
        public NVIStrategy(BarSeries series) {
            super(
                new UnderIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 255)
                ),
                new OverIndicatorRule(
                    new OnBalanceVolumeIndicator(series),
                    new SMAIndicator(new OnBalanceVolumeIndicator(series), 255)
                )
            );
        }
    }

    /**
     * 策略68: 成交量RSI策略
     */
    public static class VRSIStrategy extends BaseStrategy {
        public VRSIStrategy(BarSeries series) {
            super(
                new UnderIndicatorRule(
                    new RSIIndicator(new VolumeIndicator(series), 14),
                    DecimalNum.valueOf(30)
                ),
                new OverIndicatorRule(
                    new RSIIndicator(new VolumeIndicator(series), 14),
                    DecimalNum.valueOf(70)
                )
            );
        }
    }

    /**
     * 策略69: 成交量加权RSI策略
     */
    public static class VolumeWeightedRSIStrategy extends BaseStrategy {
        public VolumeWeightedRSIStrategy(BarSeries series) {
            super(
                new AndRule(
                    new UnderIndicatorRule(
                        new RSIIndicator(new ClosePriceIndicator(series), 14),
                        DecimalNum.valueOf(30)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                ),
                new AndRule(
                    new OverIndicatorRule(
                        new RSIIndicator(new ClosePriceIndicator(series), 14),
                        DecimalNum.valueOf(70)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                )
            );
        }
    }

    /**
     * 策略70: 成交量突破确认策略
     */
    public static class VolumeBreakoutConfirmationStrategy extends BaseStrategy {
        public VolumeBreakoutConfirmationStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                )
            );
        }
    }

    /**
     * 策略71: 历史波动率策略
     */
    public static class HistoricalVolatilityStrategy extends BaseStrategy {
        public HistoricalVolatilityStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 14),
                    new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 14), 20)
                ),
                new UnderIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 14),
                    new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 14), 20)
                )
            );
        }
    }

    /**
     * 策略72: 标准差通道策略
     */
    public static class StandardDeviationChannelStrategy extends BaseStrategy {
        public StandardDeviationChannelStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 20)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略73: 变异系数策略
     */
    public static class CoefficientOfVariationStrategy extends BaseStrategy {
        public CoefficientOfVariationStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 14),
                    new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 14), 20)
                ),
                new UnderIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 14),
                    new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 14), 20)
                )
            );
        }
    }

    /**
     * 策略74: 偏度策略
     */
    public static class SkewnessStrategy extends BaseStrategy {
        public SkewnessStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 20),
                    DecimalNum.valueOf(0)
                ),
                new UnderIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 20),
                    DecimalNum.valueOf(0)
                )
            );
        }
    }

    /**
     * 策略75: 峰度策略 - 改进为基于标准差的波动性策略
     */
    public static class KurtosisStrategy extends BaseStrategy {
        public KurtosisStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 14),
                    new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 14), 10)
                ),
                new UnderIndicatorRule(
                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 14),
                    new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 14), 10)
                )
            );
        }
    }

    /**
     * 策略76: Z分数策略
     */
    public static class ZScoreStrategy extends BaseStrategy {
        public ZScoreStrategy(BarSeries series) {
            super(
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 20)
                ),
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略77: 百分位数策略 - 改进为更敏感的参数
     */
    public static class PercentileStrategy extends BaseStrategy {
        public PercentileStrategy(BarSeries series) {
            super(
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new LowestValueIndicator(new ClosePriceIndicator(series), 20)
                ),
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new HighestValueIndicator(new ClosePriceIndicator(series), 20)
                )
            );
        }
    }

    /**
     * 策略78: 线性回归策略
     */
    public static class LinearRegressionStrategy extends BaseStrategy {
        public LinearRegressionStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 14)
                ),
                new UnderIndicatorRule(
                    new ClosePriceIndicator(series),
                    new SMAIndicator(new ClosePriceIndicator(series), 14)
                )
            );
        }
    }

    /**
     * 策略79: 线性回归斜率策略
     */
    public static class LinearRegressionSlopeStrategy extends BaseStrategy {
        public LinearRegressionSlopeStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new ROCIndicator(new SMAIndicator(new ClosePriceIndicator(series), 14), 1),
                    DecimalNum.valueOf(0)
                ),
                new UnderIndicatorRule(
                    new ROCIndicator(new SMAIndicator(new ClosePriceIndicator(series), 14), 1),
                    DecimalNum.valueOf(0)
                )
            );
        }
    }

    /**
     * 策略80: R平方策略
     */
    public static class RSquaredStrategy extends BaseStrategy {
        public RSquaredStrategy(BarSeries series) {
            super(
                new OverIndicatorRule(
                    new PearsonCorrelationIndicator(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 14),
                        14
                    ),
                    DecimalNum.valueOf(0.5)
                ),
                new UnderIndicatorRule(
                    new CorrelationCoefficientIndicator(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 14),
                        14
                    ),
                    DecimalNum.valueOf(0.5)
                )
            );
        }
    }

    /**
     * 策略81: 多重移动平均确认策略
     */
    public static class MultipleMAConfirmationStrategy extends BaseStrategy {
        public MultipleMAConfirmationStrategy(BarSeries series) {
            super(
                new AndRule(
                    new AndRule(
                        new OverIndicatorRule(
                            new SMAIndicator(new ClosePriceIndicator(series), 10),
                            new SMAIndicator(new ClosePriceIndicator(series), 20)
                        ),
                        new OverIndicatorRule(
                            new SMAIndicator(new ClosePriceIndicator(series), 20),
                            new SMAIndicator(new ClosePriceIndicator(series), 50)
                        )
                    ),
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 10)
                    )
                ),
                new AndRule(
                    new AndRule(
                        new UnderIndicatorRule(
                            new SMAIndicator(new ClosePriceIndicator(series), 10),
                            new SMAIndicator(new ClosePriceIndicator(series), 20)
                        ),
                        new UnderIndicatorRule(
                            new SMAIndicator(new ClosePriceIndicator(series), 20),
                            new SMAIndicator(new ClosePriceIndicator(series), 50)
                        )
                    ),
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 10)
                    )
                )
            );
        }
    }

    /**
     * 策略82: RSI-MACD确认策略
     */
    public static class RSIMACDConfirmationStrategy extends BaseStrategy {
        public RSIMACDConfirmationStrategy(BarSeries series) {
            super(
                new AndRule(
                    new UnderIndicatorRule(
                        new RSIIndicator(new ClosePriceIndicator(series), 14),
                        DecimalNum.valueOf(30)
                    ),
                    new OverIndicatorRule(
                        new MACDIndicator(new ClosePriceIndicator(series), 12, 26),
                        new EMAIndicator(new MACDIndicator(new ClosePriceIndicator(series), 12, 26), 9)
                    )
                ),
                new AndRule(
                    new OverIndicatorRule(
                        new RSIIndicator(new ClosePriceIndicator(series), 14),
                        DecimalNum.valueOf(70)
                    ),
                    new UnderIndicatorRule(
                        new MACDIndicator(new ClosePriceIndicator(series), 12, 26),
                        new EMAIndicator(new MACDIndicator(new ClosePriceIndicator(series), 12, 26), 9)
                    )
                )
            );
        }
    }

    /**
     * 策略83: 布林带RSI组合策略
     */
    public static class BollingerRSIComboStrategy extends BaseStrategy {
        public BollingerRSIComboStrategy(BarSeries series) {
            super(
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new BollingerBandsLowerIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), 20)), new StandardDeviationIndicator(new ClosePriceIndicator(series), 20), DecimalNum.valueOf(2))
                    ),
                    new UnderIndicatorRule(
                        new RSIIndicator(new ClosePriceIndicator(series), 14),
                        DecimalNum.valueOf(30)
                    )
                ),
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new BollingerBandsUpperIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator(new ClosePriceIndicator(series), 20)), new StandardDeviationIndicator(new ClosePriceIndicator(series), 20), DecimalNum.valueOf(2))
                    ),
                    new OverIndicatorRule(
                        new RSIIndicator(new ClosePriceIndicator(series), 14),
                        DecimalNum.valueOf(70)
                    )
                )
            );
        }
    }

    /**
     * 策略84: 三重指标确认策略
     */
    public static class TripleIndicatorConfirmationStrategy extends BaseStrategy {
        public TripleIndicatorConfirmationStrategy(BarSeries series) {
            super(
                new AndRule(
                    new AndRule(
                        new OverIndicatorRule(
                            new ClosePriceIndicator(series),
                            new SMAIndicator(new ClosePriceIndicator(series), 20)
                        ),
                        new UnderIndicatorRule(
                            new RSIIndicator(new ClosePriceIndicator(series), 14),
                            DecimalNum.valueOf(30)
                        )
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                ),
                new AndRule(
                    new AndRule(
                        new UnderIndicatorRule(
                            new ClosePriceIndicator(series),
                            new SMAIndicator(new ClosePriceIndicator(series), 20)
                        ),
                        new OverIndicatorRule(
                            new RSIIndicator(new ClosePriceIndicator(series), 14),
                            DecimalNum.valueOf(70)
                        )
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                )
            );
        }
    }

    /**
     * 策略85: 动量突破策略
     */
    public static class MomentumBreakoutStrategy extends BaseStrategy {
        public MomentumBreakoutStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new ROCIndicator(new ClosePriceIndicator(series), 10),
                        DecimalNum.valueOf(0)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new UnderIndicatorRule(
                        new ROCIndicator(new ClosePriceIndicator(series), 10),
                        DecimalNum.valueOf(0)
                    )
                )
            );
        }
    }

    /**
     * 策略86: 波动率突破策略
     */
    public static class VolatilityBreakoutStrategy extends BaseStrategy {
        public VolatilityBreakoutStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new StandardDeviationIndicator(new ClosePriceIndicator(series), 10),
                        new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 10), 10)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new StandardDeviationIndicator(new ClosePriceIndicator(series), 10),
                        new SMAIndicator(new StandardDeviationIndicator(new ClosePriceIndicator(series), 10), 10)
                    )
                )
            );
        }
    }

    /**
     * 策略87: 趋势强度策略
     */
    public static class TrendStrengthStrategy extends BaseStrategy {
        public TrendStrengthStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 50)
                    ),
                    new OverIndicatorRule(
                        new SMAIndicator(new ClosePriceIndicator(series), 12),
                        new SMAIndicator(new ClosePriceIndicator(series), 50)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 50)
                    ),
                    new UnderIndicatorRule(
                        new SMAIndicator(new ClosePriceIndicator(series), 12),
                        new SMAIndicator(new ClosePriceIndicator(series), 50)
                    )
                )
            );
        }
    }

    /**
     * 策略88: 支撑阻力突破策略
     */
    public static class SupportResistanceBreakoutStrategy extends BaseStrategy {
        public SupportResistanceBreakoutStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new HighestValueIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new LowestValueIndicator(new ClosePriceIndicator(series), 20)
                    ),
                    new OverIndicatorRule(
                        new VolumeIndicator(series),
                        new SMAIndicator(new VolumeIndicator(series), 20)
                    )
                )
            );
        }
    }

    /**
     * 策略89: 价格形态识别策略
     */
    public static class PricePatternRecognitionStrategy extends BaseStrategy {
        public PricePatternRecognitionStrategy(BarSeries series) {
            super(
                new AndRule(
                    new OverIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 5)
                    ),
                    new OverIndicatorRule(
                        new SMAIndicator(new ClosePriceIndicator(series), 5),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    )
                ),
                new AndRule(
                    new UnderIndicatorRule(
                        new ClosePriceIndicator(series),
                        new SMAIndicator(new ClosePriceIndicator(series), 5)
                    ),
                    new UnderIndicatorRule(
                        new SMAIndicator(new ClosePriceIndicator(series), 5),
                        new SMAIndicator(new ClosePriceIndicator(series), 20)
                    )
                )
            );
        }
    }

    /**
     * 策略90: 综合评分策略
     */
    public static class ComprehensiveScoringStrategy extends BaseStrategy {
        public ComprehensiveScoringStrategy(BarSeries series) {
            super(
                new AndRule(
                    new AndRule(
                        new AndRule(
                            new OverIndicatorRule(
                                new ClosePriceIndicator(series),
                                new SMAIndicator(new ClosePriceIndicator(series), 20)
                            ),
                            new UnderIndicatorRule(
                                new RSIIndicator(new ClosePriceIndicator(series), 14),
                                DecimalNum.valueOf(50)
                            )
                        ),
                        new OverIndicatorRule(
                            new VolumeIndicator(series),
                            new SMAIndicator(new VolumeIndicator(series), 20)
                        )
                    ),
                    new OverIndicatorRule(
                        new ROCIndicator(new ClosePriceIndicator(series), 10),
                        DecimalNum.valueOf(0)
                    )
                ),
                new AndRule(
                    new AndRule(
                        new AndRule(
                            new UnderIndicatorRule(
                                new ClosePriceIndicator(series),
                                new SMAIndicator(new ClosePriceIndicator(series), 20)
                            ),
                            new OverIndicatorRule(
                                new RSIIndicator(new ClosePriceIndicator(series), 14),
                                DecimalNum.valueOf(50)
                            )
                        ),
                        new OverIndicatorRule(
                            new VolumeIndicator(series),
                            new SMAIndicator(new VolumeIndicator(series), 20)
                        )
                    ),
                    new UnderIndicatorRule(
                        new ROCIndicator(new ClosePriceIndicator(series), 10),
                        DecimalNum.valueOf(0)
                    )
                )
            );
        }
    }
}
