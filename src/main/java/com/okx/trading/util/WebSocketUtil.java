package com.okx.trading.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.okx.trading.config.OkxApiConfig;
import com.okx.trading.exception.OkxApiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * WebSocket工具类
 * 处理与OKX交易所的WebSocket连接和消息
 */
@Slf4j
@Component
public class WebSocketUtil{

    private final OkxApiConfig okxApiConfig;
    private final OkHttpClient okHttpClient;

    private WebSocket publicWebSocket;
    private WebSocket bussinessWebSocket;
    private WebSocket privateWebSocket;

    private final Map<String,Consumer<JSONObject>> messageHandlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 添加队列存储待执行的操作
    private final ConcurrentLinkedQueue<PendingOperation> publicPendingOperations = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PendingOperation> privatePendingOperations = new ConcurrentLinkedQueue<>();
    
    // 保存已订阅的主题
    private final Set<String> publicSubscribedTopics = ConcurrentHashMap.newKeySet();
    private final Set<String> privateSubscribedTopics = ConcurrentHashMap.newKeySet();
    
    // 连接状态标志
    private final AtomicBoolean publicConnected = new AtomicBoolean(false);
    private final AtomicBoolean privateConnected = new AtomicBoolean(false);
    private final AtomicBoolean bussinessConnected = new AtomicBoolean(false);

