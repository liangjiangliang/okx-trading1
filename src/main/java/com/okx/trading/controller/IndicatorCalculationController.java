//package com.okx.trading.controller;
//
//import com.okx.trading.service.IndicatorCalculationService;
//import com.okx.trading.util.ApiResponse;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * 指标计算控制器
// * 提供技术指标计算相关的API接口
// */
//@RestController
//@RequestMapping("/indicator")
//@Api(tags = "技术指标计算接口")
//public class IndicatorCalculationController {
//
//    private static final Logger logger = LoggerFactory.getLogger(IndicatorCalculationController.class);
//
//    /**
//     * 指标计算服务
//     */
//    private final IndicatorCalculationService indicatorCalculationService;
//
//    /**
//     * 构造函数注入
//     * @param indicatorCalculationService 指标计算服务
//     */
//    public IndicatorCalculationController(IndicatorCalculationService indicatorCalculationService) {
//        this.indicatorCalculationService = indicatorCalculationService;
//    }
//
//    /**
//     * 订阅指标计算
//     *
//     * @param symbol   交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 操作结果
//     */
//    @PostMapping("/subscribe")
//    @ApiOperation("订阅指标计算")
//    public ApiResponse<?> subscribeIndicator(
//            @ApiParam(value = "交易对符号", required = true) @RequestParam String symbol,
//            @ApiParam(value = "K线间隔", required = true) @RequestParam String interval) {
//
//        boolean success = indicatorCalculationService.subscribeIndicatorCalculation(symbol, interval);
//
//        if (success) {
//            logger.info("成功订阅指标计算: {} {}", symbol, interval);
//            return ApiResponse.success("成功订阅指标计算: " + symbol + " " + interval, null);
//        } else {
//            logger.warn("订阅指标计算失败: {} {}", symbol, interval);
//            return ApiResponse.error(400, "订阅指标计算失败: " + symbol + " " + interval);
//        }
//    }
//
//    /**
//     * 取消订阅指标计算
//     *
//     * @param symbol   交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 操作结果
//     */
//    @PostMapping("/unsubscribe")
//    @ApiOperation("取消订阅指标计算")
//    public ApiResponse<?> unsubscribeIndicator(
//            @ApiParam(value = "交易对符号", required = true) @RequestParam String symbol,
//            @ApiParam(value = "K线间隔", required = true) @RequestParam String interval) {
//
//        boolean success = indicatorCalculationService.unsubscribeIndicatorCalculation(symbol, interval);
//
//        if (success) {
//            logger.info("成功取消订阅指标计算: {} {}", symbol, interval);
//            return ApiResponse.success("成功取消订阅指标计算: " + symbol + " " + interval, null);
//        } else {
//            logger.warn("取消订阅指标计算失败: {} {}", symbol, interval);
//            return ApiResponse.error(400, "取消订阅指标计算失败: " + symbol + " " + interval);
//        }
//    }
//
//    /**
//     * 获取所有已订阅的指标计算任务
//     *
//     * @return 已订阅的指标计算任务
//     */
//    @GetMapping("/subscriptions")
//    @ApiOperation("获取所有已订阅的指标计算任务")
//    public ApiResponse<Map<String, List<String>>> getAllSubscriptions() {
//        Map<String, List<String>> subscriptions = indicatorCalculationService.getAllSubscribedIndicators();
//        return ApiResponse.success("获取订阅列表成功", subscriptions);
//    }
//
//    /**
//     * 获取MACD指标
//     *
//     * @param symbol   交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return MACD指标数据
//     */
//    @GetMapping("/macd")
//    @ApiOperation("获取MACD指标")
//    public ApiResponse<Map<String, Object>> getMACDIndicator(
//            @ApiParam(value = "交易对符号", required = true) @RequestParam String symbol,
//            @ApiParam(value = "K线间隔", required = true) @RequestParam String interval) {
//
//        Map<String, Object> macdData = indicatorCalculationService.getMACDIndicator(symbol, interval);
//
//        if (macdData != null && !macdData.isEmpty()) {
//            return ApiResponse.success("获取MACD指标成功", macdData);
//        } else {
//            return ApiResponse.error(404, "获取MACD指标失败");
//        }
//    }
//
//    /**
//     * 获取RSI指标
//     *
//     * @param symbol   交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @param period   RSI周期，默认为14
//     * @return RSI指标数据
//     */
//    @GetMapping("/rsi")
//    @ApiOperation("获取RSI指标")
//    public ApiResponse<List<Double>> getRSIIndicator(
//            @ApiParam(value = "交易对符号", required = true) @RequestParam String symbol,
//            @ApiParam(value = "K线间隔", required = true) @RequestParam String interval,
//            @ApiParam(value = "RSI周期", defaultValue = "14") @RequestParam(defaultValue = "14") int period) {
//
//        List<Double> rsiData = indicatorCalculationService.getRSIIndicator(symbol, interval, period);
//
//        if (rsiData != null && !rsiData.isEmpty()) {
//            return ApiResponse.success("获取RSI指标成功", rsiData);
//        } else {
//            return ApiResponse.error(404, "获取RSI指标失败");
//        }
//    }
//
//    /**
//     * 获取KDJ指标
//     *
//     * @param symbol   交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return KDJ指标数据
//     */
//    @GetMapping("/kdj")
//    @ApiOperation("获取KDJ指标")
//    public ApiResponse<Map<String, Object>> getKDJIndicator(
//            @ApiParam(value = "交易对符号", required = true) @RequestParam String symbol,
//            @ApiParam(value = "K线间隔", required = true) @RequestParam String interval) {
//
//        Map<String, Object> kdjData = indicatorCalculationService.getKDJIndicator(symbol, interval);
//
//        if (kdjData != null && !kdjData.isEmpty()) {
//            return ApiResponse.success("获取KDJ指标成功", kdjData);
//        } else {
//            return ApiResponse.error(404, "获取KDJ指标失败");
//        }
//    }
//
//    /**
//     * 获取布林带指标
//     *
//     * @param symbol   交易对符号，如 BTC-USDT
//     * @param interval K线间隔，如 1m, 5m, 15m, 30m, 1H, 4H, 1D 等
//     * @return 布林带指标数据
//     */
//    @GetMapping("/boll")
//    @ApiOperation("获取布林带指标")
//    public ApiResponse<Map<String, Object>> getBollingerBandsIndicator(
//            @ApiParam(value = "交易对符号", required = true) @RequestParam String symbol,
//            @ApiParam(value = "K线间隔", required = true) @RequestParam String interval) {
//
//        Map<String, Object> bollData = indicatorCalculationService.getBollingerBandsIndicator(symbol, interval);
//
//        if (bollData != null && !bollData.isEmpty()) {
//            return ApiResponse.success("获取布林带指标成功", bollData);
//        } else {
//            return ApiResponse.error(404, "获取布林带指标失败");
//        }
//    }
//}
