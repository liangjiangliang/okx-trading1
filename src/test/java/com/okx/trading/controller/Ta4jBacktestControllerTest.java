package com.okx.trading.controller;

import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.HistoricalDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ta4j回测控制器测试类
 */
@ExtendWith(MockitoExtension.class)
public class Ta4jBacktestControllerTest {

    @Mock
    private HistoricalDataService historicalDataService;

    @InjectMocks
    private Ta4jBacktestController ta4jBacktestController;

    private MockMvc mockMvc;
    private List<CandlestickEntity> mockCandlesticks;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ta4jBacktestController).build();
        mockCandlesticks = createMockCandlesticks();
    }

    @Test
    public void testSMAStrategy_WithValidParams_ShouldReturnSuccess() throws Exception {
        // 模拟历史数据服务返回模拟K线数据
        when(historicalDataService.getHistoricalData(
                anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCandlesticks);

        // 执行GET请求
        mockMvc.perform(get("/api/ta4j/backtest/sma")
                .param("symbol", "BTC-USDT")
                .param("interval", "1H")
                .param("startTime", "2023-01-01 00:00:00")
                .param("endTime", "2023-02-01 00:00:00")
                .param("shortPeriod", "5")
                .param("longPeriod", "20")
                .param("initialBalance", "10000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.symbol").value("BTC-USDT"))
                .andExpect(jsonPath("$.data.interval").value("1H"))
                .andExpect(jsonPath("$.data.strategyType").value("SMA交叉策略"));
    }

    @Test
    public void testBollingerBandsStrategy_WithValidParams_ShouldReturnSuccess() throws Exception {
        // 模拟历史数据服务返回模拟K线数据
        when(historicalDataService.getHistoricalData(
                anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCandlesticks);

        // 执行GET请求 - 测试反转模式
        mockMvc.perform(get("/api/ta4j/backtest/bollinger")
                .param("symbol", "BTC-USDT")
                .param("interval", "1H")
                .param("startTime", "2023-01-01 00:00:00")
                .param("endTime", "2023-02-01 00:00:00")
                .param("period", "20")
                .param("deviation", "2.0")
                .param("initialBalance", "10000")
                .param("tradingMode", "REVERSAL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.symbol").value("BTC-USDT"))
                .andExpect(jsonPath("$.data.interval").value("1H"))
                .andExpect(jsonPath("$.data.strategyType").value("布林带REVERSAL策略"));
    }

    @Test
    public void testBollingerBandsStrategy_WithBreakoutMode_ShouldReturnSuccess() throws Exception {
        // 模拟历史数据服务返回模拟K线数据
        when(historicalDataService.getHistoricalData(
                anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCandlesticks);

        // 执行GET请求 - 测试突破模式
        mockMvc.perform(get("/api/ta4j/backtest/bollinger")
                .param("symbol", "BTC-USDT")
                .param("interval", "1H")
                .param("startTime", "2023-01-01 00:00:00")
                .param("endTime", "2023-02-01 00:00:00")
                .param("period", "20")
                .param("deviation", "2.0")
                .param("initialBalance", "10000")
                .param("tradingMode", "BREAKOUT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.symbol").value("BTC-USDT"))
                .andExpect(jsonPath("$.data.interval").value("1H"))
                .andExpect(jsonPath("$.data.strategyType").value("布林带BREAKOUT策略"));
    }

    @Test
    public void testStrategy_WithEmptyCandlesticks_ShouldReturnError() throws Exception {
        // 模拟历史数据服务返回空列表
        when(historicalDataService.getHistoricalData(
                anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // 执行GET请求
        mockMvc.perform(get("/api/ta4j/backtest/sma")
                .param("symbol", "BTC-USDT")
                .param("interval", "1H")
                .param("startTime", "2023-01-01 00:00:00")
                .param("endTime", "2023-02-01 00:00:00")
                .param("shortPeriod", "5")
                .param("longPeriod", "20")
                .param("initialBalance", "10000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 创建模拟的K线数据
     */
    private List<CandlestickEntity> createMockCandlesticks() {
        List<CandlestickEntity> candlesticks = new ArrayList<>();

        // 创建60条模拟K线数据 - 足够计算SMA和布林带
        LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        BigDecimal basePrice = new BigDecimal("30000.00");

        for (int i = 0; i < 60; i++) {
            // 创建模拟的K线数据，使用mock
            CandlestickEntity mockCandle = org.mockito.Mockito.mock(CandlestickEntity.class);

            LocalDateTime openTime = startTime.plusHours(i);
            LocalDateTime closeTime = openTime.plusHours(1);

            // 模拟价格波动
            double variation = Math.sin(i * 0.1) * 500 + (Math.random() - 0.5) * 200;
            BigDecimal open = basePrice.add(new BigDecimal(variation));
            BigDecimal high = open.add(new BigDecimal(Math.random() * 100));
            BigDecimal low = open.subtract(new BigDecimal(Math.random() * 100));
            BigDecimal close = high.add(low).divide(new BigDecimal("2"));

            // 设置mock的返回值
            when(mockCandle.getSymbol()).thenReturn("BTC-USDT");
            when(mockCandle.getIntervalVal()).thenReturn("1H");
            when(mockCandle.getOpenTime()).thenReturn(openTime);
            when(mockCandle.getCloseTime()).thenReturn(closeTime);
            when(mockCandle.getOpen()).thenReturn(open);
            when(mockCandle.getHigh()).thenReturn(high);
            when(mockCandle.getLow()).thenReturn(low);
            when(mockCandle.getClose()).thenReturn(close);
            when(mockCandle.getVolume()).thenReturn(new BigDecimal(Math.random() * 100 + 10));

            candlesticks.add(mockCandle);
        }

        return candlesticks;
    }
}
