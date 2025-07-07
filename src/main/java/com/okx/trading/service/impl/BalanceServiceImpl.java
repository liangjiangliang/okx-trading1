package com.okx.trading.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.okx.trading.service.BalanceService;
import com.okx.trading.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.okx.trading.constant.IndicatorInfo.BALANCE;

/**
 * 余额服务实现类
 * 从Redis获取账户余额信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private final RedisCacheService redisCacheService;
    private final OkxApiWebSocketServiceImpl okxApiWebSocketService;

    @Override
    public Map<String, BigDecimal> getAllBalances() {
        try {
            String balanceJson = redisCacheService.getCache(BALANCE, String.class);
            if (balanceJson != null) {
                return JSON.parseObject(balanceJson, new TypeReference<Map<String, BigDecimal>>() {});
            } else {
                log.warn("Redis中不存在余额信息，尝试刷新");
                if (refreshBalances()) {
                    balanceJson = redisCacheService.getCache(BALANCE, String.class);
                    if (balanceJson != null) {
                        return JSON.parseObject(balanceJson, new TypeReference<Map<String, BigDecimal>>() {});
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取所有余额信息失败", e);
        }
        return Collections.emptyMap();
    }

    @Override
    public BigDecimal getBalance(String asset) {
        try {
            Map<String, BigDecimal> balances = getAllBalances();
            return balances.get(asset);
        } catch (Exception e) {
            log.error("获取{}余额信息失败", asset, e);
            return null;
        }
    }

    @Override
    public boolean refreshBalances() {
        try {
            // 调用API获取最新余额信息
            okxApiWebSocketService.getAccountBalance();
            log.info("余额信息刷新成功");
            return true;
        } catch (Exception e) {
            log.error("刷新余额信息失败", e);
            return false;
        }
    }
} 