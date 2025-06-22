package com.okx.trading.service.impl;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.RealTimeOrderService;
import com.okx.trading.service.RealTimeStrategyService;
import com.okx.trading.controller.TradeController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.DayOfWeek;
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
public class RealTimeStrategyManager implements ApplicationRunner {

    private final OkxApiWebSocketServiceImpl webSocketService;
    private final RealTimeOrderService realTimeOrderService;
    private final TradeController tradeController;
    private final HistoricalDataService historicalDataService;
    private final RealTimeStrategyService realTimeStrategyService;

    public RealTimeStrategyManager(@Lazy OkxApiWebSocketServiceImpl webSocketService,
                                   RealTimeOrderService realTimeOrderService,
                                   TradeController tradeController,
                                   HistoricalDataService historicalDataService,
                                   RealTimeStrategyService realTimeStrategyService) {
        this.webSocketService = webSocketService;
        this.realTimeOrderService = realTimeOrderService;
        this.tradeController = tradeController;
        this.historicalDataService = historicalDataService;
        this.realTimeStrategyService = realTimeStrategyService;
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
        private LocalDateTime startTime;
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
                                    Strategy strategy, BarSeries series, BigDecimal tradeAmount, LocalDateTime startTime) {
            this.strategyCode = strategyCode;
            this.symbol = symbol;
            this.interval = interval;
            this.strategy = strategy;
            this.series = series;
            this.tradeAmount = tradeAmount;
            this.isInPosition = false;
            this.totalTrades = 0;
            this.successfulTrades = 0;
            this.orders = new ArrayList<>();
            this.lastUpdateTime = LocalDateTime.now();
            this.startTime = startTime;
        }

        // Getters and Setters
        public String getStrategyCode() {
            return strategyCode;
        }

        public String getSymbol() {
            return symbol;
        }

        public Boolean getInPosition() {
            return isInPosition;
        }

        public void setInPosition(Boolean inPosition) {
            isInPosition = inPosition;
        }

        public BigDecimal getTradeAmount() {
            return tradeAmount;
        }

        public void setTradeAmount(BigDecimal tradeAmount) {
            this.tradeAmount = tradeAmount;
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
                                        Strategy strategy, BarSeries series, BigDecimal tradeAmount, LocalDateTime startTime) {
        String key = buildStrategyKey(strategyCode, symbol, interval);

        // 检查是否已经在运行
        if (runningStrategies.containsKey(key)) {
            log.warn("策略已在运行: {}", key);
            return key;
        }

        // 创建策略运行状态
        StrategyRunningState state = new StrategyRunningState(strategyCode, symbol, interval, strategy, series, tradeAmount, startTime);

        // 保存策略到MySQL（如果不存在）
        try {
            saveStrategyToDatabase(strategyCode, symbol, interval, tradeAmount.doubleValue());
        } catch (Exception e) {
            log.warn("保存策略到数据库失败: {}", e.getMessage());
        }

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

        // 更新BarSeries - 智能判断是更新还是添加新bar
        Bar newBar = createBarFromCandlestick(candlestick);
        boolean shouldReplace = shouldReplaceLastBar(state.getSeries(), newBar, state.getInterval());
        state.getSeries().addBar(newBar, shouldReplace);

        // 检查交易信号
        int currentIndex = state.getSeries().getEndIndex();
        boolean shouldBuy = state.getStrategy().shouldEnter(currentIndex);
        boolean shouldSell = state.getStrategy().shouldExit(currentIndex);

        // 处理买入信号
        if (shouldBuy) {
            executeTradeSignal(state, candlestick, "BUY");
        }

        // 处理卖出信号
        if (shouldSell) {
            executeTradeSignal(state, candlestick, "SELL");
        }

        // 更新状态
        state.setCurrentPrice(candlestick.getClose());
        state.setLastUpdateTime(LocalDateTime.now());
    }

    /**
     * 判断是否应该替换最后一个bar（同一周期更新）还是添加新bar（不同周期）
     *
     * @param series   BarSeries
     * @param newBar   新的Bar
     * @param interval K线间隔
     * @return true表示替换最后一个bar（同一周期），false表示添加新bar（不同周期）
     */
    private boolean shouldReplaceLastBar(BarSeries series, Bar newBar, String interval) {
        // 如果series为空或没有bar，直接添加新bar
        if (series.isEmpty()) {
            return false;
        }

        Bar lastBar = series.getLastBar();
        LocalDateTime newBarStartTime = newBar.getBeginTime().toLocalDateTime();
        LocalDateTime lastBarStartTime = lastBar.getBeginTime().toLocalDateTime();

        // 计算周期的开始时间
        LocalDateTime newPeriodStart = getPeriodStartTime(newBarStartTime, interval);
        LocalDateTime lastPeriodStart = getPeriodStartTime(lastBarStartTime, interval);

        // 如果是同一个周期，则替换；否则添加新bar
        return newPeriodStart.equals(lastPeriodStart);
    }

