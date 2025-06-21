package com.okx.trading.service.impl;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.service.RealTimeOrderService;
import com.okx.trading.controller.TradeController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

/**
 * 实时策略管理器
 * 管理正在运行的实时策略，处理WebSocket推送的K线数据
 */
@Slf4j
@Service
public class RealTimeStrategyManager {

    private final OkxApiWebSocketServiceImpl webSocketService;
    private final RealTimeOrderService realTimeOrderService;
    private final TradeController tradeController;

    public RealTimeStrategyManager(@Lazy OkxApiWebSocketServiceImpl webSocketService,
                                   RealTimeOrderService realTimeOrderService,
                                   TradeController tradeController) {
        this.webSocketService = webSocketService;
        this.realTimeOrderService = realTimeOrderService;
        this.tradeController = tradeController;
    }

    // 存储正在运行的策略信息
    // key: strategyCode_symbol_interval, value: 策略运行状态
    private final Map<String, StrategyRunningState> runningStrategies = new ConcurrentHashMap<>();

    /**
     * 策略运行状态
     */
    public static class StrategyRunningState {
        private String strategyCode;
        private String symbol;
        private String interval;
        private Strategy strategy;
        private BarSeries series;
        private LocalDateTime endTime;
        private Boolean simulated;
        private String orderType;
        private BigDecimal tradeAmount;
        private Boolean isInPosition;
        private int totalTrades;
        private int successfulTrades;
        private List<RealTimeOrderEntity> orders;
        private LocalDateTime lastUpdateTime;
        private BigDecimal currentPrice;
        private CompletableFuture<Map<String, Object>> future;

        // 构造函数
        public StrategyRunningState(String strategyCode, String symbol, String interval,
                                    Strategy strategy, BarSeries series, LocalDateTime endTime,
                                    Boolean simulated, String orderType, BigDecimal tradeAmount) {
            this.strategyCode = strategyCode;
            this.symbol = symbol;
            this.interval = interval;
            this.strategy = strategy;
            this.series = series;
            this.endTime = endTime;
            this.simulated = simulated;
            this.orderType = orderType;
            this.tradeAmount = tradeAmount;
            this.isInPosition = false;
            this.totalTrades = 0;
            this.successfulTrades = 0;
            this.orders = new ArrayList<>();
            this.lastUpdateTime = LocalDateTime.now();
        }