    // 添加静态Logger以解决编译问题
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketUtil.class);

    @Autowired
    public WebSocketUtil(OkxApiConfig okxApiConfig, @Qualifier("webSocketHttpClient") OkHttpClient okHttpClient){
        this.okxApiConfig = okxApiConfig;
        this.okHttpClient = okHttpClient;
    }

    /**
     * 初始化WebSocket连接
     */
    @PostConstruct
    public void init(){
        try{
            if(okxApiConfig.isWebSocketMode()){
                logger.info("初始化WebSocket连接，模式: {}", okxApiConfig.getConnectionMode());
                logger.info("公共频道URL: {}", okxApiConfig.getWs().getPublicChannel());
                logger.info("私有频道URL: {}", okxApiConfig.getWs().getPrivateChannel());

                // 连接公共频道
                try{
                    connectPublicChannel();
                    connectBussinessChannel();
                }catch(Exception e){
                    logger.error("连接公共频道失败: {}", e.getMessage(), e);
                }

                // 连接私有频道
                try{
                    connectPrivateChannel();
                }catch(Exception e){
                    logger.error("连接私有频道失败: {}", e.getMessage(), e);
                }

                // 定时发送ping消息，保持连接
                pingScheduler.scheduleAtFixedRate(this :: pingWebSockets, 15, 15, TimeUnit.SECONDS);
                
                // 定时检查连接状态，实现自动恢复
                reconnectScheduler.scheduleAtFixedRate(this :: checkConnectionsAndReconnect, 30, 30, TimeUnit.SECONDS);
            }else{
                logger.info("WebSocket模式未启用，使用REST模式");
            }
        }catch(Exception e){
            logger.error("初始化WebSocket连接失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void cleanup(){
        pingScheduler.shutdown();
        reconnectScheduler.shutdown();
        try{
            if(! pingScheduler.awaitTermination(5, TimeUnit.SECONDS)){
                pingScheduler.shutdownNow();
            }
            if(! reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)){
                reconnectScheduler.shutdownNow();
            }
        }catch(InterruptedException e){
            pingScheduler.shutdownNow();
            reconnectScheduler.shutdownNow();
        }

        if(publicWebSocket != null){
            publicWebSocket.close(1000, "Application shutting down");
        }

        if(bussinessWebSocket != null){
            bussinessWebSocket.close(1000, "Application shutting down");
        }

        if(privateWebSocket != null){
            privateWebSocket.close(1000, "Application shutting down");
        }
    }

    /**
     * 连接bussiness频道
     */
    private void connectBussinessChannel(){
        try{
            Request request = new Request.Builder()
                .url(okxApiConfig.getWs().getBussinessChannel())
                .build();

            bussinessWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener(){
                @Override
                public void onOpen(WebSocket webSocket, Response response){
                    logger.info("业务频道WebSocket连接成功");
                    bussinessConnected.set(true);
                    
                    // 恢复之前的操作
                    restorePublicOperations();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text){
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response){
                    logger.error("业务频道WebSocket连接失败", t);
                    bussinessConnected.set(false);
                    // 尝试重连
                    schedulePublicReconnect();
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason){
                    logger.info("业务频道WebSocket连接关闭: {}, {}", code, reason);
                    bussinessConnected.set(false);
                    // 如果不是应用主动关闭，尝试重连
                    if(code != 1000){
                        schedulePublicReconnect();
                    }
                }
            });
        }catch(Exception e){
            logger.error("创建业务频道WebSocket连接失败", e);
            bussinessConnected.set(false);
            schedulePublicReconnect();
        }
    }

    /**
     * 连接公共频道
     */
    private void connectPublicChannel(){
        try{
            Request request = new Request.Builder()
                .url(okxApiConfig.getWs().getPublicChannel())
                .build();

            publicWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener(){
                @Override
                public void onOpen(WebSocket webSocket, Response response){
                    logger.info("公共频道WebSocket连接成功");
                    publicConnected.set(true);
                    
                    // 恢复之前的操作
                    restorePublicOperations();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text){
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response){
                    logger.error("公共频道WebSocket连接失败", t);
                    publicConnected.set(false);
                    // 尝试重连
                    schedulePublicReconnect();
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason){
                    logger.info("公共频道WebSocket连接关闭: {}, {}", code, reason);
                    publicConnected.set(false);
                    // 如果不是应用主动关闭，尝试重连
                    if(code != 1000){
                        schedulePublicReconnect();
                    }
                }
            });
        }catch(Exception e){
            logger.error("创建公共频道WebSocket连接失败", e);
            publicConnected.set(false);
            schedulePublicReconnect();
        }
    }

    /**
     * 安排公共频道重连
     */
    private void schedulePublicReconnect(){
        // 使用原子整数跟踪重试次数，实现指数退避
        AtomicInteger retryCount = new AtomicInteger(0);
        
        reconnectScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    int currentRetry = retryCount.getAndIncrement();
                    if (currentRetry > 10) {
                        logger.warn("公共频道重连尝试次数过多，将降低重试频率");
                        retryCount.set(10); // 限制最大重试次数
                    }
                    
                    // 指数退避，但有最大延迟限制
                    long delaySeconds = Math.min(30, (long)Math.pow(2, currentRetry));
                    
                    if(publicWebSocket != null){
                        try {
                            publicWebSocket.close(1000, "Reconnecting");
                        } catch (Exception e) {
                            logger.debug("关闭旧公共频道连接失败", e);
                        }
                    }
                    connectPublicChannel();

                    if(bussinessWebSocket != null){
                        try {
                            bussinessWebSocket.close(1000, "Reconnecting");
                        } catch (Exception e) {
                            logger.debug("关闭旧业务频道连接失败", e);
                        }
                    }
                    connectBussinessChannel();
                    
                    // 如果重连成功，重置重试计数器
                    if (isWebSocketConnected(publicWebSocket) && isWebSocketConnected(bussinessWebSocket)) {
                        retryCount.set(0);
                    } else {
                        // 如果失败，安排下一次重试
                        reconnectScheduler.schedule(this, delaySeconds, TimeUnit.SECONDS);
                    }
                } catch(Exception e){
                    logger.error("重连公共频道失败", e);
                    // 安排下一次重试
                    reconnectScheduler.schedule(this, 5, TimeUnit.SECONDS);
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 连接私有频道
     */
    private void connectPrivateChannel(){
        try{
            // 登录认证参数
            String timestamp = System.currentTimeMillis() / 1000 + "";
            String method = "GET";
            String requestPath = "/users/self/verify";
            String body = "";
            String sign = SignatureUtil.sign(timestamp, method, requestPath, body, okxApiConfig.getSecretKey());

            Request request = new Request.Builder()
                .url(okxApiConfig.getWs().getPrivateChannel())
                .build();

            privateWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener(){
                @Override
                public void onOpen(WebSocket webSocket, Response response){
                    logger.info("私有频道WebSocket连接成功");
                    // 发送登录消息 - 登录成功后会在登录响应中恢复订阅
                    sendLoginMessage(webSocket, timestamp, sign);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text){
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response){
                    logger.error("私有频道WebSocket连接失败", t);
                    privateConnected.set(false);
                    // 尝试重连
                    schedulePrivateReconnect();
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason){
                    logger.info("私有频道WebSocket连接关闭: {}, {}", code, reason);
                    privateConnected.set(false);
                    // 如果不是应用主动关闭，尝试重连
                    if(code != 1000){
                        schedulePrivateReconnect();
                    }
                }
            });
        }catch(Exception e){
            logger.error("连接私有频道失败", e);
            privateConnected.set(false);
            schedulePrivateReconnect();
        }
    }

    /**
     * 发送登录消息
     */
    private void sendLoginMessage(WebSocket webSocket, String timestamp, String sign){
        try{
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
            logger.debug("发送登录消息: {}", loginMessage);
        }catch(Exception e){
            logger.error("发送登录消息失败", e);
        }
    }

    /**
     * 安排私有频道重连
     */
    private void schedulePrivateReconnect(){
        // 使用原子整数跟踪重试次数，实现指数退避
        AtomicInteger retryCount = new AtomicInteger(0);
        
        reconnectScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    int currentRetry = retryCount.getAndIncrement();
                    if (currentRetry > 10) {
                        logger.warn("私有频道重连尝试次数过多，将降低重试频率");
                        retryCount.set(10); // 限制最大重试次数
                    }
                    
                    // 指数退避，但有最大延迟限制
                    long delaySeconds = Math.min(30, (long)Math.pow(2, currentRetry));
                    
                    if(privateWebSocket != null){
                        try {
                            privateWebSocket.close(1000, "Reconnecting");
                        } catch (Exception e) {
                            logger.debug("关闭旧私有频道连接失败", e);
                        }
                    }
                    connectPrivateChannel();
                    
                    // 如果重连成功，重置重试计数器
                    if (isWebSocketConnected(privateWebSocket)) {
                        retryCount.set(0);
                    } else {
                        // 如果失败，安排下一次重试
                        reconnectScheduler.schedule(this, delaySeconds, TimeUnit.SECONDS);
                    }
                } catch(Exception e){
                    logger.error("重连私有频道失败", e);
                    // 安排下一次重试
                    reconnectScheduler.schedule(this, 5, TimeUnit.SECONDS);
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 发送ping消息，保持连接活跃
     */
    private void pingWebSockets(){
        try{
            // 对每个连接尝试发送ping消息
            if(publicWebSocket != null){
                try {
                    publicWebSocket.send("ping");
                    logger.debug("发送公共频道ping消息");
                } catch (Exception e) {
                    logger.warn("发送公共频道ping消息失败，将尝试重连", e);
                    schedulePublicReconnect();
                }
            }

            if(bussinessWebSocket != null){
                try {
                    bussinessWebSocket.send("ping");
                    logger.debug("发送业务频道ping消息");
                } catch (Exception e) {
                    logger.warn("发送业务频道ping消息失败，将尝试重连", e);
                    schedulePublicReconnect();
                }
            }

            if(privateWebSocket != null){
                try {
                    privateWebSocket.send("ping");
                    logger.debug("发送私有频道ping消息");
                } catch (Exception e) {
                    logger.warn("发送私有频道ping消息失败，将尝试重连", e);
                    schedulePrivateReconnect();
                }
            }
        }catch(Exception e){
            logger.error("发送ping消息失败", e);
        }
    }

    /**
     * 处理接收到的WebSocket消息
     */
    private void handleMessage(String message){
        try{
            // 处理简单的ping-pong响应
            if("ping".equals(message)){
                if(publicWebSocket != null){
                    publicWebSocket.send("pong");
                }
                if(privateWebSocket != null){
                    privateWebSocket.send("pong");
                }
                logger.debug("收到ping消息，已回复pong");
                return;
            }

            if("pong".equals(message)){
                logger.debug("收到简单pong响应");
                return;
            }

            JSONObject jsonMessage;
            try{
                jsonMessage = JSON.parseObject(message);
            }catch(Exception e){
                logger.warn("无法解析WebSocket消息为JSON: {}", message);
                return;
            }

            // 处理错误消息
            if(jsonMessage.containsKey("event") && "error".equals(jsonMessage.getString("event"))){
                String errorCode = jsonMessage.getString("code");
                String errorMsg = jsonMessage.getString("msg");

//                logger.error("收到WebSocket错误: code={}, msg={}", errorCode, errorMsg);

                // 处理特定错误
                switch(errorCode){
                    case "60004": // 时间戳错误
                        logger.info("时间戳错误，尝试重新连接私有频道");
                        // 立即尝试重新连接
                        if(privateWebSocket != null){
                            privateWebSocket.close(1000, "Reconnecting due to timestamp error");
                        }

                        // 使用短延迟重连
                        reconnectScheduler.schedule(this :: connectPrivateChannel, 1, TimeUnit.SECONDS);
                        break;
                    case "60012": // 非法请求
                    case "60018": // 非法请求
                    case "60013":
//                        logger.warn("非法请求错误: {}", errorMsg);
//                        break;
                    default:
//                        logger.warn("未处理的WebSocket错误: code={}, msg={}", errorCode, errorMsg);
                        break;
                }

                throw new OkxApiException(Integer.parseInt(errorCode), "WebSocket错误: " + errorMsg);
            }

            // 处理JSON格式的pong响应
            if(jsonMessage.containsKey("op") && "pong".equals(jsonMessage.getString("op"))){
                logger.debug("收到JSON格式pong响应: {}", message);
                return;
            }

            // 处理登录事件响应 - 处理 {"event":"login","msg":"","code":"0","connId":"b0a88f7d"} 格式
            if(jsonMessage.containsKey("event") && "login".equals(jsonMessage.getString("event"))){
                if(jsonMessage.containsKey("code") && "0".equals(jsonMessage.getString("code"))){
                    // 登录成功
                    logger.info("WebSocket登录成功(事件方式): connId={}", jsonMessage.getString("connId"));
                    privateConnected.set(true);
                    // 登录成功后恢复私有频道的订阅
                    restorePrivateOperations();
                }else{
                    // 登录失败
                    String code = jsonMessage.getString("code");
                    String msg = jsonMessage.getString("msg");
                    logger.error("WebSocket登录失败(事件方式): code={}, msg={}", code, msg);
                    privateConnected.set(false);

                    // 如果是时间戳错误，重新连接
                    if("60004".equals(code)){
                        logger.info("登录时间戳错误，重新连接");
                        schedulePrivateReconnect();
                    }
                }
                return;
            }

            // 处理频道连接计数 - 处理 {"event":"channel-conn-count","channel":"account","connCount":"1","connId":"b0a88f7d"} 格式
            if(jsonMessage.containsKey("event") && "channel-conn-count".equals(jsonMessage.getString("event"))){
                String channel = jsonMessage.getString("channel");
                String connCount = jsonMessage.getString("connCount");
                String connId = jsonMessage.getString("connId");
                logger.info("频道连接计数: channel={}, connCount={}, connId={}", channel, connCount, connId);
                return;
            }

            // 处理OP类型登录响应 - 处理 {"op":"login"} 格式
            if(jsonMessage.containsKey("op") && "login".equals(jsonMessage.getString("op"))){
                if(jsonMessage.containsKey("code") && ! "0".equals(jsonMessage.getString("code"))){
                    // 登录失败
                    logger.error("WebSocket登录失败(op方式): {}", message);
                    privateConnected.set(false);

                    // 如果是时间戳错误，重新连接
                    if("60004".equals(jsonMessage.getString("code"))){
                        logger.info("登录时间戳错误，重新连接");
                        schedulePrivateReconnect();
                    }
                }else{
                    // 登录成功
                    logger.info("WebSocket登录成功(op方式): {}", message);
                    privateConnected.set(true);
                    // 登录成功后恢复私有频道的订阅
                    restorePrivateOperations();
                }
                return;
            }

            // 根据消息类型路由到相应的处理器
            String topic = null;
            if(jsonMessage.containsKey("arg") && jsonMessage.getJSONObject("arg").containsKey("channel")){
                topic = jsonMessage.getJSONObject("arg").getString("channel");
            }

            if(jsonMessage.containsKey("op")){
                topic = jsonMessage.getString("op");
            }

            if(topic != null && messageHandlers.containsKey(topic)){
                messageHandlers.get(topic).accept(jsonMessage);
            }else{
                logger.debug("收到未处理的WebSocket消息: {}", message);
            }

        }catch(Exception e){
            logger.error("解析WebSocket消息失败: {}", message, e);
        }
    }

    /**
     * 注册消息处理器
     *
     * @param topic   订阅主题
     * @param handler 消息处理器
     */
    public void registerHandler(String topic, Consumer<JSONObject> handler){
        messageHandlers.put(topic, handler);
    }

    /**
     * 订阅公共频道主题
     *
     * @param topic  主题
     * @param symbol 交易对
     */
    public void subscribePublicTopic(String topic, String symbol){
        // 创建订阅消息
        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");

        JSONObject arg = new JSONObject();
        arg.put("channel", topic);
        arg.put("instId", symbol);

        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);

        // 将主题添加到已订阅集合中
        String key = topic + ":" + symbol;
        publicSubscribedTopics.add(key);
        
        // 检查连接状态，决定是立即发送还是加入待执行队列
        if (publicConnected.get() && publicWebSocket != null) {
            publicWebSocket.send(subscribeMessage.toJSONString());
            logger.info("订阅公共频道主题: {}, 交易对: {}", topic, symbol);
        } else {
            // 如果未连接，加入待执行队列
            PendingOperation operation = new PendingOperation(
                "公共频道订阅: " + key,
                () -> {
                    if (publicWebSocket != null) {
                        publicWebSocket.send(subscribeMessage.toJSONString());
                        logger.info("恢复订阅公共频道主题: {}, 交易对: {}", topic, symbol);
                    } else {
                        throw new OkxApiException("公共频道WebSocket未连接");
                    }
                }
            );
            publicPendingOperations.offer(operation);
            logger.info("添加待执行的公共频道订阅: {}, 交易对: {}", topic, symbol);
        }
    }

    /**
     * 订阅私有频道主题
     *
     * @param topic 主题
     */
    public void subscribePrivateTopic(String topic){
        // 创建订阅消息
        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");

        JSONObject arg = new JSONObject();
        arg.put("channel", topic);

        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);

        // 将主题添加到已订阅集合中
        privateSubscribedTopics.add(topic);
        
        // 检查连接状态，决定是立即发送还是加入待执行队列
        if (privateConnected.get() && privateWebSocket != null) {
            privateWebSocket.send(subscribeMessage.toJSONString());
            logger.info("订阅私有频道主题: {}", topic);
        } else {
            // 如果未连接，加入待执行队列
            PendingOperation operation = new PendingOperation(
                "私有频道订阅: " + topic,
                () -> {
                    if (privateWebSocket != null) {
                        privateWebSocket.send(subscribeMessage.toJSONString());
                        logger.info("恢复订阅私有频道主题: {}", topic);
                    } else {
                        throw new OkxApiException("私有频道WebSocket未连接");
                    }
                }
            );
            privatePendingOperations.offer(operation);
            logger.info("添加待执行的私有频道订阅: {}", topic);
        }
    }

    /**
     * 取消订阅公共频道主题
     *
     * @param topic  主题
     * @param symbol 交易对
     */
    public void unsubscribePublicTopic(String topic, String symbol){
        if(publicWebSocket == null){
            return;
        }

        JSONObject unsubscribeMessage = new JSONObject();
        unsubscribeMessage.put("op", "unsubscribe");

        JSONObject arg = new JSONObject();
        arg.put("channel", topic);
        arg.put("instId", symbol);

        JSONObject[] args = new JSONObject[]{arg};
        unsubscribeMessage.put("args", args);

        // 从已订阅集合中移除
        String key = topic + ":" + symbol;
        publicSubscribedTopics.remove(key);

        // 仅在连接可用时发送
        if (publicConnected.get()) {
            publicWebSocket.send(unsubscribeMessage.toJSONString());
            logger.info("取消订阅公共频道主题: {}, 交易对: {}", topic, symbol);
        }
    }

    /**
     * 取消订阅私有频道主题
     *
     * @param topic 主题
     */
    public void unsubscribePrivateTopic(String topic){
        if(privateWebSocket == null){
            return;
        }

        JSONObject unsubscribeMessage = new JSONObject();
        unsubscribeMessage.put("op", "unsubscribe");

        JSONObject arg = new JSONObject();
        arg.put("channel", topic);

        JSONObject[] args = new JSONObject[]{arg};
        unsubscribeMessage.put("args", args);

        // 从已订阅集合中移除
        privateSubscribedTopics.remove(topic);

        // 仅在连接可用时发送
        if (privateConnected.get()) {
            privateWebSocket.send(unsubscribeMessage.toJSONString());
            logger.info("取消订阅私有频道主题: {}", topic);
        }
    }

    /**
     * 发送私有请求消息
     *
     * @param message 请求消息
     */
    public void sendPrivateRequest(String message){
        if (privateConnected.get() && privateWebSocket != null) {
            privateWebSocket.send(message);
            logger.info("发送私有请求: {}", message);
        } else {
            // 如果未连接，加入待执行队列
            PendingOperation operation = new PendingOperation(
                "私有频道请求: " + message,
                () -> {
                    if (privateWebSocket != null) {
                        privateWebSocket.send(message);
                        logger.info("恢复发送私有请求: {}", message);
                    } else {
                        throw new OkxApiException("私有频道WebSocket未连接");
                    }
                }
            );
            privatePendingOperations.offer(operation);
            logger.info("添加待执行的私有频道请求");
        }
    }

    /**
     * 订阅公共频道主题（带自定义参数）
     *
     * @param arg 订阅参数对象
     */
    public void subscribePublicTopicWithArgs(JSONObject arg, String... symbols){
        // 创建订阅消息
        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");

        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);

        // 生成一个唯一标识
        String key = "custom:" + arg.toJSONString();
        publicSubscribedTopics.add(key);
        
        WebSocket targetSocket;
        ConcurrentLinkedQueue<PendingOperation> targetQueue;
        
        if(symbols != null && symbols.length > 0) {
            targetSocket = bussinessWebSocket;
            if (!bussinessConnected.get() || targetSocket == null) {
                // 如果未连接，加入待执行队列
                PendingOperation operation = new PendingOperation(
                    "业务频道自定义订阅: " + key,
                    () -> {
                        if (bussinessWebSocket != null) {
                            bussinessWebSocket.send(subscribeMessage.toJSONString());
                            logger.info("恢复订阅业务频道自定义主题，参数: {}", arg);
                        } else {
                            throw new OkxApiException("业务频道WebSocket未连接");
                        }
                    }
                );
                publicPendingOperations.offer(operation);
                logger.info("添加待执行的业务频道自定义订阅，参数: {}", arg);
                return;
            }
        } else {
            targetSocket = publicWebSocket;
            if (!publicConnected.get() || targetSocket == null) {
                // 如果未连接，加入待执行队列
                PendingOperation operation = new PendingOperation(
                    "公共频道自定义订阅: " + key,
                    () -> {
                        if (publicWebSocket != null) {
                            publicWebSocket.send(subscribeMessage.toJSONString());
                            logger.info("恢复订阅公共频道自定义主题，参数: {}", arg);
                        } else {
                            throw new OkxApiException("公共频道WebSocket未连接");
                        }
                    }
                );
                publicPendingOperations.offer(operation);
                logger.info("添加待执行的公共频道自定义订阅，参数: {}", arg);
                return;
            }
        }

        // 直接发送
        targetSocket.send(subscribeMessage.toJSONString());
        logger.info("订阅公共频道主题，参数: {}", arg);
    }

    /**
     * 取消订阅公共频道主题（带自定义参数）
     *
     * @param arg 取消订阅参数对象
     */
    public void unsubscribePublicTopicWithArgs(JSONObject arg, String... symbols){
        if(publicWebSocket == null || bussinessWebSocket == null){
            logger.warn("公共频道WebSocket未连接，无法取消订阅");
            return;
        }

        JSONObject unsubscribeMessage = new JSONObject();
        unsubscribeMessage.put("op", "unsubscribe");

        JSONObject[] args = new JSONObject[]{arg};
        unsubscribeMessage.put("args", args);
        
        // 从已订阅集合中移除
        String key = "custom:" + arg.toJSONString();
        publicSubscribedTopics.remove(key);
        
        WebSocket targetSocket;
        if(symbols != null && symbols.length > 0) {
            targetSocket = bussinessWebSocket;
            if (!bussinessConnected.get()) {
                return;
            }
        } else {
            targetSocket = publicWebSocket;
            if (!publicConnected.get()) {
                return;
            }
        }
        
        // 直接发送
        targetSocket.send(unsubscribeMessage.toJSONString());
        logger.info("取消订阅公共频道主题，参数: {}", arg);
    }
    
    /**
     * 恢复公共频道的操作
     */
    private void restorePublicOperations() {
        logger.info("开始恢复公共频道订阅，共 {} 个待执行操作", publicPendingOperations.size());
        
        // 如果两个连接都已经就绪，才开始恢复操作
        if (!publicConnected.get() || !bussinessConnected.get()) {
            logger.info("公共频道连接尚未就绪，等待所有连接建立后再恢复");
            return;
        }
        
        // 遍历并执行所有待执行的操作
        int count = 0;
        while (!publicPendingOperations.isEmpty()) {
            PendingOperation operation = publicPendingOperations.poll();
            if (operation != null) {
                try {
                    logger.info("恢复执行公共频道操作: {}", operation.getDescription());
                    operation.execute();
                    count++;
                } catch (Exception e) {
                    logger.error("恢复执行公共频道操作失败: {}", operation.getDescription(), e);
                    // 如果执行失败，重新添加到队列末尾
                    publicPendingOperations.offer(operation);
                }
            }
        }
        
        logger.info("公共频道操作恢复完成，成功执行 {} 个操作", count);
    }
    
    /**
     * 恢复私有频道的操作
     */
    private void restorePrivateOperations() {
        logger.info("开始恢复私有频道订阅，共 {} 个待执行操作", privatePendingOperations.size());
        
        if (!privateConnected.get()) {
            logger.info("私有频道连接尚未就绪，等待连接建立后再恢复");
            return;
        }
        
        // 遍历并执行所有待执行的操作
        int count = 0;
        while (!privatePendingOperations.isEmpty()) {
            PendingOperation operation = privatePendingOperations.poll();
            if (operation != null) {
                try {
                    logger.info("恢复执行私有频道操作: {}", operation.getDescription());
                    operation.execute();
                    count++;
                } catch (Exception e) {
                    logger.error("恢复执行私有频道操作失败: {}", operation.getDescription(), e);
                    // 如果执行失败，重新添加到队列末尾
                    privatePendingOperations.offer(operation);
                }
            }
        }
        
        logger.info("私有频道操作恢复完成，成功执行 {} 个操作", count);
    }

    /**
     * 检查私有WebSocket是否已连接
     *
     * @return 如果私有WebSocket已连接则返回true，否则返回false
     */
    public boolean isPrivateSocketConnected(){
        return privateWebSocket != null;
    }

    /**
     * 检查公共WebSocket是否已连接
     *
     * @return 如果公共WebSocket已连接则返回true，否则返回false
     */
    public boolean isPublicSocketConnected(){
        return publicWebSocket != null;
    }

    /**
     * 定时检查所有WebSocket连接并在需要时重新连接
     */
    private void checkConnectionsAndReconnect() {
        try {
            boolean publicReconnectNeeded = false;
            boolean privateReconnectNeeded = false;
            boolean bussinessReconnectNeeded = false;
            
            // 检查公共频道连接
            if (publicWebSocket == null || !isWebSocketConnected(publicWebSocket)) {
                logger.warn("公共频道连接检测失败，需要重连");
                publicReconnectNeeded = true;
            }
            
            // 检查业务频道连接
            if (bussinessWebSocket == null || !isWebSocketConnected(bussinessWebSocket)) {
                logger.warn("业务频道连接检测失败，需要重连");
                bussinessReconnectNeeded = true;
            }
            
            // 检查私有频道连接
            if (privateWebSocket == null || !isWebSocketConnected(privateWebSocket)) {
                logger.warn("私有频道连接检测失败，需要重连");
                privateReconnectNeeded = true;
            }
            
            // 执行重连
            if (publicReconnectNeeded) {
                if (publicWebSocket != null) {
                    try {
                        publicWebSocket.close(1000, "Reconnecting due to connection check");
                    } catch (Exception e) {
                        logger.debug("关闭旧公共频道连接失败", e);
                    }
                }
                connectPublicChannel();
            }
            
            if (bussinessReconnectNeeded) {
                if (bussinessWebSocket != null) {
                    try {
                        bussinessWebSocket.close(1000, "Reconnecting due to connection check");
                    } catch (Exception e) {
                        logger.debug("关闭旧业务频道连接失败", e);
                    }
                }
                connectBussinessChannel();
            }
            
            if (privateReconnectNeeded) {
                if (privateWebSocket != null) {
                    try {
                        privateWebSocket.close(1000, "Reconnecting due to connection check");
                    } catch (Exception e) {
                        logger.debug("关闭旧私有频道连接失败", e);
                    }
                }
                connectPrivateChannel();
            }
        } catch (Exception e) {
            logger.error("检查WebSocket连接状态时出错", e);
        }
    }
    
    /**
     * 检查WebSocket连接是否有效
     * 
     * @param webSocket 要检查的WebSocket连接
     * @return 连接是否有效
     */
    private boolean isWebSocketConnected(WebSocket webSocket) {
        if (webSocket == null) {
            return false;
        }
        
        try {
            // 发送ping消息测试连接
            return webSocket.send("ping");
        } catch (Exception e) {
            logger.debug("检查WebSocket连接状态失败", e);
            return false;
        }
    }

    /**
     * 待执行操作类
     */
    private static class PendingOperation {
        private final String description;
        private final Runnable operation;
        
        public PendingOperation(String description, Runnable operation) {
            this.description = description;
            this.operation = operation;
        }
        
        public void execute() {
            operation.run();
        }
        
        public String getDescription() {
            return description;
        }
    }
}
