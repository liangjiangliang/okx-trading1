package com.okx.trading.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.alibaba.fastjson.JSONObject;
import com.okx.trading.config.OkxApiConfig;

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocketUtil测试类
 * 验证WebSocket连接和稳定性机制
 */
public class WebSocketUtilTest {

    @Mock
    private OkxApiConfig okxApiConfig;

    @Mock
    private OkxApiConfig.WebSocketConfig webSocketConfig;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private WebSocket webSocket;

    @Mock
    private ScheduledExecutorService pingScheduler;

    @Mock
    private ScheduledExecutorService reconnectScheduler;

    private WebSocketUtil webSocketUtil;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置模拟对象
        when(okxApiConfig.getWs()).thenReturn(webSocketConfig);
        when(webSocketConfig.getPublicChannel()).thenReturn("wss://test-public-ws.okx.com");
        when(webSocketConfig.getPrivateChannel()).thenReturn("wss://test-private-ws.okx.com");
        when(webSocketConfig.getBussinessChannel()).thenReturn("wss://test-business-ws.okx.com");
        when(okxApiConfig.isWebSocketMode()).thenReturn(true);
        when(okxApiConfig.getApiKey()).thenReturn("test-api-key");
        when(okxApiConfig.getSecretKey()).thenReturn("test-secret-key");
        when(okxApiConfig.getPassphrase()).thenReturn("test-passphrase");

        // 创建被测试对象
        webSocketUtil = new WebSocketUtil(okxApiConfig, okHttpClient);

        // 注入模拟的调度器
        ReflectionTestUtils.setField(webSocketUtil, "pingScheduler", pingScheduler);
        ReflectionTestUtils.setField(webSocketUtil, "reconnectScheduler", reconnectScheduler);

