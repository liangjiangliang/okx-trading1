package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.service.OkxApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 行情数据控制器
 * 提供获取K线数据、行情等接口
 */
@Api(tags = "行情数据")
@Slf4j
@Validated
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final OkxApiService okxApiService;

    /**
     * 获取K线数据
     *
     * @param symbol   交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit    获取数据条数，最大为1000
     * @return K线数据列表
     */
    @ApiOperation(value = "订阅实时标记价格K线数据,订阅完成后自动推送最新行情信息", notes = "获取指定交易对的K线数据，支持多种时间间隔")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "limit", value = "获取数据条数，最大为1000，不传默认返回500条数据",
                    required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/subscribe_klines")
    public ApiResponse<List<Candlestick>> subscribeKlineData(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @RequestParam(required = false) @Min(value = 1, message = "数据条数必须大于0") Integer limit) {

        log.info("获取K线数据, symbol: {}, interval: {}, limit: {}", symbol, interval, limit);

        List<Candlestick> candlesticks = okxApiService.getKlineData(symbol, interval, limit);

        return ApiResponse.success(candlesticks);
    }

    /**
     * 获取最新行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 行情数据
     */
    @ApiOperation(value = "获取最新行情", notes = "获取指定交易对的最新价格、24小时涨跌幅等行情数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对 (格式为 基础资产-计价资产，如BTC-USDT、ETH-USDT等)",
                    required = true, dataType = "String", example = "BTC-USDT", paramType = "query")
    })
    @GetMapping("/ticker")
    public ApiResponse<Ticker> getTicker(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {

        log.info("获取最新行情, symbol: {}", symbol);

        Ticker ticker = okxApiService.getTicker(symbol);

        return ApiResponse.success(ticker);
    }

    /**
     * 取消订阅K线数据
     *
     * @param symbol   交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @return 操作结果
     */
    @ApiOperation(value = "取消订阅K线数据", notes = "取消订阅指定交易对的K线数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M")
    })
    @GetMapping("/unsubscribe_klines")
    public ApiResponse<Boolean> unsubscribeKlineData(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval) {

        log.info("取消订阅K线数据, symbol: {}, interval: {}", symbol, interval);

        boolean result = okxApiService.unsubscribeKlineData(symbol, interval);

        return ApiResponse.success(result);
    }

    /**
     * 取消订阅行情数据
     *
     * @param symbol 交易对，如BTC-USDT
     * @return 操作结果
     */
    @ApiOperation(value = "取消订阅行情数据", notes = "取消订阅指定交易对的实时行情数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对 (格式为 基础资产-计价资产，如BTC-USDT、ETH-USDT等)",
                    required = true, dataType = "String", example = "BTC-USDT", paramType = "query")
    })
    @GetMapping("/unsubscribe_ticker")
    public ApiResponse<Boolean> unsubscribeTicker(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol) {

        log.info("取消订阅行情数据, symbol: {}", symbol);

        boolean result = okxApiService.unsubscribeTicker(symbol);

        return ApiResponse.success(result);
    }

    /**
     * 获取历史K线数据
     *
     * @param symbol    交易对，如BTC-USDT
     * @param interval  K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTime 开始时间戳（毫秒）
     * @param endTime   结束时间戳（毫秒）
     * @param limit     获取数据条数，最大为1000
     * @return K线数据列表
     */
    @ApiOperation(value = "获取历史K线数据", notes = "获取指定交易对的历史K线数据，支持时间范围查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "startTime", value = "开始时间戳（毫秒）", required = false, dataType = "Long", example = "1656086400000", paramType = "query"),
            @ApiImplicitParam(name = "endTime", value = "结束时间戳（毫秒）", required = false, dataType = "Long", example = "1656172800000", paramType = "query"),
            @ApiImplicitParam(name = "limit", value = "获取数据条数，最大为1000，不传默认返回500条数据",
                    required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/history_klines")
    public ApiResponse<List<Candlestick>> getHistoryKlineData(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) @Min(value = 1, message = "数据条数必须大于0") Integer limit) {

        log.info("获取历史K线数据, symbol: {}, interval: {}, startTime: {}, endTime: {}, limit: {}", 
               symbol, interval, startTime, endTime, limit);

        List<Candlestick> candlesticks = okxApiService.getHistoryKlineData(symbol, interval, startTime, endTime, limit);

        return ApiResponse.success(candlesticks);
    }
}
