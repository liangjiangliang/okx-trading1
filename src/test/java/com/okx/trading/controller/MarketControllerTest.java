package com.okx.trading.controller;

import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.HistoricalDataService;
import com.okx.trading.service.OkxApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 行情控制器测试类
 */
public class MarketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OkxApiService okxApiService;

    @Mock
    private HistoricalDataService historicalDataService;

    @InjectMocks
    private MarketController marketController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(marketController).build();
    }

    /**
     * 测试获取K线数据接口
     */
    @Test
    public void testGetKlineData() throws Exception {
        // 模拟数据
        List<Candlestick> candlesticks = Arrays.asList(
                createCandlestick("BTC-USDT", "1m", LocalDateTime.now(),
                        new BigDecimal("50000"), new BigDecimal("50100"),
                        new BigDecimal("49900"), new BigDecimal("50050")),
                createCandlestick("BTC-USDT", "1m", LocalDateTime.now().minusMinutes(1),
                        new BigDecimal("49900"), new BigDecimal("50000"),
                        new BigDecimal("49800"), new BigDecimal("50000"))
        );

        // 配置Mock行为
        when(okxApiService.getKlineData(eq("BTC-USDT"), eq("1m"), any())).thenReturn(candlesticks);

        // 执行请求并验证结果
        mockMvc.perform(get("/market/subscribe_klines")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].symbol").value("BTC-USDT"))
                .andExpect(jsonPath("$.data[0].interval").value("1m"))
                .andExpect(jsonPath("$.data[0].open").value("50000"))
                .andExpect(jsonPath("$.data[0].high").value("50100"));
    }

    /**
     * 测试获取行情数据接口
     */
    @Test
    public void testGetTicker() throws Exception {
        // 模拟数据
        Ticker ticker = createTicker("BTC-USDT", new BigDecimal("50000"),
                new BigDecimal("1000"), new BigDecimal("2"),
                new BigDecimal("50500"), new BigDecimal("49500"));

        // 配置Mock行为
        when(okxApiService.getTicker(eq("BTC-USDT"))).thenReturn(ticker);

        // 执行请求并验证结果
        mockMvc.perform(get("/market/ticker")
                .param("symbol", "BTC-USDT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.symbol").value("BTC-USDT"))
                .andExpect(jsonPath("$.data.lastPrice").value("50000"))
                .andExpect(jsonPath("$.data.priceChange").value("1000"))
                .andExpect(jsonPath("$.data.priceChangePercent").value("2"))
                .andExpect(jsonPath("$.data.highPrice").value("50500"))
                .andExpect(jsonPath("$.data.lowPrice").value("49500"));
    }

    /**
     * 创建K线数据
     */
    private Candlestick createCandlestick(String symbol, String interval, LocalDateTime time,
                                        BigDecimal open, BigDecimal high,
                                        BigDecimal low, BigDecimal close) {
        Candlestick candlestick = new Candlestick();
        candlestick.setSymbol(symbol);
        candlestick.setInterval(interval);
        candlestick.setOpenTime(time);
        candlestick.setOpen(open);
        candlestick.setHigh(high);
        candlestick.setLow(low);
        candlestick.setClose(close);
        candlestick.setVolume(new BigDecimal("1000"));
        candlestick.setQuoteVolume(new BigDecimal("50000000"));
        candlestick.setCloseTime(time.plusMinutes(1));
        candlestick.setTrades(100L);
        return candlestick;
    }

    /**
     * 创建行情数据
     */
    private Ticker createTicker(String symbol, BigDecimal lastPrice,
                                BigDecimal priceChange, BigDecimal priceChangePercent,
                                BigDecimal highPrice, BigDecimal lowPrice) {
        Ticker ticker = new Ticker();
        ticker.setSymbol(symbol);
        ticker.setLastPrice(lastPrice);
        ticker.setPriceChange(priceChange);
        ticker.setPriceChangePercent(priceChangePercent);
        ticker.setHighPrice(highPrice);
        ticker.setLowPrice(lowPrice);
        ticker.setVolume(new BigDecimal("10000"));
        ticker.setQuoteVolume(new BigDecimal("500000000"));
        ticker.setBidPrice(new BigDecimal("49990"));
        ticker.setBidQty(new BigDecimal("2"));
        ticker.setAskPrice(new BigDecimal("50010"));
        ticker.setAskQty(new BigDecimal("3"));
        ticker.setTimestamp(LocalDateTime.now());
        return ticker;
    }

    /**
     * 测试获取最新K线数据
     */
    @Test
    public void testGetLatestKlineData() throws Exception {
        // 准备测试数据
        String symbol = "BTC-USDT";
        String interval = "1m";
        int limit = 100;

        List<CandlestickEntity> mockEntities = new ArrayList<>();

        // 模拟服务方法
        when(historicalDataService.getLatestHistoricalData(eq(symbol), eq(interval), eq(limit)))
                .thenReturn(mockEntities);

        // 执行测试
        mockMvc.perform(get("/market/latest_klines")
                        .param("symbol", symbol)
                        .param("interval", interval)
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());

        // 验证服务调用
        verify(historicalDataService).getLatestHistoricalData(eq(symbol), eq(interval), eq(limit));
    }
}
