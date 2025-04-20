package com.okx.trading.controller;

import com.okx.trading.service.IndicatorCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 指标计算控制器测试类
 */
public class IndicatorCalculationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IndicatorCalculationService indicatorCalculationService;

    @InjectMocks
    private IndicatorCalculationController indicatorCalculationController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(indicatorCalculationController).build();
    }

    /**
     * 测试订阅指标计算
     */
    @Test
    public void testSubscribeIndicator() throws Exception {
        // 准备测试数据
        when(indicatorCalculationService.subscribeIndicatorCalculation(anyString(), anyString())).thenReturn(true);

        // 执行和验证
        mockMvc.perform(post("/indicator/subscribe")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("成功订阅指标计算: BTC-USDT 1m"));

        // 验证服务方法被调用
        verify(indicatorCalculationService).subscribeIndicatorCalculation("BTC-USDT", "1m");
    }

    /**
     * 测试取消订阅指标计算
     */
    @Test
    public void testUnsubscribeIndicator() throws Exception {
        // 准备测试数据
        when(indicatorCalculationService.unsubscribeIndicatorCalculation(anyString(), anyString())).thenReturn(true);

        // 执行和验证
        mockMvc.perform(post("/indicator/unsubscribe")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("成功取消订阅指标计算: BTC-USDT 1m"));

        // 验证服务方法被调用
        verify(indicatorCalculationService).unsubscribeIndicatorCalculation("BTC-USDT", "1m");
    }

    /**
     * 测试获取MACD指标
     */
    @Test
    public void testGetMACDIndicator() throws Exception {
        // 准备测试数据
        Map<String, Object> macdData = new HashMap<>();
        macdData.put("macdLine", Arrays.asList(0.1, 0.2, 0.3));
        macdData.put("signalLine", Arrays.asList(0.05, 0.15, 0.25));
        macdData.put("histogram", Arrays.asList(0.05, 0.05, 0.05));
        when(indicatorCalculationService.getMACDIndicator(anyString(), anyString())).thenReturn(macdData);

        // 执行和验证
        mockMvc.perform(get("/indicator/macd")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

        // 验证服务方法被调用
        verify(indicatorCalculationService).getMACDIndicator("BTC-USDT", "1m");
    }

    /**
     * 测试获取RSI指标
     */
    @Test
    public void testGetRSIIndicator() throws Exception {
        // 准备测试数据
        List<Double> rsiData = Arrays.asList(30.5, 45.2, 60.8);
        when(indicatorCalculationService.getRSIIndicator(anyString(), anyString(), any(Integer.class))).thenReturn(rsiData);

        // 执行和验证
        mockMvc.perform(get("/indicator/rsi")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .param("period", "14")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

        // 验证服务方法被调用
        verify(indicatorCalculationService).getRSIIndicator("BTC-USDT", "1m", 14);
    }

    /**
     * 测试获取KDJ指标
     */
    @Test
    public void testGetKDJIndicator() throws Exception {
        // 准备测试数据
        Map<String, Object> kdjData = new HashMap<>();
        kdjData.put("kValues", Arrays.asList(70.5, 65.2, 60.8));
        kdjData.put("dValues", Arrays.asList(60.5, 62.2, 59.8));
        kdjData.put("jValues", Arrays.asList(80.5, 75.2, 70.8));
        when(indicatorCalculationService.getKDJIndicator(anyString(), anyString())).thenReturn(kdjData);

        // 执行和验证
        mockMvc.perform(get("/indicator/kdj")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

        // 验证服务方法被调用
        verify(indicatorCalculationService).getKDJIndicator("BTC-USDT", "1m");
    }

    /**
     * 测试获取布林带指标
     */
    @Test
    public void testGetBollingerBandsIndicator() throws Exception {
        // 准备测试数据
        Map<String, Object> bollData = new HashMap<>();
        bollData.put("upper", Arrays.asList(31000.5, 31200.2, 31100.8));
        bollData.put("middle", Arrays.asList(30000.5, 30200.2, 30100.8));
        bollData.put("lower", Arrays.asList(29000.5, 29200.2, 29100.8));
        when(indicatorCalculationService.getBollingerBandsIndicator(anyString(), anyString())).thenReturn(bollData);

        // 执行和验证
        mockMvc.perform(get("/indicator/boll")
                .param("symbol", "BTC-USDT")
                .param("interval", "1m")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

        // 验证服务方法被调用
        verify(indicatorCalculationService).getBollingerBandsIndicator("BTC-USDT", "1m");
    }
} 