    /**
     * 根据时间和间隔计算周期的开始时间
     *
     * @param dateTime 时间
     * @param interval K线间隔（如1m, 5m, 1H, 1D等）
     * @return 周期开始时间
     */
    private LocalDateTime getPeriodStartTime(LocalDateTime dateTime, String interval) {
        if (dateTime == null || interval == null) {
            return dateTime;
        }

        // 解析时间单位和数量
        String unit = interval.substring(interval.length() - 1);
        int amount;
        try {
            amount = Integer.parseInt(interval.substring(0, interval.length() - 1));
        } catch (NumberFormatException e) {
            amount = 1;
        }

        switch (unit) {
            case "m": // 分钟
                int minute = dateTime.getMinute();
                int periodMinute = (minute / amount) * amount;
                return dateTime.withMinute(periodMinute).withSecond(0).withNano(0);

            case "H": // 小时
                int hour = dateTime.getHour();
                int periodHour = (hour / amount) * amount;
                return dateTime.withHour(periodHour).withMinute(0).withSecond(0).withNano(0);

            case "D": // 天
                return dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);

            case "W": // 周
                // 计算本周的周一
                return dateTime.with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);

            case "M": // 月
                return dateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

            default:
                return dateTime.withSecond(0).withNano(0);
        }
    }

    /**
     * 执行交易信号
     */
    private void executeTradeSignal(StrategyRunningState state, Candlestick candlestick, String side) {
        try {
            Order order = tradeController.createSpotOrder(
                    state.getSymbol(),
                    null,
                    side,
                    null,
                    null,
                    state.getTradeAmount(),
                    null, null, null, null,
                    false
            ).getData();

            if (order != null) {
                // 保存订单记录
                RealTimeOrderEntity orderEntity = realTimeOrderService.createOrderRecord(
                        state.getStrategyCode(),
                        state.getSymbol(),
                        order,
                        side + "_SIGNAL",
                        candlestick.getClose().toString(),
                        false);
                state.getOrders().add(orderEntity);

                // 更新持仓状态
                state.setInPosition("BUY".equals(side));
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
        long intervalMinutes = historicalDataService.getIntervalMinutes(candlestick.getIntervalVal());

        // 计算endTime：如果closeTime为null，则根据openTime和interval计算
        LocalDateTime endTime;
        if (candlestick.getCloseTime() != null) {
            endTime = candlestick.getCloseTime();
        } else {
            // 根据openTime和interval计算closeTime
            endTime = calculateEndTimeFromInterval(candlestick.getOpenTime(), candlestick.getIntervalVal());
        }

        return BaseBar.builder()
                .timePeriod(java.time.Duration.ofMinutes(intervalMinutes)) // 根据实际interval调整
                .openPrice(DecimalNum.valueOf(candlestick.getOpen()))
                .endTime(endTime.atZone(ZoneId.systemDefault()))
                .highPrice(DecimalNum.valueOf(candlestick.getHigh()))
                .lowPrice(DecimalNum.valueOf(candlestick.getLow()))
                .closePrice(DecimalNum.valueOf(candlestick.getClose()))
                .volume(DecimalNum.valueOf(candlestick.getVolume()))
                .build();
    }

    /**
     * 根据开盘时间和K线间隔计算收盘时间
     */
    private LocalDateTime calculateEndTimeFromInterval(LocalDateTime openTime, String interval) {
        if (openTime == null || interval == null) {
            return LocalDateTime.now();
        }

        // 解析时间单位和数量
        String unit = interval.substring(interval.length() - 1);
        int amount;
        try {
            amount = Integer.parseInt(interval.substring(0, interval.length() - 1));
        } catch (NumberFormatException e) {
            // 如果解析失败，使用默认值1
            amount = 1;
        }

        switch (unit) {
            case "m":
                return openTime.plusMinutes(amount);
            case "H":
                return openTime.plusHours(amount);
            case "D":
                return openTime.plusDays(amount);
            case "W":
                return openTime.plusWeeks(amount);
            case "M":
                return openTime.plusMonths(amount);
            default:
                return openTime.plusMinutes(1); // 默认1分钟
        }
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
        return result;
    }

    /**
     * 程序启动时执行，从MySQL加载有效策略
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("程序启动，开始加载有效的实时策略...");

        try {
            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getStrategiesToAutoStart();

            if (strategies.isEmpty()) {
                log.info("没有找到需要自动启动的策略");
                return;
            }

            log.info("找到 {} 个需要自动启动的策略", strategies.size());

            for (RealTimeStrategyEntity strategyEntity : strategies) {
                try {
                    // 这里需要根据实际情况构建Strategy对象和BarSeries
                    // 由于Strategy和BarSeries的创建逻辑比较复杂，这里只是记录日志
                    // 实际使用时需要根据strategyInfoCode等信息来构建具体的策略对象

                    log.info("准备启动策略: strategyCode={}, symbol={}, interval={}",
                            strategyEntity.getStrategyCode(),
                            strategyEntity.getSymbol(),
                            strategyEntity.getInterval());

                    // TODO: 根据strategyInfoCode创建具体的Strategy实例
                    // TODO: 创建对应的BarSeries
                    // TODO: 调用startRealTimeStrategy方法启动策略

                } catch (Exception e) {
                    log.error("启动策略失败: strategyCode={}, error={}",
                            strategyEntity.getStrategyCode(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("加载策略失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存策略到数据库
     */
    private void saveStrategyToDatabase(String strategyCode,
                                        String symbol, String interval, Double tradeAmount) {
        try {
            // 检查策略是否已存在
            // 这里假设RealTimeStrategyService有相应的查询方法
            // 如果没有，可能需要先实现或者直接创建

            realTimeStrategyService.createRealTimeStrategy(strategyCode, symbol, interval, tradeAmount);

            log.info("策略已保存到数据库: strategyCode={}, symbol={}, interval={}",
                    strategyCode, symbol, interval);

        } catch (Exception e) {
            log.error("保存策略到数据库失败: strategyCode={}, error={}", strategyCode, e.getMessage(), e);
            throw e;
        }
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
