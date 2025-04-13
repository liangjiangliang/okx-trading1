package com.okx.trading.service.impl;

import com.okx.trading.model.dto.BollingerBandsDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.MarketDataService;
import com.okx.trading.util.TechnicalIndicatorUtil;
import com.okx.trading.util.TechnicalIndicatorUtil.BollingerBands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 市场数据服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    private final HistoricalDataService historicalDataService;

    /**
     * 获取特定交易对的布林带数据
     *
     * @param symbol   交易对,如 BTC-USDT
     * @param interval K线间隔,如 1m, 5m, 15m, 1h, 4h, 1d
     * @param period   布林带周期,默认20
     * @param stdDev   标准差倍数,默认2
     * @param limit    返回数据数量,默认500
     * @return 布林带数据列表
     */
    @Override
    public List<BollingerBandsDTO> getBollingerBandsData(String symbol, String interval,
                                                        Integer period, Double stdDev, Integer limit) {
        // 参数处理
        int actualPeriod = period != null ? period : 20;
        double actualStdDev = stdDev != null ? stdDev : 2.0;
        int actualLimit = limit != null ? limit : 500;
        
        // 获取历史K线数据
        List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalDataFromDb(
                symbol, interval, actualLimit + actualPeriod);
        
        if (candlesticks.size() < actualPeriod) {
            log.warn("获取到的K线数据不足以计算布林带,symbol:{},interval:{},期望数量:{},实际数量:{}",
                    symbol, interval, actualPeriod, candlesticks.size());
            return new ArrayList<>();
        }
        
        // 提取收盘价列表
        List<BigDecimal> closePrices = candlesticks.stream()
                .map(CandlestickEntity::getClose)
                .collect(Collectors.toList());
        
        // 计算布林带
        BollingerBands bollingerBands = TechnicalIndicatorUtil.calculateBollingerBands(
                closePrices, actualPeriod, actualStdDev, 8);
        
        // 转换为DTO
        List<BollingerBandsDTO> result = new ArrayList<>();
        for (int i = actualPeriod - 1; i < candlesticks.size(); i++) {
            CandlestickEntity candle = candlesticks.get(i);
            int bbIndex = i - actualPeriod + 1;
            
            // 计算%B值 (Price - Lower) / (Upper - Lower)
            BigDecimal price = candle.getClose();
            BigDecimal upper = bollingerBands.getUpper().get(bbIndex);
            BigDecimal middle = bollingerBands.getMiddle().get(bbIndex);
            BigDecimal lower = bollingerBands.getLower().get(bbIndex);
            
            BigDecimal percentB = null;
            BigDecimal bandwidth = null;
            
            if (upper.compareTo(lower) != 0) {
                // 计算%B = (Price - Lower) / (Upper - Lower)
                percentB = price.subtract(lower)
                        .divide(upper.subtract(lower), 4, BigDecimal.ROUND_HALF_UP);
                
                // 计算Bandwidth = (Upper - Lower) / Middle
                if (middle.compareTo(BigDecimal.ZERO) != 0) {
                    bandwidth = upper.subtract(lower)
                            .divide(middle, 4, BigDecimal.ROUND_HALF_UP);
                }
            }
            
            result.add(BollingerBandsDTO.builder()
                    .timestamp(candle.getOpenTime())
                    .price(price)
                    .middle(middle)
                    .upper(upper)
                    .lower(lower)
                    .percentB(percentB)
                    .bandwidth(bandwidth)
                    .build());
        }
        
        // 仅返回最新的limit条记录
        if (result.size() > actualLimit) {
            return result.subList(result.size() - actualLimit, result.size());
        }
        
        return result;
    }
} 