# WebSocket重连后K线重新订阅解决方案

## 问题描述

用户反映WebSocket断联重启后，之前订阅的K线数据没有重新订阅，导致数据流中断。

## 问题分析

通过代码分析发现以下问题：

1. **缺少重连事件监听器**：WebSocket重连成功后发布了重连事件，但没有监听器处理这些事件
2. **业务频道重连逻辑不完整**：业务频道重连后没有恢复操作和事件发布
3. **应用启动时没有恢复订阅**：程序重启后没有自动恢复之前的K线订阅
4. **订阅状态管理**：虽然订阅状态保存在Redis中，但重连后没有利用这些信息

## 解决方案

### 1. 创建WebSocket重连事件监听器

**文件**：`src/main/java/com/okx/trading/listener/WebSocketReconnectEventListener.java`

**功能**：
- 监听WebSocket重连事件
- 公共频道重连后自动重新订阅所有K线数据
- 支持异步批量重新订阅
- 提供详细的订阅成功/失败统计

**关键特性**：
- 使用`@Async`异步处理，避免阻塞主线程
- 延迟3秒等待连接稳定后再开始重新订阅
- 批量处理所有缓存的订阅记录
- 添加适当延迟避免API频率限制

### 2. 完善业务频道重连逻辑

**更新文件**：`src/main/java/com/okx/trading/util/WebSocketUtil.java`

**改进内容**：
- 添加`restoreBusinessOperations()`方法
- 业务频道重连成功后发布重连事件
- 在`WebSocketReconnectEvent`中添加`BUSINESS`重连类型

### 3. 应用启动时恢复订阅

**更新文件**：`src/main/java/com/okx/trading/component/KlineDataInitializer.java`

**功能**：
- 应用启动时延迟5秒等待WebSocket连接建立
- 自动从Redis缓存中读取之前的订阅记录
- 重新订阅所有之前订阅的K线数据
- 提供详细的恢复统计信息

### 4. 订阅状态持久化

**利用现有机制**：`KlineCacheServiceImpl`

K线订阅状态已经通过以下方式持久化到Redis：
- 订阅成功时：`redisTemplate.opsForSet().add(KLINE_SUBSCRIPTION_KEY, subscriptionKey)`
- 取消订阅时：`redisTemplate.opsForSet().remove(KLINE_SUBSCRIPTION_KEY, subscriptionKey)`
- 获取所有订阅：`getAllSubscribedKlines()`方法

## 实现流程

### WebSocket重连场景

```
WebSocket断开 → 自动重连 → 连接成功 → 发布重连事件 → 事件监听器触发 → 重新订阅K线数据
```

### 应用重启场景

```
应用启动 → KlineDataInitializer执行 → 延迟等待WebSocket连接 → 从Redis读取订阅记录 → 恢复所有订阅
```

## 关键代码逻辑

### 重新订阅核心逻辑

```java
// 从缓存获取所有订阅
Map<String, List<String>> allSubscribedKlines = klineCacheService.getAllSubscribedKlines();

// 遍历重新订阅
for (Map.Entry<String, List<String>> entry : allSubscribedKlines.entrySet()) {
    String symbol = entry.getKey();
    List<String> intervals = entry.getValue();
    
    for (String interval : intervals) {
        boolean success = okxApiService.subscribeKlineData(symbol, interval);
        // 处理结果...
    }
}
```

### 事件监听处理

```java
@EventListener
@Async
public void handleWebSocketReconnect(WebSocketReconnectEvent event) {
    if (event.getType() == WebSocketReconnectEvent.ReconnectType.PUBLIC) {
        Thread.sleep(3000); // 等待连接稳定
        resubscribeAllKlineData(); // 重新订阅
    }
}
```

## 预期效果

1. **WebSocket重连后**：自动恢复所有之前的K线订阅，无需手动操作
2. **应用重启后**：自动恢复所有持久化的订阅状态
3. **容错能力增强**：即使部分订阅失败，也会继续处理其他订阅
4. **监控友好**：提供详细的日志记录，便于问题排查

## 日志输出示例

### 重连后重新订阅

```
2025-06-26 21:35:12.123 INFO  WebSocketReconnectEventListener - 收到WebSocket重连事件，重连类型: PUBLIC
2025-06-26 21:35:15.456 INFO  WebSocketReconnectEventListener - 开始重新订阅WebSocket重连前的K线数据(公共频道重连)...
2025-06-26 21:35:15.789 INFO  WebSocketReconnectEventListener - 发现 3 个交易对的K线订阅，开始重新订阅...
2025-06-26 21:35:18.123 INFO  WebSocketReconnectEventListener - K线数据重新订阅完成: 总计 12 个，成功 12 个，失败 0 个
```

### 应用启动时恢复

```
2025-06-26 21:30:01.000 INFO  KlineDataInitializer - K线数据初始化开始...
2025-06-26 21:30:06.000 INFO  KlineDataInitializer - 发现 3 个交易对的K线订阅记录，开始恢复订阅...
2025-06-26 21:30:08.500 INFO  KlineDataInitializer - K线订阅恢复完成: 总计 12 个，成功 12 个，失败 0 个
2025-06-26 21:30:08.501 INFO  KlineDataInitializer - K线数据初始化完成
```

## 注意事项

1. **异步执行**：确保AsyncConfig正确配置，启用异步支持
2. **连接稳定性**：重新订阅前等待连接稳定，避免立即重连失败
3. **频率限制**：添加适当延迟避免触发API频率限制
4. **错误处理**：单个订阅失败不影响其他订阅的处理
5. **资源使用**：使用`@Lazy`注解避免循环依赖

## 测试验证

可以通过以下方式验证解决方案：

1. **手动断开WebSocket连接**：观察重连后是否自动恢复订阅
2. **重启应用程序**：检查是否自动恢复之前的订阅状态
3. **查看日志输出**：确认重新订阅的成功率和详细信息
4. **监控K线数据流**：验证数据是否正常接收 