package com.okx.trading.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.repository.CandlestickRepository;
import com.okx.trading.service.impl.HistoricalDataServiceImpl;

/**
 * 历史K线数据服务测试类
 */
public class HistoricalDataServiceTest {

    @Mock
    private CandlestickRepository candlestickRepository;

    @Mock
    private OkxApiService okxApiService;

    @InjectMocks
    private HistoricalDataServiceImpl historicalDataService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试获取最新K线数据
     */
    @Test
    public void testGetLatestHistoricalData() {
        // 准备测试数据
        String symbol = "BTC-USDT";
        String interval = "1m";
        int limit = 10;
        
        List<CandlestickEntity> mockEntities = new ArrayList<>();
        
        // 模拟repository方法
        when(candlestickRepository.findLatestBySymbolAndInterval(
                eq(symbol), eq(interval), any(PageRequest.class))).thenReturn(mockEntities);
        
        // 执行测试
        List<CandlestickEntity> result = historicalDataService.getLatestHistoricalData(symbol, interval, limit);
        
        // 验证结果
        assertNotNull(result);
        
        // 验证调用
        verify(candlestickRepository).findLatestBySymbolAndInterval(
                eq(symbol), eq(interval), any(PageRequest.class));
    }
} 