package com.okx.trading.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.okx.trading.config.OkxApiConfig;
import com.okx.trading.exception.OkxApiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
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
public class WebSocketUtil{

    private final OkxApiConfig okxApiConfig;
    private final OkHttpClient okHttpClient;

    private WebSocket publicWebSocket;
    private WebSocket bussinessWebSocket;
    private WebSocket privateWebSocket;

    private final Map<String,Consumer<JSONObject>> messageHandlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

    // 添加静态Logger以解决编译问题
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketUtil.class);

    @Autowired
    public WebSocketUtil(OkxApiConfig okxApiConfig, OkHttpClient okHttpClient){
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
                pingScheduler.scheduleAtFixedRate(this :: pingWebSockets, 20, 20, TimeUnit.SECONDS);
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
                    logger.info("公共频道WebSocket连接成功");
                }

                @Override
                public void onMessage(WebSocket webSocket, String text){
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response){
                    logger.error("公共频道WebSocket连接失败", t);
                    // 尝试重连
                    schedulePublicReconnect();
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason){
                    logger.info("公共频道WebSocket连接关闭: {}, {}", code, reason);
                    // 如果不是应用主动关闭，尝试重连
                    if(code != 1000){
                        schedulePublicReconnect();
                    }
                }
            });
        }catch(Exception e){
            logger.error("创建公共频道WebSocket连接失败", e);
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
                }

                @Override
                public void onMessage(WebSocket webSocket, String text){
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response){
                    logger.error("公共频道WebSocket连接失败", t);
                    // 尝试重连
                    schedulePublicReconnect();
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason){
                    logger.info("公共频道WebSocket连接关闭: {}, {}", code, reason);
                    // 如果不是应用主动关闭，尝试重连
                    if(code != 1000){
                        schedulePublicReconnect();
                    }
                }
            });
        }catch(Exception e){
            logger.error("创建公共频道WebSocket连接失败", e);
            schedulePublicReconnect();
        }
    }

    /**
     * 安排公共频道重连
     */
    private void schedulePublicReconnect(){
        reconnectScheduler.schedule(() -> {
            try{
                if(publicWebSocket != null){
                    publicWebSocket.close(1000, "Reconnecting");
                }
                connectPublicChannel();

                if(bussinessWebSocket != null){
                    bussinessWebSocket.close(1000, "Reconnecting");
                }
                connectBussinessChannel();
            }catch(Exception e){
                logger.error("重连公共频道失败", e);
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
                    // 发送登录消息
                    sendLoginMessage(webSocket, timestamp, sign);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text){
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response){
                    logger.error("私有频道WebSocket连接失败", t);
                    // 尝试重连
                    schedulePrivateReconnect();
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason){
                    logger.info("私有频道WebSocket连接关闭: {}, {}", code, reason);
                    // 如果不是应用主动关闭，尝试重连
                    if(code != 1000){
                        schedulePrivateReconnect();
                    }
                }
            });
        }catch(Exception e){
            logger.error("连接私有频道失败", e);
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
        reconnectScheduler.schedule(() -> {
            try{
                if(privateWebSocket != null){
                    privateWebSocket.close(1000, "Reconnecting");
                }
                connectPrivateChannel();
            }catch(Exception e){
                logger.error("重连私有频道失败", e);
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 发送ping消息，保持连接活跃
     */
    private void pingWebSockets(){
        try{
            // OKX WebSocket要求ping包含id字段
            String timestamp = String.valueOf(System.currentTimeMillis());

            if(publicWebSocket != null){
                // 简单的ping字符串格式
                publicWebSocket.send("ping");
                logger.debug("发送公共频道ping消息");
            }

            if(privateWebSocket != null){
                // 简单的ping字符串格式
                privateWebSocket.send("ping");
                logger.debug("发送私有频道ping消息");
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

                logger.error("收到WebSocket错误: code={}, msg={}", errorCode, errorMsg);

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
                        logger.warn("非法请求错误: {}", errorMsg);
                        break;
                    default:
                        logger.warn("未处理的WebSocket错误: code={}, msg={}", errorCode, errorMsg);
                        break;
                }
                return;
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
                }else{
                    // 登录失败
                    String code = jsonMessage.getString("code");
                    String msg = jsonMessage.getString("msg");
                    logger.error("WebSocket登录失败(事件方式): code={}, msg={}", code, msg);

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

                    // 如果是时间戳错误，重新连接
                    if("60004".equals(jsonMessage.getString("code"))){
                        logger.info("登录时间戳错误，重新连接");
                        schedulePrivateReconnect();
                    }
                }else{
                    // 登录成功
                    logger.info("WebSocket登录成功(op方式): {}", message);
                    // 可以在这里订阅私有频道数据
                }
                return;
            }

            // 根据消息类型路由到相应的处理器
            String topic = null;
            if(jsonMessage.containsKey("arg") && jsonMessage.getJSONObject("arg").containsKey("channel")){
                topic = jsonMessage.getJSONObject("arg").getString("channel");
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
        if(publicWebSocket == null){
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
        logger.info("订阅公共频道主题: {}, 交易对: {}", topic, symbol);
    }

    /**
     * 订阅私有频道主题
     *
     * @param topic 主题
     */
    public void subscribePrivateTopic(String topic){
        if(privateWebSocket == null){
            throw new OkxApiException("私有频道WebSocket未连接");
        }

        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");

        JSONObject arg = new JSONObject();
        arg.put("channel", topic);

        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);

        privateWebSocket.send(subscribeMessage.toJSONString());
        logger.info("订阅私有频道主题: {}", topic);
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

        publicWebSocket.send(unsubscribeMessage.toJSONString());
        logger.info("取消订阅公共频道主题: {}, 交易对: {}", topic, symbol);
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

        privateWebSocket.send(unsubscribeMessage.toJSONString());
        logger.info("取消订阅私有频道主题: {}", topic);
    }

    /**
     * 发送私有请求消息
     *
     * @param message 请求消息
     */
    public void sendPrivateRequest(String message){
        if(privateWebSocket == null){
            throw new OkxApiException("私有频道WebSocket未连接");
        }

        privateWebSocket.send(message);
        logger.info("发送私有请求: {}", message);
    }

    /**
     * 订阅公共频道主题（带自定义参数）
     *
     * @param arg 订阅参数对象
     */
    public void subscribePublicTopicWithArgs(JSONObject arg, String... symbols){
        if(publicWebSocket == null || bussinessWebSocket == null){
            throw new OkxApiException("公共频道WebSocket未连接");
        }

        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("op", "subscribe");

        JSONObject[] args = new JSONObject[]{arg};
        subscribeMessage.put("args", args);
        if(symbols != null && symbols.length > 0){
            bussinessWebSocket.send(subscribeMessage.toJSONString());
        }else{
            publicWebSocket.send(subscribeMessage.toJSONString());
        }


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
        if(symbols != null && symbols.length > 0){
            bussinessWebSocket.send(unsubscribeMessage.toJSONString());
        }else{
            publicWebSocket.send(unsubscribeMessage.toJSONString());
        }
        logger.info("取消订阅公共频道主题，参数: {}", arg);
    }
}
