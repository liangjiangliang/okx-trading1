package com.okx.trading.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.RedisCacheService;
import com.okx.trading.service.KlineCacheService;
import com.okx.trading.util.TechnicalIndicatorUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static com.okx.trading.constant.IndicatorInfo.ALL_COIN_RT_PRICE;
import static com.okx.trading.util.BacktestDataGenerator.parseIntervalToMinutes;

/**
 * 市场数据控制器
 * 提供K线数据获取和技术指标计算的接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/market")
@Api(tags = "市场数据接口", description = "提供K线数据获取和技术指标计算的接口")
public class MarketController {

    private final OkxApiService okxApiService;
    private final HistoricalDataService historicalDataService;
    private final RedisCacheService redisCacheService;
    private final KlineCacheService klineCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public MarketController(OkxApiService okxApiService,
                            HistoricalDataService historicalDataService,
                            RedisCacheService redisCacheService,
                            KlineCacheService klineCacheService, RedisTemplate<String, Object> redisTemplate) {
        this.okxApiService = okxApiService;
        this.historicalDataService = historicalDataService;
        this.redisCacheService = redisCacheService;
        this.klineCacheService = klineCacheService;
        this.redisTemplate = redisTemplate;
    }

    // 判断是否为开发环境，用于控制日志详细程度
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 判断是否为开发环境
     */
    private boolean isDevelopmentEnvironment() {
        return "dev".equals(activeProfile) || "development".equals(activeProfile);
    }

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
     * 获取最新的K线数据
     *
     * @param symbol   交易对，如BTC-USDT
     * @param interval K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param limit    获取数据条数，默认100
     * @return 最新的K线数据列表
     */
    @ApiOperation(value = "获取最新K线数据", notes = "从数据库获取最新的K线数据，按时间降序排列")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "limit", value = "获取数据条数，默认100",
                    required = false, dataType = "Integer", example = "100", paramType = "query")
    })
    @GetMapping("/latest_klines")
    public ApiResponse<List<CandlestickEntity>> getLatestKlineData(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @RequestParam(required = false, defaultValue = "100") @Min(value = 1, message = "数据条数必须大于0") Integer limit) {

        log.info("获取最新K线数据, symbol: {}, interval: {}, limit: {}", symbol, interval, limit);

        List<CandlestickEntity> candlesticks = historicalDataService.getLatestHistoricalData(symbol, interval, limit);

        return ApiResponse.success(candlesticks);
    }

    /**
     * 查询数据库中已保存的历史K线数据
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @return 历史K线数据列表
     */
    @ApiOperation(value = "查询已保存的历史K线数据", notes = "查询数据库中已保存的历史K线数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2023-01-01 00:00:00", paramType = "query"),
            @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2023-01-02 00:00:00", paramType = "query")
    })
    @GetMapping("/query_saved_history")
    public ApiResponse<List<CandlestickEntity>> querySavedHistoricalData(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
            @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr) {

        log.info("查询已保存的历史K线数据, symbol: {}, interval: {}, startTime: {}, endTime: {}",
                symbol, interval, startTimeStr, endTimeStr);

        try {
            // 将字符串时间转换为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 查询数据
            List<CandlestickEntity> data = historicalDataService.getHistoricalData(symbol, interval, startTime, endTime);

            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("查询历史K线数据失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "查询历史K线数据失败: " + e.getMessage());
        }
    }


    /**
     * 获取历史K线数据并保存，智能计算需要获取的数据量
     *
     * @param symbol       交易对，如BTC-USDT
     * @param interval     K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     * @param startTimeStr 开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTimeStr   结束时间 (yyyy-MM-dd HH:mm:ss)
     * @return 操作结果，包含获取的K线数据
     */
    @ApiOperation(value = "智能获取历史K线数据", notes = "根据入参计算需要获取的K线数量，扣除已有数据，按需获取并保存")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "symbol", value = "交易对", required = true, dataType = "String", example = "BTC-USDT", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "K线间隔 (1m=1分钟, 5m=5分钟, 15m=15分钟, 30m=30分钟, 1H=1小时, 2H=2小时, 4H=4小时, 6H=6小时, 12H=12小时, 1D=1天, 1W=1周, 1M=1个月)",
                    required = true, dataType = "String", example = "1m", paramType = "query",
                    allowableValues = "1m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M"),
            @ApiImplicitParam(name = "startTimeStr", value = "开始时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2018-01-01 00:00:00", paramType = "query"),
            @ApiImplicitParam(name = "endTimeStr", value = "结束时间 (yyyy-MM-dd HH:mm:ss)", required = true, dataType = "String", example = "2025-04-01 00:00:00", paramType = "query")
    })
    @GetMapping("/fetch_history_with_integrity_check")
    public ApiResponse<List<CandlestickEntity>> fetchAndSaveHistoryWithIntegrityCheck(
            @NotBlank(message = "交易对不能为空") @RequestParam String symbol,
            @NotBlank(message = "K线间隔不能为空") @RequestParam String interval,
            @NotBlank(message = "开始时间不能为空") @RequestParam String startTimeStr,
            @NotBlank(message = "结束时间不能为空") @RequestParam String endTimeStr) {

        try {
            List<CandlestickEntity> candlestickEntities = historicalDataService.fetchAndSaveHistoryWithIntegrityCheck(symbol, interval, startTimeStr, endTimeStr);
            return ApiResponse.success(candlestickEntities);
        } catch (Exception e) {
            log.error("❌ 智能获取历史K线数据失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取历史K线数据失败: " + e.getMessage());
        }

    }

    /**
     * 查看Redis中已有的K线订阅数据
     * 用于调试和检查当前订阅状态
     */
    @ApiOperation(value = "查看已有订阅", notes = "查看Redis中已保存的K线订阅数据")
    @GetMapping("/subscriptions")
    public ApiResponse<Set<String>> getSubscriptions() {
        log.info("查看Redis中已有的K线订阅数据");

        Set<String> subscriptions = klineCacheService.getAllSubscribedKlines();

        log.info("发现 {} 个订阅记录: {}", subscriptions.size(), subscriptions);

        return ApiResponse.success(subscriptions);
    }

    /**
     * 获取所有订阅币种的最新行情数据
     *
     * @param filter 可选的过滤条件（默认为空，可选值：all=所有币种, hot=热门币种, rise=涨幅最大, fall=跌幅最大）
     * @param search 搜索币种名称（可以是部分匹配，不区分大小写）
     * @param limit  返回的数据条数，默认为50
     * @return 所有订阅币种的行情数据列表
     */
    @ApiOperation(value = "获取所有币种最新行情", notes = "获取所有已订阅币种的最新价格、24小时涨跌幅等行情数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "filter", value = "过滤条件（all=所有币种, hot=热门币种, rise=涨幅最大, fall=跌幅最大）",
                    required = false, dataType = "String", example = "hot", paramType = "query"),
            @ApiImplicitParam(name = "search", value = "搜索币种名称，如BTC、ETH等（不区分大小写）",
                    required = false, dataType = "String", example = "BTC", paramType = "query"),
            @ApiImplicitParam(name = "limit", value = "返回数据条数，默认为50",
                    required = false, dataType = "Integer", example = "20", paramType = "query")
    })
    @GetMapping("/all_tickers")
    public ApiResponse<List<Ticker>> getAllTickers(
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        log.info("获取所有币种最新行情, filter: {}, search: {}, limit: {}", filter, search, limit);

        List<Ticker> tickers = null;
        Set<Object> members = redisTemplate.opsForSet().members(ALL_COIN_RT_PRICE);
        if (!CollectionUtils.isEmpty(members)) {
            tickers = members.stream().map(x -> JSONObject.parseObject((String) x, Ticker.class)).collect(Collectors.toList());
            log.info("从缓存查询所有币种价格");
        } else {
            tickers = okxApiService.getAllTickers();
            log.info("从接口查询所有币种价格");
        }

        tickers = okxApiService.getAllTickers();

        redisTemplate.opsForSet().add(ALL_COIN_RT_PRICE, Arrays.stream(tickers.toArray()).map(Object::toString).toArray(String[]::new));
        redisTemplate.expire(ALL_COIN_RT_PRICE, 10, TimeUnit.MINUTES);

        // 如果有搜索条件，先过滤
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim().toUpperCase();
            tickers = tickers.stream()
                    .filter(ticker -> ticker.getSymbol().toUpperCase().contains(searchTerm))
                    .collect(Collectors.toList());
        }

        // 根据过滤条件处理数据
        if ("hot".equalsIgnoreCase(filter)) {
            // 热门币种：按24小时成交量降序排序
            tickers.sort((t1, t2) -> t2.getQuoteVolume().compareTo(t1.getQuoteVolume()));
        } else if ("rise".equalsIgnoreCase(filter)) {
            // 涨幅最大：按涨跌幅降序排序
            tickers.sort((t1, t2) -> t2.getPriceChangePercent().compareTo(t1.getPriceChangePercent()));
        } else if ("fall".equalsIgnoreCase(filter)) {
            // 跌幅最大：按涨跌幅升序排序
            tickers.sort((t1, t2) -> t1.getPriceChangePercent().compareTo(t2.getPriceChangePercent()));
        }

        // 限制返回数量
        if (limit != null && limit > 0 && tickers.size() > limit) {
            tickers = tickers.subList(0, limit);
        }

        return ApiResponse.success(tickers);
    }

}
