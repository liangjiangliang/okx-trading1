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
    @ApiOperation(value = "获取K线数据", notes = "获取指定交易对的K线数据，支持多种时间间隔")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)", 
                    required = true, dataType = "String", example = "1H", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "limit", value = "获取数据条数，最大为1000，不传默认返回500条数据", 
                    required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/klines")
    public ApiResponse<List<Candlestick>> getKlineData(
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
} 