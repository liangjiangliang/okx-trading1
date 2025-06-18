package com.okx.trading.strategy;

import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;

import java.util.*;

/**
 * 策略工厂4 - 高级策略集合
 * 包含40个高级策略（91-130），涵盖机器学习、量化因子、高频、期权、宏观、创新和风险管理策略
 */
public class StrategyFactory4 {

    // ==================== 机器学习启发策略 (91-100) ====================

    /**
     * 91. 神经网络策略 - 基于多层感知器的技术指标融合
     */
    public static Strategy createNeuralNetworkStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 输入层：多个技术指标作为神经元输入
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        
        // 隐藏层：权重计算和激活函数模拟
        Rule neuron1 = new OverIndicatorRule(rsi, series.numOf(30)).and(new UnderIndicatorRule(rsi, series.numOf(70))); // RSI神经元
        Rule neuron2 = new OverIndicatorRule(macd, series.numOf(0)); // MACD神经元  
        Rule neuron3 = new OverIndicatorRule(closePrice, sma20); // 趋势神经元
        Rule neuron4 = new UnderIndicatorRule(volatility, series.numOf(2.0)); // 波动率神经元
        
        // 输出层：多层感知器的最终输出（降低严格性，至少3个条件满足）
        Rule entryRule = (neuron1.and(neuron2).and(neuron3))
                .or(neuron1.and(neuron2).and(neuron4))
                .or(neuron1.and(neuron3).and(neuron4))
                .or(neuron2.and(neuron3).and(neuron4));
        Rule exitRule = new UnderIndicatorRule(rsi, series.numOf(25)).or(new OverIndicatorRule(rsi, series.numOf(75)));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 92. 遗传算法策略 - 基于遗传算法的参数进化优化
     */
    public static Strategy createGeneticAlgorithmStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 种群：多组不同参数的个体
        RSIIndicator rsi1 = new RSIIndicator(closePrice, 10); // 个体1
        RSIIndicator rsi2 = new RSIIndicator(closePrice, 14); // 个体2  
        RSIIndicator rsi3 = new RSIIndicator(closePrice, 21); // 个体3
        
        // 适应度函数：评估每个个体的表现
        Rule fitness1 = new OverIndicatorRule(rsi1, 25).and(new UnderIndicatorRule(rsi1, 75));
        Rule fitness2 = new OverIndicatorRule(rsi2, 30).and(new UnderIndicatorRule(rsi2, 70));
        Rule fitness3 = new OverIndicatorRule(rsi3, 35).and(new UnderIndicatorRule(rsi3, 65));
        
        // 选择和交叉：组合最优个体特征
        Rule crossover = fitness1.or(fitness2).or(fitness3);
        
        // 变异：随机调整避免局部最优
        ROCIndicator mutation = new ROCIndicator(closePrice, 5);
        Rule mutationRule = new OverIndicatorRule(mutation, 0.01);
        
        Rule entryRule = crossover.and(mutationRule);
        Rule exitRule = new UnderIndicatorRule(rsi2, 20).or(new OverIndicatorRule(rsi2, 80));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 93. 随机森林策略 - 基于集成决策树的分类预测
     */
    public static Strategy createRandomForestStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 决策树1：基于价格趋势
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        Rule tree1 = new OverIndicatorRule(closePrice, sma);
        
        // 决策树2：基于动量指标
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        Rule tree2 = new OverIndicatorRule(rsi, 40).and(new UnderIndicatorRule(rsi, 70));
        
