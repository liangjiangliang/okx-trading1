package com.okx.trading.strategy;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.trade.Order;
import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.adapter.CandlestickBarSeriesConverter;
import com.okx.trading.repository.RealTimeStrategyRepository;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.NotificationService;
import com.okx.trading.service.RealTimeOrderService;
import com.okx.trading.service.RealTimeStrategyService;
import com.okx.trading.controller.TradeController;
import com.okx.trading.service.StrategyInfoService;
import com.okx.trading.service.impl.OkxApiWebSocketServiceImpl;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.okx.trading.constant.IndicatorInfo.*;
import static javax.print.attribute.standard.JobState.CANCELED;


/**
 * 实时策略管理器
 * 管理正在运行的实时策略，处理WebSocket推送的K线数据
 */
@Slf4j
@Service
@Data
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RealTimeStrategyManager implements ApplicationRunner {

    private final OkxApiWebSocketServiceImpl webSocketService;
    private final RealTimeOrderService realTimeOrderService;
    private final TradeController tradeController;
    private final HistoricalDataService historicalDataService;
    @Lazy
    private final RealTimeStrategyService realTimeStrategyService;
    private final CandlestickBarSeriesConverter barSeriesConverter;
    private final StrategyInfoService strategyInfoService;
    private final RealTimeStrategyRepository realTimeStrategyRepository;
    private final int kLineNum = 100;
    private boolean loadedStrategies = false;
    private final NotificationService notificationService;

    public RealTimeStrategyManager(@Lazy OkxApiWebSocketServiceImpl webSocketService,
                                   RealTimeOrderService realTimeOrderService,
                                   TradeController tradeController,
                                   HistoricalDataService historicalDataService,
                                   @Lazy RealTimeStrategyService realTimeStrategyService,
                                   CandlestickBarSeriesConverter barSeriesConverter, StrategyInfoService strategyInfoService, RealTimeStrategyRepository realTimeStrategyRepository, NotificationService notificationService) {
        this.webSocketService = webSocketService;
        this.realTimeOrderService = realTimeOrderService;
        this.tradeController = tradeController;
        this.historicalDataService = historicalDataService;
        this.realTimeStrategyService = realTimeStrategyService;
        this.barSeriesConverter = barSeriesConverter;
        this.strategyInfoService = strategyInfoService;
        this.realTimeStrategyRepository = realTimeStrategyRepository;
        this.notificationService = notificationService;
    }

    // 存储正在运行的策略信息
    // key: strategyCode_symbol_interval, value: 策略运行状态
    private final Map<String, RealTimeStrategyEntity> runningStrategies = new ConcurrentHashMap<>();
    private final Map<String, BarSeries> runningBarSeries = new ConcurrentHashMap<>();

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    /**
     * 停止实时策略
     */
    public void stopRealTimeStrategy(String strategyCode, String symbol, String interval) {
        String key = buildStrategyKey(strategyCode, symbol, interval);

        RealTimeStrategyEntity state = runningStrategies.remove(key);
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
        if (runningStrategies.isEmpty()) {
            return;
        }
        runningStrategies.entrySet().stream()
                .filter(entry -> {
                    RealTimeStrategyEntity state = entry.getValue();
                    return state.getSymbol().equals(symbol) && state.getInterval().equals(interval);
                })
                .forEach(entry -> {
                    RealTimeStrategyEntity state = entry.getValue();
                    try {
                        if (state.getStrategy() != null) {
                            processStrategySignal(state, candlestick);
                        }
                    } catch (Exception e) {
                        log.error("处理策略信号失败: key={}, error={}", buildStrategyKey(state.getStrategyCode(), state.getSymbol(), state.getInterval()), e.getMessage(), e);
                    }
                });
    }

    /**
     * 处理策略信号
     * 真正执行实时策略逻辑，判断买卖信号的地方
     */
    private void processStrategySignal(RealTimeStrategyEntity state, Candlestick candlestick) {

        // 更新BarSeries - 智能判断是更新还是添加新bar
        Bar newBar = createBarFromCandlestick(candlestick);
        BarSeries series = runningBarSeries.get(state.getSymbol() + "_" + state.getInterval());
        boolean shouldReplace = shouldReplaceLastBar(series, newBar, state.getInterval());
        series.addBar(newBar, shouldReplace);
        if (!shouldReplace) {
            series = series.getSubSeries(series.getBeginIndex() + 1, series.getEndIndex() + 1);
        }

        // 检查交易信号
        int currentIndex = series.getEndIndex();
        boolean shouldBuy = state.getStrategy().shouldEnter(currentIndex);
        boolean shouldSell = state.getStrategy().shouldExit(currentIndex);

        // 控制同一个周期内只能交易一次
        LocalDateTime lastTradeTime;
        boolean singalOfSamePeriod = false;
        if (null != state.getLastTradeTime()) {
            lastTradeTime = state.getLastTradeTime();
            singalOfSamePeriod = Duration.between(candlestick.getOpenTime(), lastTradeTime).get(ChronoUnit.SECONDS) <= historicalDataService.getIntervalMinutes(candlestick.getIntervalVal()) * 60;
        }
        // 处理买入信号 - 只有在上一次不是买入时才触发
        if (shouldBuy && (state.getLastTradeType() == null || SELL.equals(state.getLastTradeType())) && !singalOfSamePeriod) {
            executeTradeSignal(state, candlestick, BUY);
        }

        // 处理卖出信号 - 只有在上一次是买入时才触发
        if (shouldSell && BUY.equals(state.getLastTradeType())) {
            executeTradeSignal(state, candlestick, SELL);
        }
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
        if (series == null || series.isEmpty()) {
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
     * 执行交易信号
     */
    @Async("customAsyncTaskExecutor")
    private void executeTradeSignal(RealTimeStrategyEntity state, Candlestick candlestick, String side) {
        try {
            BigDecimal preAmount = null;
            BigDecimal preQuantity = null;

            // 计算交易数量
            if (BUY.equals(side)) {
                // 买入：按照给定金额买入
                if (null == state.getLastTradeType()) {
                    // 没有卖出记录，使用最初金额
                    preAmount = BigDecimal.valueOf(state.getTradeAmount());
                } else {
                    // 上次卖出剩下的钱
                    preAmount = BigDecimal.valueOf(state.getLastTradeAmount());
                }
            } else {
                // 卖出：全仓卖出买入的数量
                if (state.getLastTradeQuantity() != null && state.getLastTradeQuantity() > 0) {
                    preQuantity = BigDecimal.valueOf(state.getLastTradeQuantity());
                } else {
                    log.warn("卖出信号触发但没有持仓数量，跳过交易: strategyCode={}", state.getStrategyCode());
                    return;
                }
            }

            Order order = tradeController.createSpotOrder(
                    state.getSymbol(),
                    null,
                    side,
                    null,
                    preQuantity,
                    preAmount,
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
                        side,
                        candlestick.getClose().toString(),
                        false,
                        preAmount,
                        preQuantity);  // 打算买入金额，不是成交金额

                // 利润统计
                // 更新累计统计信息
                if (orderEntity.getSide().equals(SELL)) {
                    state.setTotalProfit(state.getTotalProfit() + (orderEntity.getExecutedAmount().doubleValue() - state.getLastTradeAmount()));
                }
                // 费用每次都有
                state.setTotalFees(state.getTotalFees() + orderEntity.getFee().doubleValue());
                // 更新策略状态
                state.setLastTradeType(orderEntity.getSide());
                // 买入时记录购买数量
                state.setLastTradeAmount(orderEntity.getExecutedAmount().doubleValue());
                state.setLastTradeQuantity(orderEntity.getExecutedQty().doubleValue());
                state.setLastTradePrice(orderEntity.getPrice().doubleValue());
                state.setLastTradeTime(orderEntity.getCreateTime());
                if (BUY.equals(side)) {
                    state.setIsInPosition(true);
                } else {
                    state.setIsInPosition(false);
                }
                // 成交次数统计
                state.setTotalTrades(state.getTotalTrades() + 1);
                if (FILLED.equals(order.getStatus())) {
                    state.setSuccessfulTrades(state.getSuccessfulTrades() + 1);
                }
                // 更新数据库中的交易信息
                RealTimeStrategyEntity realTimeStrategy = realTimeStrategyService.updateTradeInfo(state);
                // 更新订单信息
                orderEntity.setStrategyId(realTimeStrategy.getId());
                realTimeOrderService.saveOrder(orderEntity);

                log.info("执行{}订单成功: symbol={}, price={}, amount={}, quantity={}", side, state.getSymbol(), state.getLastTradePrice(),
                        state.getLastTradeAmount(), state.getLastTradeQuantity());

                // 发送交易通知
                try {
                    notificationService.sendTradeNotification(state, order, side, candlestick.getClose().toString());
                } catch (Exception e) {
                    log.error("发送交易通知失败: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            String strategyKey = buildStrategyKey(state.getStrategyCode(), state.getSymbol(), state.getInterval());
            runningStrategies.remove(strategyKey);
            state.setIsActive(false);
            state.setStatus("ERROR");
            state.setEndTime(LocalDateTime.now());
            realTimeStrategyRepository.save(state);
            log.error("执行策略 {} {}订单失败，停止策略: {},", state.getStrategyName(), side, e.getMessage(), e);

            // 发送错误通知
            try {
                notificationService.sendStrategyErrorNotification(state, e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误通知失败: {}", ex.getMessage(), ex);
            }
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
     * 构建策略键
     */
    public String buildStrategyKey(String strategyCode, String symbol, String interval) {
        return strategyCode + "_" + symbol + "_" + interval;
    }

    /**
     * 程序启动时执行，从MySQL加载有效策略
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("程序启动，开始加载有效的实时策略...");

        try {
            // 获取运行中的状态
            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getStrategiesToAutoStart();
            if (strategies.isEmpty()) {
                log.info("没有找到需要自动启动的策略");
                loadedStrategies = true;
                return;
            }
            log.info("找到 {} 个需要自动启动的策略", strategies.size());

            LocalDateTime now = LocalDateTime.now();
            for (RealTimeStrategyEntity strategyEntity : strategies) {
                try {
                    log.info("准备启动策略: strategyCode={}, symbol={}, interval={}",
                            strategyEntity.getStrategyCode(), strategyEntity.getSymbol(), strategyEntity.getInterval());
                    Map<String, Object> response = startExecuteRealTimeStrategy(strategyEntity);
                    String status = (String) response.get("status");
                    if (status.equals(SUCCESS)) {
                        log.info("策略启动成功: {}({})", strategyEntity.getStrategyName(), strategyEntity.getStrategyCode());
                    } else {
                        log.info("策略启动失败: {}({})", strategyEntity.getStrategyName(), strategyEntity.getStrategyCode());
                    }
                } catch (Exception e) {
                    log.error("启动策略失败: strategyCode={}, error={}",
                            strategyEntity.getStrategyCode(), e.getMessage(), e);
                }
            }
            loadedStrategies = true;
            log.info("完成加载 {} 个需要自动启动的策略", strategies.size());

        } catch (Exception e) {
            log.error("加载策略失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存策略到数据库
     */
    private void createAndSaveStrategy(RealTimeStrategyEntity state) {
        try {

            realTimeStrategyService.createRealTimeStrategy(state);

        } catch (Exception e) {
            log.error("保存策略到数据库失败: strategyCode={}, error={}", state.getStrategyCode(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取运行中的策略状态
     */
    public RealTimeStrategyEntity getRunningStrategy(String strategyCode, String symbol, String interval) {
        String key = buildStrategyKey(strategyCode, symbol, interval);
        return runningStrategies.get(key);
    }

    /**
     * 获取所有运行中的策略
     */
    public Map<String, RealTimeStrategyEntity> getAllRunningStrategies() {
        return new ConcurrentHashMap<>(runningStrategies);
    }

    public Map<String, Object> startExecuteRealTimeStrategy(RealTimeStrategyEntity strategyEntity) {
        Map<String, Object> response = new HashMap<>();

        response.put("strategyName", strategyEntity.getStrategyName());
        response.put("strategyCode", strategyEntity.getStrategyCode());
        response.put("symbol", strategyEntity.getSymbol());
        response.put("interval", strategyEntity.getInterval());
        response.put("tradeAmount", strategyEntity.getTradeAmount());

        response.put("startTime", strategyEntity.getStartTime());

        // 新增币种的barSeries
        String barSeriesKey = strategyEntity.getSymbol() + "_" + strategyEntity.getInterval();
        if (!runningBarSeries.containsKey(barSeriesKey)) {
            BarSeries barSeries = historicalDataService.fetchLastestedBars(strategyEntity.getSymbol(), strategyEntity.getInterval(), kLineNum);
            if (barSeries != null) {
                runningBarSeries.put(barSeriesKey, barSeries);
            }
        } else {
            response.put("message", "实时回测已经存在，跳过执行");
            response.put("status", CANCELED);
        }

        // 订阅K线数据，已订阅过会跳过
        try {
            webSocketService.subscribeKlineData(strategyEntity.getSymbol(), strategyEntity.getInterval());
        } catch (Exception e) {
            log.error("订阅K线数据失败: {}", e.getMessage(), e);
            response.put("message", "订阅K线数据失败");
            response.put("status", CANCELED);
            return response;
        }

        // 根据strategyEntity创建具体的Strategy实例
        Strategy ta4jStrategy;
        try {
            ta4jStrategy = StrategyRegisterCenter.
                    createStrategy(runningBarSeries.get(strategyEntity.getSymbol() + "_" + strategyEntity.getInterval()), strategyEntity.getStrategyCode());
            strategyEntity = realTimeStrategyRepository.save(strategyEntity);
            strategyEntity.setStrategy(ta4jStrategy);
        } catch (Exception e) {
            log.error("获取策略失败: {}", e.getMessage(), e);
            response.put("message", "获取策略失败");
            response.put("status", CANCELED);
            return response;
        }

        // 添加到运行中策略列表
        String strategyKey = buildStrategyKey(strategyEntity.getStrategyCode(), strategyEntity.getSymbol(), strategyEntity.getInterval());
        runningStrategies.put(strategyKey, strategyEntity);

        log.info("已添加策略: strategyCode={}, symbol={}, interval={}", strategyEntity.getStrategyCode(), strategyEntity.getSymbol(), strategyEntity.getInterval());

        response.put("message", "实时回测已经开始执行");
        response.put("status", SUCCESS);
        return response;

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
     * 检查symbol和interval是否还在使用中
     */
    private boolean isSymbolIntervalInUse(String symbol, String interval) {
        return runningStrategies.values().stream()
                .anyMatch(state -> state.getSymbol().equals(symbol) && state.getInterval().equals(interval));
    }

    /**
     * 构建最终结果
     */
    private Map<String, Object> buildFinalResult(RealTimeStrategyEntity state) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("status", "COMPLETED");
        result.put("totalTrades", state.getTotalTrades());
        result.put("totalProfit", state.getTotalProfit());
        result.put("successfulTrades", state.getSuccessfulTrades());
        result.put("successRate", state.getTotalTrades() > 0 ?
                (double) state.getSuccessfulTrades() / state.getTotalTrades() : 0.0);
        return result;
    }
}
