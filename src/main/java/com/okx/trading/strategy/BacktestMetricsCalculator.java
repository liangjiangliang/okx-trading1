package com.okx.trading.strategy;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.IndicatorDistributionService;
import com.okx.trading.service.IndicatorWeightService;
import com.okx.trading.service.impl.Ta4jBacktestService;
import com.okx.trading.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.Bar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.okx.trading.util.BacktestDataGenerator.parseIntervalToMinutes;

/**
 * 回测指标计算器 - 专业级交易策略评估系统
 * <p>
 * 本类是一个综合性的交易策略回测指标计算器，实现了业界领先的量化评估体系。
 * 该系统不仅计算传统的回测指标，还新增了15个高级风险指标和科学的综合评分机制，
 * 为交易策略的全面评估提供了强大的工具支持。
 * <p>
 * ================================================================================
 * 核心功能特性
 * ================================================================================
 * <p>
 * 📊 传统回测指标:
 * • 收益类: 总收益率、年化收益率、盈利因子、胜率、平均盈利
 * • 风险类: 夏普比率、Sortino比率、Calmar比率、最大回撤、波动率
 * • 交易类: 交易次数、盈利交易、亏损交易、最大单笔损失
 * <p>
 * 🔬 新增高级风险指标 (15个):
 * • 分布特征: 峰度(Kurtosis)、偏度(Skewness)
 * • 极端风险: VaR95%、VaR99%、CVaR(条件风险价值)
 * • 相对基准: 信息比率、跟踪误差、上涨/下跌捕获率
 * • 回撤分析: Sterling比率、Burke比率、痛苦指数、最大回撤持续期
 * • 风险修正: 修正夏普比率、下行偏差、风险调整收益
 * <p>
 * 🎯 综合评分系统 (0-10分):
 * • 收益指标评分 (25%): 年化收益率、总收益率、盈利因子
 * • 核心风险评分 (25%): 夏普比率、最大回撤、Sortino比率、Calmar比率
 * • 高级风险评分 (25%): VaR/CVaR、信息比率、捕获率、峰度等
 * • 交易质量评分 (15%): 胜率、交易次数、平均盈利
 * • 稳定性评分 (10%): 偏度、峰度、痛苦指数
 * <p>
 * ================================================================================
 * 技术特点
 * ================================================================================
 * <p>
 * 🚀 性能优化:
 * • 构造器模式一次性计算所有指标，避免重复计算
 * • 流式计算和缓存机制，提高大数据量处理效率
 * • 智能异常处理，保证系统稳定性
 * <p>
 * 📈 算法先进:
 * • 基于现代投资组合理论和风险管理实践
 * • 针对加密货币市场特性进行参数调整
 * • 采用对数收益率计算保证数学严谨性
 * <p>
 * 🔧 设计灵活:
 * • 支持不同时间频率的数据(1分钟到1天)
 * • 自动检测年化因子，适应不同数据周期
 * • 模块化设计，便于扩展和维护
 * <p>
 * ================================================================================
 * 使用场景
 * ================================================================================
 * <p>
 * 💼 投资管理:
 * • 量化基金策略评估和选择
 * • 投资组合风险管理和资产配置
 * • 业绩归因分析和风险预算
 * <p>
 * 🏦 风险控制:
 * • 交易策略的风险评估和监控
 * • 压力测试和情景分析
 * • 监管资本要求计算
 * <p>
 * 📊 研究分析:
 * • 策略开发和优化
 * • 市场研究和学术分析
 * • 回测报告和业绩展示
 * <p>
 * ================================================================================
 * 设计思路
 * ================================================================================
 * <p>
 * 本系统采用了"一次计算，全面评估"的设计理念，在构造器中完成所有指标的计算，
 * 避免了重复计算的性能损耗。同时，通过科学的权重分配和评分机制，
 * 为用户提供了直观、准确的策略评估结果。
 * <p>
 * 特别地，综合评分系统是本计算器的创新亮点，它不仅考虑了传统的收益风险指标，
 * 还融入了最新的量化研究成果，形成了多维度、多层次的评估体系，
 * 能够更准确地识别优秀策略和潜在风险。
 * 
 * @author OKX Trading System
 * @version 2.0
 * @since 2024
 */
@Slf4j
public class BacktestMetricsCalculator {

    private static final Logger log = LoggerFactory.getLogger(BacktestMetricsCalculator.class);

    // 计算结果
    private BacktestResultDTO result;

    // 输入参数
    private final BarSeries series;
    private final TradingRecord tradingRecord;
    private final BigDecimal initialAmount;
    private final String strategyType;
    private final String paramDescription;
    private final BigDecimal feeRatio;
    private final String interval;
    private final List<CandlestickEntity> benchmarkCandlesticks;

    // 中间计算结果
    private List<TradeRecordDTO> tradeRecords;
    private List<ArrayList<BigDecimal>> maxLossAndDrawdownList;
    // 每天资金曲线
    private List<BigDecimal> strategyEquityCurve;
    // 策略收益率序列
    private List<BigDecimal> fullPeriodStrategyReturns;
    private ArrayList<BigDecimal> dailyPrices;
    private ReturnMetrics returnMetrics;
    private RiskMetrics riskMetrics;
    private TradeStatistics tradeStats;

    // 获取权重服务
    private IndicatorWeightService weightService;
    private IndicatorDistributionService distributionService;

    /**
     * 构造器 - 在构造时完成所有指标计算
     *
     * @param series           BarSeries对象
     * @param tradingRecord    交易记录
     * @param initialAmount    初始资金
     * @param strategyType     策略类型
     * @param paramDescription 参数描述
     * @param feeRatio         交易手续费率
     */
    public BacktestMetricsCalculator(BarSeries series, TradingRecord tradingRecord, BigDecimal initialAmount, String strategyType,
                                     String paramDescription, BigDecimal feeRatio, String interval, List<CandlestickEntity> benchmarkCandlesticks) {
        this.series = series;
        this.tradingRecord = tradingRecord;
        this.initialAmount = initialAmount;
        this.strategyType = strategyType;
        this.paramDescription = paramDescription;
        this.feeRatio = feeRatio;
        this.interval = interval;
        this.benchmarkCandlesticks = benchmarkCandlesticks;
        
        // 初始化服务实例
        this.weightService = getIndicatorWeightService();
        this.distributionService = getIndicatorDistributionService();

        // 在构造器中完成所有指标计算
        calculateAllMetrics();
    }

    /**
     * 计算所有回测指标
     */
    private void calculateAllMetrics() {
        try {
            // 如果没有交易，返回简单结果
            if (tradingRecord.getPositionCount() == 0) {
                result = createEmptyResult();
                return;
            }

            // 1. 提取交易明细（包含手续费计算）
            tradeRecords = extractTradeRecords();

            // 2. 计算交易统计指标
            tradeStats = calculateTradeStatistics();

            // 3. 计算收益率相关指标
            returnMetrics = calculateReturnMetrics(tradeStats);

            // 4. 计算风险指标
            riskMetrics = calculateRiskMetrics(tradeStats, returnMetrics);

            // 5. 构建最终结果
            result = buildFinalResult();

        } catch (Exception e) {
            log.error("计算回测指标时发生错误: {}", e.getMessage(), e);
            result = createErrorResult(e.getMessage());
        }
    }

    /**
     * 创建空结果（无交易情况）
     */
    private BacktestResultDTO createEmptyResult() {
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(initialAmount);
        result.setTotalProfit(BigDecimal.ZERO);
        result.setTotalReturn(BigDecimal.ZERO);
        result.setNumberOfTrades(0);
        result.setProfitableTrades(0);
        result.setUnprofitableTrades(0);
        result.setWinRate(BigDecimal.ZERO);
        result.setAverageProfit(BigDecimal.ZERO);
        result.setMaxDrawdown(BigDecimal.ZERO);
        result.setSharpeRatio(BigDecimal.ZERO);
        result.setStrategyName(strategyType);
        result.setParameterDescription(paramDescription);
        result.setTrades(new ArrayList<>());
        result.setTotalFee(BigDecimal.ZERO);
        
        // 初始化新增的风险指标为零值
        result.setKurtosis(BigDecimal.ZERO);
        result.setCvar(BigDecimal.ZERO);
        result.setVar95(BigDecimal.ZERO);
        result.setVar99(BigDecimal.ZERO);
        result.setInformationRatio(BigDecimal.ZERO);
        result.setTrackingError(BigDecimal.ZERO);
        result.setSterlingRatio(BigDecimal.ZERO);
        result.setBurkeRatio(BigDecimal.ZERO);
        result.setModifiedSharpeRatio(BigDecimal.ZERO);
        result.setDownsideDeviation(BigDecimal.ZERO);
        result.setUptrendCapture(BigDecimal.ZERO);
        result.setDowntrendCapture(BigDecimal.ZERO);
        result.setMaxDrawdownDuration(BigDecimal.ZERO);
        result.setPainIndex(BigDecimal.ZERO);
        result.setRiskAdjustedReturn(BigDecimal.ZERO);
        result.setComprehensiveScore(BigDecimal.ZERO);
        
        return result;
    }

    /**
     * 创建错误结果
     */
    private BacktestResultDTO createErrorResult(String errorMessage) {
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(false);
        result.setErrorMessage("计算回测指标时发生错误: " + errorMessage);
        return result;
    }

