package com.okx.trading.service.impl;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.BacktestEquityCurveEntity;
import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.repository.BacktestEquityCurveRepository;
import com.okx.trading.repository.BacktestSummaryRepository;
import com.okx.trading.repository.BacktestTradeRepository;
import com.okx.trading.service.BacktestTradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 回测交易服务实现类
 */
@Service
public class BacktestTradeServiceImpl implements BacktestTradeService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestTradeServiceImpl.class);

    private final BacktestTradeRepository backtestTradeRepository;
    private final BacktestSummaryRepository backtestSummaryRepository;
    private final BacktestEquityCurveRepository backtestEquityCurveRepository;
    private Ta4jBacktestService ta4jBacktestService;

    @Autowired
    public BacktestTradeServiceImpl(BacktestTradeRepository backtestTradeRepository,
                                    BacktestSummaryRepository backtestSummaryRepository,
                                    BacktestEquityCurveRepository backtestEquityCurveRepository) {
        this.backtestTradeRepository = backtestTradeRepository;
        this.backtestSummaryRepository = backtestSummaryRepository;
        this.backtestEquityCurveRepository = backtestEquityCurveRepository;
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

        for (TradeRecordDTO trade : trades) {

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
                    .periods(trade.getPeriods())
                    .profitPercentagePerPeriod(trade.getProfitPercentagePerPeriod())
                    .totalAssets(trade.getExitAmount())
                    .maxDrawdown(trade.getMaxDrowdown())
                    .maxLoss(trade.getMaxLoss())
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
                .annualizedReturn(backtestResult.getAnnualizedReturn())
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
                .volatility(backtestResult.getVolatility())
                .totalFee(backtestResult.getTotalFee())
                // 新增指标字段
                .omega(backtestResult.getOmega())
                .alpha(backtestResult.getAlpha())
                .beta(backtestResult.getBeta())
                .treynorRatio(backtestResult.getTreynorRatio())
                .ulcerIndex(backtestResult.getUlcerIndex())
                .skewness(backtestResult.getSkewness())
                .profitFactor(backtestResult.getProfitFactor())
                .comprehensiveScore(backtestResult.getComprehensiveScore())
                // 新增高级风险指标
                .kurtosis(backtestResult.getKurtosis())
                .cvar(backtestResult.getCvar())
                .var95(backtestResult.getVar95())
                .var99(backtestResult.getVar99())
                .informationRatio(backtestResult.getInformationRatio())
                .trackingError(backtestResult.getTrackingError())
                .sterlingRatio(backtestResult.getSterlingRatio())
                .burkeRatio(backtestResult.getBurkeRatio())
                .modifiedSharpeRatio(backtestResult.getModifiedSharpeRatio())
                .downsideDeviation(backtestResult.getDownsideDeviation())
                .uptrendCapture(backtestResult.getUptrendCapture())
                .downtrendCapture(backtestResult.getDowntrendCapture())
                .maxDrawdownDuration(backtestResult.getMaxDrawdownDuration())
                .painIndex(backtestResult.getPainIndex())
                .riskAdjustedReturn(backtestResult.getRiskAdjustedReturn())
                .build();

        // 保存汇总信息
        BacktestSummaryEntity savedEntity = backtestSummaryRepository.save(summaryEntity);
        logger.info("成功保存回测汇总信息，回测ID: {}, 批量回测ID: {}", backtestId, batchBacktestId);

        // 打印详细的汇总信息
        com.okx.trading.util.BacktestResultPrinter.printSummaryEntity(savedEntity);

        return savedEntity;
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

    @Override
    @Transactional
    public void saveBacktestEquityCurve(String backtestId, List<BigDecimal> equityCurveData, List<LocalDateTime> timestamps) {
        if (backtestId == null || backtestId.isEmpty() || equityCurveData == null || equityCurveData.isEmpty() || 
            timestamps == null || timestamps.isEmpty() || equityCurveData.size() != timestamps.size()) {
            logger.warn("保存资金曲线数据失败：参数无效");
            return;
        }

        // 先删除已有数据
        backtestEquityCurveRepository.deleteByBacktestId(backtestId);

        // 批量保存数据
        List<BacktestEquityCurveEntity> entities = IntStream.range(0, equityCurveData.size())
                .mapToObj(i -> BacktestEquityCurveEntity.builder()
                        .backtestId(backtestId)
                        .equityValue(equityCurveData.get(i))
                        .timestamp(timestamps.get(i))
                        .indexPosition(i)
                        .build())
                .collect(Collectors.toList());

        backtestEquityCurveRepository.saveAll(entities);
        logger.info("成功保存回测资金曲线数据，回测ID: {}, 数据点数: {}", backtestId, equityCurveData.size());
    }

    @Override
    public List<BacktestEquityCurveEntity> getEquityCurveByBacktestId(String backtestId) {
        return backtestEquityCurveRepository.findByBacktestIdOrderByTimestampAsc(backtestId);
    }
}
