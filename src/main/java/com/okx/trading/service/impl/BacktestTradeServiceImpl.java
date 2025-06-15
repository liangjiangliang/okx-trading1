package com.okx.trading.service.impl;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.repository.BacktestSummaryRepository;
import com.okx.trading.repository.BacktestTradeRepository;
import com.okx.trading.service.BacktestTradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 回测交易服务实现类
 */
@Service
public class BacktestTradeServiceImpl implements BacktestTradeService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestTradeServiceImpl.class);

    private final BacktestTradeRepository backtestTradeRepository;
    private final BacktestSummaryRepository backtestSummaryRepository;

    @Autowired
    public BacktestTradeServiceImpl(BacktestTradeRepository backtestTradeRepository,
                                    BacktestSummaryRepository backtestSummaryRepository) {
        this.backtestTradeRepository = backtestTradeRepository;
        this.backtestSummaryRepository = backtestSummaryRepository;
    }

    @Override
    @Transactional
    public String saveBacktestTrades(String symbol, BacktestResultDTO backtestResult, String strategyParams) {
        if (backtestResult == null || !backtestResult.isSuccess()) {
            logger.warn("尝试保存无效的回测结果");
            return null;
        }

        // 生成唯一回测ID
        String backtestId = UUID.randomUUID().toString();

        List<TradeRecordDTO> trades = backtestResult.getTrades();
        if (trades == null || trades.isEmpty()) {
            logger.info("回测结果中没有交易记录");
            return backtestId;
        }

        // 计算每次交易后的资产总值和最大回撤
        BigDecimal initialAmount = backtestResult.getInitialAmount();
        BigDecimal highestValue = initialAmount;
        BigDecimal currentValue = initialAmount;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (TradeRecordDTO trade : trades) {
            // 更新当前价值
            if (trade.getProfit() != null) {
                currentValue = currentValue.add(trade.getProfit());
            }

            // 更新历史最高价值
            if (currentValue.compareTo(highestValue) > 0) {
                highestValue = currentValue;
            }

            // 计算当前回撤
            if (highestValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDrawdown = highestValue.subtract(currentValue)
                        .divide(highestValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                if (currentDrawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = currentDrawdown;
                }
            }

            BacktestTradeEntity entity = BacktestTradeEntity.builder()
                    .backtestId(backtestId)
                    .strategyName(backtestResult.getStrategyName())
                    .strategyCode(backtestResult.getStrategyCode())
                    .strategyParams(strategyParams)
                    .index(trade.getIndex())
                    .type(trade.getType())
                    .symbol(symbol)
                    .entryTime(trade.getEntryTime())
                    .entryPrice(trade.getEntryPrice())
                    .entryAmount(trade.getEntryAmount())
                    .exitTime(trade.getExitTime())
                    .exitPrice(trade.getExitPrice())
                    .exitAmount(trade.getExitAmount())
                    .profit(trade.getProfit())
                    .profitPercentage(trade.getProfitPercentage())
                    .totalAssets(currentValue)
                    .maxDrawdown(maxDrawdown)
                    .closed(trade.isClosed())
                    .fee(trade.getFee())
                    .build();

            backtestTradeRepository.save(entity);
        }

        logger.info("成功保存回测记录，回测ID: {}, 交易数量: {}", backtestId, trades.size());
        return backtestId;
    }

    @Override
    @Transactional
    public BacktestSummaryEntity saveBacktestSummary(BacktestResultDTO backtestResult,
                                                     String strategyParams,
                                                     String symbol,
                                                     String interval,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     String backtestId) {
        return saveBacktestSummary(backtestResult, strategyParams, symbol, interval, startTime, endTime, backtestId, null);
    }

    @Override
    @Transactional
    public BacktestSummaryEntity saveBacktestSummary(BacktestResultDTO backtestResult,
                                                     String strategyParams,
                                                     String symbol,
                                                     String interval,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     String backtestId,
                                                     String batchBacktestId) {
        if (backtestResult == null || !backtestResult.isSuccess()) {
            logger.warn("尝试保存无效的回测汇总结果");
            return null;
        }

        // 如果没有提供backtestId，则生成一个新的
        if (backtestId == null || backtestId.isEmpty()) {
            backtestId = UUID.randomUUID().toString();
        }

        // 计算年化收益率
        BigDecimal annualizedReturn = calculateAnnualizedReturn(
            backtestResult.getTotalReturn(),
            startTime,
            endTime
        );

        // 创建汇总实体
        BacktestSummaryEntity summaryEntity = BacktestSummaryEntity.builder()
                .backtestId(backtestId)
                .batchBacktestId(batchBacktestId)
                .strategyName(backtestResult.getStrategyName())
                .strategyCode(backtestResult.getStrategyCode())
                .strategyParams(strategyParams)
                .symbol(symbol)
                .intervalVal(interval)
                .startTime(startTime)
                .endTime(endTime)
                .initialAmount(backtestResult.getInitialAmount())
                .finalAmount(backtestResult.getFinalAmount())
                .totalProfit(backtestResult.getTotalProfit())
                .totalReturn(backtestResult.getTotalReturn())
                .annualizedReturn(annualizedReturn)
                .numberOfTrades(backtestResult.getNumberOfTrades())
                .profitableTrades(backtestResult.getProfitableTrades())
                .unprofitableTrades(backtestResult.getUnprofitableTrades())
                .winRate(backtestResult.getWinRate())
                .averageProfit(backtestResult.getAverageProfit())
                .maxDrawdown(backtestResult.getMaxDrawdown())
                .sharpeRatio(backtestResult.getSharpeRatio())
                .sortinoRatio(backtestResult.getSortinoRatio())
                .calmarRatio(backtestResult.getCalmarRatio())
                .maximumLoss(backtestResult.getMaximumLoss())
                .totalFee(backtestResult.getTotalFee())
                .build();

        // 保存汇总信息
        BacktestSummaryEntity savedEntity = backtestSummaryRepository.save(summaryEntity);
        logger.info("成功保存回测汇总信息，回测ID: {}, 批量回测ID: {}", backtestId, batchBacktestId);

        // 打印详细的汇总信息
        com.okx.trading.util.BacktestResultPrinter.printSummaryEntity(savedEntity);

        return savedEntity;
    }

    /**
     * 计算年化收益率
     *
     * @param totalReturn 总收益率（百分比）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 年化收益率（百分比）
     */
    private BigDecimal calculateAnnualizedReturn(BigDecimal totalReturn, LocalDateTime startTime, LocalDateTime endTime) {
        if (totalReturn == null || startTime == null || endTime == null || startTime.isAfter(endTime)) {
            logger.warn("计算年化收益率的参数无效");
            return BigDecimal.ZERO;
        }
//        long days = ChronoUnit.DAYS.between(startDate, endDate);
//        if (days <= 0 || totalReturn <= 0) return 0.0;
//
//        double ratio = 1 + totalReturn; // 总收益率 + 1
//        double years = 365.0 / days;
//        return Math.pow(ratio, years) - 1;

        // 计算回测持续的天数
        long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);

        // 避免除以零错误
        if (daysBetween <= 0) {
            return totalReturn; // 如果时间跨度小于1天，直接返回总收益率
        }

        // 计算年化收益率: (1 + totalReturn/100)^(365/daysBetween) - 1

        // 计算(1 + returnRate)
        BigDecimal base = BigDecimal.ONE.add(totalReturn);

        // 计算指数(365/daysBetween)
        BigDecimal exponent = new BigDecimal("365").divide(new BigDecimal(daysBetween), 8, RoundingMode.HALF_UP);

        // 计算(1 + returnRate)^(365/daysBetween)
        // 使用对数计算幂: exp(exponent * ln(base))
        BigDecimal result;
        try {
            double baseDouble = base.doubleValue();
            double exponentDouble = exponent.doubleValue();
            double power = Math.pow(baseDouble, exponentDouble);

            // 转换回BigDecimal并减去1
            result = new BigDecimal(power).subtract(BigDecimal.ONE);

        } catch (Exception e) {
            logger.error("计算年化收益率时出错", e);
            return BigDecimal.ZERO;
        }

        return result;
    }

    @Override
    public List<BacktestTradeEntity> getTradesByBacktestId(String backtestId) {
        return backtestTradeRepository.findByBacktestIdOrderByIndexAsc(backtestId);
    }

    @Override
    public double getMaxDrawdown(String backtestId) {
        BigDecimal maxDrawdown = backtestTradeRepository.findMaxDrawdownByBacktestId(backtestId);
        return maxDrawdown != null ? maxDrawdown.doubleValue() : 0.0;
    }

    @Override
    public List<String> getAllBacktestIds() {
        // 这里我们需要自定义查询来获取所有唯一的backtestId
        return backtestTradeRepository.findAll().stream()
                .map(BacktestTradeEntity::getBacktestId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBacktestRecords(String backtestId) {
        // 同时删除交易明细和汇总信息
        backtestTradeRepository.deleteByBacktestId(backtestId);
        backtestSummaryRepository.deleteByBacktestId(backtestId);
        logger.info("已删除回测ID为 {} 的所有记录", backtestId);
    }

    @Override
    public List<BacktestSummaryEntity> getAllBacktestSummaries() {
        return backtestSummaryRepository.findAll();
    }

    @Override
    public Optional<BacktestSummaryEntity> getBacktestSummaryById(String backtestId) {
        return backtestSummaryRepository.findByBacktestId(backtestId);
    }

    @Override
    public List<BacktestSummaryEntity> getBacktestSummariesByStrategy(String strategyName) {
        return backtestSummaryRepository.findByStrategyNameOrderByCreateTimeDesc(strategyName);
    }

    @Override
    public List<BacktestSummaryEntity> getBacktestSummariesBySymbol(String symbol) {
        return backtestSummaryRepository.findBySymbolOrderByCreateTimeDesc(symbol);
    }

    @Override
    public List<BacktestSummaryEntity> getBestPerformingBacktests() {
        return backtestSummaryRepository.findBestPerformingBacktests();
    }

    @Override
    public List<BacktestSummaryEntity> getBacktestSummariesByBatchId(String batchBacktestId) {
        return backtestSummaryRepository.findByBatchBacktestIdOrderByTotalReturnDesc(batchBacktestId);
    }
}