        // 决策树3：基于成交量
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);
        Rule tree3 = new OverIndicatorRule(volume, avgVolume);
        
        // 决策树4：基于波动率
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        Rule tree4 = new UnderIndicatorRule(volatility, 2.0);
        
        // 随机森林投票：多数决策树同意则执行
        Rule entryRule = tree1.and(tree2).and(tree3)
                .or(tree1.and(tree2).and(tree4))
                .or(tree1.and(tree3).and(tree4))
                .or(tree2.and(tree3).and(tree4));
                
        Rule exitRule = new NotRule(tree1.and(tree2).and(tree3).and(tree4));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 94. 支持向量机策略 - 基于SVM分类思想的趋势识别
     */
    public static Strategy createSVMStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 特征向量：多维技术指标
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        
        // SVM决策边界：多个线性分离条件
        Rule boundary1 = new OverIndicatorRule(rsi, series.numOf(40)).and(new UnderIndicatorRule(rsi, series.numOf(80)));
        Rule boundary2 = new OverIndicatorRule(macd, series.numOf(0));
        Rule boundary3 = new UnderIndicatorRule(volatility, series.numOf(2.5));
        
        // 支持向量：关键决策点（降低严格性，至少2个边界满足）
        Rule entryRule = (boundary1.and(boundary2))
                .or(boundary1.and(boundary3))
                .or(boundary2.and(boundary3));
        Rule exitRule = new OverIndicatorRule(rsi, series.numOf(85)).or(new UnderIndicatorRule(rsi, series.numOf(15)));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 95. LSTM策略 - 基于长短期记忆网络的时序预测
     */
    public static Strategy createLSTMStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 长期记忆：长周期指标
        SMAIndicator longMemory = new SMAIndicator(closePrice, 50);
        EMAIndicator mediumMemory = new EMAIndicator(closePrice, 20);
        
        // 短期记忆：短周期指标
        EMAIndicator shortMemory = new EMAIndicator(closePrice, 5);
        
        // 遗忘门：决定保留哪些长期信息
        Rule forgetGate = new OverIndicatorRule(closePrice, longMemory);
        
        // 输入门：决定更新哪些短期信息
        Rule inputGate = new OverIndicatorRule(shortMemory, mediumMemory);
        
        // 输出门：综合长短期记忆生成信号
        Rule outputGate = forgetGate.and(inputGate);
        
        Rule entryRule = outputGate;
        Rule exitRule = new UnderIndicatorRule(shortMemory, mediumMemory);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 96. K最近邻策略 - 基于历史相似模式的预测
     */
    public static Strategy createKNNStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 特征提取：当前模式特征
        ROCIndicator priceChange = new ROCIndicator(closePrice, 1);
        RSIIndicator momentum = new RSIIndicator(closePrice, 14);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 5);
        
        // K最近邻：相似模式识别
        Rule similarPattern1 = new OverIndicatorRule(priceChange, 0.01)
                .and(new OverIndicatorRule(momentum, 50))
                .and(new UnderIndicatorRule(volatility, 1.5));
                
        Rule similarPattern2 = new OverIndicatorRule(momentum, 40)
                .and(new UnderIndicatorRule(momentum, 70))
                .and(new OverIndicatorRule(priceChange, 0));
        
        // 最近邻投票
        Rule entryRule = similarPattern1.or(similarPattern2);
        Rule exitRule = new UnderIndicatorRule(momentum, 30).or(new OverIndicatorRule(momentum, 80));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 97. 朴素贝叶斯策略 - 基于贝叶斯概率的条件预测
     */
    public static Strategy createNaiveBayesStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 特征独立性假设：各指标独立计算概率
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        ROCIndicator roc = new ROCIndicator(closePrice, 10);
        
        // 先验概率：基于历史统计
        Rule prior1 = new OverIndicatorRule(rsi, 40).and(new UnderIndicatorRule(rsi, 70));
        Rule prior2 = new OverIndicatorRule(macd, 0);
        Rule prior3 = new OverIndicatorRule(roc, 0);
        
        // 后验概率：贝叶斯更新
        Rule entryRule = prior1.and(prior2).and(prior3);
        Rule exitRule = new UnderIndicatorRule(rsi, 30).or(new OverIndicatorRule(rsi, 75));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 98. 决策树策略 - 基于决策树的规则化交易
     */
    public static Strategy createDecisionTreeStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 决策树节点
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        
        // 根节点：RSI判断
        Rule rootNode = new OverIndicatorRule(rsi, 50);
        
        // 左分支：RSI > 50时的决策路径
        Rule leftBranch = rootNode.and(new OverIndicatorRule(closePrice, sma)).and(new OverIndicatorRule(macd, 0));
        
        // 右分支：RSI <= 50时的决策路径
        Rule rightBranch = new NotRule(rootNode).and(new UnderIndicatorRule(closePrice, sma)).and(new UnderIndicatorRule(macd, 0));
        
        Rule entryRule = leftBranch.or(rightBranch);
        Rule exitRule = new OverIndicatorRule(rsi, 80).or(new UnderIndicatorRule(rsi, 20));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 99. 集成学习策略 - 基于多模型融合的策略
     */
    public static Strategy createEnsembleStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 模型1：趋势模型
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        Rule model1 = new OverIndicatorRule(closePrice, sma);
        
        // 模型2：动量模型
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        Rule model2 = new OverIndicatorRule(rsi, 50);
        
        // 模型3：成交量模型
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);
        Rule model3 = new OverIndicatorRule(volume, avgVolume);
        
        // 模型4：波动率模型
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        Rule model4 = new UnderIndicatorRule(volatility, 2.0);
        
        // 集成投票：加权投票机制
        Rule entryRule = model1.and(model2).and(model3)
                .or(model1.and(model2).and(model4))
                .or(model1.and(model3).and(model4))
                .or(model2.and(model3).and(model4));
                
        Rule exitRule = new NotRule(model1.and(model2));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 100. 强化学习策略 - 基于Q学习的自适应策略
     */
    public static Strategy createReinforcementLearningStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 状态空间：市场状态
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        
        // 动作空间：买入、卖出、持有
        Rule action1 = new OverIndicatorRule(rsi, 40).and(new OverIndicatorRule(macd, 0)); // 买入
        Rule action2 = new UnderIndicatorRule(rsi, 60).and(new UnderIndicatorRule(macd, 0)); // 卖出
        
        // 奖励函数：基于收益的奖励
        ROCIndicator reward = new ROCIndicator(closePrice, 1);
        Rule positiveReward = new OverIndicatorRule(reward, 0.01);
        
        // 探索vs利用
        Rule exploration = new OverIndicatorRule(volatility, 1.5); // 高波动时探索
        Rule exploitation = new UnderIndicatorRule(volatility, 1.0); // 低波动时利用
        
        Rule entryRule = action1.and(positiveReward).and(exploitation);
        Rule exitRule = action2.or(exploration);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    // ==================== 量化因子策略 (101-105) ====================

    /**
     * 101. 动量因子策略
     */
    public static Strategy createMomentumFactorStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 短期动量
        ROCIndicator shortMomentum = new ROCIndicator(closePrice, 20);
        // 长期动量
        ROCIndicator longMomentum = new ROCIndicator(closePrice, 60);
        
        // 动量信号
        Rule entryRule = new OverIndicatorRule(shortMomentum, 0.02)
                .and(new OverIndicatorRule(longMomentum, 0.05));
        Rule exitRule = new UnderIndicatorRule(shortMomentum, -0.01);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 102. 价值因子策略
     */
    public static Strategy createValueFactorStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 价值回归：价格偏离短期均值（进一步降低要求）
        SMAIndicator shortAvg = new SMAIndicator(closePrice, 20);
        SMAIndicator mediumAvg = new SMAIndicator(closePrice, 50);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        
        // 价值信号：价格低于中期均线且RSI超卖（更宽松条件）
        Rule entryRule = new UnderIndicatorRule(closePrice, mediumAvg)
                .and(new UnderIndicatorRule(rsi, series.numOf(45))); // 放宽到45
        Rule exitRule = new OverIndicatorRule(closePrice, shortAvg)
                .or(new OverIndicatorRule(rsi, series.numOf(65))); // 提前退出
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 103. 质量因子策略
     */
    public static Strategy createQualityFactorStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 质量指标：稳定性和趋势
        StandardDeviationIndicator stability = new StandardDeviationIndicator(closePrice, 30);
        SMAIndicator shortTrend = new SMAIndicator(closePrice, 10);
        SMAIndicator longTrend = new SMAIndicator(closePrice, 30);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        
        // 高质量信号：相对低波动率 + 上升趋势
        Rule entryRule = new UnderIndicatorRule(stability, series.numOf(2.0)) // 调整为2.0
                .and(new OverIndicatorRule(shortTrend, longTrend)) // 短期均线>长期均线
                .and(new OverIndicatorRule(rsi, series.numOf(45))); // RSI中位以上
        Rule exitRule = new OverIndicatorRule(stability, series.numOf(3.5))
                .or(new UnderIndicatorRule(shortTrend, longTrend));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 104. 规模因子策略
     */
    public static Strategy createSizeFactorStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 规模效应：小盘股溢价
        SMAIndicator avgPrice = new SMAIndicator(closePrice, 252);
        SMAIndicator avgVolume = new SMAIndicator(volume, 252);
        
        // 小规模信号
        Rule entryRule = new UnderIndicatorRule(closePrice, avgPrice)
                .and(new UnderIndicatorRule(volume, avgVolume));
        Rule exitRule = new OverIndicatorRule(closePrice, avgPrice);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 105. 低波动因子策略
     */
    public static Strategy createLowVolatilityFactorStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 相对波动率策略（更实用的方法）
        StandardDeviationIndicator shortVol = new StandardDeviationIndicator(closePrice, 10);
        StandardDeviationIndicator longVol = new StandardDeviationIndicator(closePrice, 30);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        
        // 低波动信号：短期波动低于长期波动且有趋势
        Rule entryRule = new UnderIndicatorRule(shortVol, longVol)
                .and(new OverIndicatorRule(closePrice, sma)) // 上涨趋势
                .and(new OverIndicatorRule(rsi, series.numOf(40))); // RSI中位
        Rule exitRule = new OverIndicatorRule(shortVol, longVol)
                .or(new UnderIndicatorRule(closePrice, sma));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    // ==================== 高频和微观结构策略 (106-110) ====================

    /**
     * 106. 微观结构不平衡策略
     */
    public static Strategy createMicrostructureImbalanceStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 订单流失衡模拟
        ROCIndicator priceChange = new ROCIndicator(closePrice, 1);
        ROCIndicator volumeChange = new ROCIndicator(volume, 1);
        
        // 失衡信号
        Rule entryRule = new OverIndicatorRule(priceChange, series.numOf(0.005))
                .and(new OverIndicatorRule(volumeChange, series.numOf(0.5)));
        Rule exitRule = new UnderIndicatorRule(priceChange, series.numOf(-0.002));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 107. 日内均值回归策略
     */
    public static Strategy createMeanReversionIntradayStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 日内均值回归
        SMAIndicator intraAvg = new SMAIndicator(closePrice, 60); // 60分钟均值
        StandardDeviationIndicator intraStd = new StandardDeviationIndicator(closePrice, 60);
        
        // 均值回归信号
        Rule entryRule = new UnderIndicatorRule(closePrice, intraAvg)
                .and(new OverIndicatorRule(intraStd, series.numOf(1.0)));
        Rule exitRule = new OverIndicatorRule(closePrice, intraAvg);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 108. 日内动量策略
     */
    public static Strategy createMomentumIntradayStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 日内动量
        ROCIndicator shortMomentum = new ROCIndicator(closePrice, 15); // 15分钟动量
        SMAIndicator avgMomentum = new SMAIndicator(shortMomentum, 30);
        
        // 动量信号
        Rule entryRule = new OverIndicatorRule(shortMomentum, series.numOf(0.005))
                .and(new OverIndicatorRule(shortMomentum, avgMomentum));
        Rule exitRule = new UnderIndicatorRule(shortMomentum, series.numOf(0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 109. 统计套利策略
     */
    public static Strategy createArbitrageStatisticalStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 统计套利：价格偏离统计规律
        SMAIndicator avgPrice = new SMAIndicator(closePrice, 60);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 60);
        
        // 创建自定义下轨指标
        class LowerBandIndicator extends CachedIndicator<Num> {
            private final SMAIndicator avgPrice;
            private final StandardDeviationIndicator stdDev;
            private final Num multiplier;
            
            public LowerBandIndicator(SMAIndicator avgPrice, StandardDeviationIndicator stdDev, double multiplier, BarSeries series) {
                super(series);
                this.avgPrice = avgPrice;
                this.stdDev = stdDev;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return avgPrice.getValue(index).minus(stdDev.getValue(index).multipliedBy(multiplier));
            }
        }
        
        LowerBandIndicator lowerBand = new LowerBandIndicator(avgPrice, stdDev, 2.0, series);
        LowerBandIndicator exitBand = new LowerBandIndicator(avgPrice, stdDev, 0.5, series);
        
        // 套利信号
        Rule entryRule = new UnderIndicatorRule(closePrice, lowerBand);
        Rule exitRule = new OverIndicatorRule(closePrice, exitBand);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 110. 配对交易策略
     */
    public static Strategy createPairsTradingStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 配对交易：价差回归
        SMAIndicator priceMean = new SMAIndicator(closePrice, 60);
        StandardDeviationIndicator priceStd = new StandardDeviationIndicator(closePrice, 60);
        
        // 创建自定义指标
        class PairsLowerBandIndicator extends CachedIndicator<Num> {
            private final SMAIndicator priceMean;
            private final StandardDeviationIndicator priceStd;
            private final Num multiplier;
            
            public PairsLowerBandIndicator(SMAIndicator priceMean, StandardDeviationIndicator priceStd, double multiplier, BarSeries series) {
                super(series);
                this.priceMean = priceMean;
                this.priceStd = priceStd;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return priceMean.getValue(index).minus(priceStd.getValue(index).multipliedBy(multiplier));
            }
        }
        
        PairsLowerBandIndicator entryBand = new PairsLowerBandIndicator(priceMean, priceStd, 2.0, series);
        PairsLowerBandIndicator exitBand = new PairsLowerBandIndicator(priceMean, priceStd, 0.5, series);
        
        // 价差信号
        Rule entryRule = new UnderIndicatorRule(closePrice, entryBand);
        Rule exitRule = new OverIndicatorRule(closePrice, exitBand);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    // ==================== 期权和波动率策略 (111-115) ====================

    /**
     * 111. 波动率曲面策略
     */
    public static Strategy createVolatilitySurfaceStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 波动率曲面分析
        StandardDeviationIndicator shortVol = new StandardDeviationIndicator(closePrice, 10);
        StandardDeviationIndicator longVol = new StandardDeviationIndicator(closePrice, 30);
        
        // 创建自定义波动率比较指标
        class VolatilityThresholdIndicator extends CachedIndicator<Num> {
            private final StandardDeviationIndicator longVol;
            private final Num multiplier;
            
            public VolatilityThresholdIndicator(StandardDeviationIndicator longVol, double multiplier, BarSeries series) {
                super(series);
                this.longVol = longVol;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return longVol.getValue(index).multipliedBy(multiplier);
            }
        }
        
        VolatilityThresholdIndicator volThreshold = new VolatilityThresholdIndicator(longVol, 1.2, series);
        
        // 波动率结构信号
        Rule entryRule = new OverIndicatorRule(shortVol, volThreshold);
        Rule exitRule = new UnderIndicatorRule(shortVol, longVol);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 112. Gamma剥头皮策略
     */
    public static Strategy createGammaScalpingStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // Gamma交易模拟 - 使用更合适的参数
        ROCIndicator priceChange = new ROCIndicator(closePrice, 1);
        StandardDeviationIndicator gamma = new StandardDeviationIndicator(priceChange, 20); // 降低周期
        SMAIndicator avgGamma = new SMAIndicator(gamma, 10); // 添加均值参考
        
        // Gamma信号 - 降低入场门槛，增加相对比较
        Rule entryRule = new OverIndicatorRule(gamma, avgGamma)
                .or(new OverIndicatorRule(priceChange, series.numOf(0.005))); // 添加价格变化率条件
        Rule exitRule = new UnderIndicatorRule(gamma, avgGamma)
                .or(new UnderIndicatorRule(priceChange, series.numOf(-0.005)));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 113. 波动率均值回归策略
     */
    public static Strategy createVolatilityMeanReversionStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 波动率均值回归
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        SMAIndicator avgVolatility = new SMAIndicator(volatility, 60);
        
        // 创建自定义波动率阈值指标
        class VolatilityMultiplierIndicator extends CachedIndicator<Num> {
            private final SMAIndicator avgVolatility;
            private final Num multiplier;
            
            public VolatilityMultiplierIndicator(SMAIndicator avgVolatility, double multiplier, BarSeries series) {
                super(series);
                this.avgVolatility = avgVolatility;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return avgVolatility.getValue(index).multipliedBy(multiplier);
            }
        }
        
        VolatilityMultiplierIndicator volThreshold = new VolatilityMultiplierIndicator(avgVolatility, 2.0, series);
        
        // 波动率回归信号
        Rule entryRule = new OverIndicatorRule(volatility, volThreshold);
        Rule exitRule = new UnderIndicatorRule(volatility, avgVolatility);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 114. 波动率动量策略
     */
    public static Strategy createVolatilityMomentumStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 波动率动量
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 10);
        ROCIndicator volMomentum = new ROCIndicator(volatility, 5);
        
        // 波动率动量信号
        Rule entryRule = new OverIndicatorRule(volMomentum, series.numOf(0.01));
        Rule exitRule = new UnderIndicatorRule(volMomentum, series.numOf(-0.005));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 115. 隐含波动率排名策略
     */
    public static Strategy createImpliedVolatilityRankStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 隐含波动率排名模拟
        StandardDeviationIndicator currentVol = new StandardDeviationIndicator(closePrice, 20);
        StandardDeviationIndicator historicalVol = new StandardDeviationIndicator(closePrice, 252);
        
        // 创建自定义波动率比较指标
        class HistoricalVolatilityIndicator extends CachedIndicator<Num> {
            private final StandardDeviationIndicator historicalVol;
            private final Num multiplier;
            
            public HistoricalVolatilityIndicator(StandardDeviationIndicator historicalVol, double multiplier, BarSeries series) {
                super(series);
                this.historicalVol = historicalVol;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return historicalVol.getValue(index).multipliedBy(multiplier);
            }
        }
        
        HistoricalVolatilityIndicator lowThreshold = new HistoricalVolatilityIndicator(historicalVol, 0.8, series);
        HistoricalVolatilityIndicator highThreshold = new HistoricalVolatilityIndicator(historicalVol, 1.2, series);
        
        // 相对波动率信号
        Rule entryRule = new UnderIndicatorRule(currentVol, lowThreshold);
        Rule exitRule = new OverIndicatorRule(currentVol, highThreshold);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    // ==================== 宏观和基本面策略 (116-120) ====================

    /**
     * 116. 利差交易策略
     */
    public static Strategy createCarryTradeStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 利差模拟 - 使用更短的周期
        SMAIndicator shortTerm = new SMAIndicator(closePrice, 5); // 短期均线改为5天
        SMAIndicator longTerm = new SMAIndicator(closePrice, 30);  // 长期均线改为30天
        
        // 创建利差指标
        class CarryIndicator extends CachedIndicator<Num> {
            private final SMAIndicator shortTerm;
            private final SMAIndicator longTerm;
            
            public CarryIndicator(SMAIndicator shortTerm, SMAIndicator longTerm, BarSeries series) {
                super(series);
                this.shortTerm = shortTerm;
                this.longTerm = longTerm;
            }
            
            @Override
            protected Num calculate(int index) {
                return shortTerm.getValue(index).minus(longTerm.getValue(index));
            }
        }
        
        CarryIndicator carry = new CarryIndicator(shortTerm, longTerm, series);
        
        // 利差信号 - 降低条件严格性
        Rule entryRule = new OverIndicatorRule(carry, series.numOf(0)) // 只要短期均线高于长期均线即可
                .or(new OverIndicatorRule(closePrice, shortTerm)); // 增加一个入场条件
        Rule exitRule = new UnderIndicatorRule(closePrice, shortTerm); // 价格低于短期均线时退出
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 117. 基本面评分策略
     */
    public static Strategy createFundamentalScoreStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 基本面评分模拟
        ROCIndicator growth = new ROCIndicator(closePrice, 252);
        StandardDeviationIndicator stability = new StandardDeviationIndicator(closePrice, 252);
        SMAIndicator avgVolume = new SMAIndicator(volume, 252);
        
        // 综合评分信号
        Rule entryRule = new OverIndicatorRule(growth, series.numOf(0.1))
                .and(new UnderIndicatorRule(stability, series.numOf(0.3)))
                .and(new OverIndicatorRule(volume, avgVolume));
        Rule exitRule = new UnderIndicatorRule(growth, series.numOf(0.05));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 118. 宏观动量策略
     */
    public static Strategy createMacroMomentumStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 宏观动量模拟
        ROCIndicator longTermMomentum = new ROCIndicator(closePrice, 252);
        ROCIndicator mediumTermMomentum = new ROCIndicator(closePrice, 60);
        
        // 宏观信号
        Rule entryRule = new OverIndicatorRule(longTermMomentum, series.numOf(0.15))
                .and(new OverIndicatorRule(mediumTermMomentum, series.numOf(0.05)));
        Rule exitRule = new UnderIndicatorRule(longTermMomentum, series.numOf(0.05));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 119. 季节性策略
     */
    public static Strategy createSeasonalityStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 季节性效应模拟
        SMAIndicator monthlyAvg = new SMAIndicator(closePrice, 21); // 月度均值
        SMAIndicator quarterlyAvg = new SMAIndicator(closePrice, 63); // 季度均值
        
        // 季节性信号
        Rule entryRule = new OverIndicatorRule(monthlyAvg, quarterlyAvg)
                .and(new OverIndicatorRule(closePrice, monthlyAvg));
        Rule exitRule = new UnderIndicatorRule(monthlyAvg, quarterlyAvg);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 120. 日历价差策略
     */
    public static Strategy createCalendarSpreadStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 日历价差模拟
        SMAIndicator nearTerm = new SMAIndicator(closePrice, 30);
        SMAIndicator farTerm = new SMAIndicator(closePrice, 90);
        
        // 创建自定义价差指标
        class CalendarSpreadIndicator extends CachedIndicator<Num> {
            private final SMAIndicator farTerm;
            private final Num multiplier;
            
            public CalendarSpreadIndicator(SMAIndicator farTerm, double multiplier, BarSeries series) {
                super(series);
                this.farTerm = farTerm;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return farTerm.getValue(index).multipliedBy(multiplier);
            }
        }
        
        CalendarSpreadIndicator lowerThreshold = new CalendarSpreadIndicator(farTerm, 0.95, series);
        CalendarSpreadIndicator upperThreshold = new CalendarSpreadIndicator(farTerm, 1.05, series);
        
        // 日历价差信号
        Rule entryRule = new UnderIndicatorRule(nearTerm, lowerThreshold);
        Rule exitRule = new OverIndicatorRule(nearTerm, upperThreshold);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    // ==================== 创新和实验性策略 (121-125) ====================

    /**
     * 121. 情绪分析策略
     */
    public static Strategy createSentimentAnalysisStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 情绪指标模拟
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        ROCIndicator priceChange = new ROCIndicator(closePrice, 1);
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);
        
        // 市场情绪信号 - 降低入场条件严格性
        Rule bullishSentiment = new OverIndicatorRule(priceChange, series.numOf(0.005))  // 从0.01降到0.005
                .or(new OverIndicatorRule(volume, series.numOf(1.2).multipliedBy(avgVolume.getValue(series.getEndIndex())))); // 正确的方式
                
        Rule bearishSentiment = new UnderIndicatorRule(priceChange, series.numOf(-0.01))
                .and(new OverIndicatorRule(volatility, series.numOf(1.5)));  // 降低波动率要求
        
        Rule entryRule = bullishSentiment;
        Rule exitRule = bearishSentiment;
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 122. 网络分析策略
     */
    public static Strategy createNetworkAnalysisStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 网络效应模拟
        ROCIndicator centrality = new ROCIndicator(closePrice, 20);
        StandardDeviationIndicator connectivity = new StandardDeviationIndicator(closePrice, 20);
        
        // 网络信号
        Rule entryRule = new OverIndicatorRule(centrality, series.numOf(0.02))
                .and(new OverIndicatorRule(connectivity, series.numOf(1.0)));
        Rule exitRule = new UnderIndicatorRule(centrality, series.numOf(0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 123. 分形几何策略
     */
    public static Strategy createFractalGeometryStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 分形结构模拟
        StandardDeviationIndicator fractalDim = new StandardDeviationIndicator(closePrice, 20);
        ROCIndicator hurst = new ROCIndicator(closePrice, 50);
        
        // 分形信号
        Rule entryRule = new OverIndicatorRule(fractalDim, series.numOf(1.5))
                .and(new OverIndicatorRule(hurst, series.numOf(0.5)));
        Rule exitRule = new UnderIndicatorRule(fractalDim, series.numOf(1.0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 124. 混沌理论策略
     */
    public static Strategy createChaosTheoryStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 混沌系统模拟 - 使用更短周期
        StandardDeviationIndicator lyapunov = new StandardDeviationIndicator(closePrice, 10); // 从20降到10
        ROCIndicator attractor = new ROCIndicator(closePrice, 5); // 从10降到5
        SMAIndicator avgLyapunov = new SMAIndicator(lyapunov, 5); // 添加均值参考
        
        // 混沌信号 - 降低入场门槛
        Rule entryRule = new OverIndicatorRule(lyapunov, avgLyapunov) // 相对比较而非绝对值
                .or(new OverIndicatorRule(attractor, series.numOf(0.01))); // 从0.02降到0.01
        Rule exitRule = new UnderIndicatorRule(attractor, series.numOf(0))
                .and(new UnderIndicatorRule(lyapunov, avgLyapunov));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 125. 量子启发策略
     */
    public static Strategy createQuantumInspiredStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 量子效应模拟
        ROCIndicator superposition = new ROCIndicator(closePrice, 10);
        StandardDeviationIndicator entanglement = new StandardDeviationIndicator(closePrice, 10);
        MACDIndicator interference = new MACDIndicator(closePrice, 12, 26);
        
        // 量子信号
        Rule entryRule = new OverIndicatorRule(superposition, series.numOf(0.01))
                .and(new OverIndicatorRule(entanglement, series.numOf(0.5)))
                .and(new OverIndicatorRule(interference, series.numOf(0)));
        Rule exitRule = new UnderIndicatorRule(superposition, series.numOf(0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    // ==================== 风险管理策略 (126-130) ====================

    /**
     * 126. 凯利公式策略
     */
    public static Strategy createKellyCriterionStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 计算收益和风险
        ROCIndicator returns = new ROCIndicator(closePrice, 1);
        StandardDeviationIndicator risk = new StandardDeviationIndicator(closePrice, 14);  // 减少回看周期
        SMAIndicator avgReturns = new SMAIndicator(returns, 14);
        SMAIndicator avgRisk = new SMAIndicator(risk, 14);
        
        // 凯利比率模拟 - 降低入场条件严格性
        Rule entryRule = new OverIndicatorRule(returns, series.numOf(0.5).multipliedBy(avgReturns.getValue(series.getEndIndex())))  // 使用相对比较
                .and(new UnderIndicatorRule(risk, series.numOf(1.2).multipliedBy(avgRisk.getValue(series.getEndIndex()))));
        
        Rule exitRule = new UnderIndicatorRule(returns, series.numOf(0))
                .or(new OverIndicatorRule(risk, series.numOf(1.5).multipliedBy(avgRisk.getValue(series.getEndIndex()))));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 127. VaR风险管理策略
     */
    public static Strategy createVarRiskManagementStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // VaR计算
        ROCIndicator returns = new ROCIndicator(closePrice, 1);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(returns, 60);
        
        // 风险控制信号
        Rule entryRule = new UnderIndicatorRule(volatility, series.numOf(0.02));
        Rule exitRule = new OverIndicatorRule(volatility, series.numOf(0.05));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 128. 最大回撤控制策略
     */
    public static Strategy createMaximumDrawdownControlStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 回撤控制
        HighestValueIndicator highestPrice = new HighestValueIndicator(closePrice, 60);
        
        // 创建自定义回撤阈值指标
        class DrawdownThresholdIndicator extends CachedIndicator<Num> {
            private final HighestValueIndicator highestPrice;
            private final Num multiplier;
            
            public DrawdownThresholdIndicator(HighestValueIndicator highestPrice, double multiplier, BarSeries series) {
                super(series);
                this.highestPrice = highestPrice;
                this.multiplier = series.numOf(multiplier);
            }
            
            @Override
            protected Num calculate(int index) {
                return highestPrice.getValue(index).multipliedBy(multiplier);
            }
        }
        
        DrawdownThresholdIndicator entryThreshold = new DrawdownThresholdIndicator(highestPrice, 0.95, series);
        DrawdownThresholdIndicator exitThreshold = new DrawdownThresholdIndicator(highestPrice, 0.9, series);
        
        // 回撤计算
        Rule entryRule = new OverIndicatorRule(closePrice, entryThreshold);
        Rule exitRule = new UnderIndicatorRule(closePrice, exitThreshold);
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 129. 头寸规模策略
     */
    public static Strategy createPositionSizingStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 头寸规模管理
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 20);
        ROCIndicator returns = new ROCIndicator(closePrice, 1);
        
        // 风险调整头寸
        Rule entryRule = new UnderIndicatorRule(volatility, series.numOf(2.0))
                .and(new OverIndicatorRule(returns, series.numOf(0.01)));
        Rule exitRule = new OverIndicatorRule(volatility, series.numOf(3.0));
        
        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 130. 相关性过滤策略
     */
    public static Strategy createCorrelationFilterStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        // 相关性分析
        ROCIndicator priceChange = new ROCIndicator(closePrice, 1);
        ROCIndicator volumeChange = new ROCIndicator(volume, 1);
        
        // 低相关性信号
        Rule entryRule = new OverIndicatorRule(priceChange, series.numOf(0.01))
                .and(new UnderIndicatorRule(volumeChange, series.numOf(0.5)));
        Rule exitRule = new OverIndicatorRule(volumeChange, series.numOf(1.0));
        
        return new BaseStrategy(entryRule, exitRule);
    }
} 