    /**
     * 从交易记录中提取交易明细（带手续费计算）
     */
    private List<TradeRecordDTO> extractTradeRecords() {
        List<TradeRecordDTO> records = new ArrayList<>();
        int index = 1;
        BigDecimal tradeAmount = initialAmount;

        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                // 获取入场和出场信息
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();

                Bar entryBar = series.getBar(entryIndex);
                Bar exitBar = series.getBar(exitIndex);

                ZonedDateTime entryTime = entryBar.getEndTime();
                ZonedDateTime exitTime = exitBar.getEndTime();

                BigDecimal entryPrice = new BigDecimal(entryBar.getClosePrice().doubleValue());
                BigDecimal exitPrice = new BigDecimal(exitBar.getClosePrice().doubleValue());

                // 计算入场手续费
                BigDecimal entryFee = tradeAmount.multiply(feeRatio);

                // 扣除入场手续费后的实际交易金额
                BigDecimal actualTradeAmount = tradeAmount.subtract(entryFee);

                // 交易盈亏百分比
                BigDecimal profitPercentage;

                if (position.getEntry().isBuy()) {
                    // 如果是买入操作，盈亏百分比 = (卖出价 - 买入价) / 买入价
                    profitPercentage = exitPrice.subtract(entryPrice)
                            .divide(entryPrice, 4, RoundingMode.HALF_UP);
                } else {
                    // 如果是卖出操作（做空），盈亏百分比 = (买入价 - 卖出价) / 买入价
                    profitPercentage = entryPrice.subtract(exitPrice)
                            .divide(entryPrice, 4, RoundingMode.HALF_UP);
                }

                // 计算出场金额（包含盈亏）
                BigDecimal exitAmount = actualTradeAmount.add(actualTradeAmount.multiply(profitPercentage));

                // 计算出场手续费
                BigDecimal exitFee = exitAmount.multiply(feeRatio);

                // 扣除出场手续费后的实际出场金额
                BigDecimal actualExitAmount = exitAmount.subtract(exitFee);

                // 总手续费
                BigDecimal totalFee = entryFee.add(exitFee);

                // 实际盈亏（考虑手续费）
                BigDecimal actualProfit = actualExitAmount.subtract(tradeAmount);

                // 创建交易记录DTO
                TradeRecordDTO recordDTO = new TradeRecordDTO();
                recordDTO.setIndex(index++);
                recordDTO.setType(position.getEntry().isBuy() ? "BUY" : "SELL");
                recordDTO.setEntryTime(entryTime.toLocalDateTime());
                recordDTO.setExitTime(exitTime.toLocalDateTime());
                recordDTO.setEntryPrice(entryPrice);
                recordDTO.setExitPrice(exitPrice);
                recordDTO.setEntryAmount(tradeAmount);
                recordDTO.setExitAmount(actualExitAmount);
                recordDTO.setProfit(actualProfit);
                recordDTO.setProfitPercentage(profitPercentage);
                recordDTO.setClosed(true);
                recordDTO.setFee(totalFee);

                records.add(recordDTO);

                // 更新下一次交易的资金（全仓交易）
                tradeAmount = actualExitAmount;
            }
        }

        return records;
    }

    /**
     * 基于strategyEquityCurve计算全周期的每日最大回撤和最大亏损
     */
    private List<ArrayList<BigDecimal>> calculateMaximumLossAndDrawdown() {
        if (strategyEquityCurve == null || strategyEquityCurve.isEmpty()) {
            return Arrays.asList(new ArrayList<>(), new ArrayList<>());
        }

        ArrayList<BigDecimal> dailyLossList = new ArrayList<>();
        ArrayList<BigDecimal> dailyDrawdownList = new ArrayList<>();

        // 初始资金作为基准
        BigDecimal initialAmount = strategyEquityCurve.get(0);
        BigDecimal peakAmount = initialAmount; // 历史最高资金

        // 遍历每日资金曲线
        for (int i = 0; i < strategyEquityCurve.size(); i++) {
            BigDecimal currentAmount = strategyEquityCurve.get(i);

            // 更新历史最高资金
            if (currentAmount.compareTo(peakAmount) > 0) {
                peakAmount = currentAmount;
            }

            // 计算当日相对于初始资金的损失率
            BigDecimal lossRate = BigDecimal.ZERO;
            if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal returnRate = currentAmount.subtract(initialAmount).divide(initialAmount, 8, RoundingMode.HALF_UP);
                if (returnRate.compareTo(BigDecimal.ZERO) < 0) {
                    lossRate = returnRate.abs(); // 转为正值表示损失幅度
                }
            }

            // 计算当日回撤率（从历史最高点到当前的下跌幅度）
            BigDecimal drawdownRate = BigDecimal.ZERO;
            if (peakAmount.compareTo(BigDecimal.ZERO) > 0) {
                drawdownRate = peakAmount.subtract(currentAmount).divide(peakAmount, 8, RoundingMode.HALF_UP);
                // 确保回撤率为非负值
                if (drawdownRate.compareTo(BigDecimal.ZERO) < 0) {
                    drawdownRate = BigDecimal.ZERO;
                }
            }

            dailyLossList.add(lossRate);
            dailyDrawdownList.add(drawdownRate);
        }

        // 设置最大损失和最大回撤到交易记录中 - 修复边界问题
        for (int i = 0; i < tradeRecords.size() && i < tradingRecord.getPositionCount(); i++) {
            try {
                Position position = tradingRecord.getPositions().get(i);
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();

                // 边界检查
                if (entryIndex >= 0 && exitIndex >= entryIndex &&
                        entryIndex < dailyLossList.size() && exitIndex < dailyLossList.size()) {

                    // 正确处理subList边界（exitIndex+1因为subList是左闭右开）
                    int actualExitIndex = Math.min(exitIndex + 1, dailyLossList.size());

                    List<BigDecimal> tradePeriodsLoss = dailyLossList.subList(entryIndex, actualExitIndex);
                    tradeRecords.get(i).setMaxLoss(tradePeriodsLoss.stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO));

                    List<BigDecimal> tradePeriodsDrawdown = dailyDrawdownList.subList(entryIndex, actualExitIndex);
                    tradeRecords.get(i).setMaxDrowdown(tradePeriodsDrawdown.stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO));
                    } else {
                    // 索引异常时设置默认值
                    log.warn("交易 {} 的索引异常: entry={}, exit={}, dailyListSize={}，设置默认值",
                            i, entryIndex, exitIndex, dailyLossList.size());
                    tradeRecords.get(i).setMaxLoss(BigDecimal.ZERO);
                    tradeRecords.get(i).setMaxDrowdown(BigDecimal.ZERO);
                }
            } catch (Exception e) {
                log.error("计算交易 {} 的最大损失和回撤时出错: {}", i, e.getMessage());
                tradeRecords.get(i).setMaxLoss(BigDecimal.ZERO);
                tradeRecords.get(i).setMaxDrowdown(BigDecimal.ZERO);
            }
        }

        List<ArrayList<BigDecimal>> result = new ArrayList<>();
        result.add(dailyLossList);
        result.add(dailyDrawdownList);

        // 添加调试日志
        if (!dailyDrawdownList.isEmpty()) {
            BigDecimal maxGlobalDrawdown = dailyDrawdownList.stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO);
            log.debug("策略 {} 全局最大回撤: {}, 交易数量: {}", strategyType, maxGlobalDrawdown, tradeRecords.size());
        }

        return result;
    }

    /**
     * 交易统计指标
     */
    private static class TradeStatistics {
        int tradeCount;
        int profitableTrades;
        BigDecimal totalProfit;
        BigDecimal totalFee;
        BigDecimal finalAmount;
        BigDecimal totalGrossProfit;
        BigDecimal totalGrossLoss;
        BigDecimal profitFactor;
        BigDecimal winRate;
        BigDecimal averageProfit;
        BigDecimal maximumLoss;
        BigDecimal maxDrawdown;
    }

    /**
     * 计算交易统计指标
     */
    private TradeStatistics calculateTradeStatistics() {
        TradeStatistics stats = new TradeStatistics();

        stats.tradeCount = tradeRecords.size();
        stats.profitableTrades = 0;
        stats.totalProfit = BigDecimal.ZERO;
        stats.totalFee = BigDecimal.ZERO;
        stats.finalAmount = initialAmount;
        stats.totalGrossProfit = BigDecimal.ZERO;
        stats.totalGrossLoss = BigDecimal.ZERO;

        for (TradeRecordDTO trade : tradeRecords) {
            BigDecimal profit = trade.getProfit();

            if (profit != null) {
                stats.totalProfit = stats.totalProfit.add(profit);

                // 分别累计总盈利和总亏损
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    stats.profitableTrades++;
                    stats.totalGrossProfit = stats.totalGrossProfit.add(profit);
                } else {
                    stats.totalGrossLoss = stats.totalGrossLoss.add(profit.abs());
                }
            }

            if (trade.getFee() != null) {
                stats.totalFee = stats.totalFee.add(trade.getFee());
            }
        }

        stats.finalAmount = initialAmount.add(stats.totalProfit);

        // 计算盈利因子 (Profit Factor)
        stats.profitFactor = BigDecimal.ONE;
        if (stats.totalGrossLoss.compareTo(BigDecimal.ZERO) > 0) {
            stats.profitFactor = stats.totalGrossProfit.divide(stats.totalGrossLoss, 4, RoundingMode.HALF_UP);
        } else if (stats.totalGrossProfit.compareTo(BigDecimal.ZERO) > 0) {
            stats.profitFactor = new BigDecimal("999.9999");
        }

        // 计算胜率
        stats.winRate = BigDecimal.ZERO;
        if (stats.tradeCount > 0) {
            stats.winRate = new BigDecimal(stats.profitableTrades).divide(new BigDecimal(stats.tradeCount), 4, RoundingMode.HALF_UP);
        }

        // 计算平均盈利,百分百
        stats.averageProfit = BigDecimal.ZERO;
        if (stats.tradeCount > 0) {
            BigDecimal totalReturn = BigDecimal.ZERO;
            if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
                totalReturn = stats.totalProfit.divide(initialAmount, 4, RoundingMode.HALF_UP);
            }
            stats.averageProfit = totalReturn.divide(new BigDecimal(stats.tradeCount), 4, RoundingMode.HALF_UP);
        }

        return stats;
    }

    /**
     * 收益率指标
     */
    private static class ReturnMetrics {
        BigDecimal totalReturn;
        BigDecimal annualizedReturn;
    }

    /**
     * 计算收益率相关指标
     */
    private ReturnMetrics calculateReturnMetrics(TradeStatistics stats) {
        ReturnMetrics metrics = new ReturnMetrics();

        // 计算总收益率
        metrics.totalReturn = BigDecimal.ZERO;
        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            metrics.totalReturn = stats.totalProfit.divide(initialAmount, 4, RoundingMode.HALF_UP);
        }

        // 计算年化收益率
        metrics.annualizedReturn = calculateAnnualizedReturn(
                metrics.totalReturn,
                series.getFirstBar().getEndTime().toLocalDateTime(),
                series.getLastBar().getEndTime().toLocalDateTime()
        );

        return metrics;
    }

    /**
     * 风险指标度量类
     * 包含所有风险相关的计算指标，用于全面评估交易策略的风险特征
     */
    private static class RiskMetrics {
        // ========== 传统风险指标 ==========
        BigDecimal sharpeRatio;      // 夏普比率 - 风险调整收益
        BigDecimal sortinoRatio;     // Sortino比率 - 下行风险调整收益
        BigDecimal calmarRatio;      // Calmar比率 - 年化收益/最大回撤
        BigDecimal omega;            // Omega比率 - 收益分布比率
        BigDecimal volatility;       // 波动率 - 价格变动的标准差
        BigDecimal[] alphaBeta;      // Alpha和Beta - 超额收益和系统性风险
        BigDecimal treynorRatio;     // Treynor比率 - 单位系统性风险的超额收益
        BigDecimal ulcerIndex;       // 溃疡指数 - 深度和持续回撤的综合指标
        BigDecimal skewness;         // 偏度 - 收益率分布的对称性
        
        // ========== 新增高级风险指标 ==========
        
        /**
         * 峰度 (Kurtosis) - 衡量收益率分布的尾部风险
         * 计算公式: E[(r-μ)^4] / σ^4 - 3
         * 数值含义: 
         * - 正态分布的峰度为0
         * - 峰度>0表示分布有厚尾（极端事件发生概率更高）
         * - 峰度<0表示分布平坦（极端事件发生概率较低）
         * 应用场景: 评估策略在极端市场条件下的风险暴露程度
         * 风险解读: 峰度越高，出现极端收益（大盈利或大亏损）的概率越大
         */
        BigDecimal kurtosis;
        
        /**
         * 条件风险价值 (CVaR, Conditional Value at Risk) - 极端损失的期望值
         * 计算公式: E[损失 | 损失 > VaR5%]
         * 也称为期望损失(Expected Shortfall, ES)
         * 数值含义: 在最坏5%情况下的平均损失
         * 应用场景: 风险管理中评估极端不利情况下的预期损失
         * 风险解读: CVaR越大，在极端不利情况下面临的损失越严重
         */
        BigDecimal cvar;
        
        /**
         * 95%置信度下的风险价值 (VaR95) - 95%概率下的最大损失
         * 计算公式: 收益率分布的5%分位数
         * 数值含义: 在正常市场条件下，95%的时间内损失不会超过该值
         * 应用场景: 日常风险管理和头寸规模确定
         * 风险解读: VaR95越大，策略的日常交易风险越高
         */
        BigDecimal var95;
        
        /**
         * 99%置信度下的风险价值 (VaR99) - 99%概率下的最大损失
         * 计算公式: 收益率分布的1%分位数
         * 数值含义: 在极端市场条件下，99%的时间内损失不会超过该值
         * 应用场景: 压力测试和极端风险情景分析
         * 风险解读: VaR99越大，策略在黑天鹅事件中的风险越高
         */
        BigDecimal var99;
        
        /**
         * 信息比率 (Information Ratio) - 主动管理效率指标
         * 计算公式: (策略收益率 - 基准收益率) / 跟踪误差
         * 数值含义: 单位主动风险所获得的超额收益
         * 应用场景: 评估主动管理策略相对于被动指数的价值
         * 风险解读: 信息比率>0.5为良好，>1.0为优秀的主动管理表现
         */
        BigDecimal informationRatio;
        
        /**
         * 跟踪误差 (Tracking Error) - 相对基准的波动性
         * 计算公式: std(策略收益率 - 基准收益率)
         * 数值含义: 策略收益率相对于基准收益率的标准差
         * 应用场景: 衡量策略偏离基准的程度，控制主动风险
         * 风险解读: 跟踪误差越大，策略与基准的偏差越大，主动风险越高
         */
        BigDecimal trackingError;
        
        /**
         * Sterling比率 - 回撤风险调整收益指标
         * 计算公式: 年化收益率 / 平均最大回撤
         * 数值含义: 每单位平均回撤风险所获得的年化收益
         * 应用场景: 比较不同策略的回撤风险调整表现
         * 风险解读: Sterling比率>1.0为良好，>2.0为优秀的风险收益比
         */
        BigDecimal sterlingRatio;
        
        /**
         * Burke比率 - 极端回撤风险调整收益指标
         * 计算公式: 年化收益率 / sqrt(sum(回撤^2))
         * 数值含义: 对较大回撤给予更多惩罚的风险调整收益
         * 应用场景: 评估策略在控制极端回撤方面的表现
         * 风险解读: Burke比率比Sterling比率更严格，更适合风险厌恶投资者
         */
        BigDecimal burkeRatio;
        
        /**
         * 修正夏普比率 (Modified Sharpe Ratio) - 非正态分布修正的夏普比率
         * 计算公式: Sharpe * [1 + (偏度/6)*Sharpe - (峰度-3)/24*Sharpe^2]
         * 数值含义: 考虑收益率分布偏度和峰度的夏普比率修正值
         * 应用场景: 当收益率分布显著偏离正态分布时使用
         * 风险解读: 修正夏普比率更准确地反映非正态分布下的真实风险调整收益
         */
        BigDecimal modifiedSharpeRatio;
        
        /**
         * 下行偏差 (Downside Deviation) - 下行风险度量
         * 计算公式: sqrt(E[min(收益率-目标收益率, 0)^2])
         * 数值含义: 只考虑负收益的标准差，忽略上行波动
         * 应用场景: 风险厌恶投资者关注的下行保护能力评估
         * 风险解读: 下行偏差越小，策略的下行保护能力越强
         */
        BigDecimal downsideDeviation;
        
        /**
         * 上涨捕获率 (Uptrend Capture Ratio) - 牛市表现指标
         * 计算公式: 基准上涨期间策略平均收益率 / 基准平均收益率
         * 数值含义: 策略在市场上涨时的跟随能力
         * 应用场景: 评估策略在牛市环境中的收益获取能力
         * 风险解读: 上涨捕获率<80%可能意味着错失上涨机会
         */
        BigDecimal uptrendCapture;
        
        /**
         * 下跌捕获率 (Downtrend Capture Ratio) - 熊市防御指标
         * 计算公式: 基准下跌期间策略平均收益率 / 基准平均收益率
         * 数值含义: 策略在市场下跌时的防御能力
         * 应用场景: 评估策略在熊市环境中的风险控制能力
         * 风险解读: 下跌捕获率>80%表示策略在市场下跌时损失较大
         */
        BigDecimal downtrendCapture;
        
        /**
         * 最大回撤持续期 (Maximum Drawdown Duration) - 回撤时间风险
         * 计算方法: 统计从净值峰值到恢复峰值之间的最长时间间隔
         * 数值含义: 策略从最大亏损状态恢复到盈利状态所需的最长时间
         * 单位: 根据数据频率而定（交易日、小时等）
         * 应用场景: 评估策略的资金流动性需求和投资者心理承受能力
         * 风险解读: 持续期越长，投资者需要越强的耐心和资金承受能力
         */
        BigDecimal maxDrawdownDuration;
        
        /**
         * 痛苦指数 (Pain Index) - 综合痛苦体验指标
         * 计算公式: sum(每期回撤百分比) / 总期数
         * 数值含义: 平均每期的回撤痛苦程度
         * 应用场景: 全面评估策略给投资者带来的心理痛苦程度
         * 风险解读: 痛苦指数越高，策略的整体投资体验越差
         */
        BigDecimal painIndex;
        
        /**
         * 风险调整收益 (Risk-Adjusted Return) - 综合风险收益指标
         * 计算方法: 基于多个风险指标的加权综合评分
         * 考虑因素: 波动性、最大回撤、VaR、下行偏差等多个风险维度
         * 应用场景: 策略的整体风险收益综合评估
         * 风险解读: 风险调整收益越高，策略在控制风险前提下的盈利能力越强
         */
        BigDecimal riskAdjustedReturn;

        // ========== 综合评分指标 ==========
        
        /**
         * 综合评分 (Comprehensive Score) - 策略综合表现评分
         * 评分范围: 0-10分（10分为最佳表现）
         * 评分维度及权重: 
         * - 收益指标评分 (35%权重): 年化收益率、总收益率、盈利因子
         * - 核心风险评分 (35%权重): 夏普比率、最大回撤、Sortino比率、VaR、Calmar比率
         * - 交易质量评分 (20%权重): 胜率、交易次数、平均盈利
         * - 稳定性评分 (10%权重): 偏度、峰度、痛苦指数
         * 应用场景: 策略排序、策略选择、投资组合构建
         * 评分标准: 
         * - 8-10分: 卓越表现，适合重点关注
         * - 6-8分:  良好表现，收益风险平衡，样本充足，可考虑配置
         * - 4-6分:  一般表现，存在不足，谨慎考虑
         * - 2-4分:  较差表现，样本不足或风险过高，不建议使用
         * - 0-2分:  极差表现，无效或危险策略，应当避免
         */
        BigDecimal comprehensiveScore;
    }

    /**
     * 计算风险指标
     */
    private RiskMetrics calculateRiskMetrics(TradeStatistics tradeStats, ReturnMetrics returnMetrics) {

        RiskMetrics metrics = new RiskMetrics();

        // 假设无风险收益率为0（可根据实际情况调整）
        BigDecimal riskFreeRate = BigDecimal.valueOf(0);

        // 获取年化因子（基于时间间隔）
        int annualizationFactor = detectAnnualizationFactor(series);

        // 计算策略的每日收益率序列（对数收益率）
        fullPeriodStrategyReturns = calculateFullPeriodStrategyReturns(series, tradingRecord, true);

        // 计算包含手续费的真实策略资金曲线（基于实际交易记录）
        strategyEquityCurve = calculateRealStrategyEquityCurve();

        metrics.sharpeRatio = Ta4jBacktestService.calculateSharpeRatio(fullPeriodStrategyReturns, riskFreeRate, annualizationFactor);
        metrics.omega = Ta4jBacktestService.calculateOmegaRatio(fullPeriodStrategyReturns, riskFreeRate);

        // 计算Sortino比率
        metrics.sortinoRatio = Ta4jBacktestService.calculateSortinoRatio(fullPeriodStrategyReturns, riskFreeRate, annualizationFactor);

        // 计算所有日期的价格数据用于其他指标计算
        dailyPrices = new ArrayList<>();
        for (int i = 0; i <= series.getEndIndex(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            dailyPrices.add(BigDecimal.valueOf(closePrice));
        }

        // 计算波动率（基于收盘价）
        metrics.volatility = calculateVolatility(series, annualizationFactor);

        // Alpha 表示策略超额收益，Beta 表示策略相对于基准收益的敏感度（风险）
        metrics.alphaBeta = calculateAlphaBeta(fullPeriodStrategyReturns, benchmarkCandlesticks);

        // 计算 Treynor 比率
        metrics.treynorRatio = Ta4jBacktestService.calculateTreynorRatio(fullPeriodStrategyReturns, riskFreeRate, metrics.alphaBeta[1]);

        // 计算 Ulcer Index - 使用策略资金曲线
        metrics.ulcerIndex = Ta4jBacktestService.calculateUlcerIndex(strategyEquityCurve);

        // 计算收益率序列的偏度 (Skewness)
        metrics.skewness = Ta4jBacktestService.calculateSkewness(fullPeriodStrategyReturns);

        // 新增风险指标计算
        
        // 计算峰度 (Kurtosis) - 衡量收益率分布的尾部风险
        metrics.kurtosis = calculateKurtosis(fullPeriodStrategyReturns);
        
        // 计算风险价值 (VaR) 和条件风险价值 (CVaR)
        BigDecimal[] varResults = calculateVaRAndCVaR(fullPeriodStrategyReturns);
        metrics.var95 = varResults[0];  // 95% VaR
        metrics.var99 = varResults[1];  // 99% VaR
        metrics.cvar = varResults[2];   // CVaR (Expected Shortfall)
        
        // 计算下行偏差 (Downside Deviation)
        metrics.downsideDeviation = calculateDownsideDeviation(fullPeriodStrategyReturns, riskFreeRate);
        
        // 计算跟踪误差和信息比率
        List<BigDecimal> benchmarkReturns = calculateBenchmarkReturns();
        metrics.trackingError = calculateTrackingError(fullPeriodStrategyReturns, benchmarkReturns);
        metrics.informationRatio = calculateInformationRatio(fullPeriodStrategyReturns, benchmarkReturns, metrics.trackingError);
        
        // 计算Sterling比率和Burke比率 - 使用策略资金曲线
        metrics.sterlingRatio = calculateSterlingRatio(returnMetrics.annualizedReturn, strategyEquityCurve);
        metrics.burkeRatio = calculateBurkeRatio(returnMetrics.annualizedReturn, strategyEquityCurve);
        
        // 计算修正夏普比率（考虑偏度和峰度）
        metrics.modifiedSharpeRatio = calculateModifiedSharpeRatio(metrics.sharpeRatio, metrics.skewness, metrics.kurtosis);
        
        // 计算上涨和下跌捕获率
        BigDecimal[] captureRatios = calculateCaptureRatios(fullPeriodStrategyReturns, benchmarkReturns);
        metrics.uptrendCapture = captureRatios[0];
        metrics.downtrendCapture = captureRatios[1];
        
        // 计算最大回撤持续期和痛苦指数 - 使用策略资金曲线
        metrics.maxDrawdownDuration = calculateMaxDrawdownDuration(strategyEquityCurve);
        metrics.painIndex = calculatePainIndex(strategyEquityCurve);
        
        // 计算风险调整收益
        metrics.riskAdjustedReturn = calculateRiskAdjustedReturn(returnMetrics.totalReturn, metrics);
        
        // 计算最大损失和最大回撤
        maxLossAndDrawdownList = calculateMaximumLossAndDrawdown();

        // 计算最大损失和最大回撤
        tradeStats.maximumLoss = maxLossAndDrawdownList.get(0).stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO);
        tradeStats.maxDrawdown = maxLossAndDrawdownList.get(1).stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO);

        // 计算Calmar比率
        metrics.calmarRatio = Ta4jBacktestService.calculateCalmarRatio(returnMetrics.annualizedReturn, tradeStats.maxDrawdown);

        // 计算综合评分 (0-10分) - 优先使用基于数据库分布的动态评分
        metrics.comprehensiveScore = calculateDatabaseBasedScore(returnMetrics, tradeStats, metrics);

        return metrics;
    }

    /**
     * 构建最终结果
     */
    private BacktestResultDTO buildFinalResult() {
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSuccess(true);
        result.setInitialAmount(initialAmount);
        result.setFinalAmount(tradeStats.finalAmount);
        result.setTotalProfit(tradeStats.totalProfit);
        result.setTotalReturn(returnMetrics.totalReturn);
        result.setAnnualizedReturn(returnMetrics.annualizedReturn);
        result.setNumberOfTrades(tradeStats.tradeCount);
        result.setProfitableTrades(tradeStats.profitableTrades);
        result.setUnprofitableTrades(tradeStats.tradeCount - tradeStats.profitableTrades);
        result.setWinRate(tradeStats.winRate);
        result.setAverageProfit(tradeStats.averageProfit);
        result.setMaxDrawdown(tradeStats.maxDrawdown);
        result.setSharpeRatio(riskMetrics.sharpeRatio);
        result.setSortinoRatio(riskMetrics.sortinoRatio);
        result.setCalmarRatio(riskMetrics.calmarRatio);
        result.setOmega(riskMetrics.omega);
        result.setAlpha(riskMetrics.alphaBeta[0]);
        result.setBeta(riskMetrics.alphaBeta[1]);
        result.setTreynorRatio(riskMetrics.treynorRatio);
        result.setUlcerIndex(riskMetrics.ulcerIndex);
        result.setSkewness(riskMetrics.skewness);
        result.setMaximumLoss(tradeStats.maximumLoss);
        result.setVolatility(riskMetrics.volatility);
        result.setProfitFactor(tradeStats.profitFactor);
        result.setStrategyName(strategyType);
        result.setParameterDescription(paramDescription);
        result.setTrades(tradeRecords);
        result.setTotalFee(tradeStats.totalFee);
        
        // 设置新增的风险指标
        result.setKurtosis(riskMetrics.kurtosis);
        result.setCvar(riskMetrics.cvar);
        result.setVar95(riskMetrics.var95);
        result.setVar99(riskMetrics.var99);
        result.setInformationRatio(riskMetrics.informationRatio);
        result.setTrackingError(riskMetrics.trackingError);
        result.setSterlingRatio(riskMetrics.sterlingRatio);
        result.setBurkeRatio(riskMetrics.burkeRatio);
        result.setModifiedSharpeRatio(riskMetrics.modifiedSharpeRatio);
        result.setDownsideDeviation(riskMetrics.downsideDeviation);
        result.setUptrendCapture(riskMetrics.uptrendCapture);
        result.setDowntrendCapture(riskMetrics.downtrendCapture);
        result.setMaxDrawdownDuration(riskMetrics.maxDrawdownDuration);
        result.setPainIndex(riskMetrics.painIndex);
        result.setRiskAdjustedReturn(riskMetrics.riskAdjustedReturn);
        
        // 设置综合评分
        result.setComprehensiveScore(riskMetrics.comprehensiveScore);

        return result;
    }

    /**
     * 基于数据库分布和权重配置的动态评分算法
     * 使用所有33个指标进行综合评分
     */
    private BigDecimal calculateDatabaseBasedScore(ReturnMetrics returnMetrics,
                                                   TradeStatistics tradeStats,
                                                   RiskMetrics riskMetrics) {

        try {

            if (weightService == null || weightService.getCurrentConfig() == null) {
                log.warn("权重配置服务不可用，使用默认评分逻辑");
                return BigDecimal.ZERO;
            }

            // 准备所有指标值映射
            Map<String, BigDecimal> indicatorValues = buildAllIndicatorValues(returnMetrics, tradeStats, riskMetrics);

            // 计算各指标的评分（8分制）
            Map<String, Double> indicatorScores = distributionService.calculateIndicatorScores(indicatorValues);

            // 使用权重配置计算综合评分
            BigDecimal comprehensiveScore = weightService.calculateComprehensiveScore(indicatorValues, indicatorScores);

            // 获取维度评分详情用于调试
            Map<String, Object> dimensionDetails = weightService.getDimensionScoreDetails(indicatorValues, indicatorScores);

            log.debug("=== 基于权重配置的综合评分详情 ===");
            log.debug("最终综合评分: {}/10", comprehensiveScore);

            for (Map.Entry<String, Object> entry : dimensionDetails.entrySet()) {
                Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                log.debug("维度 {}: 评分={}, 权重={}, 加权评分={}, 指标数量={}",
                        entry.getKey(),
                        detail.get("score"),
                        detail.get("weight"),
                        detail.get("weightedScore"),
                        detail.get("indicatorCount"));
            }
            log.debug("=====================================");

            return comprehensiveScore;

        } catch (Exception e) {
            log.error("计算基于权重配置的评分时发生错误: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 构建所有指标值映射
     */
    private Map<String, BigDecimal> buildAllIndicatorValues(ReturnMetrics returnMetrics,
                                                            TradeStatistics tradeStats,
                                                            RiskMetrics riskMetrics) {
        Map<String, BigDecimal> indicatorValues = new HashMap<>();

        // ========== 收益性能指标 ==========
        indicatorValues.put("totalReturn", returnMetrics.totalReturn != null ? returnMetrics.totalReturn : BigDecimal.ZERO);
        indicatorValues.put("annualizedReturn", returnMetrics.annualizedReturn != null ? returnMetrics.annualizedReturn : BigDecimal.ZERO);
        indicatorValues.put("averageProfit", tradeStats.averageProfit != null ? tradeStats.averageProfit : BigDecimal.ZERO);
        indicatorValues.put("profitFactor", tradeStats.profitFactor != null ? tradeStats.profitFactor : BigDecimal.ZERO);
        indicatorValues.put("riskAdjustedReturn", riskMetrics.riskAdjustedReturn != null ? riskMetrics.riskAdjustedReturn : BigDecimal.ZERO);

        // ========== 风险控制指标 ==========
        indicatorValues.put("maxDrawdown", tradeStats.maxDrawdown != null ? tradeStats.maxDrawdown : BigDecimal.ZERO);
        indicatorValues.put("maximumLoss", tradeStats.maximumLoss != null ? tradeStats.maximumLoss : BigDecimal.ZERO);
        indicatorValues.put("volatility", riskMetrics.volatility != null ? riskMetrics.volatility : BigDecimal.ZERO);
        indicatorValues.put("ulcerIndex", riskMetrics.ulcerIndex != null ? riskMetrics.ulcerIndex : BigDecimal.ZERO);
        indicatorValues.put("painIndex", riskMetrics.painIndex != null ? riskMetrics.painIndex : BigDecimal.ZERO);
        indicatorValues.put("downsideDeviation", riskMetrics.downsideDeviation != null ? riskMetrics.downsideDeviation : BigDecimal.ZERO);
        indicatorValues.put("cvar", riskMetrics.cvar != null ? riskMetrics.cvar : BigDecimal.ZERO);
        indicatorValues.put("var95", riskMetrics.var95 != null ? riskMetrics.var95 : BigDecimal.ZERO);
        indicatorValues.put("var99", riskMetrics.var99 != null ? riskMetrics.var99 : BigDecimal.ZERO);
        indicatorValues.put("trackingError", riskMetrics.trackingError != null ? riskMetrics.trackingError : BigDecimal.ZERO);
        indicatorValues.put("maxDrawdownDuration", riskMetrics.maxDrawdownDuration != null ? riskMetrics.maxDrawdownDuration : BigDecimal.ZERO);
        indicatorValues.put("downtrendCapture", riskMetrics.downtrendCapture != null ? riskMetrics.downtrendCapture : BigDecimal.ZERO);

        // ========== 风险调整比率指标 ==========
        indicatorValues.put("sharpeRatio", riskMetrics.sharpeRatio != null ? riskMetrics.sharpeRatio : BigDecimal.ZERO);
        indicatorValues.put("sortinoRatio", riskMetrics.sortinoRatio != null ? riskMetrics.sortinoRatio : BigDecimal.ZERO);
        indicatorValues.put("calmarRatio", riskMetrics.calmarRatio != null ? riskMetrics.calmarRatio : BigDecimal.ZERO);
        indicatorValues.put("treynorRatio", riskMetrics.treynorRatio != null ? riskMetrics.treynorRatio : BigDecimal.ZERO);
        indicatorValues.put("informationRatio", riskMetrics.informationRatio != null ? riskMetrics.informationRatio : BigDecimal.ZERO);
        indicatorValues.put("sterlingRatio", riskMetrics.sterlingRatio != null ? riskMetrics.sterlingRatio : BigDecimal.ZERO);
        indicatorValues.put("burkeRatio", riskMetrics.burkeRatio != null ? riskMetrics.burkeRatio : BigDecimal.ZERO);
        indicatorValues.put("modifiedSharpeRatio", riskMetrics.modifiedSharpeRatio != null ? riskMetrics.modifiedSharpeRatio : BigDecimal.ZERO);
        indicatorValues.put("omega", riskMetrics.omega != null ? riskMetrics.omega : BigDecimal.ZERO);

        // ========== 交易效率指标 ==========
        indicatorValues.put("winRate", tradeStats.winRate != null ? tradeStats.winRate : BigDecimal.ZERO);
        indicatorValues.put("numberOfTrades", BigDecimal.valueOf(tradeStats.tradeCount));

        // Alpha和Beta需要特殊处理
        if (riskMetrics.alphaBeta != null && riskMetrics.alphaBeta.length >= 2) {
            indicatorValues.put("alpha", riskMetrics.alphaBeta[0] != null ? riskMetrics.alphaBeta[0] : BigDecimal.ZERO);
            indicatorValues.put("beta", riskMetrics.alphaBeta[1] != null ? riskMetrics.alphaBeta[1] : BigDecimal.ZERO);
        } else {
            indicatorValues.put("alpha", BigDecimal.ZERO);
            indicatorValues.put("beta", BigDecimal.ONE); // Beta默认为1
        }

        indicatorValues.put("uptrendCapture", riskMetrics.uptrendCapture != null ? riskMetrics.uptrendCapture : BigDecimal.ZERO);
        indicatorValues.put("skewness", riskMetrics.skewness != null ? riskMetrics.skewness : BigDecimal.ZERO);
        indicatorValues.put("kurtosis", riskMetrics.kurtosis != null ? riskMetrics.kurtosis : BigDecimal.ZERO);

        // 记录指标值统计（开发环境）
        long validIndicatorCount = indicatorValues.values().stream()
                .mapToLong(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0 ? 1 : 0)
                .sum();
        log.debug("构建指标值映射完成: 总数={}, 有效数量={}", indicatorValues.size(), validIndicatorCount);


        return indicatorValues;
    }

    /**
     * 获取指标权重服务实例
     */
    private IndicatorWeightService getIndicatorWeightService() {
        try {
            return SpringContextUtil.getBean(IndicatorWeightService.class);
        } catch (Exception e) {
            log.debug("无法获取指标权重服务: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取指标分布服务实例
     */
    private IndicatorDistributionService getIndicatorDistributionService() {
        try {
            return SpringContextUtil.getBean(IndicatorDistributionService.class);
        } catch (Exception e) {
            log.debug("无法获取指标分布服务: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取计算结果
     */
    public BacktestResultDTO getResult() {
        return result;
    }

    // ====================== 新增风险指标计算方法 ======================

    /**
     * 计算峰度 (Kurtosis) - 衡量收益率分布的尾部风险
     * <p>
     * 峰度是描述数据分布形态的统计量，用于衡量分布的"尖峭程度"和尾部厚度
     * <p>
     * 计算步骤:
     * 1. 计算收益率的均值μ
     * 2. 计算方差σ²
     * 3. 计算四阶中心矩: E[(r-μ)⁴]
     * 4. 峰度 = E[(r-μ)⁴]/σ⁴ - 3
     * <p>
     * 数值解读:
     * - 正态分布的峰度为0
     * - 峰度>0: 厚尾分布，极端事件发生概率较高
     * - 峰度<0: 薄尾分布，数据更加集中
     * - 峰度>3: 高度风险，需要特别关注
     * <p>
     * 在交易策略中的应用:
     * - 评估策略在极端市场条件下的表现
     * - 识别可能存在的"黑天鹅"风险
     * - 风险管理中的压力测试参考
     * 
     * @param returns 策略收益率序列
     * @return 峰度值，保留4位小数
     */
    private BigDecimal calculateKurtosis(List<BigDecimal> returns) {
        if (returns == null || returns.size() < 4) {
            return BigDecimal.ZERO;
        }

        // 计算均值
        double mean = returns.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        
        // 计算方差
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 2))
                .average().orElse(0.0);
        
        if (variance <= 0) {
            return BigDecimal.ZERO;
        }

        // 计算四阶中心矩
        double fourthMoment = returns.stream()
                .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 4))
                .average().orElse(0.0);

        // 峰度 = 四阶中心矩 / 方差^2 - 3
        double kurtosis = (fourthMoment / Math.pow(variance, 2)) - 3.0;
        
        return BigDecimal.valueOf(kurtosis).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算风险价值 (VaR) 和条件风险价值 (CVaR)
     * <p>
     * VaR (Value at Risk) 是在正常市场条件下，在给定置信度和时间段内，
     * 投资组合可能遭受的最大损失
     * <p>
     * CVaR (Conditional VaR) 是在损失超过VaR阈值的条件下，
     * 损失的期望值，也称为期望损失(Expected Shortfall)
     * <p>
     * 计算方法:
     * 1. 将收益率从小到大排序
     * 2. VaR95% = 收益率序列的5%分位数的负值
     * 3. VaR99% = 收益率序列的1%分位数的负值
     * 4. CVaR = 所有小于5%分位数的收益率的平均值的负值
     * <p>
     * 数值解读:
     * - VaR95%=5%表示：95%的时间损失不会超过5%
     * - VaR99%=10%表示：99%的时间损失不会超过10%
     * - CVaR=8%表示：在最坏5%情况下，平均损失为8%
     * <p>
     * 在交易策略中的应用:
     * - 风险预算和头寸规模管理
     * - 设置止损水平的参考
     * - 监管资本要求计算
     * - 压力测试和情景分析
     * 
     * @param returns 策略收益率序列
     * @return 数组[VaR95%, VaR99%, CVaR]，都以正数表示损失
     */
    private BigDecimal[] calculateVaRAndCVaR(List<BigDecimal> returns) {
        if (returns == null || returns.isEmpty()) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        }

        // 排序收益率（从小到大）
        List<Double> sortedReturns = returns.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        int n = sortedReturns.size();
        
        // 计算VaR (95%和99%置信度)
        int var95Index = (int) Math.ceil(n * 0.05) - 1; // 5%分位数
        int var99Index = (int) Math.ceil(n * 0.01) - 1; // 1%分位数
        
        var95Index = Math.max(0, Math.min(var95Index, n - 1));
        var99Index = Math.max(0, Math.min(var99Index, n - 1));
        
        BigDecimal var95 = BigDecimal.valueOf(-sortedReturns.get(var95Index));
        BigDecimal var99 = BigDecimal.valueOf(-sortedReturns.get(var99Index));

        // 计算CVaR (条件风险价值) - 超过VaR95的损失的平均值
        double cvarSum = 0.0;
        int cvarCount = 0;
        for (int i = 0; i <= var95Index; i++) {
            cvarSum += sortedReturns.get(i);
            cvarCount++;
        }
        
        BigDecimal cvar = BigDecimal.ZERO;
        if (cvarCount > 0) {
            cvar = BigDecimal.valueOf(-cvarSum / cvarCount);
        }

        return new BigDecimal[]{
                var95.setScale(4, RoundingMode.HALF_UP),
                var99.setScale(4, RoundingMode.HALF_UP),
                cvar.setScale(4, RoundingMode.HALF_UP)
        };
    }

    /**
     * 计算下行偏差 (Downside Deviation) - 只考虑负收益的标准差
     */
    private BigDecimal calculateDownsideDeviation(List<BigDecimal> returns, BigDecimal target) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<Double> downsideReturns = returns.stream()
                .filter(r -> r.compareTo(target) < 0)
                .mapToDouble(r -> Math.pow(r.subtract(target).doubleValue(), 2))
                .boxed()
                .collect(Collectors.toList());

        if (downsideReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double variance = downsideReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算基准收益率序列
     */
    private List<BigDecimal> calculateBenchmarkReturns() {
        List<BigDecimal> benchmarkReturns = new ArrayList<>();
        
        if (benchmarkCandlesticks == null || benchmarkCandlesticks.size() < 2) {
            // 如果没有基准数据，返回与策略收益率相同长度的零收益率
            for (int i = 0; i < strategyEquityCurve.size(); i++) {
                benchmarkReturns.add(BigDecimal.ZERO);
            }
            return benchmarkReturns;
        }

        // 计算基准的对数收益率
        for (int i = 1; i < benchmarkCandlesticks.size(); i++) {
            BigDecimal current = benchmarkCandlesticks.get(i).getClose();
            BigDecimal previous = benchmarkCandlesticks.get(i - 1).getClose();
            
            if (previous.compareTo(BigDecimal.ZERO) > 0) {
                double logReturn = Math.log(current.doubleValue() / previous.doubleValue());
                benchmarkReturns.add(BigDecimal.valueOf(logReturn));
            } else {
                benchmarkReturns.add(BigDecimal.ZERO);
            }
        }

        // 确保长度匹配
        while (benchmarkReturns.size() < strategyEquityCurve.size()) {
            benchmarkReturns.add(BigDecimal.ZERO);
        }
        
        // 截取到相同长度
        if (benchmarkReturns.size() > strategyEquityCurve.size()) {
            benchmarkReturns = benchmarkReturns.subList(0, strategyEquityCurve.size());
        }

        return benchmarkReturns;
    }

    /**
     * 计算跟踪误差 (Tracking Error) - 策略与基准收益率差异的标准差
     */
    private BigDecimal calculateTrackingError(List<BigDecimal> strategyReturns, List<BigDecimal> benchmarkReturns) {
        if (strategyReturns == null || benchmarkReturns == null || 
            strategyReturns.size() != benchmarkReturns.size()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> trackingDiffs = new ArrayList<>();
        for (int i = 0; i < strategyReturns.size(); i++) {
            BigDecimal diff = strategyReturns.get(i).subtract(benchmarkReturns.get(i));
            trackingDiffs.add(diff);
        }

        // 计算跟踪差异的标准差
        double mean = trackingDiffs.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        double variance = trackingDiffs.stream()
                .mapToDouble(d -> Math.pow(d.doubleValue() - mean, 2))
                .average().orElse(0.0);

        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算信息比率 (Information Ratio) - 超额收益相对于跟踪误差的比率
     */
    private BigDecimal calculateInformationRatio(List<BigDecimal> strategyReturns, 
                                                List<BigDecimal> benchmarkReturns, 
                                                BigDecimal trackingError) {
        if (trackingError.compareTo(BigDecimal.ZERO) == 0 || 
            strategyReturns == null || benchmarkReturns == null ||
            strategyReturns.size() != benchmarkReturns.size()) {
            return BigDecimal.ZERO;
        }

        // 计算平均超额收益
        double avgExcessReturn = 0.0;
        for (int i = 0; i < strategyReturns.size(); i++) {
            avgExcessReturn += strategyReturns.get(i).subtract(benchmarkReturns.get(i)).doubleValue();
        }
        avgExcessReturn /= strategyReturns.size();

        return BigDecimal.valueOf(avgExcessReturn).divide(trackingError, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算Sterling比率 - 年化收益与平均最大回撤的比率
     */
    private BigDecimal calculateSterlingRatio(BigDecimal annualizedReturn, List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal avgMaxDrawdown = calculateAverageMaxDrawdown(prices);
        
        if (avgMaxDrawdown.compareTo(BigDecimal.ZERO) == 0) {
            return annualizedReturn.compareTo(BigDecimal.ZERO) > 0 ? 
                   new BigDecimal("999.9999") : BigDecimal.ZERO;
        }

        return annualizedReturn.divide(avgMaxDrawdown, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算Burke比率 - 年化收益与平方根回撤的比率
     */
    private BigDecimal calculateBurkeRatio(BigDecimal annualizedReturn, List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal sqrtDrawdown = calculateSquareRootDrawdown(prices);
        
        if (sqrtDrawdown.compareTo(BigDecimal.ZERO) == 0) {
            return annualizedReturn.compareTo(BigDecimal.ZERO) > 0 ? 
                   new BigDecimal("999.9999") : BigDecimal.ZERO;
        }

        return annualizedReturn.divide(sqrtDrawdown, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算修正夏普比率 - 考虑偏度和峰度的夏普比率
     */
    private BigDecimal calculateModifiedSharpeRatio(BigDecimal sharpeRatio, BigDecimal skewness, BigDecimal kurtosis) {
        if (sharpeRatio == null) {
            return BigDecimal.ZERO;
        }

        // 修正因子：考虑偏度和峰度的影响
        // 修正夏普比率 = 夏普比率 * (1 + (偏度/6)*夏普比率 + (峰度-3)/24*夏普比率^2)
        BigDecimal sr = sharpeRatio;
        BigDecimal s = skewness != null ? skewness : BigDecimal.ZERO;
        BigDecimal k = kurtosis != null ? kurtosis : BigDecimal.ZERO;

        BigDecimal term1 = s.divide(BigDecimal.valueOf(6), 8, RoundingMode.HALF_UP).multiply(sr);
        BigDecimal term2 = k.subtract(BigDecimal.valueOf(3))
                           .divide(BigDecimal.valueOf(24), 8, RoundingMode.HALF_UP)
                           .multiply(sr.multiply(sr));

        BigDecimal modifier = BigDecimal.ONE.add(term1).subtract(term2);
        
        return sr.multiply(modifier).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算上涨和下跌捕获率
     *
     * @return [上涨捕获率, 下跌捕获率]
     */
    private BigDecimal[] calculateCaptureRatios(List<BigDecimal> strategyReturns, List<BigDecimal> benchmarkReturns) {
        if (strategyReturns == null || benchmarkReturns == null || 
            strategyReturns.size() != benchmarkReturns.size()) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }

        double upStrategySum = 0.0, upBenchmarkSum = 0.0;
        double downStrategySum = 0.0, downBenchmarkSum = 0.0;
        int upCount = 0, downCount = 0;

        for (int i = 0; i < strategyReturns.size(); i++) {
            double strategyReturn = strategyReturns.get(i).doubleValue();
            double benchmarkReturn = benchmarkReturns.get(i).doubleValue();

            if (benchmarkReturn > 0) {
                upStrategySum += strategyReturn;
                upBenchmarkSum += benchmarkReturn;
                upCount++;
            } else if (benchmarkReturn < 0) {
                downStrategySum += strategyReturn;
                downBenchmarkSum += benchmarkReturn;
                downCount++;
            }
        }

        BigDecimal uptrendCapture = BigDecimal.ZERO;
        BigDecimal downtrendCapture = BigDecimal.ZERO;

        if (upCount > 0 && upBenchmarkSum != 0) {
            uptrendCapture = BigDecimal.valueOf(upStrategySum / upBenchmarkSum).setScale(4, RoundingMode.HALF_UP);
        }

        if (downCount > 0 && downBenchmarkSum != 0) {
            downtrendCapture = BigDecimal.valueOf(downStrategySum / downBenchmarkSum).setScale(4, RoundingMode.HALF_UP);
        }

        return new BigDecimal[]{uptrendCapture, downtrendCapture};
    }

    /**
     * 计算最大回撤持续期 - 从峰值到恢复的最长时间（以交易日计算）
     */
    private BigDecimal calculateMaxDrawdownDuration(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        int maxDuration = 0;
        int currentDuration = 0;
        BigDecimal peak = prices.get(0);
        boolean inDrawdown = false;

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i);

            if (currentPrice.compareTo(peak) >= 0) {
                // 新高或恢复到峰值
                if (inDrawdown) {
                    maxDuration = Math.max(maxDuration, currentDuration);
                    inDrawdown = false;
                    currentDuration = 0;
                }
                peak = currentPrice;
            } else {
                // 在回撤中
                if (!inDrawdown) {
                    inDrawdown = true;
                    currentDuration = 1;
                } else {
                    currentDuration++;
                }
            }
        }

        // 如果结束时仍在回撤中
        if (inDrawdown) {
            maxDuration = Math.max(maxDuration, currentDuration);
        }

        return BigDecimal.valueOf(maxDuration);
    }

    /**
     * 计算痛苦指数 - 回撤深度与持续时间的综合指标
     */
    private BigDecimal calculatePainIndex(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        double totalPain = 0.0;
        BigDecimal peak = prices.get(0);

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i);
            
            if (currentPrice.compareTo(peak) > 0) {
                peak = currentPrice;
            } else {
                if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                // 计算回撤百分比
                BigDecimal drawdown = peak.subtract(currentPrice).divide(peak, 8, RoundingMode.HALF_UP);
                totalPain += drawdown.doubleValue();
                }
            }
        }

        // 平均痛苦指数
        return BigDecimal.valueOf(totalPain / prices.size()).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算风险调整收益 - 综合多种风险因素的收益评估指标
     * <p>
     * 风险调整收益是将策略的总收益除以综合风险因子得到的指标，
     * 用于评估策略在承担风险的前提下获得收益的效率。
     * <p>
     * 计算公式:
     * 风险调整收益 = 总收益 / (1 + 综合风险因子)
     * <p>
     * 综合风险因子构成:
     * - 波动率因子 (40%权重): 反映策略收益的不稳定性
     * - 最大回撤因子 (40%权重): 反映策略可能面临的最大损失
     * - 下行偏差因子 (20%权重): 反映策略的下行风险
     * <p>
     * 数值解读:
     * - 值越高，表示策略在控制风险的前提下获得收益的能力越强
     * - 值接近总收益率，表示策略风险较低
     * - 值远小于总收益率，表示策略承担了较高风险
     * <p>
     * 应用场景:
     * - 比较不同策略的风险调整后表现
     * - 评估策略是否值得承担相应风险
     * - 投资组合构建中的策略权重分配参考
     * 
     * @param totalReturn 策略总收益率
     * @param riskMetrics 风险指标集合
     * @return 风险调整收益，保留4位小数
     */
    private BigDecimal calculateRiskAdjustedReturn(BigDecimal totalReturn, RiskMetrics riskMetrics) {
        if (totalReturn == null) {
            return BigDecimal.ZERO;
        }

        // 风险调整收益 = 总收益 / (1 + 综合风险因子)
        // 综合风险因子考虑波动率、最大回撤、下行偏差等
        
        BigDecimal volatilityFactor = riskMetrics.volatility != null ? 
                riskMetrics.volatility.abs() : BigDecimal.ZERO;
        BigDecimal maxDrawdownFactor = tradeStats.maxDrawdown != null ? 
                tradeStats.maxDrawdown.abs() : BigDecimal.ZERO;
        BigDecimal downsideFactor = riskMetrics.downsideDeviation != null ? 
                riskMetrics.downsideDeviation.abs() : BigDecimal.ZERO;

        // 综合风险因子 = 0.4*波动率 + 0.4*最大回撤 + 0.2*下行偏差
        BigDecimal riskFactor = volatilityFactor.multiply(new BigDecimal("0.4"))
                .add(maxDrawdownFactor.multiply(new BigDecimal("0.4")))
                .add(downsideFactor.multiply(new BigDecimal("0.2")));

        BigDecimal denominator = BigDecimal.ONE.add(riskFactor);
        
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return totalReturn;
        }

        return totalReturn.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    /**
     * 年化收益率评分 - 基于真实数据分位数
     * 分位数分布: 10%(-5.08%) 25%(11.10%) 50%(26.35%) 75%(45.90%) 90%(63.82%)
     */
    private double calculateAnnualReturnScore(BigDecimal annualizedReturn) {
        if (annualizedReturn == null) return 0.0;

        double returnRate = annualizedReturn.doubleValue();

        if (returnRate >= 0.6382) {        // >= 90%分位数 (63.82%)
            return 10.0;
        } else if (returnRate >= 0.4590) { // 75%-90%分位数 (45.90%-63.82%)
            return 8.5 + (returnRate - 0.4590) / (0.6382 - 0.4590) * 1.5;
        } else if (returnRate >= 0.2635) { // 50%-75%分位数 (26.35%-45.90%)
            return 6.5 + (returnRate - 0.2635) / (0.4590 - 0.2635) * 2.0;
        } else if (returnRate >= 0.1110) { // 25%-50%分位数 (11.10%-26.35%)
            return 4.0 + (returnRate - 0.1110) / (0.2635 - 0.1110) * 2.5;
        } else if (returnRate >= -0.0508) { // 10%-25%分位数 (-5.08%-11.10%)
            return 1.5 + (returnRate + 0.0508) / (0.1110 + 0.0508) * 2.5;
        } else {                           // < 10%分位数 (< -5.08%)
            return Math.max(0.0, 1.5 + (returnRate + 0.0508) / 0.05 * 1.5);
        }
    }

    /**
     * 风险控制评分 - 基于最大回撤分位数 (越小越好)
     * 分位数分布: 10%(8.99%) 25%(16.55%) 50%(21.5%) 75%(26.02%) 90%(32.23%)
     */
    private double calculateRiskControlScore(BigDecimal maxDrawdown) {
        if (maxDrawdown == null) return 5.0; // 无数据时给中等分

        double drawdown = Math.abs(maxDrawdown.doubleValue());

        if (drawdown <= 0.0899) {          // <= 10%分位数 (8.99%)
            return 10.0;
        } else if (drawdown <= 0.1655) {   // 10%-25%分位数 (8.99%-16.55%)
            return 8.5 + (0.1655 - drawdown) / (0.1655 - 0.0899) * 1.5;
        } else if (drawdown <= 0.215) {    // 25%-50%分位数 (16.55%-21.5%)
            return 6.5 + (0.215 - drawdown) / (0.215 - 0.1655) * 2.0;
        } else if (drawdown <= 0.2602) {   // 50%-75%分位数 (21.5%-26.02%)
            return 4.0 + (0.2602 - drawdown) / (0.2602 - 0.215) * 2.5;
        } else if (drawdown <= 0.3223) {   // 75%-90%分位数 (26.02%-32.23%)
            return 1.5 + (0.3223 - drawdown) / (0.3223 - 0.2602) * 2.5;
        } else {                           // > 90%分位数 (> 32.23%)
            return Math.max(0.0, 1.5 - (drawdown - 0.3223) / 0.1 * 1.5);
        }
    }

    /**
     * 交易质量评分 - 基于胜率分位数
     * 分位数分布: 10%(25%) 25%(33.33%) 50%(46.15%) 75%(66.67%) 90%(100%)
     */
    private double calculateTradingQualityScore(BigDecimal winRate, BigDecimal profitFactor) {
        double score = 0.0;

        // 胜率评分 (权重70%)
        if (winRate != null) {
            double rate = winRate.doubleValue();

            if (rate >= 1.0) {              // >= 90%分位数 (100%)
                score += 10.0 * 0.7;
            } else if (rate >= 0.6667) {    // 75%-90%分位数 (66.67%-100%)
                score += (8.5 + (rate - 0.6667) / (1.0 - 0.6667) * 1.5) * 0.7;
            } else if (rate >= 0.4615) {    // 50%-75%分位数 (46.15%-66.67%)
                score += (6.5 + (rate - 0.4615) / (0.6667 - 0.4615) * 2.0) * 0.7;
            } else if (rate >= 0.3333) {    // 25%-50%分位数 (33.33%-46.15%)
                score += (4.0 + (rate - 0.3333) / (0.4615 - 0.3333) * 2.5) * 0.7;
            } else if (rate >= 0.25) {      // 10%-25%分位数 (25%-33.33%)
                score += (1.5 + (rate - 0.25) / (0.3333 - 0.25) * 2.5) * 0.7;
            } else {                        // < 10%分位数 (< 25%)
                score += Math.max(0.0, 1.5 * rate / 0.25) * 0.7;
            }
        }

        // 盈利因子评分 (权重30%) - 简化评分
        if (profitFactor != null) {
            double pf = profitFactor.doubleValue();
            if (pf >= 3.0) {
                score += 10.0 * 0.3;
            } else if (pf >= 2.0) {
                score += (6.0 + (pf - 2.0) / 1.0 * 4.0) * 0.3;
            } else if (pf >= 1.5) {
                score += (3.0 + (pf - 1.5) / 0.5 * 3.0) * 0.3;
            } else if (pf > 1.0) {
                score += ((pf - 1.0) / 0.5 * 3.0) * 0.3;
            }
        } else {
            score += 5.0 * 0.3; // 无数据时给中等分
        }

        return Math.min(10.0, score);
    }

    /**
     * 稳定性评分V2 - 简化版本
     */
    private double calculateStabilityScoreV2(RiskMetrics riskMetrics, TradeStatistics tradeStats) {
        double score = 0.0;

        // 1. 交易次数合理性 (权重50%)
        if (tradeStats.tradeCount >= 5) {
            score += 10.0 * 0.5; // 交易次数充分
        } else if (tradeStats.tradeCount >= 2) {
            score += 7.0 * 0.5;  // 交易次数适中
        } else {
            score += 4.0 * 0.5;  // 交易次数偏少但可接受
        }

        // 2. 波动率控制 (权重30%)
        if (riskMetrics.volatility != null) {
            double volatility = riskMetrics.volatility.doubleValue();
            if (volatility <= 0.2) {        // 低波动率
                score += 10.0 * 0.3;
            } else if (volatility <= 0.4) { // 中等波动率
                score += (8.0 - (volatility - 0.2) / 0.2 * 3.0) * 0.3;
            } else {                        // 高波动率
                score += Math.max(2.0, 5.0 - (volatility - 0.4) * 5) * 0.3;
            }
        } else {
            score += 6.0 * 0.3; // 无数据时给中等分
        }

        // 3. 偏度控制 (权重20%) - 收益分布对称性
        if (riskMetrics.skewness != null) {
            double absSkewness = Math.abs(riskMetrics.skewness.doubleValue());
            if (absSkewness <= 1.0) {
                score += (10.0 - absSkewness * 3.0) * 0.2;
            } else {
                score += Math.max(2.0, 7.0 - absSkewness * 2.0) * 0.2;
            }
        } else {
            score += 6.0 * 0.2;
        }

        return Math.min(10.0, score);
    }


    // ====================== 辅助计算方法 ======================

    /**
     * 计算平均最大回撤
     */
    private BigDecimal calculateAverageMaxDrawdown(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> drawdowns = new ArrayList<>();
        BigDecimal peak = prices.get(0);

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i);

            if (currentPrice.compareTo(peak) > 0) {
                peak = currentPrice;
            } else {
                if (peak.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal drawdown = peak.subtract(currentPrice).divide(peak, 8, RoundingMode.HALF_UP);
                    drawdowns.add(drawdown);
                }

            }
        }

        if (drawdowns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = drawdowns.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(drawdowns.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算平方根回撤
     */
    private BigDecimal calculateSquareRootDrawdown(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        double sumSquaredDrawdowns = 0.0;
        int drawdownCount = 0;
        BigDecimal peak = prices.get(0);

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i);

            if (currentPrice.compareTo(peak) > 0) {
                peak = currentPrice;
            } else {
                if (peak.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal drawdown = peak.subtract(currentPrice).divide(peak, 8, RoundingMode.HALF_UP);
                    sumSquaredDrawdowns += Math.pow(drawdown.doubleValue(), 2);
                    drawdownCount++;
                }

            }
        }

        if (drawdownCount == 0) {
            return BigDecimal.ZERO;
        }

        double avgSquaredDrawdown = sumSquaredDrawdowns / drawdownCount;
        return BigDecimal.valueOf(Math.sqrt(avgSquaredDrawdown)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 将收益率序列转换为策略资金曲线
     *
     * @param returns       收益率序列（对数收益率或简单收益率）
     * @param initialAmount 初始金额
     * @return 策略资金曲线（累积价值序列）
     */
    private List<BigDecimal> convertReturnsToEquityCurve(List<BigDecimal> returns, BigDecimal initialAmount) {
        List<BigDecimal> equityCurve = new ArrayList<>();
        if (returns == null || returns.isEmpty()) {
            equityCurve.add(initialAmount);
            return equityCurve;
        }
        // 第一个点是初始金
        BigDecimal currentValue = initialAmount;
        equityCurve.add(currentValue);
        // 根据收益率序列计算累积资金曲线
        for (BigDecimal dailyReturn : returns) {
            if (dailyReturn == null) {
                dailyReturn = BigDecimal.ZERO;
            }
            // 对于对数收益率，使用指数函数还原equity(t) = equity(t-1) * exp(log_return)
            double returnRate = dailyReturn.doubleValue();
            if (returnRate != 0) {
                currentValue = currentValue.multiply(BigDecimal.valueOf(Math.exp(returnRate)));
            }
            equityCurve.add(currentValue);
        }
        return equityCurve;
    }

    /**
     * 计算包含手续费的真实策略资金曲线（基于实际交易记录）
     * 该方法将计算每一天的实际资金价值，包含手续费的影响
     */
    private List<BigDecimal> calculateRealStrategyEquityCurve() {
        List<BigDecimal> equityCurve = new ArrayList<>();
        if (series == null || series.getBarCount() < 2) {
            equityCurve.add(initialAmount);
            return equityCurve;
        }

        // 如果没有交易记录，整个期间都是初始金额
        if (tradingRecord == null || tradingRecord.getPositionCount() == 0 || tradeRecords.isEmpty()) {
            for (int i = 0; i < series.getBarCount(); i++) {
                equityCurve.add(initialAmount);
            }
            return equityCurve;
        }

        // 创建一个简化的方法：基于交易完成时点来构建资金曲线
        // 第一天是初始金额
        equityCurve.add(initialAmount);

        // 创建交易时间到金额的映射
        Map<LocalDateTime, BigDecimal> tradeAmounts = new HashMap<>();
        BigDecimal currentAmount = initialAmount;

        // 按时间顺序处理所有交易，记录每笔交易完成后的金额
        for (TradeRecordDTO trade : tradeRecords) {
            currentAmount = trade.getExitAmount(); // 交易完成后的金额（已扣除手续费）
            tradeAmounts.put(trade.getExitTime(), currentAmount);
        }

        // 从第二天开始，逐日构建资金曲线
        BigDecimal latestAmount = initialAmount;

        for (int i = 1; i < series.getBarCount(); i++) {
            LocalDateTime barTime = series.getBar(i).getEndTime().toLocalDateTime();

            // 检查这一天是否有交易完成
            if (tradeAmounts.containsKey(barTime)) {
                latestAmount = tradeAmounts.get(barTime);
            }
            // 如果这一天没有交易完成，检查是否在持仓期间
            else {
                // 查找是否处于某个交易的持仓期间
                BigDecimal dailyValue = calculateDailyValueInPosition(barTime, latestAmount);
                if (dailyValue != null) {
                    latestAmount = dailyValue;
                }
                // 如果不在持仓期间，保持上一个金额
            }

            equityCurve.add(latestAmount);
        }

        // 验证最终金额是否与实际交易收益一致
        BigDecimal expectedFinalAmount = tradeRecords.get(tradeRecords.size() - 1).getExitAmount();
        BigDecimal actualFinalAmount = equityCurve.get(equityCurve.size() - 1);
        if (!tradeRecords.isEmpty()) {
            if (expectedFinalAmount.subtract(actualFinalAmount).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                log.warn("策略资金曲线最终金额与实际交易收益不一致！预期: {}, 实际: {}",
                        expectedFinalAmount.setScale(4, RoundingMode.HALF_UP), actualFinalAmount.setScale(4, RoundingMode.HALF_UP));
                // 修正最终金额
                equityCurve.set(equityCurve.size() - 1, expectedFinalAmount);
            } else {
                log.info("策略资金曲线最终金额与实际交易收益一致！预期: {}, 实际: {}", expectedFinalAmount.setScale(4, RoundingMode.HALF_UP), actualFinalAmount.setScale(4, RoundingMode.HALF_UP));
            }
        }

        return equityCurve;
    }

    /**
     * 计算持仓期间某一天的资金价值
     */
    private BigDecimal calculateDailyValueInPosition(LocalDateTime targetTime, BigDecimal baseAmount) {
        // 查找包含目标时间的交易
        for (TradeRecordDTO trade : tradeRecords) {
            if (!targetTime.isBefore(trade.getEntryTime()) && !targetTime.isAfter(trade.getExitTime())) {
                // 在持仓期间，根据价格变动计算价值
                BigDecimal entryPrice = trade.getEntryPrice();

                // 找到目标时间对应的价格
                for (int i = 0; i < series.getBarCount(); i++) {
                    LocalDateTime barTime = series.getBar(i).getEndTime().toLocalDateTime();
                    if (barTime.equals(targetTime)) {
                        BigDecimal currentPrice = BigDecimal.valueOf(series.getBar(i).getClosePrice().doubleValue());

                        // 计算入场时的实际交易金额（扣除手续费）
                        BigDecimal entryAmount = trade.getEntryAmount();
                        BigDecimal entryFee = entryAmount.multiply(feeRatio);
                        BigDecimal actualTradeAmount = entryAmount.subtract(entryFee);

                        // 根据价格变动计算当前持仓价值
                        BigDecimal priceRatio = currentPrice.divide(entryPrice, 10, RoundingMode.HALF_UP);
                        return actualTradeAmount.multiply(priceRatio);
                    }
                }
            }
        }
        return null; // 不在任何持仓期间
    }

    /**
     * 动态检测年化因子
     * 根据BarSeries的时间间隔自动检测合适的年化因子
     */
    private int detectAnnualizationFactor(BarSeries series) {
        if (series == null || series.getBarCount() < 2) {
            return 252; // 默认日级别
        }

        try {
            // 获取前两个Bar的时间间隔
            long minutesBetween = parseIntervalToMinutes(interval);

            // 根据时间间隔判断数据周期
            if (minutesBetween <= 1) {
                // 1分钟级别: 1年 = 365天 * 24小时 * 60分钟 = 525,600
                return 525600;
            } else if (minutesBetween <= 5) {
                // 5分钟级别: 525,600 / 5 = 105,120
                return 105120;
            } else if (minutesBetween <= 15) {
                // 15分钟级别: 525,600 / 15 = 35,040
                return 35040;
            } else if (minutesBetween <= 30) {
                // 30分钟级别: 525,600 / 30 = 17,520
                return 17520;
            } else if (minutesBetween <= 60) {
                // 1小时级别: 365天 * 24小时 = 8,760
                return 8760;
            } else if (minutesBetween <= 240) {
                // 4小时级别: 8,760 / 4 = 2,190
                return 2190;
            } else if (minutesBetween <= 360) {
                // 6小时级别: 8,760 / 6 = 1,460
                return 1460;
            } else if (minutesBetween <= 720) {
                // 12小时级别: 8,760 / 12 = 730
                return 730;
            } else if (minutesBetween <= 1440) {
                // 1天级别: 365天
                return 365;
            } else if (minutesBetween <= 10080) {
                // 1周级别: 52周
                return 52;
            } else {
                // 1月级别: 12个月
                return 12;
            }
        } catch (Exception e) {
            log.warn("检测年化因子时出错，使用默认值252: {}", e.getMessage());
            return 252; // 出错时使用默认日级别
        }
    }

    /**
     * 计算波动率（基于收盘价）
     */
    private BigDecimal calculateVolatility(BarSeries series, int annualizationFactor) {
        if (series == null || series.getBarCount() < 2) {
            return BigDecimal.ZERO;
        }

        // 收集所有收盘价
        List<BigDecimal> closePrices = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            closePrices.add(BigDecimal.valueOf(closePrice));
        }

        // 计算收盘价的对数收益率
        List<BigDecimal> logReturns = new ArrayList<>();
        for (int i = 1; i < closePrices.size(); i++) {
            BigDecimal today = closePrices.get(i);
            BigDecimal yesterday = closePrices.get(i - 1);

            if (yesterday.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            double logReturn = Math.log(today.doubleValue() / yesterday.doubleValue());
            logReturns.add(BigDecimal.valueOf(logReturn));
        }

        if (logReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算对数收益率的平均值
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal r : logReturns) {
            sum = sum.add(r);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(logReturns.size()), 10, RoundingMode.HALF_UP);

        // 计算对数收益率的方差
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal r : logReturns) {
            BigDecimal diff = r.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(logReturns.size()), 10, RoundingMode.HALF_UP);

        // 计算标准差（波动率）
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // 年化波动率（使用动态年化因子）
        BigDecimal annualizedVolatility = stdDev.multiply(BigDecimal.valueOf(Math.sqrt(annualizationFactor)));

        return annualizedVolatility.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算年化收益率
     */
    private BigDecimal calculateAnnualizedReturn(BigDecimal totalReturn, LocalDateTime startTime, LocalDateTime endTime) {
        if (totalReturn == null || startTime == null || endTime == null || startTime.isAfter(endTime)) {
            log.warn("计算年化收益率的参数无效");
            return BigDecimal.ZERO;
        }

        // 计算回测持续的天数
        long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);

        // 避免除以零错误
        if (daysBetween <= 0) {
            return totalReturn; // 如果时间跨度小于1天，直接返回总收益率
        }

        // 计算年化收益率: (1 + totalReturn)^(365/daysBetween) - 1
        BigDecimal base = BigDecimal.ONE.add(totalReturn);
        BigDecimal exponent = new BigDecimal("365").divide(new BigDecimal(daysBetween), 8, RoundingMode.HALF_UP);

        BigDecimal result;
        try {
            double baseDouble = base.doubleValue();
            double exponentDouble = exponent.doubleValue();
            double power = Math.pow(baseDouble, exponentDouble);

            result = new BigDecimal(power).subtract(BigDecimal.ONE);
        } catch (Exception e) {
            log.error("计算年化收益率时出错", e);
            return BigDecimal.ZERO;
        }

        return result;
    }

    /**
     * 计算 Alpha 和 Beta
     * Alpha 表示策略超额收益，Beta 表示策略相对于基准收益的敏感度（风险）
     *
     * @param strategyReturns  策略每日收益率序列
     * @param benchmarkReturns 基准每日收益率序列
     * @return 包含Alpha和Beta的数组 [Alpha, Beta]
     */
    public static BigDecimal[] calculateAlphaBeta(List<BigDecimal> strategyReturns, List<CandlestickEntity> benchmarkCandlesticks) {

        List<BigDecimal> benchmarkPriceList = benchmarkCandlesticks.stream().map(CandlestickEntity::getClose).collect(Collectors.toList());
        List<BigDecimal> benchmarkReturns = new ArrayList<>();
        benchmarkReturns.add(BigDecimal.ZERO);
        for (int i = 1; i < benchmarkPriceList.size(); i++) {
            // 使用对数收益率保持与策略收益率计算的一致性
            double logReturn = Math.log(benchmarkPriceList.get(i).doubleValue() / benchmarkPriceList.get(i - 1).doubleValue());
            benchmarkReturns.add(BigDecimal.valueOf(logReturn));
        }

        // 添加空值检查和长度验证，避免抛出异常
        if (strategyReturns == null || strategyReturns.isEmpty()) {
            System.out.println("策略收益率序列为空，返回默认Alpha=0, Beta=1");
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ONE};
        }

        if (benchmarkReturns == null || benchmarkReturns.isEmpty()) {
            System.out.println("基准收益率序列为空，返回默认Alpha=0, Beta=1");
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ONE};
        }

        // 如果长度不匹配，取较短的长度，避免抛出异常
        int minLength = Math.min(strategyReturns.size(), benchmarkReturns.size());
        if (minLength == 0) {
            System.out.println("收益率序列长度为0，返回默认Alpha=0, Beta=1");
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ONE};
        }

        // 截取到相同长度，确保不会出现长度不匹配问题
        List<BigDecimal> adjustedStrategyReturns = strategyReturns.subList(0, minLength);
        List<BigDecimal> adjustedBenchmarkReturns = benchmarkReturns.subList(0, minLength);

        System.out.println("计算Alpha/Beta: 策略收益率数量=" + adjustedStrategyReturns.size() + ", 基准收益率数量=" + adjustedBenchmarkReturns.size());

        int n = adjustedStrategyReturns.size();

        // 计算策略和基准的平均收益率
        double meanStrategy = adjustedStrategyReturns.stream().mapToDouble(d -> d.doubleValue()).average().orElse(0.0);
        double meanBenchmark = adjustedBenchmarkReturns.stream().mapToDouble(d -> d.doubleValue()).average().orElse(0.0);

        double covariance = 0.0;        // 协方差 numerator部分
        double varianceBenchmark = 0.0; // 基准收益率的方差 denominator部分

        // 计算协方差和基准方差
        for (int i = 0; i < n; i++) {
            double sDiff = adjustedStrategyReturns.get(i).doubleValue() - meanStrategy;
            double bDiff = adjustedBenchmarkReturns.get(i).doubleValue() - meanBenchmark;

            covariance += sDiff * bDiff;
            varianceBenchmark += bDiff * bDiff;
        }

        covariance /= n;        // 求平均协方差
        varianceBenchmark /= n; // 求平均方差

        // 防止除以0
        double beta = varianceBenchmark == 0 ? 0 : covariance / varianceBenchmark;

        // Alpha = 策略平均收益 - Beta * 基准平均收益
        double alpha = meanStrategy - beta * meanBenchmark;

        return new BigDecimal[]{BigDecimal.valueOf(alpha), BigDecimal.valueOf(beta)};
    }


    /**
     * 计算全周期策略收益率序列
     */
    private List<BigDecimal> calculateFullPeriodStrategyReturns(BarSeries series, TradingRecord tradingRecord, boolean useLogReturn) {
        List<BigDecimal> returns = new ArrayList<>();

        if (series == null || series.getBarCount() < 2) {
            return returns;
        }

        // 如果没有交易记录，整个期间都是0收益
        if (tradingRecord == null || tradingRecord.getPositionCount() == 0) {
            for (int i = 1; i < series.getBarCount(); i++) {
                returns.add(BigDecimal.ZERO);
            }
            return returns;
        }

        // 创建持仓期间标记数组
        boolean[] isInPosition = new boolean[series.getBarCount()];
        boolean[] isEntryDay = new boolean[series.getBarCount()];
        boolean[] isExitDay = new boolean[series.getBarCount()];

        // 标记所有持仓期间、买入日和卖出日
        for (Position position : tradingRecord.getPositions()) {
            if (position.isClosed()) {
                int entryIndex = position.getEntry().getIndex();
                int exitIndex = position.getExit().getIndex();

                // 标记买入日和卖出日
                if (entryIndex < isEntryDay.length) {
                    isEntryDay[entryIndex] = true;
                }
                if (exitIndex < isExitDay.length) {
                    isExitDay[exitIndex] = true;
                }

                // 从入场时间点到出场时间点都标记为持仓状态
                for (int i = entryIndex; i <= exitIndex; i++) {
                    if (i < isInPosition.length) {
                        isInPosition[i] = true;
                    }
                }
            }
        }

        // 计算每个时间点的收益率
        for (int i = 0; i < series.getBarCount(); i++) {
            BigDecimal dailyReturn = BigDecimal.ZERO;

            // 边界条件1：持仓第一天（买入日）收益率为0，因为只是买入，没有收益
            if (isEntryDay[i]) {
                dailyReturn = BigDecimal.ZERO;
            }
            // 边界条件2：卖出日的后一天收益率为0（已经没有持仓）
            else if (i > 0 && isExitDay[i - 1]) {
                dailyReturn = BigDecimal.ZERO;
            }
            // 正常持仓期间：计算价格收益率（排除买入日）
            else if (isInPosition[i] && !isEntryDay[i]) {
                BigDecimal today = BigDecimal.valueOf(series.getBar(i).getClosePrice().doubleValue());
                BigDecimal yesterday = BigDecimal.valueOf(series.getBar(i - 1).getClosePrice().doubleValue());

                if (yesterday.compareTo(BigDecimal.ZERO) > 0) {
                    if (useLogReturn) {
                        double logR = Math.log(today.doubleValue() / yesterday.doubleValue());
                        dailyReturn = BigDecimal.valueOf(logR);
            } else {
                        BigDecimal change = today.subtract(yesterday).divide(yesterday, 10, RoundingMode.HALF_UP);
                        dailyReturn = change;
                    }
                } else {
                    dailyReturn = BigDecimal.ZERO;
                }
            }
            // 未持仓期间：收益率为0
            else {
                dailyReturn = BigDecimal.ZERO;
            }

            returns.add(dailyReturn);
        }

        return returns;
    }


}