        // Getters and Setters
        public String getStrategyCode() {
            return strategyCode;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getInterval() {
            return interval;
        }

        public Strategy getStrategy() {
            return strategy;
        }

        public BarSeries getSeries() {
            return series;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public Boolean getSimulated() {
            return simulated;
        }

        public String getOrderType() {
            return orderType;
        }

        public BigDecimal getTradeAmount() {
            return tradeAmount;
        }

        public Boolean getIsInPosition() {
            return isInPosition;
        }

        public void setIsInPosition(Boolean isInPosition) {
            this.isInPosition = isInPosition;
        }

        public int getTotalTrades() {
            return totalTrades;
        }

        public void setTotalTrades(int totalTrades) {
            this.totalTrades = totalTrades;
        }

        public int getSuccessfulTrades() {
            return successfulTrades;
        }

        public void setSuccessfulTrades(int successfulTrades) {
            this.successfulTrades = successfulTrades;
        }

        public List<RealTimeOrderEntity> getOrders() {
            return orders;
        }

        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public CompletableFuture<Map<String, Object>> getFuture() {
            return future;
        }

        public void setFuture(CompletableFuture<Map<String, Object>> future) {
            this.future = future;
        }
    }

    /**
     * 启动实时策略
     */
    public String startRealTimeStrategy(String strategyCode, String symbol, String interval,
                                        Strategy strategy, BarSeries series, LocalDateTime endTime,
                                        Boolean simulated, String orderType, BigDecimal tradeAmount) {
        String key = buildStrategyKey(strategyCode, symbol, interval);

        // 检查是否已经在运行
        if (runningStrategies.containsKey(key)) {
            log.warn("策略已在运行: {}", key);
            return key;
        }

        // 创建策略运行状态
        StrategyRunningState state = new StrategyRunningState(
                strategyCode, symbol, interval, strategy, series, endTime,
                simulated, orderType, tradeAmount);

        // 订阅K线数据
        try {
            webSocketService.subscribeKlineData(symbol, interval);
            log.info("已订阅K线数据: symbol={}, interval={}", symbol, interval);
        } catch (Exception e) {
            log.error("订阅K线数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("订阅K线数据失败: " + e.getMessage());
        }

        // 存储策略状态
        runningStrategies.put(key, state);

        log.info("实时策略已启动: {}", key);
        return key;
    }

    /**
     * 停止实时策略
     */
    public void stopRealTimeStrategy(String strategyCode, String symbol, String interval) {
        String key = buildStrategyKey(strategyCode, symbol, interval);

        StrategyRunningState state = runningStrategies.remove(key);
        if (state != null) {
            // 取消订阅K线数据（如果没有其他策略使用）
            if (!isSymbolIntervalInUse(symbol, interval)) {
                try {
                    webSocketService.unsubscribeKlineData(symbol, interval);
                    log.info("已取消订阅K线数据: symbol={}, interval={}", symbol, interval);
                } catch (Exception e) {
                    log.error("取消订阅K线数据失败: {}", e.getMessage(), e);
                }
            }

            // 完成Future
            if (state.getFuture() != null && !state.getFuture().isDone()) {
                Map<String, Object> result = buildFinalResult(state);
                state.getFuture().complete(result);
            }

            log.info("实时策略已停止: {}", key);
        }
    }

    /**
     * 处理新的K线数据
     * 由WebSocket服务调用
     */
    public void handleNewKlineData(String symbol, String interval, Candlestick candlestick) {
        // 查找使用该symbol和interval的所有策略
        runningStrategies.entrySet().stream()
                .filter(entry -> {
                    StrategyRunningState state = entry.getValue();
                    return state.getSymbol().equals(symbol) && state.getInterval().equals(interval);
                })
                .forEach(entry -> {
                    String key = entry.getKey();
                    StrategyRunningState state = entry.getValue();

                    try {
                        processStrategySignal(key, state, candlestick);
                    } catch (Exception e) {
                        log.error("处理策略信号失败: key={}, error={}", key, e.getMessage(), e);
                    }
                });
    }

    /**
     * 处理策略信号
     */
    private void processStrategySignal(String key, StrategyRunningState state, Candlestick candlestick) {
        // 检查是否超过结束时间
        if (LocalDateTime.now().isAfter(state.getEndTime())) {
            log.info("策略已到达结束时间，停止运行: {}", key);
            stopRealTimeStrategy(state.getStrategyCode(), state.getSymbol(), state.getInterval());
            return;
        }

        // 更新BarSeries
        Bar newBar = createBarFromCandlestick(candlestick);
        state.getSeries().addBar(newBar, true);

        // 检查交易信号
        int currentIndex = state.getSeries().getEndIndex();
        boolean shouldBuy = state.getStrategy().shouldEnter(currentIndex);
        boolean shouldSell = state.getStrategy().shouldExit(currentIndex);

        // 处理买入信号
        if (shouldBuy && !state.getIsInPosition()) {
            executeTradeSignal(state, candlestick, "BUY");
        }

        // 处理卖出信号
        if (shouldSell && state.getIsInPosition()) {
            executeTradeSignal(state, candlestick, "SELL");
        }

        // 更新状态
        state.setCurrentPrice(candlestick.getClose());
        state.setLastUpdateTime(LocalDateTime.now());
    }

    /**
     * 执行交易信号
     */
    private void executeTradeSignal(StrategyRunningState state, Candlestick candlestick, String side) {
        try {
            Order order = tradeController.createSpotOrder(
                    state.getSymbol(),
                    state.getOrderType(),
                    side,
                    "LIMIT".equals(state.getOrderType()) ? candlestick.getClose() : null,
                    null,
                    state.getTradeAmount(),
                    null, null, null, null,
                    state.getSimulated()
            ).getData();

            if (order != null) {
                // 保存订单记录
                RealTimeOrderEntity orderEntity = realTimeOrderService.createOrderRecord(
                        state.getStrategyCode(),
                        state.getSymbol(),
                        order,
                        side + "_SIGNAL",
                        candlestick.getClose().toString(),
                        state.getSimulated());
                state.getOrders().add(orderEntity);

                // 更新持仓状态
                state.setIsInPosition("BUY".equals(side));
                state.setTotalTrades(state.getTotalTrades() + 1);
                if ("FILLED".equals(order.getStatus())) {
                    state.setSuccessfulTrades(state.getSuccessfulTrades() + 1);
                }

                log.info("执行{}订单: symbol={}, price={}, amount={}, orderId={}",
                        side, state.getSymbol(), candlestick.getClose(),
                        state.getTradeAmount(), order.getOrderId());
            }
        } catch (Exception e) {
            log.error("执行{}订单失败: {}", side, e.getMessage(), e);
        }
    }

    /**
     * 从Candlestick创建Bar
     */
    private Bar createBarFromCandlestick(Candlestick candlestick) {
        return BaseBar.builder()
                .timePeriod(java.time.Duration.ofMinutes(1)) // 根据实际interval调整
                .endTime(candlestick.getCloseTime().atZone(ZoneId.systemDefault()))
                .openPrice(DecimalNum.valueOf(candlestick.getOpen()))
                .highPrice(DecimalNum.valueOf(candlestick.getHigh()))
                .lowPrice(DecimalNum.valueOf(candlestick.getLow()))
                .closePrice(DecimalNum.valueOf(candlestick.getClose()))
                .volume(DecimalNum.valueOf(candlestick.getVolume()))
                .build();
    }

    /**
     * 构建策略键
     */
    private String buildStrategyKey(String strategyCode, String symbol, String interval) {
        return strategyCode + "_" + symbol + "_" + interval;
    }

    /**
     * 检查symbol和interval是否还在使用中
     */
    private boolean isSymbolIntervalInUse(String symbol, String interval) {
        return runningStrategies.values().stream()
                .anyMatch(state -> state.getSymbol().equals(symbol) && state.getInterval().equals(interval));
    }

    /**
     * 构建最终结果
     */
    private Map<String, Object> buildFinalResult(StrategyRunningState state) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("status", "COMPLETED");
        result.put("totalTrades", state.getTotalTrades());
        result.put("successfulTrades", state.getSuccessfulTrades());
        result.put("successRate", state.getTotalTrades() > 0 ?
                (double) state.getSuccessfulTrades() / state.getTotalTrades() : 0.0);
        result.put("orders", state.getOrders());
        result.put("endTime", LocalDateTime.now());
        return result;
    }

    /**
     * 获取运行中的策略状态
     */
    public StrategyRunningState getRunningStrategy(String strategyCode, String symbol, String interval) {
        String key = buildStrategyKey(strategyCode, symbol, interval);
        return runningStrategies.get(key);
    }

    /**
     * 获取所有运行中的策略
     */
    public Map<String, StrategyRunningState> getAllRunningStrategies() {
        return new ConcurrentHashMap<>(runningStrategies);
    }
}
