package com.okx.trading.service;

import com.okx.trading.model.dto.BollingerBandsDTO;

import java.util.List;

/**
 * 市场数据服务接口
 */
public interface MarketDataService {
    
    /**
     * 获取特定交易对的布林带数据
     *
     * @param symbol 交易对,如 BTC-USDT
     * @param interval K线间隔,如 1m, 5m, 15m, 1h, 4h, 1d
     * @param period 布林带周期,默认20
     * @param stdDev 标准差倍数,默认2
     * @param limit 返回数据数量,默认500
     * @return 布林带数据列表
     */
    List<BollingerBandsDTO> getBollingerBandsData(String symbol, String interval, 
                                                 Integer period, Double stdDev, Integer limit);
} 