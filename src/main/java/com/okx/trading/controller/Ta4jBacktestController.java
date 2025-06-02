package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.BacktestResultDTO;
import com.okx.trading.model.entity.BacktestTradeEntity;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.entity.BacktestSummaryEntity;
import com.okx.trading.service.BacktestTradeService;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.MarketDataService;
import com.okx.trading.ta4j.Ta4jBacktestService;
import com.okx.trading.ta4j.strategy.StrategyFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ta4j回测控制器
 * 专门用于Ta4j库的回测及结果存储
 */
@Slf4j
@RestController
@RequestMapping("/api/backtest/ta4j")
@Api(tags = "Ta4j回测控制器", description = "提供基于Ta4j库的策略回测及结果存储接口")
@RequiredArgsConstructor
public class Ta4jBacktestController {

    private final HistoricalDataService historicalDataService;
    private final Ta4jBacktestService ta4jBacktestService;
    private final BacktestTradeService backtestTradeService;
    private final MarketDataService marketDataService;

    @GetMapping("/run")
    @ApiOperation(value = "执行Ta4j策略回测", notes = "使用Ta4j库进行策略回测，可选保存结果")
    public ApiResponse<BacktestResultDTO> runBacktest(
            @ApiParam(value = "交易对", defaultValue = "BTC-USDT", required = true, type = "string") @RequestParam String symbol,
            @ApiParam(value = "时间间隔",defaultValue = "1h", required = true, type = "string") @RequestParam String interval,
            @ApiParam(value = "开始时间 (格式: yyyy-MM-dd HH:mm:ss)",
                defaultValue = "2018-01-01 00:00:00",
                example = "2018-01-01 00:00:00",
                required = true,
                type = "string")
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam(value = "结束时间 (格式: yyyy-MM-dd HH:mm:ss)",
                defaultValue = "2025-04-01 00:00:00",
                example = "2025-04-01 00:00:00",
                required = true,
                type = "string")
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @ApiParam(value = "策略类型",
                   required = true,
                   allowableValues = "SMA,BOLLINGER,MACD,RSI,STOCHASTIC,ADX,CCI,WILLIAMS_R,TRIPLE_EMA,ICHIMOKU,PARABOLIC_SAR,CHANDELIER_EXIT",
                   example = "BOLLINGER",
                   type = "string")
            @RequestParam String strategyType,
            @ApiParam(value = "策略参数 (以逗号分隔的数字)\n" +
                         "- SMA策略参数: 短期均线周期,长期均线周期 (例如：5,20)\n" +
                         "- BOLLINGER策略参数: 周期,标准差倍数 (例如：20,2.0)\n" +
                         "- MACD策略参数: 短周期,长周期,信号周期 (例如：12,26,9)\n" +
                         "- RSI策略参数: RSI周期,超卖阈值,超买阈值 (例如：14,30,70)\n" +
                         "- STOCHASTIC策略参数: K周期,%K平滑周期,%D平滑周期,超卖阈值,超买阈值 (例如：14,3,3,20,80)\n" +
                         "- ADX策略参数: ADX周期,DI周期,阈值 (例如：14,14,25)\n" +
                         "- CCI策略参数: CCI周期,超卖阈值,超买阈值 (例如：20,-100,100)\n" +
                         "- WILLIAMS_R策略参数: 周期,超卖阈值,超买阈值 (例如：14,-80,-20)\n" +
                         "- TRIPLE_EMA策略参数: 短期EMA,中期EMA,长期EMA (例如：5,10,20)\n" +
                         "- ICHIMOKU策略参数: 转换线周期,基准线周期,延迟跨度 (例如：9,26,52)\n" +
                         "- PARABOLIC_SAR策略参数: 步长,最大步长 (例如：0.02,0.2)\n" +
                         "- CHANDELIER_EXIT策略参数: 周期,乘数 (例如：22,3.0)\n" +
                         "- 不传或传空字符串将使用默认参数",
                   required = false,
                   example = "20,2.0",
                   type = "string")
            @RequestParam(required = false) String strategyParams,
            @ApiParam(value = "初始资金",
                   defaultValue = "100000",
                   required = true,
                   type = "number",
                   format = "decimal")
            @RequestParam BigDecimal initialAmount,
            @ApiParam(value = "是否保存结果",
                   required = true,
                   defaultValue = "true",
                   type = "boolean")
            @RequestParam(defaultValue = "true") boolean saveResult) {

        log.info("开始执行Ta4j回测，交易对: {}, 间隔: {}, 时间范围: {} - {}, 策略: {}, 参数: {}, 初始资金: {}",
                symbol, interval, startTime, endTime, strategyType, strategyParams, initialAmount);

        try {
            // 使用策略工厂验证策略类型
            String supportedStrategies = "SMA, BOLLINGER, MACD, RSI, STOCHASTIC, ADX, CCI, WILLIAMS_R, TRIPLE_EMA, ICHIMOKU, PARABOLIC_SAR, CHANDELIER_EXIT";
            if (!isValidStrategyType(strategyType)) {
                return ApiResponse.error(400, "无效的策略类型: " + strategyType +
                        "，支持的策略类型: " + supportedStrategies);
            }

            // 如果策略参数为空，使用默认参数
            if (strategyParams == null || strategyParams.trim().isEmpty()) {
                strategyParams = getDefaultParams(strategyType);
                log.info("使用默认参数: {}", strategyParams);
            }

            // 验证策略参数
            if (!validateStrategyParams(strategyType, strategyParams)) {
                return ApiResponse.error(400, "无效的策略参数: " + strategyParams +
                        "，正确格式: " + getStrategyParamsDescription(strategyType));
            }

            // 获取历史数据
            List<CandlestickEntity> candlesticks = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);
            if (candlesticks == null || candlesticks.isEmpty()) {
                return ApiResponse.error(404, "未找到指定条件的历史数据");
            }

            // 执行回测
            BacktestResultDTO result = ta4jBacktestService.backtest(candlesticks, strategyType, initialAmount, strategyParams);

            // 如果需要保存结果到数据库
            if (saveResult && result.isSuccess()) {
                // 保存交易明细
                String backtestId = backtestTradeService.saveBacktestTrades(symbol,result, strategyParams);

                // 保存汇总信息
                backtestTradeService.saveBacktestSummary(result, strategyParams, symbol, interval, startTime, endTime, backtestId);

                result.setParameterDescription(result.getParameterDescription() + " (BacktestID: " + backtestId + ")");

                // 打印回测ID信息
                log.info("回测结果已保存，回测ID: {}", backtestId);
            }

            // 打印总体执行信息
            if (result.isSuccess()) {
                log.info("回测执行成功 - {} {}，交易次数: {}，总收益率: {:.2f}%",
                    result.getStrategyName(),
                    result.getParameterDescription(),
                    result.getNumberOfTrades(),
                    result.getTotalReturn().multiply(new BigDecimal("100")));
            } else {
                log.warn("回测执行失败 - 错误信息: {}", result.getErrorMessage());
            }

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("回测过程中发生错误: {}", e.getMessage(), e);
            return ApiResponse.error(500, "回测过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 验证策略类型是否有效
     * @param strategyType 策略类型
     * @return 是否有效
     */
    private boolean isValidStrategyType(String strategyType) {
        return strategyType != null && (
                strategyType.equals(StrategyFactory.STRATEGY_SMA) ||
                strategyType.equals(StrategyFactory.STRATEGY_BOLLINGER_BANDS) ||
                strategyType.equals(StrategyFactory.STRATEGY_MACD) ||
                strategyType.equals(StrategyFactory.STRATEGY_RSI) ||
                strategyType.equals(StrategyFactory.STRATEGY_STOCHASTIC) ||
                strategyType.equals(StrategyFactory.STRATEGY_ADX) ||
                strategyType.equals(StrategyFactory.STRATEGY_CCI) ||
                strategyType.equals(StrategyFactory.STRATEGY_WILLIAMS_R) ||
                strategyType.equals(StrategyFactory.STRATEGY_TRIPLE_EMA) ||
                strategyType.equals(StrategyFactory.STRATEGY_ICHIMOKU) ||
                strategyType.equals(StrategyFactory.STRATEGY_PARABOLIC_SAR) ||
                strategyType.equals(StrategyFactory.STRATEGY_CHANDELIER_EXIT)
        );
    }

    /**
     * 验证策略参数是否合法
     * @param strategyType 策略类型
     * @param params 策略参数
     * @return 是否合法
     */
    private boolean validateStrategyParams(String strategyType, String params) {
        try {
            // 使用策略工厂验证参数
            Map<String, Object> paramMap = StrategyFactory.parseParams(strategyType, params);
            return paramMap != null && !paramMap.isEmpty();
        } catch (Exception e) {
            log.warn("验证策略参数失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取策略参数说明
     * @param strategyType 策略类型
     * @return 参数说明
     */
    private String getStrategyParamsDescription(String strategyType) {
        return StrategyFactory.getStrategyParamsDescription(strategyType);
    }

    /**
     * 获取策略的默认参数
     * @param strategyType 策略类型
     * @return 默认参数字符串
     */
    private String getDefaultParams(String strategyType) {
        switch (strategyType) {
            case StrategyFactory.STRATEGY_SMA:
                return "9,21"; // 短期均线周期,长期均线周期
            case StrategyFactory.STRATEGY_BOLLINGER_BANDS:
                return "20,2.0"; // 周期,标准差倍数
            case StrategyFactory.STRATEGY_MACD:
                return "12,26,9"; // 短周期,长周期,信号周期
            case StrategyFactory.STRATEGY_RSI:
                return "14,30,70"; // RSI周期,超卖阈值,超买阈值
            case StrategyFactory.STRATEGY_STOCHASTIC:
                return "14,3,3,20,80"; // K周期,%K平滑周期,%D平滑周期,超卖阈值,超买阈值
            case StrategyFactory.STRATEGY_ADX:
                return "14,14,25"; // ADX周期,DI周期,阈值
            case StrategyFactory.STRATEGY_CCI:
                return "20,-100,100"; // CCI周期,超卖阈值,超买阈值
            case StrategyFactory.STRATEGY_WILLIAMS_R:
                return "14,-80,-20"; // 周期,超卖阈值,超买阈值
            case StrategyFactory.STRATEGY_TRIPLE_EMA:
                return "5,10,20"; // 短期EMA,中期EMA,长期EMA
            case StrategyFactory.STRATEGY_ICHIMOKU:
                return "9,26,52"; // 转换线周期,基准线周期,延迟跨度
            case StrategyFactory.STRATEGY_PARABOLIC_SAR:
                return "0.02,0.2"; // 步长,最大步长
            case StrategyFactory.STRATEGY_CHANDELIER_EXIT:
                return "22,3.0"; // 周期,乘数
            default:
                return "";
        }
    }

    @GetMapping("/strategies")
    @ApiOperation(value = "获取支持的策略类型和参数说明", notes = "返回系统支持的所有策略类型和对应的参数说明")
    public ApiResponse<Map<String, Map<String, String>>> getStrategies() {
        try {
            Map<String, Map<String, String>> strategies = new HashMap<>();

            // SMA策略
            Map<String, String> smaInfo = new HashMap<>();
            smaInfo.put("name", "简单移动平均线策略");
            smaInfo.put("description", "基于短期和长期移动平均线的交叉信号产生买卖信号");
            smaInfo.put("params", StrategyFactory.SMA_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_SMA, smaInfo);

            // 布林带策略
            Map<String, String> bollingerInfo = new HashMap<>();
            bollingerInfo.put("name", "布林带策略");
            bollingerInfo.put("description", "基于价格突破布林带上下轨或回归中轨产生买卖信号");
            bollingerInfo.put("params", StrategyFactory.BOLLINGER_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_BOLLINGER_BANDS, bollingerInfo);

            // MACD策略
            Map<String, String> macdInfo = new HashMap<>();
            macdInfo.put("name", "MACD策略");
            macdInfo.put("description", "基于MACD线与信号线的交叉以及柱状图的变化产生买卖信号");
            macdInfo.put("params", StrategyFactory.MACD_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_MACD, macdInfo);

            // RSI策略
            Map<String, String> rsiInfo = new HashMap<>();
            rsiInfo.put("name", "RSI相对强弱指标策略");
            rsiInfo.put("description", "基于RSI指标的超买超卖区域产生买卖信号");
            rsiInfo.put("params", StrategyFactory.RSI_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_RSI, rsiInfo);

            // 随机指标策略
            Map<String, String> stochasticInfo = new HashMap<>();
            stochasticInfo.put("name", "随机指标策略");
            stochasticInfo.put("description", "基于随机指标的K线与D线交叉以及超买超卖区域产生买卖信号");
            stochasticInfo.put("params", StrategyFactory.STOCHASTIC_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_STOCHASTIC, stochasticInfo);

            // ADX策略
            Map<String, String> adxInfo = new HashMap<>();
            adxInfo.put("name", "ADX趋向指标策略");
            adxInfo.put("description", "基于ADX趋向指标和DI方向指标的变化产生买卖信号");
            adxInfo.put("params", StrategyFactory.ADX_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_ADX, adxInfo);

            // CCI策略
            Map<String, String> cciInfo = new HashMap<>();
            cciInfo.put("name", "CCI顺势指标策略");
            cciInfo.put("description", "基于CCI指标的超买超卖区域产生买卖信号");
            cciInfo.put("params", StrategyFactory.CCI_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_CCI, cciInfo);

            // Williams %R策略
            Map<String, String> williamsInfo = new HashMap<>();
            williamsInfo.put("name", "威廉指标策略");
            williamsInfo.put("description", "基于威廉指标的超买超卖区域产生买卖信号");
            williamsInfo.put("params", StrategyFactory.WILLIAMS_R_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_WILLIAMS_R, williamsInfo);

            // 三重EMA策略
            Map<String, String> tripleEmaInfo = new HashMap<>();
            tripleEmaInfo.put("name", "三重EMA策略");
            tripleEmaInfo.put("description", "基于三条不同周期的指数移动平均线之间的关系产生买卖信号");
            tripleEmaInfo.put("params", StrategyFactory.TRIPLE_EMA_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_TRIPLE_EMA, tripleEmaInfo);

            // 一目均衡表策略
            Map<String, String> ichimokuInfo = new HashMap<>();
            ichimokuInfo.put("name", "一目均衡表策略");
            ichimokuInfo.put("description", "基于日本一目均衡表的云层、转换线和基准线之间的关系产生买卖信号");
            ichimokuInfo.put("params", StrategyFactory.ICHIMOKU_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_ICHIMOKU, ichimokuInfo);

            // 抛物线SAR策略
            Map<String, String> pSarInfo = new HashMap<>();
            pSarInfo.put("name", "抛物线SAR策略");
            pSarInfo.put("description", "基于抛物线转向系统(SAR)指标的方向变化产生买卖信号");
            pSarInfo.put("params", StrategyFactory.PARABOLIC_SAR_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_PARABOLIC_SAR, pSarInfo);

            // 吊灯线退出策略
            Map<String, String> chandelierInfo = new HashMap<>();
            chandelierInfo.put("name", "吊灯线退出策略");
            chandelierInfo.put("description", "基于ATR波动率的吊灯线退出法则产生主要用于止损的卖出信号");
            chandelierInfo.put("params", StrategyFactory.CHANDELIER_EXIT_PARAMS_DESC);
            strategies.put(StrategyFactory.STRATEGY_CHANDELIER_EXIT, chandelierInfo);

            return ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("获取策略信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取策略信息出错: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    @ApiOperation(value = "获取回测历史记录", notes = "获取所有已保存的回测历史ID")
    public ApiResponse<List<String>> getBacktestHistory() {
        try {
            List<String> backtestIds = backtestTradeService.getAllBacktestIds();
            return ApiResponse.success(backtestIds);
        } catch (Exception e) {
            log.error("获取回测历史记录出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测历史记录出错: " + e.getMessage());
        }
    }

    @GetMapping("/detail/{backtestId}")
    @ApiOperation(value = "获取回测详情", notes = "获取指定回测ID的详细交易记录")
    public ApiResponse<List<BacktestTradeEntity>> getBacktestDetail(
            @ApiParam(value = "回测ID", required = true, type = "string") @PathVariable String backtestId) {
        try {
            List<BacktestTradeEntity> trades = backtestTradeService.getTradesByBacktestId(backtestId);
            if (trades.isEmpty()) {
                return ApiResponse.error(404, "未找到指定回测ID的交易记录");
            }
            return ApiResponse.success(trades);
        } catch (Exception e) {
            log.error("获取回测详情出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测详情出错: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{backtestId}")
    @ApiOperation(value = "删除回测记录", notes = "删除指定回测ID的所有交易记录")
    public ApiResponse<Void> deleteBacktestRecord(
            @ApiParam(value = "回测ID", required = true, type = "string") @PathVariable String backtestId) {
        try {
            backtestTradeService.deleteBacktestRecords(backtestId);
            return ApiResponse.success("成功删除回测记录", null);
        } catch (Exception e) {
            log.error("删除回测记录出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "删除回测记录出错: " + e.getMessage());
        }
    }

    @GetMapping("/summaries")
    @ApiOperation(value = "获取所有回测汇总信息", notes = "获取所有已保存的回测汇总信息")
    public ApiResponse<List<BacktestSummaryEntity>> getAllBacktestSummaries() {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getAllBacktestSummaries();
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测汇总信息出错: " + e.getMessage());
        }
    }

    @GetMapping("/summary/{backtestId}")
    @ApiOperation(value = "获取回测汇总信息", notes = "根据回测ID获取回测汇总信息")
    public ApiResponse<BacktestSummaryEntity> getBacktestSummary(
            @ApiParam(value = "回测ID", required = true, type = "string") @PathVariable String backtestId) {
        try {
            Optional<BacktestSummaryEntity> summary = backtestTradeService.getBacktestSummaryById(backtestId);
            if (summary.isPresent()) {
                return ApiResponse.success(summary.get());
            } else {
                return ApiResponse.error(404, "未找到指定回测ID的汇总信息");
            }
        } catch (Exception e) {
            log.error("获取回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取回测汇总信息出错: " + e.getMessage());
        }
    }

    @GetMapping("/summaries/strategy/{strategyName}")
    @ApiOperation(value = "根据策略名称获取回测汇总信息", notes = "获取特定策略的所有回测汇总信息")
    public ApiResponse<List<BacktestSummaryEntity>> getBacktestSummariesByStrategy(
            @ApiParam(value = "策略名称", required = true, type = "string") @PathVariable String strategyName) {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getBacktestSummariesByStrategy(strategyName);
            if (summaries.isEmpty()) {
                return ApiResponse.error(404, "未找到该策略的回测汇总信息");
            }
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取策略回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取策略回测汇总信息出错: " + e.getMessage());
        }
    }

    @GetMapping("/summaries/symbol/{symbol}")
    @ApiOperation(value = "根据交易对获取回测汇总信息", notes = "获取特定交易对的所有回测汇总信息")
    public ApiResponse<List<BacktestSummaryEntity>> getBacktestSummariesBySymbol(
            @ApiParam(value = "交易对", required = true, type = "string") @PathVariable String symbol) {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getBacktestSummariesBySymbol(symbol);
            if (summaries.isEmpty()) {
                return ApiResponse.error(404, "未找到该交易对的回测汇总信息");
            }
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取交易对回测汇总信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取交易对回测汇总信息出错: " + e.getMessage());
        }
    }

    @GetMapping("/summaries/best")
    @ApiOperation(value = "获取最佳表现的回测", notes = "根据策略名称和交易对获取表现最好的回测")
    public ApiResponse<List<BacktestSummaryEntity>> getBestPerformingBacktests(
            @ApiParam(value = "策略名称", required = true, type = "string") @RequestParam String strategyName,
            @ApiParam(value = "交易对", required = true, type = "string") @RequestParam String symbol) {
        try {
            List<BacktestSummaryEntity> summaries = backtestTradeService.getBestPerformingBacktests(strategyName, symbol);
            if (summaries.isEmpty()) {
                return ApiResponse.error(404, "未找到符合条件的回测汇总信息");
            }
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("获取最佳表现回测信息出错: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取最佳表现回测信息出错: " + e.getMessage());
        }
    }
}
