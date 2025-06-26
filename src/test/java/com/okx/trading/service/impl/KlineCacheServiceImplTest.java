package com.okx.trading.service.impl;

import com.okx.trading.event.KlineSubscriptionEvent;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KlineCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private KlineCacheServiceImpl klineCacheService;

    @Captor
    private ArgumentCaptor<KlineSubscriptionEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void testSubscribeKline_Success() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        String field = symbol + ":" + interval;

        when(hashOperations.hasKey(anyString(), eq(field))).thenReturn(false);

        // 执行
        boolean result = klineCacheService.subscribeKline(symbol, interval);

        // 验证
        assertTrue(result);
        verify(hashOperations).put("kline-subscriptions", field, "1");
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        KlineSubscriptionEvent event = eventCaptor.getValue();
        assertEquals(symbol, event.getSymbol());
        assertEquals(interval, event.getInterval());
        assertEquals(KlineSubscriptionEvent.EventType.SUBSCRIBE, event.getType());
    }

    @Test
    void testSubscribeKline_AlreadySubscribed() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        String field = symbol + ":" + interval;

        when(hashOperations.hasKey(anyString(), eq(field))).thenReturn(true);

        // 执行
        boolean result = klineCacheService.subscribeKline(symbol, interval);

        // 验证
        assertFalse(result);
        verify(hashOperations, never()).put(anyString(), anyString(), anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testUnsubscribeKline_Success() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        String field = symbol + ":" + interval;

        when(hashOperations.hasKey(anyString(), eq(field))).thenReturn(true);
        when(hashOperations.delete(anyString(), eq(field))).thenReturn(1L);

        // 执行
        boolean result = klineCacheService.unsubscribeKline(symbol, interval);

        // 验证
        assertTrue(result);
        verify(hashOperations).delete("kline-subscriptions", field);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        KlineSubscriptionEvent event = eventCaptor.getValue();
        assertEquals(symbol, event.getSymbol());
        assertEquals(interval, event.getInterval());
        assertEquals(KlineSubscriptionEvent.EventType.UNSUBSCRIBE, event.getType());
    }

    @Test
    void testUnsubscribeKline_NotSubscribed() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        String field = symbol + ":" + interval;

        when(hashOperations.hasKey(anyString(), eq(field))).thenReturn(false);

        // 执行
        boolean result = klineCacheService.unsubscribeKline(symbol, interval);

        // 验证
        assertFalse(result);
        verify(hashOperations, never()).delete(anyString(), anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testBatchSubscribeKline() {
        // 准备
        String symbol = "BTC-USDT";
        List<String> intervals = Arrays.asList("1m", "5m", "15m");

        // 模拟1m和5m能成功订阅，15m已经订阅过了
        String field1 = symbol + ":1m";
        String field2 = symbol + ":5m";
        String field3 = symbol + ":15m";

        when(hashOperations.hasKey(anyString(), eq(field1))).thenReturn(false);
        when(hashOperations.hasKey(anyString(), eq(field2))).thenReturn(false);
        when(hashOperations.hasKey(anyString(), eq(field3))).thenReturn(true);

        // 执行
        List<String> result = klineCacheService.batchSubscribeKline(symbol, intervals);

        // 验证
        assertEquals(2, result.size());
        assertTrue(result.contains("1m"));
        assertTrue(result.contains("5m"));
        assertFalse(result.contains("15m"));

        verify(hashOperations, times(2)).put(anyString(), anyString(), anyString());
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void testCacheKlineData() {
        // 准备
        Candlestick candlestick = new Candlestick();
        candlestick.setSymbol("BTC-USDT");
        candlestick.setIntervalVal("1m");
        candlestick.setOpenTime(LocalDateTime.now());
        candlestick.setOpen(new BigDecimal("50000"));
        candlestick.setHigh(new BigDecimal("51000"));
        candlestick.setLow(new BigDecimal("49000"));
        candlestick.setClose(new BigDecimal("50500"));
        candlestick.setVolume(new BigDecimal("10"));

        String key = "kline-data:BTC-USDT:1m";
        long timestamp = candlestick.getOpenTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double score = Double.parseDouble(String.valueOf(timestamp));

        when(zSetOperations.rangeByScore(eq(key), eq(score), eq(score))).thenReturn(Collections.emptySet());
        when(zSetOperations.add(eq(key), eq(candlestick), eq(score))).thenReturn(true);

        // 执行
        boolean result = klineCacheService.cacheKlineData(candlestick);

        // 验证
        assertTrue(result);
        verify(zSetOperations, never()).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        verify(zSetOperations).add(key, candlestick, score);
    }

    @Test
    void testCacheKlineData_Overwrite() {
        // 准备
        Candlestick candlestick = new Candlestick();
        candlestick.setSymbol("BTC-USDT");
        candlestick.setIntervalVal("1m");
        candlestick.setOpenTime(LocalDateTime.now());
        candlestick.setOpen(new BigDecimal("50000"));
        candlestick.setHigh(new BigDecimal("51000"));
        candlestick.setLow(new BigDecimal("49000"));
        candlestick.setClose(new BigDecimal("50500"));
        candlestick.setVolume(new BigDecimal("10"));

        String key = "kline-data:BTC-USDT:1m";
        long timestamp = candlestick.getOpenTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double score = Double.parseDouble(String.valueOf(timestamp));

        Set<Object> existingData = new HashSet<>();
        existingData.add(new Candlestick());

        when(zSetOperations.rangeByScore(eq(key), eq(score), eq(score))).thenReturn(existingData);
        when(zSetOperations.add(eq(key), eq(candlestick), eq(score))).thenReturn(true);

        // 执行
        boolean result = klineCacheService.cacheKlineData(candlestick);

        // 验证
        assertTrue(result);
        verify(zSetOperations).removeRangeByScore(key, score, score);
        verify(zSetOperations).add(key, candlestick, score);
    }



    @Test
    void testGetLatestKlineData() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        int limit = 5;
        String key = "kline-data:" + symbol + ":" + interval;

        // 模拟Redis返回的K线数据
        Set<Object> redisResult = new HashSet<>();
        Candlestick c1 = new Candlestick();
        c1.setSymbol(symbol);
        c1.setIntervalVal(interval);
        c1.setOpenTime(LocalDateTime.now().minusMinutes(2));

        Candlestick c2 = new Candlestick();
        c2.setSymbol(symbol);
        c2.setIntervalVal(interval);
        c2.setOpenTime(LocalDateTime.now().minusMinutes(1));

        redisResult.add(c1);
        redisResult.add(c2);

        when(zSetOperations.reverseRange(eq(key), eq(0L), eq((long)(limit - 1)))).thenReturn(redisResult);

        // 执行
        List<CandlestickEntity> result = klineCacheService.getLatestKlineData(symbol, interval, limit);

        // 验证
        assertEquals(2, result.size());
        verify(zSetOperations).reverseRange(key, 0, limit - 1);
    }

    @Test
    void testIsKlineSubscribed() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        String field = symbol + ":" + interval;

        when(hashOperations.hasKey(anyString(), eq(field))).thenReturn(true);

        // 执行
        boolean result = klineCacheService.isKlineSubscribed(symbol, interval);

        // 验证
        assertTrue(result);
        verify(hashOperations).hasKey("kline-subscriptions", field);
    }

    @Test
    void testClearKlineCache() {
        // 准备
        String symbol = "BTC-USDT";
        String interval = "1m";
        String key = "kline-data:" + symbol + ":" + interval;

        when(redisTemplate.delete(eq(key))).thenReturn(true);

        // 执行
        boolean result = klineCacheService.clearKlineCache(symbol, interval);

        // 验证
        assertTrue(result);
        verify(redisTemplate).delete(key);
    }
}
