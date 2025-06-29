package com.okx.trading.controller;


import com.okx.trading.adapter.CandlestickBarSeriesConverter;
import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.entity.StrategyInfoEntity;
import com.okx.trading.service.*;
import com.okx.trading.service.impl.*;
import com.okx.trading.strategy.RealTimeStrategyManager;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.okx.trading.constant.IndicatorInfo.RUNNING;

/**
 * 实时运行策略控制器
 * 提供实时策略的CRUD操作和状态管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/real-time-strategy")
@Api(tags = "实时运行策略管理")
public class RealTimeStrategyController {

    private final HistoricalDataService historicalDataService;
    private final Ta4jBacktestService ta4jBacktestService;
    private final BacktestTradeService backtestTradeService;
    private final MarketDataService marketDataService;
    private final StrategyInfoService strategyInfoService;
    private final DeepSeekApiService deepSeekApiService;
    private final DynamicStrategyService dynamicStrategyService;
    private final JavaCompilerDynamicStrategyService javaCompilerDynamicStrategyService;
    private final SmartDynamicStrategyService smartDynamicStrategyService;
    private final StrategyConversationService strategyConversationService;
    private final CandlestickBarSeriesConverter barSeriesConverter;
    private final RealTimeOrderService realTimeOrderService;
    private final KlineCacheService klineCacheService;
    private final OkxApiService okxApiService;
    private final TradeController tradeController;
    private final RealTimeStrategyManager realTimeStrategyManager;
    private final RealTimeStrategyService realTimeStrategyService;
    private final ExecutorService realTimeTradeScheduler;
    private final ExecutorService scheduler;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public RealTimeStrategyController(HistoricalDataService historicalDataService,
                                      Ta4jBacktestService ta4jBacktestService,
                                      BacktestTradeService backtestTradeService,
                                      MarketDataService marketDataService,
                                      StrategyInfoService strategyInfoService,
                                      DeepSeekApiService deepSeekApiService,
                                      DynamicStrategyService dynamicStrategyService,
                                      JavaCompilerDynamicStrategyService javaCompilerDynamicStrategyService,
                                      SmartDynamicStrategyService smartDynamicStrategyService,
                                      StrategyConversationService strategyConversationService,
                                      CandlestickBarSeriesConverter barSeriesConverter,
                                      RealTimeOrderService realTimeOrderService,
                                      KlineCacheService klineCacheService,
                                      OkxApiService okxApiService,
                                      TradeController tradeController,
                                      RealTimeStrategyManager realTimeStrategyManager,
                                      RealTimeStrategyService realTimeStrategyService,
                                      @Qualifier("tradeIndicatorCalculateScheduler") ExecutorService scheduler,
                                      @Qualifier("realTimeTradeIndicatorCalculateScheduler") ExecutorService realTimeTradeScheduler) {
        this.historicalDataService = historicalDataService;
        this.ta4jBacktestService = ta4jBacktestService;
        this.backtestTradeService = backtestTradeService;
        this.marketDataService = marketDataService;
        this.strategyInfoService = strategyInfoService;
        this.deepSeekApiService = deepSeekApiService;
        this.dynamicStrategyService = dynamicStrategyService;
        this.javaCompilerDynamicStrategyService = javaCompilerDynamicStrategyService;
        this.smartDynamicStrategyService = smartDynamicStrategyService;
        this.strategyConversationService = strategyConversationService;
        this.barSeriesConverter = barSeriesConverter;
        this.realTimeOrderService = realTimeOrderService;
        this.klineCacheService = klineCacheService;
        this.okxApiService = okxApiService;
        this.tradeController = tradeController;
        this.realTimeStrategyManager = realTimeStrategyManager;
        this.realTimeStrategyService = realTimeStrategyService;
        this.scheduler = scheduler;
        this.realTimeTradeScheduler = realTimeTradeScheduler;
    }


    /**
     * 实时策略回测接口
     * 获取实时K线数据和历史300根K线数据进行策略回测
     * 当触发交易信号时，实时调用交易接口创建订单
     */
    @PostMapping("/real-time")
    @ApiOperation(value = "实时策略回测", notes = "基于实时K线数据进行策略回测，触发信号时自动下单")
    public com.okx.trading.model.common.ApiResponse<Map<String, Object>> realTimeBacktest(
            @ApiParam(value = "策略代码", required = true, example = "STOCHASTIC") @RequestParam String strategyCode,
            @ApiParam(value = "交易对", required = true, example = "BTC-USDT") @RequestParam String symbol,
            @ApiParam(value = "时间间隔", required = true, example = "1D") @RequestParam String interval,
            @ApiParam(value = "交易金额", required = false, example = "20") @RequestParam(required = true) BigDecimal tradeAmount) {
        try {
            if (!realTimeStrategyManager.isLoadedStrategies()) {
                return com.okx.trading.model.common.ApiResponse.error(500, "策略未加载完成，请稍后再试");
            }

            log.info("开始实时策略回测: strategyCode={}, symbol={}, interval={}", strategyCode, symbol, interval);
            LocalDateTime now = LocalDateTime.now();
            // 1. 验证策略是否存在
            Optional<StrategyInfoEntity> strategyOpt = strategyInfoService.getStrategyByCode(strategyCode);
            if (!strategyOpt.isPresent()) {
                return com.okx.trading.model.common.ApiResponse.error(404, "策略不存在: " + strategyCode);
            }
            StrategyInfoEntity strategy = strategyOpt.get();
            RealTimeStrategyEntity realTimeStrategy = new RealTimeStrategyEntity(strategyCode, symbol, interval, now, tradeAmount.doubleValue(), strategy.getStrategyName());
            realTimeStrategy.setStatus(RUNNING);
            realTimeStrategy.setIsActive(true);
            // 策略已经入库， k线订阅
            Map<String, Object> createStrategyResponse = realTimeStrategyManager.startExecuteRealTimeStrategy(realTimeStrategy);
            // 6. 返回初始状态
            return com.okx.trading.model.common.ApiResponse.success(createStrategyResponse);

        } catch (Exception e) {
            log.error("实时回测启动失败: {}", e.getMessage(), e);
            return com.okx.trading.model.common.ApiResponse.error(500, "实时回测启动失败: " + e.getMessage());
        }
    }


    /**
     * 获取实时回测订单记录
     */
    @GetMapping("/real-time/orders")
    @ApiOperation(value = "获取实时回测订单记录", notes = "查询指定策略的实时交易订单记录")
    public com.okx.trading.model.common.ApiResponse<List<RealTimeOrderEntity>> getRealTimeOrders(
            @ApiParam(value = "策略代码", required = false) @RequestParam(required = false) String strategyCode,
            @ApiParam(value = "交易对", required = false) @RequestParam(required = false) String symbol,
            @ApiParam(value = "开始时间", required = false) @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = false) @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            List<RealTimeOrderEntity> orders;

            if (StringUtils.isNotBlank(strategyCode) && StringUtils.isNotBlank(symbol)) {
                orders = realTimeOrderService.getOrdersByStrategyAndSymbol(strategyCode, symbol);
            } else if (StringUtils.isNotBlank(strategyCode)) {
                orders = realTimeOrderService.getOrdersByStrategy(strategyCode);
            } else if (StringUtils.isNotBlank(symbol)) {
                orders = realTimeOrderService.getOrdersBySymbol(symbol);
            } else if (startTime != null && endTime != null) {
                orders = realTimeOrderService.getOrdersByTimeRange(startTime, endTime);
            } else {
                orders = realTimeOrderService.getAllOrders();
            }

            return com.okx.trading.model.common.ApiResponse.success(orders);

        } catch (Exception e) {
            log.error("获取实时订单记录失败: {}", e.getMessage(), e);
            return com.okx.trading.model.common.ApiResponse.error(500, "获取实时订单记录失败: " + e.getMessage());
        }
    }


    /**
     * 更新指标分布数据
     * 从所有历史回测记录中重新计算指标分布
     */
    @PostMapping("/update-indicator-distributions")
    public com.okx.trading.model.common.ApiResponse<Map<String, Object>> updateIndicatorDistributions() {
        try {
            log.info("收到更新指标分布数据的请求");

            // 1. 查询所有有交易记录的回测数据
            List<BacktestSummaryEntity> allBacktests = backtestTradeService.getAllBacktestSummaries();
            List<BacktestSummaryEntity> validBacktests = allBacktests.stream()
                    .filter(bt -> bt.getNumberOfTrades() != null && bt.getNumberOfTrades() > 0)
                    .collect(Collectors.toList());

            if (validBacktests.isEmpty()) {
                return com.okx.trading.model.common.ApiResponse.error(400, "没有找到有效的回测数据，无法计算指标分布");
            }

            log.info("找到 {} 条有效回测记录", validBacktests.size());

            // 2. 计算关键指标的分布统计
            Map<String, Object> distributionStats = ta4jBacktestService.calculateDistributionStats(validBacktests);

            log.info("成功计算指标分布统计数据");
            return com.okx.trading.model.common.ApiResponse.success(distributionStats);

        } catch (Exception e) {
            log.error("更新指标分布数据失败: {}", e.getMessage(), e);
            return com.okx.trading.model.common.ApiResponse.error(500, "更新指标分布数据失败: " + e.getMessage());
        }
    }


    /**
     * 查看指标分布详情
     */
    @GetMapping("/indicator-distribution-details")
    public com.okx.trading.model.common.ApiResponse<Map<String, Object>> getIndicatorDistributionDetails() {
        try {
            log.info("查看指标分布详情");

            // 查询有效回测数据
            List<BacktestSummaryEntity> allBacktests = backtestTradeService.getAllBacktestSummaries();
            List<BacktestSummaryEntity> validBacktests = allBacktests.stream()
                    .filter(bt -> bt.getNumberOfTrades() != null && bt.getNumberOfTrades() > 0)
                    .collect(Collectors.toList());

            if (validBacktests.isEmpty()) {
                return com.okx.trading.model.common.ApiResponse.error(400, "没有找到有效的回测数据");
            }

            // 年化收益率详细分析
            List<BigDecimal> annualizedReturns = validBacktests.stream()
                    .map(BacktestSummaryEntity::getAnnualizedReturn)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("totalValidBacktests", validBacktests.size());
            result.put("analysisTime", LocalDateTime.now());

            // 年化收益率分析
            if (!annualizedReturns.isEmpty()) {
                Map<String, Object> returnAnalysis = new HashMap<>();
                returnAnalysis.put("sampleCount", annualizedReturns.size());
                returnAnalysis.put("min", annualizedReturns.get(0));
                returnAnalysis.put("max", annualizedReturns.get(annualizedReturns.size() - 1));
                returnAnalysis.put("p10", ta4jBacktestService.calculatePercentile(annualizedReturns, 0.10));
                returnAnalysis.put("p25", ta4jBacktestService.calculatePercentile(annualizedReturns, 0.25));
                returnAnalysis.put("p50", ta4jBacktestService.calculatePercentile(annualizedReturns, 0.50));
                returnAnalysis.put("p75", ta4jBacktestService.calculatePercentile(annualizedReturns, 0.75));
                returnAnalysis.put("p90", ta4jBacktestService.calculatePercentile(annualizedReturns, 0.90));
                result.put("annualizedReturnAnalysis", returnAnalysis);
            }

            // 最大回撤分析
            List<BigDecimal> maxDrawdowns = validBacktests.stream()
                    .map(BacktestSummaryEntity::getMaxDrawdown)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            if (!maxDrawdowns.isEmpty()) {
                Map<String, Object> drawdownAnalysis = new HashMap<>();
                drawdownAnalysis.put("sampleCount", maxDrawdowns.size());
                drawdownAnalysis.put("min", maxDrawdowns.get(0));
                drawdownAnalysis.put("max", maxDrawdowns.get(maxDrawdowns.size() - 1));
                drawdownAnalysis.put("p10", ta4jBacktestService.calculatePercentile(maxDrawdowns, 0.10));
                drawdownAnalysis.put("p25", ta4jBacktestService.calculatePercentile(maxDrawdowns, 0.25));
                drawdownAnalysis.put("p50", ta4jBacktestService.calculatePercentile(maxDrawdowns, 0.50));
                drawdownAnalysis.put("p75", ta4jBacktestService.calculatePercentile(maxDrawdowns, 0.75));
                drawdownAnalysis.put("p90", ta4jBacktestService.calculatePercentile(maxDrawdowns, 0.90));
                result.put("maxDrawdownAnalysis", drawdownAnalysis);
            }

            // 胜率分析
            List<BigDecimal> winRates = validBacktests.stream()
                    .map(BacktestSummaryEntity::getWinRate)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            if (!winRates.isEmpty()) {
                Map<String, Object> winRateAnalysis = new HashMap<>();
                winRateAnalysis.put("sampleCount", winRates.size());
                winRateAnalysis.put("min", winRates.get(0));
                winRateAnalysis.put("max", winRates.get(winRates.size() - 1));
                winRateAnalysis.put("p10", ta4jBacktestService.calculatePercentile(winRates, 0.10));
                winRateAnalysis.put("p25", ta4jBacktestService.calculatePercentile(winRates, 0.25));
                winRateAnalysis.put("p50", ta4jBacktestService.calculatePercentile(winRates, 0.50));
                winRateAnalysis.put("p75", ta4jBacktestService.calculatePercentile(winRates, 0.75));
                winRateAnalysis.put("p90", ta4jBacktestService.calculatePercentile(winRates, 0.90));
                result.put("winRateAnalysis", winRateAnalysis);
            }

            return com.okx.trading.model.common.ApiResponse.success(result);

        } catch (Exception e) {
            log.error("查看指标分布详情失败: {}", e.getMessage(), e);
            return com.okx.trading.model.common.ApiResponse.error(500, "查看指标分布详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有实时策略
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取所有实时策略", notes = "获取系统中所有的实时策略列表，包括已激活和未激活的策略")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getAllRealTimeStrategies() {
        try {
            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getAllRealTimeStrategies();
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("获取所有实时策略失败", e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有有效的实时策略
     */
    @GetMapping("/active")
    @ApiOperation(value = "获取所有有效的实时策略", notes = "获取系统中所有已激活状态的实时策略列表")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getActiveRealTimeStrategies() {
        try {
            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getActiveRealTimeStrategies();
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("获取有效实时策略失败", e);
            return com.okx.trading.util.ApiResponse.error(503, "获取有效实时策略列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取正在运行的实时策略
     */
    @GetMapping("/running")
    @ApiOperation(value = "获取正在运行的实时策略", notes = "获取系统中所有状态为RUNNING的实时策略列表")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getRunningRealTimeStrategies() {
        try {
            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getRunningRealTimeStrategies();
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("获取运行中实时策略失败", e);
            return com.okx.trading.util.ApiResponse.error(503, "获取运行中实时策略列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据策略代码获取实时策略
     */
    @GetMapping("/code/{strategyCode}")
    @ApiOperation(value = "根据策略代码获取实时策略", notes = "通过唯一的策略代码查询特定的实时策略详情")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> getRealTimeStrategyByCode(
            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
            }

            Optional<RealTimeStrategyEntity> strategy = realTimeStrategyService.getRealTimeStrategyByCode(strategyCode);
            if (strategy.isPresent()) {
                return com.okx.trading.util.ApiResponse.success(strategy.get());
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "策略不存在: " + strategyCode);
            }
        } catch (Exception e) {
            log.error("根据策略代码获取实时策略失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取实时策略
     */
    @GetMapping("/id/{id}")
    @ApiOperation(value = "根据ID获取实时策略", notes = "通过数据库主键ID查询特定的实时策略详情")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 400, message = "策略ID不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> getRealTimeStrategyById(
            @ApiParam(value = "策略ID", required = true, example = "1") @PathVariable Long id) {
        try {
            if (id == null) {
                return com.okx.trading.util.ApiResponse.error(503, "策略ID不能为空");
            }

            Optional<RealTimeStrategyEntity> strategy = realTimeStrategyService.getRealTimeStrategyById(id);
            if (strategy.isPresent()) {
                return com.okx.trading.util.ApiResponse.success(strategy.get());
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "策略不存在，ID: " + id);
            }
        } catch (Exception e) {
            log.error("根据ID获取实时策略失败: {}", id, e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 根据策略信息代码获取有效的实时策略
     */
    @GetMapping("/info-code/{strategyCode}")
    @ApiOperation(value = "根据策略信息代码获取有效的实时策略", notes = "通过策略信息代码查询所有关联的有效实时策略")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 400, message = "策略信息代码不能为空"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getActiveRealTimeStrategiesByInfoCode(
            @ApiParam(value = "策略信息代码", required = true, example = "MA_CROSS_STRATEGY") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略信息代码不能为空");
            }

            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getActiveRealTimeStrategiesByCode(strategyCode);
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("根据策略信息代码获取实时策略失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 根据交易对获取有效的实时策略
     */
    @GetMapping("/symbol/{symbol}")
    @ApiOperation(value = "根据交易对获取有效的实时策略", notes = "通过交易对符号查询所有关联的有效实时策略")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 400, message = "交易对符号不能为空"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getActiveRealTimeStrategiesBySymbol(
            @ApiParam(value = "交易对符号", required = true, example = "BTC-USDT") @PathVariable String symbol) {
        try {
            if (StringUtils.isBlank(symbol)) {
                return com.okx.trading.util.ApiResponse.error(503, "交易对符号不能为空");
            }

            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getActiveRealTimeStrategiesBySymbol(symbol);
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("根据交易对获取实时策略失败: {}", symbol, e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 根据状态获取实时策略
     */
    @GetMapping("/status/{status}")
    @ApiOperation(value = "根据状态获取实时策略", notes = "通过运行状态查询所有匹配的实时策略")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 400, message = "运行状态不能为空"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getRealTimeStrategiesByStatus(
            @ApiParam(value = "运行状态", required = true, example = "RUNNING", allowableValues = "RUNNING,STOPPED,COMPLETED,ERROR") @PathVariable String status) {
        try {
            if (StringUtils.isBlank(status)) {
                return com.okx.trading.util.ApiResponse.error(503, "运行状态不能为空");
            }

            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getRealTimeStrategiesByStatus(status);
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("根据状态获取实时策略失败: {}", status, e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定时间范围内创建的实时策略
     */
    @GetMapping("/time-range")
    @ApiOperation(value = "获取指定时间范围内创建的实时策略", notes = "查询在指定时间范围内创建的所有实时策略")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 400, message = "时间参数错误"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getRealTimeStrategiesByTimeRange(
            @ApiParam(value = "开始时间", required = true, example = "2024-01-01 00:00:00") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间", required = true, example = "2024-12-31 23:59:59") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            if (startTime == null || endTime == null) {
                return com.okx.trading.util.ApiResponse.error(503, "开始时间和结束时间不能为空");
            }

            if (startTime.isAfter(endTime)) {
                return com.okx.trading.util.ApiResponse.error(503, "开始时间不能晚于结束时间");
            }

            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getRealTimeStrategiesByTimeRange(startTime, endTime);
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("根据时间范围获取实时策略失败: {} - {}", startTime, endTime, e);
            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 更新实时策略
     */
    @PutMapping("/update")
    @ApiOperation(value = "更新实时策略", notes = "更新现有实时策略的配置信息")
    @ApiResponses({
            @ApiResponse(code = 200, message = "更新成功"),
            @ApiResponse(code = 400, message = "参数错误"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> updateRealTimeStrategy(
            @ApiParam(value = "实时策略实体", required = true) @RequestBody RealTimeStrategyEntity realTimeStrategy) {
        try {
            if (realTimeStrategy == null) {
                return com.okx.trading.util.ApiResponse.error(503, "实时策略不能为空");
            }
            if (realTimeStrategy.getId() == null) {
                return com.okx.trading.util.ApiResponse.error(503, "策略ID不能为空");
            }

            RealTimeStrategyEntity updated = realTimeStrategyService.updateRealTimeStrategy(realTimeStrategy);
            return com.okx.trading.util.ApiResponse.success(updated);
        } catch (Exception e) {
            log.error("更新实时策略失败", e);
            return com.okx.trading.util.ApiResponse.error(503, "更新实时策略失败: " + e.getMessage());
        }
    }

    /**
     * 启动实时策略
     */
    @PostMapping("/start/{id}")
    @ApiOperation(value = "启动实时策略", notes = "启动指定的实时策略，将状态设置为RUNNING")
    @ApiResponses({
            @ApiResponse(code = 200, message = "启动成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<String> startRealTimeStrategy(
            @ApiParam(value = "策略id", required = true, example = "58") @PathVariable String id) {
        try {

            boolean success = realTimeStrategyService.startRealTimeStrategy(Long.parseLong(id));
            if (success) {
                return com.okx.trading.util.ApiResponse.success("启动策略成功: " + id);
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "启动策略失败: " + id);
            }
        } catch (Exception e) {
            log.error("启动实时策略失败: {}", id, e);
            return com.okx.trading.util.ApiResponse.error(503, "启动策略失败: " + e.getMessage());
        }
    }

    /**
     * 停止实时策略
     */
    @PostMapping("/stop/{strategyCode}")
    @ApiOperation(value = "停止实时策略", notes = "停止指定的实时策略，将状态设置为STOPPED")
    @ApiResponses({
            @ApiResponse(code = 200, message = "停止成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<String> stopRealTimeStrategy(
            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
            }

            boolean success = realTimeStrategyService.stopRealTimeStrategy(strategyCode);
            if (success) {
                return com.okx.trading.util.ApiResponse.success("停止策略成功: " + strategyCode);
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "停止策略失败: " + strategyCode);
            }
        } catch (Exception e) {
            log.error("停止实时策略失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "停止策略失败: " + e.getMessage());
        }
    }

    /**
     * 激活策略
     */
    @PostMapping("/activate/{strategyCode}")
    @ApiOperation(value = "激活策略", notes = "激活指定的实时策略，将isActive设置为true")
    @ApiResponses({
            @ApiResponse(code = 200, message = "激活成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<String> activateStrategy(
            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
            }

            boolean success = realTimeStrategyService.activateStrategy(strategyCode);
            if (success) {
                return com.okx.trading.util.ApiResponse.success("激活策略成功: " + strategyCode);
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "激活策略失败: " + strategyCode);
            }
        } catch (Exception e) {
            log.error("激活策略失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "激活策略失败: " + e.getMessage());
        }
    }

    /**
     * 停用策略
     */
    @PostMapping("/deactivate/{strategyCode}")
    @ApiOperation(value = "停用策略", notes = "停用指定的实时策略，将isActive设置为false")
    @ApiResponses({
            @ApiResponse(code = 200, message = "停用成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<String> deactivateStrategy(
            @ApiParam("策略代码") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
            }

            boolean success = realTimeStrategyService.deactivateStrategy(strategyCode);
            if (success) {
                return com.okx.trading.util.ApiResponse.success("停用策略成功: " + strategyCode);
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "停用策略失败: " + strategyCode);
            }
        } catch (Exception e) {
            log.error("停用策略失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "停用策略失败: " + e.getMessage());
        }
    }

    /**
     * 删除实时策略
     */
    @DeleteMapping("/delete/{strategyCode}")
    @ApiOperation(value = "删除实时策略", notes = "永久删除指定的实时策略记录")
    @ApiResponses({
            @ApiResponse(code = 200, message = "删除成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 404, message = "策略不存在"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<String> deleteRealTimeStrategy(
            @ApiParam("策略代码") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
            }

            boolean success = realTimeStrategyService.deleteRealTimeStrategy(strategyCode);
            if (success) {
                return com.okx.trading.util.ApiResponse.success("删除策略成功: " + strategyCode);
            } else {
                return com.okx.trading.util.ApiResponse.error(503, "删除策略失败: " + strategyCode);
            }
        } catch (Exception e) {
            log.error("删除实时策略失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "删除策略失败: " + e.getMessage());
        }
    }

    /**
     * 检查策略代码是否已存在
     */
    @GetMapping("/exists/{strategyCode}")
    @ApiOperation(value = "检查策略代码是否已存在", notes = "验证指定的策略代码是否已被使用")
    @ApiResponses({
            @ApiResponse(code = 200, message = "检查成功"),
            @ApiResponse(code = 400, message = "策略代码不能为空"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<Boolean> existsByStrategyCode(
            @ApiParam("策略代码") @PathVariable String strategyCode) {
        try {
            if (StringUtils.isBlank(strategyCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
            }

            boolean exists = realTimeStrategyService.existsByStrategyCode(strategyCode);
            return com.okx.trading.util.ApiResponse.success(exists);
        } catch (Exception e) {
            log.error("检查策略代码是否存在失败: {}", strategyCode, e);
            return com.okx.trading.util.ApiResponse.error(503, "检查策略代码失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否存在运行中的策略
     */
    @GetMapping("/has-running")
    @ApiOperation(value = "检查是否存在运行中的策略", notes = "检查指定策略信息代码和交易对是否有正在运行的策略")
    @ApiResponses({
            @ApiResponse(code = 200, message = "检查成功"),
            @ApiResponse(code = 400, message = "参数不能为空"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<Boolean> hasRunningStrategy(
            @ApiParam(value = "策略信息代码", required = true, example = "MA_CROSS_STRATEGY") @RequestParam String strategyInfoCode,
            @ApiParam(value = "交易对符号", required = true, example = "BTC-USDT") @RequestParam String symbol) {
        try {
            if (StringUtils.isBlank(strategyInfoCode)) {
                return com.okx.trading.util.ApiResponse.error(503, "策略信息代码不能为空");
            }
            if (StringUtils.isBlank(symbol)) {
                return com.okx.trading.util.ApiResponse.error(503, "交易对符号不能为空");
            }

            boolean hasRunning = realTimeStrategyService.hasRunningStrategy(strategyInfoCode, symbol);
            return com.okx.trading.util.ApiResponse.success(hasRunning);
        } catch (Exception e) {
            log.error("检查运行中策略失败: {} - {}", strategyInfoCode, symbol, e);
            return com.okx.trading.util.ApiResponse.error(503, "检查运行中策略失败: " + e.getMessage());
        }
    }

    /**
     * 获取需要自动启动的策略
     */
    @GetMapping("/auto-start")
    @ApiOperation(value = "获取需要自动启动的策略", notes = "获取所有标记为自动启动且状态为RUNNING的策略列表")
    @ApiResponses({
            @ApiResponse(code = 200, message = "获取成功"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getStrategiesToAutoStart() {
        try {
            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getStrategiesToAutoStart();
            return com.okx.trading.util.ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("获取自动启动策略失败", e);
            return com.okx.trading.util.ApiResponse.error(503, "获取自动启动策略失败: " + e.getMessage());
        }
    }
}
