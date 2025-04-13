package com.okx.trading.service.impl;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.dto.TradeRecordDTO;
import com.okx.trading.model.entity.BacktestTradeEntity;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 回测交易服务实现类
 */
@Service
public class BacktestTradeServiceImpl implements BacktestTradeService {
    
    private static final Logger logger = LoggerFactory.getLogger(BacktestTradeServiceImpl.class);
    
    private final BacktestTradeRepository backtestTradeRepository;
    
    @Autowired
    public BacktestTradeServiceImpl(BacktestTradeRepository backtestTradeRepository) {
        this.backtestTradeRepository = backtestTradeRepository;
    }
    
    @Override
    @Transactional
    public String saveBacktestTrades(BacktestResultDTO backtestResult, String strategyParams) {
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
        
        // 先从第一个交易记录中获取交易对信息
        String symbol = trades.get(0).getType().contains("BTC") ? "BTC-USDT" : "ETH-USDT";
        
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
                    .build();
            
            backtestTradeRepository.save(entity);
        }
        
        logger.info("成功保存回测记录，回测ID: {}, 交易数量: {}", backtestId, trades.size());
        return backtestId;
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
        backtestTradeRepository.deleteByBacktestId(backtestId);
        logger.info("已删除回测ID为 {} 的所有记录", backtestId);
    }
} 