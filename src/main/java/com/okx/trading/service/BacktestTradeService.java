package com.okx.trading.service;

import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.BacktestTradeEntity;

import java.util.List;

/**
 * 回测交易服务接口
 */
public interface BacktestTradeService {
    
    /**
     * 保存回测交易记录
     *
     * @param backtestResult 回测结果
     * @param strategyParams 策略参数
     * @return 保存的回测ID
     */
    String saveBacktestTrades(BacktestResultDTO backtestResult, String strategyParams);
    
    /**
     * 根据回测ID查询交易记录列表
     *
     * @param backtestId 回测ID
     * @return 交易记录列表
     */
    List<BacktestTradeEntity> getTradesByBacktestId(String backtestId);
    
    /**
     * 获取回测的最大回撤
     *
     * @param backtestId 回测ID
     * @return 最大回撤百分比
     */
    double getMaxDrawdown(String backtestId);
    
    /**
     * 获取所有回测ID
     *
     * @return 回测ID列表
     */
    List<String> getAllBacktestIds();
    
    /**
     * 删除指定回测的所有记录
     *
     * @param backtestId 回测ID
     */
    void deleteBacktestRecords(String backtestId);
} 