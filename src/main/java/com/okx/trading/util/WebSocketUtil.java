package com.okx.trading.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.okx.trading.config.OkxApiConfig;
import com.okx.trading.exception.OkxApiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * WebSocket工具类
 * 处理与OKX交易所的WebSocket连接和消息
 */
@Slf4j
@Component
public class WebSocketUtil {
    
    private final OkxApiConfig okxApiConfig;
    private final OkHttpClient okHttpClient;
    
    private WebSocket publicWebSocket;
    private WebSocket privateWebSocket;
    
    private final Map<String, Consumer<JSONObject>> messageHandlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    
    @Autowired
    public WebSocketUtil(OkxApiConfig okxApiConfig, OkHttpClient okHttpClient) {
        this.okxApiConfig = okxApiConfig;
        this.okHttpClient = okHttpClient;
    }
    
    /**
     * 初始化WebSocket连接
     */
    @PostConstruct
    public void init() {
        if (okxApiConfig.isWebSocketMode()) {
            connectPublicChannel();
            connectPrivateChannel();
            
            // 定时发送ping消息，保持连接
            pingScheduler.scheduleAtFixedRate(this::pingWebSockets, 20, 20, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void cleanup() {
        pingScheduler.shutdown();
        reconnectScheduler.shutdown();
        try {
            if (!pingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                pingScheduler.shutdownNow();
            }
            if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            pingScheduler.shutdownNow();
            reconnectScheduler.shutdownNow();
        }
        
        if (publicWebSocket != null) {
            publicWebSocket.close(1000, "Application shutting down");
        }
        
        if (privateWebSocket != null) {
            privateWebSocket.close(1000, "Application shutting down");
        }
    }
    
    /**
     * 连接公共频道
     */
    private void connectPublicChannel() {
        try {
            Request request = new Request.Builder()
                    .url(okxApiConfig.getWs().getPublicChannel())
                    .build();
            
            publicWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("公共频道WebSocket连接成功");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    handleMessage(text);
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("公共频道WebSocket连接失败", t);
                    // 尝试重连
                    schedulePublicReconnect();
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("公共频道WebSocket连接关闭: {}, {}", code, reason);
                    // 如果不是应用主动关闭，尝试重连
                    if (code != 1000) {
                        schedulePublicReconnect();
                    }
                }
            });
        } catch (Exception e) {
            log.error("创建公共频道WebSocket连接失败", e);
            schedulePublicReconnect();
        }
    }
    
