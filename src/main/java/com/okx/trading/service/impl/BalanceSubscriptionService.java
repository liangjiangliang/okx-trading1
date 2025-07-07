package com.okx.trading.service.impl;

import com.okx.trading.util.WebSocketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * 余额订阅服务
 * 负责通过WebSocket订阅balance频道，并定期更新余额信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSubscriptionService {

    private final WebSocketUtil webSocketUtil;
    private final OkxApiWebSocketServiceImpl okxApiWebSocketService;

    /**
     * 初始化时订阅balance频道
     */
    @PostConstruct
    public void init() {
        subscribeBalanceChannel();
    }

    /**
     * 订阅balance频道
     */
    public void subscribeBalanceChannel() {
        try {
            log.info("开始订阅balance频道...");
            webSocketUtil.subscribePrivateTopic("account");
            log.info("balance频道订阅成功");
        } catch (Exception e) {
            log.error("订阅balance频道失败", e);
        }
    }

    /**
     * 每5分钟定时刷新余额信息
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void refreshBalanceInfo() {
        try {
            log.info("定时刷新余额信息...");
            okxApiWebSocketService.getAccountBalance();
            log.info("余额信息刷新成功");
        } catch (Exception e) {
            log.error("刷新余额信息失败", e);
        }
    }
} 