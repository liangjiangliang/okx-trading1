package com.okx.trading.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 余额服务接口
 * 提供获取账户余额信息的方法
 */
public interface BalanceService {

    /**
     * 获取所有币种的余额信息
     *
     * @return 币种余额Map，key为币种代码，value为可用余额
     */
    Map<String, BigDecimal> getAllBalances();

    /**
     * 获取指定币种的余额
     *
     * @param asset 币种代码，如BTC、ETH等
     * @return 可用余额，如果币种不存在则返回null
     */
    BigDecimal getBalance(String asset);

    /**
     * 刷新余额信息
     * 
     * @return 是否刷新成功
     */
    boolean refreshBalances();
} 