        // 模拟WebSocket连接
        when(okHttpClient.newWebSocket(any(), any())).thenReturn(webSocket);
        when(webSocket.send(anyString())).thenReturn(true);
    }

    /**
     * 测试初始化方法
     */
    @Test
    public void testInit() {
        // 执行被测试方法
        webSocketUtil.init();

        // 验证WebSocket连接是否被创建
        verify(okHttpClient, times(3)).newWebSocket(any(), any());
        
        // 验证是否设置了定时任务
        verify(pingScheduler).scheduleAtFixedRate(any(Runnable.class), eq(15L), eq(15L), eq(TimeUnit.SECONDS));
        verify(reconnectScheduler).scheduleAtFixedRate(any(Runnable.class), eq(30L), eq(30L), eq(TimeUnit.SECONDS));
    }

    /**
     * 测试连接恢复机制
     */
    @Test
    public void testConnectionRecovery() {
        // 设置WebSocketUtil中的私有字段
        ReflectionTestUtils.setField(webSocketUtil, "publicWebSocket", webSocket);
        ReflectionTestUtils.setField(webSocketUtil, "privateWebSocket", webSocket);
        ReflectionTestUtils.setField(webSocketUtil, "bussinessWebSocket", webSocket);

        // 模拟连接断开（send方法返回false）
        when(webSocket.send(anyString())).thenReturn(false);

        // 执行检查连接状态的方法
        ReflectionTestUtils.invokeMethod(webSocketUtil, "checkConnectionsAndReconnect");

        // 验证是否尝试重新连接
        verify(webSocket, times(3)).close(eq(1000), anyString());
        verify(okHttpClient, times(3)).newWebSocket(any(), any());
    }

    /**
     * 测试ping机制
     */
    @Test
    public void testPingMechanism() {
        // 设置WebSocketUtil中的私有字段
        ReflectionTestUtils.setField(webSocketUtil, "publicWebSocket", webSocket);
        ReflectionTestUtils.setField(webSocketUtil, "privateWebSocket", webSocket);
        ReflectionTestUtils.setField(webSocketUtil, "bussinessWebSocket", webSocket);

        // 执行ping方法
        ReflectionTestUtils.invokeMethod(webSocketUtil, "pingWebSockets");

        // 验证是否发送了ping消息
        verify(webSocket, times(3)).send("ping");
    }

    /**
     * 测试消息处理
     */
    @Test
    public void testMessageHandling() throws InterruptedException {
        // 注册消息处理器
        CountDownLatch latch = new CountDownLatch(1);
        webSocketUtil.registerHandler("test-channel", message -> latch.countDown());

        // 创建测试消息
        JSONObject arg = new JSONObject();
        arg.put("channel", "test-channel");
        
        JSONObject message = new JSONObject();
        message.put("arg", arg);

        // 调用消息处理方法
        ReflectionTestUtils.invokeMethod(webSocketUtil, "handleMessage", message.toString());

        // 验证消息处理器是否被调用
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * 测试指数退避重连机制
     */
    @Test
    public void testExponentialBackoffReconnect() {
        // 捕获重连调用
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        
        // 触发重连
        ReflectionTestUtils.invokeMethod(webSocketUtil, "schedulePublicReconnect");
        
        // 验证是否使用指数退避策略
        verify(reconnectScheduler).schedule(runnableCaptor.capture(), eq(5L), eq(TimeUnit.SECONDS));
        
        // 获取重连Runnable
        Runnable reconnectTask = runnableCaptor.getValue();
        assertNotNull(reconnectTask, "重连任务不应为空");
        
        // 模拟连接失败
        when(webSocket.send(anyString())).thenReturn(false);
        when(okHttpClient.newWebSocket(any(), any())).thenReturn(webSocket);
        
        // 执行重连任务
        reconnectTask.run();
        
        // 验证是否尝试了新的连接
        verify(okHttpClient, times(2)).newWebSocket(any(), any());
    }

    /**
     * 测试WebSocket断开后重连时的操作恢复功能
     */
    @Test
    public void testOperationRecoveryAfterReconnection() {
        // 1. 首先订阅一个主题，但模拟连接未就绪
        AtomicBoolean publicConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "publicConnected");
        AtomicBoolean bussinessConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "bussinessConnected");
        AtomicBoolean privateConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "privateConnected");
        
        // 保存原始值以便恢复
        boolean origPublicConnected = publicConnected.get();
        boolean origBussinessConnected = bussinessConnected.get();
        boolean origPrivateConnected = privateConnected.get();
        
        try {
            // 设置为false模拟未连接
            publicConnected.set(false);
            bussinessConnected.set(false);
            privateConnected.set(false);
            
            // 2. 执行订阅操作，应该加入待执行队列
            webSocketUtil.subscribePublicTopic("test-channel", "BTC-USDT");
            webSocketUtil.subscribePrivateTopic("account");
            
            // 3. 验证操作已加入队列
            Object publicQueue = ReflectionTestUtils.getField(webSocketUtil, "publicPendingOperations");
            Object privateQueue = ReflectionTestUtils.getField(webSocketUtil, "privatePendingOperations");
            assertTrue(publicQueue != null && !publicQueue.toString().contains("size=0"));
            assertTrue(privateQueue != null && !privateQueue.toString().contains("size=0"));
            
            // 4. 模拟连接建立
            ArgumentCaptor<WebSocketListener> listenerCaptor = ArgumentCaptor.forClass(WebSocketListener.class);
            when(okHttpClient.newWebSocket(any(), listenerCaptor.capture())).thenReturn(webSocket);
            
            // 5. 触发连接
            ReflectionTestUtils.invokeMethod(webSocketUtil, "connectPublicChannel");
            ReflectionTestUtils.invokeMethod(webSocketUtil, "connectBussinessChannel");
            ReflectionTestUtils.invokeMethod(webSocketUtil, "connectPrivateChannel");
            
            // 6. 捕获监听器并模拟公共频道连接成功
            if (!listenerCaptor.getAllValues().isEmpty()) {
                WebSocketListener publicListener = listenerCaptor.getAllValues().get(0);
                if (listenerCaptor.getAllValues().size() > 1) {
                    WebSocketListener bussinessListener = listenerCaptor.getAllValues().get(1);
                    // 模拟连接成功
                    publicListener.onOpen(webSocket, null);
                    bussinessListener.onOpen(webSocket, null);
                }
            }
            
            // 模拟私有频道登录成功（需要修改标记并触发恢复）
            privateConnected.set(true);
            ReflectionTestUtils.invokeMethod(webSocketUtil, "restorePrivateOperations");
            
            // 验证是否发送了订阅消息
            verify(webSocket, times(2)).send(anyString()); // 公共和私有消息各一次
        } finally {
            // 恢复原始状态
            publicConnected.set(origPublicConnected);
            bussinessConnected.set(origBussinessConnected);
            privateConnected.set(origPrivateConnected);
        }
    }
    
    /**
     * 测试连接断开时的自动重连和恢复功能
     */
    @Test
    public void testAutoReconnectAndRecovery() {
        // 获取原子布尔变量
        AtomicBoolean publicConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "publicConnected");
        AtomicBoolean bussinessConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "bussinessConnected");
        AtomicBoolean privateConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "privateConnected");
        
        // 保存原始值以便恢复
        boolean origPublicConnected = publicConnected.get();
        boolean origBussinessConnected = bussinessConnected.get();
        boolean origPrivateConnected = privateConnected.get();
        
        try {
            // 1. 设置WebSocket和连接状态
            WebSocket mockWebSocket = webSocket; // 保存原始mock
            ReflectionTestUtils.setField(webSocketUtil, "publicWebSocket", mockWebSocket);
            ReflectionTestUtils.setField(webSocketUtil, "bussinessWebSocket", mockWebSocket);
            ReflectionTestUtils.setField(webSocketUtil, "privateWebSocket", mockWebSocket);
            publicConnected.set(true);
            bussinessConnected.set(true);
            privateConnected.set(true);
            
            // 2. 订阅主题
            webSocketUtil.subscribePublicTopic("test-channel", "BTC-USDT");
            
            // 3. 捕获重连任务
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            
            // 4. 模拟连接关闭
            ArgumentCaptor<WebSocketListener> listenerCaptor = ArgumentCaptor.forClass(WebSocketListener.class);
            when(okHttpClient.newWebSocket(any(), listenerCaptor.capture())).thenReturn(webSocket);
            ReflectionTestUtils.invokeMethod(webSocketUtil, "connectPublicChannel");
            
            if (!listenerCaptor.getAllValues().isEmpty()) {
                WebSocketListener listener = listenerCaptor.getValue();
                
                // 5. 触发onClosed回调
                listener.onClosed(mockWebSocket, 1001, "Connection closed");
                
                // 6. 验证是否调用了重连方法
                verify(reconnectScheduler).schedule(runnableCaptor.capture(), eq(5L), eq(TimeUnit.SECONDS));
                
                // 7. 执行重连任务
                when(webSocket.send(anyString())).thenReturn(true); // 确保连接成功
                if (runnableCaptor.getValue() != null) {
                    runnableCaptor.getValue().run();
                }
            }
        } finally {
            // 恢复原始状态
            publicConnected.set(origPublicConnected);
            bussinessConnected.set(origBussinessConnected);
            privateConnected.set(origPrivateConnected);
        }
    }
    
    /**
     * 测试待执行操作队列
     */
    @Test
    public void testPendingOperationsQueue() {
        // 获取原子布尔变量
        AtomicBoolean publicConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "publicConnected");
        AtomicBoolean bussinessConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "bussinessConnected");
        AtomicBoolean privateConnected = (AtomicBoolean) ReflectionTestUtils.getField(webSocketUtil, "privateConnected");
        
        // 保存原始值以便恢复
        boolean origPublicConnected = publicConnected.get();
        boolean origBussinessConnected = bussinessConnected.get();
        boolean origPrivateConnected = privateConnected.get();
        
        try {
            // 1. 模拟连接未就绪
            publicConnected.set(false);
            privateConnected.set(false);
            
            // 2. 执行多个订阅操作
            webSocketUtil.subscribePublicTopic("test-channel1", "BTC-USDT");
            webSocketUtil.subscribePublicTopic("test-channel2", "ETH-USDT");
            webSocketUtil.subscribePrivateTopic("account");
            webSocketUtil.subscribePrivateTopic("orders");
            
            // 3. 验证队列中的操作数量
            Object publicQueue = ReflectionTestUtils.getField(webSocketUtil, "publicPendingOperations");
            Object privateQueue = ReflectionTestUtils.getField(webSocketUtil, "privatePendingOperations");
            assertTrue(publicQueue != null && !publicQueue.toString().contains("size=0")); // 确认公共频道有操作
            assertTrue(privateQueue != null && !privateQueue.toString().contains("size=0")); // 确认私有频道有操作
            
            // 4. 模拟连接就绪并恢复操作
            publicConnected.set(true);
            bussinessConnected.set(true);
            ReflectionTestUtils.setField(webSocketUtil, "publicWebSocket", webSocket);
            ReflectionTestUtils.setField(webSocketUtil, "bussinessWebSocket", webSocket);
            ReflectionTestUtils.invokeMethod(webSocketUtil, "restorePublicOperations");
            
            privateConnected.set(true);
            ReflectionTestUtils.setField(webSocketUtil, "privateWebSocket", webSocket);
            ReflectionTestUtils.invokeMethod(webSocketUtil, "restorePrivateOperations");
            
            // 5. 验证队列是否已处理
            publicQueue = ReflectionTestUtils.getField(webSocketUtil, "publicPendingOperations");
            privateQueue = ReflectionTestUtils.getField(webSocketUtil, "privatePendingOperations");
            
            // 6. 验证是否发送了订阅消息
            verify(webSocket, times(4)).send(anyString()); // 总共4个订阅消息
        } finally {
            // 恢复原始状态
            publicConnected.set(origPublicConnected);
            bussinessConnected.set(origBussinessConnected);
            privateConnected.set(origPrivateConnected);
        }
    }
} 