    /**
     * 安排公共频道重连
     */
    private void schedulePublicReconnect() {
        reconnectScheduler.schedule(() -> {
            try {
                if (publicWebSocket != null) {
                    publicWebSocket.close(1000, "Reconnecting");
                }
                connectPublicChannel();
            } catch (Exception e) {
                log.error("重连公共频道失败", e);
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 连接私有频道
     */
    private void connectPrivateChannel() {
        try {
            // 登录认证参数
            String timestamp = SignatureUtil.getIsoTimestamp();
            String method = "GET";
            String requestPath = "/users/self/verify";
            String body = "";
            String sign = SignatureUtil.sign(timestamp, method, requestPath, body, okxApiConfig.getSecretKey());
            
            Request request = new Request.Builder()
                    .url(okxApiConfig.getWs().getPrivateChannel())
                    .build();
            
            privateWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("私有频道WebSocket连接成功");
                    // 发送登录消息
                    JSONObject loginMessage = new JSONObject();
                    loginMessage.put("op", "login");
                    
                    JSONObject arg = new JSONObject();
                    arg.put("apiKey", okxApiConfig.getApiKey());
                    arg.put("passphrase", okxApiConfig.getPassphrase());
                    arg.put("timestamp", timestamp);
                    arg.put("sign", sign);
                    
                    JSONObject[] args = new JSONObject[]{arg};
                    loginMessage.put("args", args);
                    
                    webSocket.send(loginMessage.toJSONString());
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    handleMessage(text);
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("私有频道WebSocket连接失败", t);
                    // 尝试重连
                    schedulePrivateReconnect();
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("私有频道WebSocket连接关闭: {}, {}", code, reason);
                    // 如果不是应用主动关闭，尝试重连
                    if (code != 1000) {
                        schedulePrivateReconnect();
                    }
                }
            });
        } catch (Exception e) {
            log.error("创建私有频道WebSocket连接失败", e);
            schedulePrivateReconnect();
        }
    }
    
    /**
     * 安排私有频道重连
     */
    private void schedulePrivateReconnect() {
        reconnectScheduler.schedule(() -> {
            try {
                if (privateWebSocket != null) {
                    privateWebSocket.close(1000, "Reconnecting");
                }
                connectPrivateChannel();
            } catch (Exception e) {
                log.error("重连私有频道失败", e);
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 发送ping消息，保持连接活跃
     */
    private void pingWebSockets() {
        try {
            if (publicWebSocket != null) {
                JSONObject pingMessage = new JSONObject();
                pingMessage.put("op", "ping");
                publicWebSocket.send(pingMessage.toJSONString());
            }
            
            if (privateWebSocket != null) {
                JSONObject pingMessage = new JSONObject();
                pingMessage.put("op", "ping");
                privateWebSocket.send(pingMessage.toJSONString());
            }
        } catch (Exception e) {
            log.error("发送ping消息失败", e);
        }
    }
    
    /**
     * 处理接收到的WebSocket消息
     */
    private void handleMessage(String message) {
        try {
            JSONObject jsonMessage = JSON.parseObject(message);
            
            // 处理错误消息
            if (jsonMessage.containsKey("event") && "error".equals(jsonMessage.getString("event"))) {
                log.error("收到WebSocket错误: code={}, msg={}", 
                    jsonMessage.getString("code"), 
                    jsonMessage.getString("msg"));
                
                // 如果是时间戳错误，尝试重新连接
                if ("60004".equals(jsonMessage.getString("code"))) {
                    log.info("时间戳错误，尝试重新连接私有频道");
                    schedulePrivateReconnect();
                }
                return;
            }
            
            // 处理pong响应
            if (jsonMessage.containsKey("op") && "pong".equals(jsonMessage.getString("op"))) {
                log.debug("收到pong响应: {}", message);
                return;
            }
            
            // 处理登录响应
            if (jsonMessage.containsKey("op") && "login".equals(jsonMessage.getString("op"))) {
                log.info("WebSocket登录响应: {}", message);
                return;
            }
            
            // 根据消息类型路由到相应的处理器
            String topic = null;
            if (jsonMessage.containsKey("arg") && jsonMessage.getJSONObject("arg").containsKey("channel")) {
                topic = jsonMessage.getJSONObject("arg").getString("channel");
            }
            
            if (topic != null && messageHandlers.containsKey(topic)) {
                messageHandlers.get(topic).accept(jsonMessage);
            } else {
                log.debug("收到未处理的WebSocket消息: {}", message);
            }
        } catch (Exception e) {
            log.error("解析WebSocket消息失败: {}", message, e);
        }
    }
    
    /**
     * 注册消息处理器
     *
     * @param topic 订阅主题
     * @param handler 消息处理器
     */
    public void registerHandler(String topic, Consumer<JSONObject> handler) {
        messageHandlers.put(topic, handler);
    }
    
    /**
     * 订阅公共频道主题
     *
     * @param topic 主题
     * @param symbol 交易对
     */
    public void subscribePublicTopic(String topic, String symbol) {
        if (publicWebSocket == null) {
            throw new OkxApiException("公共频道WebSocket未连接");
        }
        
        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");
        
        JSONObject arg = new JSONObject();
        arg.put("channel", topic);
        arg.put("instId", symbol);
        
        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);
        
        publicWebSocket.send(subscribeMessage.toJSONString());
        log.info("订阅公共频道主题: {}, 交易对: {}", topic, symbol);
    }
    
    /**
     * 订阅私有频道主题
     *
     * @param topic 主题
     */
    public void subscribePrivateTopic(String topic) {
        if (privateWebSocket == null) {
            throw new OkxApiException("私有频道WebSocket未连接");
        }
        
        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");
        
        JSONObject arg = new JSONObject();
        arg.put("channel", topic);
        
        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);
        
        privateWebSocket.send(subscribeMessage.toJSONString());
        log.info("订阅私有频道主题: {}", topic);
    }
    
    /**
     * 取消订阅公共频道主题
     *
     * @param topic 主题
     * @param symbol 交易对
     */
    public void unsubscribePublicTopic(String topic, String symbol) {
        if (publicWebSocket == null) {
            return;
        }
        
        JSONObject unsubscribeMessage = new JSONObject();
        unsubscribeMessage.put("op", "unsubscribe");
        
        JSONObject arg = new JSONObject();
        arg.put("channel", topic);
        arg.put("instId", symbol);
        
        JSONObject[] args = new JSONObject[]{arg};
        unsubscribeMessage.put("args", args);
        
        publicWebSocket.send(unsubscribeMessage.toJSONString());
        log.info("取消订阅公共频道主题: {}, 交易对: {}", topic, symbol);
    }
    
    /**
     * 取消订阅私有频道主题
     *
     * @param topic 主题
     */
    public void unsubscribePrivateTopic(String topic) {
        if (privateWebSocket == null) {
            return;
        }
        
        JSONObject unsubscribeMessage = new JSONObject();
        unsubscribeMessage.put("op", "unsubscribe");
        
        JSONObject arg = new JSONObject();
        arg.put("channel", topic);
        
        JSONObject[] args = new JSONObject[]{arg};
        unsubscribeMessage.put("args", args);
        
        privateWebSocket.send(unsubscribeMessage.toJSONString());
        log.info("取消订阅私有频道主题: {}", topic);
    }
    
    /**
     * 发送私有请求消息
     *
     * @param message 请求消息
     */
    public void sendPrivateRequest(String message) {
        if (privateWebSocket == null) {
            throw new OkxApiException("私有频道WebSocket未连接");
        }
        
        privateWebSocket.send(message);
        log.info("发送私有请求: {}", message);
    }
} 