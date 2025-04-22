package com.okx.trading.service;

import com.okx.trading.model.dto.IndicatorValueDTO;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.service.impl.TechnicalIndicatorServiceImpl;
import com.okx.trading.ta4j.CandlestickBarSeriesConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 技术指标服务测试类
 */
@ExtendWith(MockitoExtension.class)
public class TechnicalIndicatorServiceTest {

    @Mock
    private KlineCacheService klineCacheService;
    
    @Mock
    private CandlestickBarSeriesConverter barSeriesConverter;
    
    @InjectMocks
    private TechnicalIndicatorServiceImpl technicalIndicatorService;
    
    private List<CandlestickEntity> mockKlines;
    private BarSeries mockBarSeries;
    
    @BeforeEach
    public void setUp() {
        // 创建模拟的K线数据
        mockKlines = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CandlestickEntity kline = new CandlestickEntity();
            kline.setSymbol("BTC-USDT");
            kline.setIntervalVal("1h");
            kline.setOpenTime(LocalDateTime.now().minusHours(100 - i));
            kline.setCloseTime(LocalDateTime.now().minusHours(99 - i));
            kline.setOpen(new BigDecimal("30000").add(new BigDecimal(String.valueOf(i * 10))));
            kline.setHigh(new BigDecimal("30100").add(new BigDecimal(String.valueOf(i * 10))));
            kline.setLow(new BigDecimal("29900").add(new BigDecimal(String.valueOf(i * 10))));
            kline.setClose(new BigDecimal("30050").add(new BigDecimal(String.valueOf(i * 10))));
            kline.setVolume(new BigDecimal("100").add(new BigDecimal(String.valueOf(i))));
            mockKlines.add(kline);
        }
        
        // 创建模拟的BarSeries
        mockBarSeries = new BaseBarSeries("BTC-USDT_1h");
        
        // 设置模拟行为
        when(klineCacheService.getKlineData(anyString(), anyString(), anyInt())).thenReturn(mockKlines);
        when(barSeriesConverter.convert(anyList(), anyString())).thenReturn(mockBarSeries);
    }
    
    @Test
    public void testCalculateLastIndicator() {
        // 执行方法
        IndicatorValueDTO result = technicalIndicatorService.calculateLastIndicator("BTC-USDT", "1h", "RSI", "14");
        
        // 验证结果
        assertNotNull(result);
        assertEquals("BTC-USDT", result.getSymbol());
        assertEquals("1h", result.getInterval());
        assertEquals("RSI", result.getIndicatorType());
        
        // 验证调用
        verify(klineCacheService).getKlineData(eq("BTC-USDT"), eq("1h"), anyInt());
        verify(barSeriesConverter).convert(anyList(), anyString());
    }
    
    @Test
    public void testCalculateMultipleIndicators() {
        // 准备测试数据
        Map<String, String> indicators = new HashMap<>();
        indicators.put("RSI", "14");
        indicators.put("MACD", "12,26,9");
        
        // 执行方法
        Map<String, IndicatorValueDTO> results = technicalIndicatorService.calculateMultipleIndicators("BTC-USDT", "1h", indicators);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("RSI"));
        assertTrue(results.containsKey("MACD"));
        
        // 验证调用
        verify(klineCacheService).getKlineData(eq("BTC-USDT"), eq("1h"), anyInt());
        verify(barSeriesConverter, atLeastOnce()).convert(anyList(), anyString());
    }
    
    @Test
    public void testGetSupportedIndicators() {
        // 执行方法
        List<String> indicators = technicalIndicatorService.getSupportedIndicators();
        
        // 验证结果
        assertNotNull(indicators);
        assertFalse(indicators.isEmpty());
        assertTrue(indicators.contains("RSI"));
        assertTrue(indicators.contains("MACD"));
    }
    
    @Test
    public void testGetIndicatorParamsDescription() {
        // 执行方法
        String description = technicalIndicatorService.getIndicatorParamsDescription("RSI");
        
        // 验证结果
        assertNotNull(description);
        assertFalse(description.isEmpty());
    }
    
    @Test
    public void testCalculateIndicator_WithEmptyKlines() {
        // 准备测试数据
        List<CandlestickEntity> emptyList = new ArrayList<>();
        
        // 执行方法
        IndicatorValueDTO result = technicalIndicatorService.calculateIndicator(emptyList, "RSI", "14");
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    public void testCalculateIndicator_WithUnsupportedIndicator() {
        // 执行方法
        IndicatorValueDTO result = technicalIndicatorService.calculateIndicator(mockKlines, "UNSUPPORTED", "14");
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
} 