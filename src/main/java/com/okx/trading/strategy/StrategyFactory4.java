package com.okx.trading.strategy;

import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 策略工厂4 - 高级策略集合
 * 包含40个高级策略（91-130），涵盖机器学习、量化因子、高频、期权、宏观、创新和风险管理策略
 */
public class StrategyFactory4 {

    // ==================== 机器学习启发策略 (91-100) ====================

    /**
     * 创建神经网络策略（修复版）- 简化为多指标组合决策
     */
    public static Strategy createNeuralNetworkStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 输入层：多个技术指标（模拟神经网络输入）
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 10);
        SMAIndicator avgVolume = new SMAIndicator(volume, 10);

        // 隐藏层：权重组合（模拟神经网络权重）
        // 节点1：趋势信号 (权重35%)
        Rule trendSignal = new OverIndicatorRule(sma10, sma20)
                .and(new OverIndicatorRule(closePrice, sma20));

        // 节点2：动量信号 (权重30%)
        Rule momentumSignal = new OverIndicatorRule(rsi, series.numOf(45))
                .and(new OverIndicatorRule(macd, series.numOf(0)));

        // 节点3：成交量信号 (权重20%)
        Rule volumeSignal = new OverIndicatorRule(volume, TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.1)));

        // 节点4：波动率信号 (权重15%)
        Rule volatilitySignal = new UnderIndicatorRule(volatility, series.numOf(1.5));

        // 输出层：激活函数（模拟神经网络输出）
        // 买入：至少3个信号激活
        Rule buyRule = trendSignal.and(momentumSignal).and(volumeSignal)
                .or(trendSignal.and(momentumSignal).and(volatilitySignal))
                .or(trendSignal.and(volumeSignal).and(volatilitySignal))
                .or(momentumSignal.and(volumeSignal).and(volatilitySignal));

        // 卖出：趋势反转或RSI超买
        Rule sellRule = new UnderIndicatorRule(sma10, sma20)
                .or(new OverIndicatorRule(rsi, series.numOf(75)))
                .or(new UnderIndicatorRule(macd, series.numOf(0)));

        return new BaseStrategy("神经网络策略", buyRule, sellRule);
    }

    /**
     * 创建遗传算法策略（修复版）- 简化为适应度评估策略
     */
    public static Strategy createGeneticAlgorithmStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 基因1：趋势适应度
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        Rule gene1 = new OverIndicatorRule(sma5, sma20); // 短期趋势向上

        // 基因2：动量适应度
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        Rule gene2 = new OverIndicatorRule(rsi, series.numOf(45))
                .and(new UnderIndicatorRule(rsi, series.numOf(75))); // RSI在合理区间

        // 基因3：成交量适应度
        SMAIndicator avgVolume = new SMAIndicator(volume, 10);
        // 使用TransformIndicator正确创建乘法指标
        Indicator<Num> volumeThreshold = TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.2));
        Rule gene3 = new OverIndicatorRule(volume, volumeThreshold); // 成交量放大

        // 基因4：价格位置适应度
        HighestValueIndicator highest20 = new HighestValueIndicator(closePrice, 20);
        LowestValueIndicator lowest20 = new LowestValueIndicator(closePrice, 20);
        // 使用TransformIndicator正确创建减法和乘法指标
        Indicator<Num> range20 = new CachedIndicator<Num>(series) {
            @Override
            protected Num calculate(int index) {
                return highest20.getValue(index).minus(lowest20.getValue(index));
            }
        };
        Indicator<Num> rangeMultiplied20 = TransformIndicator.multiply(range20, BigDecimal.valueOf(0.3));

        // 创建一个CachedIndicator来正确处理两个Indicator的相加
        Indicator<Num> threshold20 = new CachedIndicator<Num>(series) {
            @Override
            protected Num calculate(int index) {
                return lowest20.getValue(index).plus(rangeMultiplied20.getValue(index));
            }
        };
        Rule gene4 = new OverIndicatorRule(closePrice, threshold20); // 价格在20%以上位置

        // 适应度评估：至少3个基因激活（高适应度个体）
        Rule highFitness = gene1.and(gene2).and(gene3)
                .or(gene1.and(gene2).and(gene4))
                .or(gene1.and(gene3).and(gene4))
                .or(gene2.and(gene3).and(gene4));

        // 淘汰条件：适应度低（趋势反转或超买）
        Rule lowFitness = new UnderIndicatorRule(sma5, sma20)
                .or(new OverIndicatorRule(rsi, series.numOf(80)));

        return new BaseStrategy("遗传算法策略", highFitness, lowFitness);
    }

    /**
     * 创建随机森林策略（修复版）- 简化为多决策树投票
     */
    public static Strategy createRandomForestStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 决策树1：趋势树
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        Rule tree1 = new OverIndicatorRule(sma10, sma20)
                .and(new OverIndicatorRule(closePrice, sma20));

        // 决策树2：动量树
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        Rule tree2 = new OverIndicatorRule(rsi, series.numOf(50))
                .and(new OverIndicatorRule(macd, series.numOf(0)));

        // 决策树3：成交量树
        SMAIndicator avgVolume = new SMAIndicator(volume, 15);
        Indicator<Num> avgVolume13 = TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.3));
        Rule tree3 = new OverIndicatorRule(volume, avgVolume13);

        // 决策树4：波动率树
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 10);
        ATRIndicator atr = new ATRIndicator(series, 14);
        Rule tree4 = new UnderIndicatorRule(volatility, series.numOf(2.0))
                .and(new OverIndicatorRule(atr, series.numOf(0.01)));

        // 决策树5：价格位置树
        HighestValueIndicator highest15 = new HighestValueIndicator(closePrice, 15);
        LowestValueIndicator lowest15 = new LowestValueIndicator(closePrice, 15);
        Indicator<Num> range15_2 = new CachedIndicator<Num>(series) {
            @Override
            protected Num calculate(int index) {
                return highest15.getValue(index).minus(lowest15.getValue(index));
            }
        };
        Indicator<Num> rangeMultiplied15 = TransformIndicator.multiply(range15_2, BigDecimal.valueOf(0.4));

        // 创建一个CachedIndicator来正确处理两个Indicator的相加
        Indicator<Num> threshold15 = new CachedIndicator<Num>(series) {
            @Override
            protected Num calculate(int index) {
                return lowest15.getValue(index).plus(rangeMultiplied15.getValue(index));
            }
        };
        Rule tree5 = new OverIndicatorRule(closePrice, threshold15);

        // 随机森林投票：至少3棵树支持（多数投票）
        Rule forestBuy = tree1.and(tree2).and(tree3)
                .or(tree1.and(tree2).and(tree4))
                .or(tree1.and(tree2).and(tree5))
                .or(tree1.and(tree3).and(tree4))
                .or(tree1.and(tree3).and(tree5))
                .or(tree1.and(tree4).and(tree5))
                .or(tree2.and(tree3).and(tree4))
                .or(tree2.and(tree3).and(tree5))
                .or(tree2.and(tree4).and(tree5))
                .or(tree3.and(tree4).and(tree5));

        // 卖出：多数树反对
        Rule forestSell = new UnderIndicatorRule(sma10, sma20)
                .or(new OverIndicatorRule(rsi, series.numOf(75)))
                .or(new UnderIndicatorRule(volume, TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(0.8))));

        return new BaseStrategy("随机森林策略", forestBuy, forestSell);
    }

    /**
     * 创建SVM策略（修复版）- 简化为支持向量分类
     */
    public static Strategy createSVMStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 特征向量：多维技术指标
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 14);

        // 支持向量：定义分类边界（更宽松条件）
        Rule boundary1 = new OverIndicatorRule(rsi, series.numOf(35))
                .and(new UnderIndicatorRule(rsi, series.numOf(75))); // 放宽RSI范围
        Rule boundary2 = new OverIndicatorRule(macd, series.numOf(0));
        Rule boundary3 = new OverIndicatorRule(closePrice, sma);
        Rule boundary4 = new OverIndicatorRule(volume, TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.1)));
        Rule boundary5 = new UnderIndicatorRule(volatility, series.numOf(2.0));

        // SVM分类：至少3个支持向量支持（多数决策）
        Rule svmBuy = boundary1.and(boundary2).and(boundary3)
                .or(boundary1.and(boundary2).and(boundary4))
                .or(boundary1.and(boundary2).and(boundary5))
                .or(boundary1.and(boundary3).and(boundary4))
                .or(boundary1.and(boundary3).and(boundary5))
                .or(boundary1.and(boundary4).and(boundary5))
                .or(boundary2.and(boundary3).and(boundary4))
                .or(boundary2.and(boundary3).and(boundary5))
                .or(boundary2.and(boundary4).and(boundary5))
                .or(boundary3.and(boundary4).and(boundary5));

        // SVM卖出：支持向量反转
        Rule svmSell = new UnderIndicatorRule(closePrice, sma)
                .or(new OverIndicatorRule(rsi, series.numOf(80)))
                .or(new UnderIndicatorRule(macd, series.numOf(0)));

        return new BaseStrategy("SVM策略", svmBuy, svmSell);
    }

    /**
     * 创建LSTM策略（修复版）- 简化为时间序列记忆策略
     */
    public static Strategy createLSTMStrategy(BarSeries series) {
        if (series.getBarCount() <= 50) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 短期记忆（类似LSTM的短期状态）
        SMAIndicator shortMemory = new SMAIndicator(closePrice, 5);
        RSIIndicator shortRSI = new RSIIndicator(closePrice, 7);

        // 长期记忆（类似LSTM的长期状态）
        SMAIndicator longMemory = new SMAIndicator(closePrice, 30);
        SMAIndicator longVolumeMemory = new SMAIndicator(volume, 30);

        // 遗忘门：决定是否忘记旧信息
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 10);
        Rule forgetGate = new UnderIndicatorRule(volatility, series.numOf(2.0)); // 低波动时保持记忆

        // 输入门：决定是否接受新信息
        Rule inputGate = new OverIndicatorRule(volume, TransformIndicator.multiply(longVolumeMemory, BigDecimal.valueOf(1.2))); // 成交量放大

        // 输出门：决定输出什么信息
        Rule outputGate = new OverIndicatorRule(shortMemory, longMemory) // 短期>长期
                .and(new OverIndicatorRule(shortRSI, series.numOf(45)))
                .and(new UnderIndicatorRule(shortRSI, series.numOf(75)));

        // LSTM输出：综合所有门的决策
        Rule lstmBuy = forgetGate.and(inputGate).and(outputGate);

        // LSTM卖出：记忆衰减或趋势反转
        Rule lstmSell = new UnderIndicatorRule(shortMemory, longMemory)
                .or(new OverIndicatorRule(shortRSI, series.numOf(80)))
                .or(new OverIndicatorRule(volatility, series.numOf(3.0)));

        return new BaseStrategy("LSTM策略", lstmBuy, lstmSell);
    }

    /**
     * 创建KNN策略（修复版）- 基于历史相似模式预测
     */
    public static Strategy createKNNStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 特征提取：多维特征向量
        ROCIndicator priceChange = new ROCIndicator(closePrice, 1);
        RSIIndicator momentum = new RSIIndicator(closePrice, 14);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 5);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        SMAIndicator avgVolume = new SMAIndicator(volume, 10);

        // K=3最近邻：寻找3个相似模式
        // 邻居1：价格上涨 + 动量良好
        Rule neighbor1 = new OverIndicatorRule(priceChange, series.numOf(0.005)) // 降低阈值
                .and(new OverIndicatorRule(momentum, series.numOf(45)))
                .and(new OverIndicatorRule(closePrice, sma));

        // 邻居2：成交量确认 + 低波动
        Rule neighbor2 = new OverIndicatorRule(volume, TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.1)))
                .and(new UnderIndicatorRule(volatility, series.numOf(1.8)))
                .and(new OverIndicatorRule(momentum, series.numOf(40)));

        // 邻居3：趋势延续模式
        Rule neighbor3 = new OverIndicatorRule(momentum, series.numOf(35))
                .and(new UnderIndicatorRule(momentum, series.numOf(75)))
                .and(new OverIndicatorRule(priceChange, series.numOf(0)));

        // KNN投票：至少2个邻居支持
        Rule knnBuy = neighbor1.and(neighbor2)
                .or(neighbor1.and(neighbor3))
                .or(neighbor2.and(neighbor3));

        // KNN卖出：邻居模式反转
        Rule knnSell = new UnderIndicatorRule(momentum, series.numOf(30))
                .or(new OverIndicatorRule(momentum, series.numOf(80)))
                .or(new UnderIndicatorRule(closePrice, sma));

        return new BaseStrategy("KNN策略", knnBuy, knnSell);
    }

    /**
     * 创建朴素贝叶斯策略（修复版）- 基于贝叶斯概率预测
     */
    public static Strategy createNaiveBayesStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 特征独立性假设：各指标独立计算概率
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        ROCIndicator roc = new ROCIndicator(closePrice, 10);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        SMAIndicator avgVolume = new SMAIndicator(volume, 15);

        // 先验概率P(买入)：基于历史统计（放宽条件）
        Rule prior1 = new OverIndicatorRule(rsi, series.numOf(35))
                .and(new UnderIndicatorRule(rsi, series.numOf(75))); // 扩大RSI范围
        Rule prior2 = new OverIndicatorRule(macd, series.numOf(0));
        Rule prior3 = new OverIndicatorRule(roc, series.numOf(-0.005)); // 允许小幅下跌
        Rule prior4 = new OverIndicatorRule(closePrice, sma);
        Rule prior5 = new OverIndicatorRule(volume, TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.05))); // 降低成交量要求

        // 后验概率P(买入|特征)：贝叶斯更新（至少4个条件满足）
        Rule bayesBuy = prior1.and(prior2).and(prior3).and(prior4)
                .or(prior1.and(prior2).and(prior3).and(prior5))
                .or(prior1.and(prior2).and(prior4).and(prior5))
                .or(prior1.and(prior3).and(prior4).and(prior5))
                .or(prior2.and(prior3).and(prior4).and(prior5));

        // 后验概率P(卖出|特征)：反向贝叶斯更新
        Rule bayesSell = new UnderIndicatorRule(rsi, series.numOf(25))
                .or(new OverIndicatorRule(rsi, series.numOf(80)))
                .or(new UnderIndicatorRule(closePrice, sma))
                .or(new UnderIndicatorRule(macd, series.numOf(0)));

        return new BaseStrategy("朴素贝叶斯策略", bayesBuy, bayesSell);
    }

    /**
     * 创建决策树策略（修复版）- 基于规则化决策树
     */
    public static Strategy createDecisionTreeStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 决策树节点特征
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        SMAIndicator avgVolume = new SMAIndicator(volume, 15);
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 10);

        // 根节点：RSI阈值判断（降低门槛）
        Rule rootNode = new OverIndicatorRule(rsi, series.numOf(45)); // 降低阈值（原来50）

        // 左分支：RSI > 45时的决策路径
        Rule leftBranch = rootNode
                .and(new OverIndicatorRule(closePrice, sma))
                .and(new OverIndicatorRule(macd, series.numOf(0)))
                .and(new OverIndicatorRule(volume, TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.1))));

        // 右分支：RSI <= 45时的决策路径（逆向策略）
        Rule rightBranch = new NotRule(rootNode)
                .and(new UnderIndicatorRule(rsi, series.numOf(35))) // 超卖
                .and(new UnderIndicatorRule(volatility, series.numOf(2.0))) // 低波动
                .and(new OverIndicatorRule(volume, avgVolume)); // 成交量确认

        // 决策树买入：任一分支满足
        Rule treeBuy = leftBranch.or(rightBranch);

        // 决策树卖出：条件反转
        Rule treeSell = new OverIndicatorRule(rsi, series.numOf(75))
                .or(new UnderIndicatorRule(rsi, series.numOf(25)))
                .or(new UnderIndicatorRule(closePrice, sma))
                .or(new OverIndicatorRule(volatility, series.numOf(3.0)));

        return new BaseStrategy("决策树策略", treeBuy, treeSell);
    }

    /**
     * 创建集成学习策略（修复版）- 基于多模型融合
     */
    public static Strategy createEnsembleStrategy(BarSeries series) {
        if (series.getBarCount() <= 30) {
            throw new IllegalArgumentException("数据点不足以计算指标");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        // 模型1：趋势模型（权重30%）
        SMAIndicator sma = new SMAIndicator(closePrice, 20);
        SMAIndicator fastSma = new SMAIndicator(closePrice, 10);
        Rule model1 = new OverIndicatorRule(fastSma, sma)
                .and(new OverIndicatorRule(closePrice, sma));

        // 模型2：动量模型（权重25%）
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        Rule model2 = new OverIndicatorRule(rsi, series.numOf(45))
                .and(new OverIndicatorRule(macd, series.numOf(0)));

        // 模型3：成交量模型（权重20%）
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);
        // 使用TransformIndicator正确创建乘法指标
        Indicator<Num> volumeThreshold = TransformIndicator.multiply(avgVolume, BigDecimal.valueOf(1.2));
        Rule model3 = new OverIndicatorRule(volume, volumeThreshold);

        // 模型4：波动率模型（权重15%）
        StandardDeviationIndicator volatility = new StandardDeviationIndicator(closePrice, 15);
        Rule model4 = new UnderIndicatorRule(volatility, series.numOf(2.0));

        // 模型5：价格位置模型（权重10%）
        HighestValueIndicator highest = new HighestValueIndicator(closePrice, 20);
        LowestValueIndicator lowest = new LowestValueIndicator(closePrice, 20);
        // 使用TransformIndicator正确创建减法和乘法指标
        Indicator<Num> range = new CachedIndicator<Num>(series) {
            @Override
            protected Num calculate(int index) {
                return highest.getValue(index).minus(lowest.getValue(index));
            }
        };
        Indicator<Num> rangeMultiplied = TransformIndicator.multiply(range, BigDecimal.valueOf(0.3));

        // 创建一个CachedIndicator来正确处理两个Indicator的相加
        Indicator<Num> threshold = new CachedIndicator<Num>(series) {
            @Override
            protected Num calculate(int index) {
                return lowest.getValue(index).plus(rangeMultiplied.getValue(index));
            }
        };
        Rule model5 = new OverIndicatorRule(closePrice, threshold);

        // 集成投票：加权投票机制（至少3个强模型支持）
        Rule ensembleBuy = model1.and(model2).and(model3) // 趋势+动量+成交量
                .or(model1.and(model2).and(model4)) // 趋势+动量+波动率
                .or(model1.and(model2).and(model5)) // 趋势+动量+位置
                .or(model1.and(model3).and(model4)) // 趋势+成交量+波动率
                .or(model2.and(model3).and(model4)) // 动量+成交量+波动率
                .or(model1.and(model3).and(model5)) // 趋势+成交量+位置
                .or(model2.and(model3).and(model5)); // 动量+成交量+位置

        // 集成卖出：主要模型反对
        Rule ensembleSell = new UnderIndicatorRule(fastSma, sma)
                .or(new OverIndicatorRule(rsi, series.numOf(80)))
                .or(new UnderIndicatorRule(macd, series.numOf(0)))
                .or(new OverIndicatorRule(volatility, series.numOf(3.0)));

        return new BaseStrategy("集成学习策略", ensembleBuy, ensembleSell);
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
                .and(new OverIndicatorRule(closePrice, sma)); // 上涨趋势
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

        // 市场情绪信号 - 修正逻辑错误
        // 创建成交量阈值指标
        TransformIndicator volumeThreshold = TransformIndicator.multiply(avgVolume, 1.2);

        Rule bullishSentiment = new OverIndicatorRule(priceChange, series.numOf(0.005))  // 价格上涨
                .or(new OverIndicatorRule(volume, volumeThreshold)); // 成交量放大

        Rule bearishSentiment = new UnderIndicatorRule(priceChange, series.numOf(-0.005))  // 价格下跌
                .or(new OverIndicatorRule(volatility, series.numOf(1.5)));  // 波动率升高

        Rule entryRule = bullishSentiment;
        Rule exitRule = bearishSentiment;

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 122. 网络分析策略
     */
    public static Strategy createNetworkAnalysisStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 网络效应模拟 - 改进指标选择
        ROCIndicator centrality = new ROCIndicator(closePrice, 10); // 减少周期
        StandardDeviationIndicator connectivity = new StandardDeviationIndicator(closePrice, 10); // 减少周期
        SMAIndicator avgConnectivity = new SMAIndicator(connectivity, 5); // 添加参考

        // 网络信号 - 降低条件严格性
        Rule entryRule = new OverIndicatorRule(centrality, series.numOf(0.01)) // 降低阈值
                .or(new OverIndicatorRule(connectivity, avgConnectivity)); // 使用相对比较
        Rule exitRule = new UnderIndicatorRule(centrality, series.numOf(-0.01)) // 添加负值条件
                .and(new UnderIndicatorRule(connectivity, avgConnectivity));

        return new BaseStrategy(entryRule, exitRule);
    }

    /**
     * 123. 分形几何策略
     */
    public static Strategy createFractalGeometryStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 分形结构模拟 - 降低条件严格性
        StandardDeviationIndicator fractalDim = new StandardDeviationIndicator(closePrice, 10); // 减少周期
        ROCIndicator hurst = new ROCIndicator(closePrice, 20); // 减少周期
        SMAIndicator avgFractal = new SMAIndicator(fractalDim, 5); // 添加平均值参考

        // 分形信号 - 使用相对比较
        Rule entryRule = new OverIndicatorRule(fractalDim, avgFractal) // 相对比较
                .and(new OverIndicatorRule(hurst, series.numOf(0.01))); // 降低阈值
        Rule exitRule = new UnderIndicatorRule(fractalDim, avgFractal) // 相对比较
                .or(new UnderIndicatorRule(hurst, series.numOf(0)));

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
