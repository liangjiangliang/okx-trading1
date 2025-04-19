package com.okx.trading.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KlineCleanupServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private Cursor<byte[]> cursor;

    private KlineCleanupServiceImpl klineCleanupService;

    @BeforeEach
    public void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);

        klineCleanupService = new KlineCleanupServiceImpl(redisTemplate);

        // 设置默认配置
        ReflectionTestUtils.setField(klineCleanupService, "cleanupIntervalMinutes", 30);
        ReflectionTestUtils.setField(klineCleanupService, "maxKlineCount", 300);
    }

    @Test
    public void testCleanupKlineData_WhenKeysFound_AndSizeExceedsLimit() {
        // 准备测试数据
        String testKey = "coin-rt-kline:BTC-USDT:1h";
        Set<String> testKeys = new HashSet<>();
        testKeys.add(testKey);

        // 模拟scanKeys的行为
        when(redisTemplate.execute(any(RedisTemplate.RedisCallback.class))).thenAnswer(invocation -> {
            return testKeys;
        });

        // 模拟zSet大小为500，超过默认的300
        when(zSetOperations.size(testKey)).thenReturn(500L);

        // 模拟获取降序排列的元素
        Set<ZSetOperations.TypedTuple<Object>> mockElementsSet = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            ZSetOperations.TypedTuple<Object> mockTuple = mock(ZSetOperations.TypedTuple.class);
            when(mockTuple.getValue()).thenReturn("element_" + i);
            when(mockTuple.getScore()).thenReturn((double) (500 - i)); // 降序，分数从500降到1
            mockElementsSet.add(mockTuple);
        }

        when(zSetOperations.reverseRangeWithScores(testKey, 0, -1)).thenReturn(mockElementsSet);

        // 模拟成功删除200条(500-300)数据
        when(zSetOperations.remove(eq(testKey), any())).thenReturn(200L);

        // 执行清理方法
        klineCleanupService.cleanupKlineData();

        // 验证方法调用
        verify(zSetOperations).size(testKey);
        verify(zSetOperations).reverseRangeWithScores(testKey, 0, -1);

        // 捕获传递给remove方法的参数
        ArgumentCaptor<Object[]> removeArgsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(zSetOperations).remove(eq(testKey), removeArgsCaptor.capture());

        // 验证实际删除的元素数量
        Object[] removedElements = removeArgsCaptor.getValue();
        assertEquals(200, removedElements.length, "应该删除超出限制的200条数据");

        // 验证删除的是后200个元素（低分数的元素）
        boolean allLowerScores = true;
        for (Object element : removedElements) {
            String elementStr = (String) element;
            int index = Integer.parseInt(elementStr.substring("element_".length()));
            if (index < 300) { // 前300个元素（高分数的）不应被删除
                allLowerScores = false;
                break;
            }
        }

        assertTrue(allLowerScores, "应该删除score较低的元素（索引300-499）");
    }

    @Test
    public void testCleanupKlineData_WhenSizeDoesNotExceedLimit() {
        // 准备测试数据
        String testKey = "coin-rt-kline:BTC-USDT:1h";
        Set<String> testKeys = new HashSet<>();
        testKeys.add(testKey);

        // 模拟scanKeys的行为
        when(redisTemplate.execute(any(RedisTemplate.RedisCallback.class))).thenAnswer(invocation -> {
            return testKeys;
        });

        // 模拟zSet大小为200，不超过默认的300
        when(zSetOperations.size(testKey)).thenReturn(200L);

        // 执行清理方法
        klineCleanupService.cleanupKlineData();

        // 验证方法调用
        verify(zSetOperations).size(testKey);
        // 数据量未超限，不应调用删除方法
        verify(zSetOperations, never()).reverseRangeWithScores(anyString(), anyLong(), anyLong());
        verify(zSetOperations, never()).remove(anyString(), any());
    }

    @Test
    public void testStartAndStopCleanupThread() throws Exception {
        // 测试启动清理线程
        assertFalse(klineCleanupService.isRunning(), "初始状态应为未运行");

        klineCleanupService.startCleanupThread();
        assertTrue(klineCleanupService.isRunning(), "启动后状态应为运行中");

        // 尝试再次启动，应该不会有影响
        klineCleanupService.startCleanupThread();
        assertTrue(klineCleanupService.isRunning(), "重复启动后状态应为运行中");

        // 测试停止清理线程
        klineCleanupService.stopCleanupThread();
        assertFalse(klineCleanupService.isRunning(), "停止后状态应为未运行");

        // 尝试再次停止，应该不会有影响
        klineCleanupService.stopCleanupThread();
        assertFalse(klineCleanupService.isRunning(), "重复停止后状态应为未运行");
    }

    @Test
    public void testSetCleanupInterval() {
        // 测试设置正常值
        klineCleanupService.setCleanupInterval(60);
        assertEquals(60, klineCleanupService.getCleanupInterval(), "清理间隔应被正确设置为60分钟");

        // 测试设置小于最小值的值
        klineCleanupService.setCleanupInterval(3);
        assertEquals(5, klineCleanupService.getCleanupInterval(), "清理间隔不应小于5分钟");
    }

    @Test
    public void testSetMaxKlineCount() {
        // 测试设置正常值
        klineCleanupService.setMaxKlineCount(500);
        assertEquals(500, klineCleanupService.getMaxKlineCount(), "最大K线数应被正确设置为500");

        // 测试设置小于最小值的值
        klineCleanupService.setMaxKlineCount(30);
        assertEquals(50, klineCleanupService.getMaxKlineCount(), "最大K线数不应小于50");
    }
}
