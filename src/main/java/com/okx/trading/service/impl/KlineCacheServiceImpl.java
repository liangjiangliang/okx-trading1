package com.okx.trading.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okx.trading.event.KlineSubscriptionEvent;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.service.KlineCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * K线缓存服务实现类
 * 负责K线数据的缓存管理，订阅与取消订阅功能。
 * 使用Redis存储K线数据，并通过内存维护当前订阅状态。
 */
@Service
public class KlineCacheServiceImpl implements KlineCacheService{

    private static final Logger log = LoggerFactory.getLogger(KlineCacheServiceImpl.class);

    private static final String KLINE_CACHE_KEY_PREFIX = "kline:data:";
    private static final String KLINE_SUBSCRIPTION_KEY = "kline:subscriptions";
    private static final Duration KLINE_CACHE_DURATION = Duration.ofHours(24);

    // 默认时间间隔
    private static final String[] DEFAULT_INTERVALS = {"1m", "5m", "15m", "1H", "4H", "1D"};

    // 默认交易对
    private static final String[] DEFAULT_SYMBOLS = {"BTC-USDT", "ETH-USDT", "SOL-USDT"};

    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // 内存中维护的当前订阅状态，避免频繁读取Redis
    private final Map<String,Set<String>> subscriptionMap = new ConcurrentHashMap<>();

    @Autowired
    public KlineCacheServiceImpl(RedisTemplate<String,String> redisTemplate,
                                 ObjectMapper objectMapper,
                                 ApplicationEventPublisher eventPublisher){
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;

        // 初始化时同步一次Redis中的订阅数据到内存
        updateSubscriptionsFromRedis();
    }

    /**
     * 将Redis中的订阅信息同步到内存
     */
    private void updateSubscriptionsFromRedis(){
        try{
            Map<String,Set<String>> redisSubscriptions = getAllSubscriptions();
            if(! redisSubscriptions.isEmpty()){
                // 更新内存中的订阅状态
                subscriptionMap.clear();
                subscriptionMap.putAll(redisSubscriptions);
                log.info("从Redis加载了{}个交易对的订阅信息", redisSubscriptions.size());
            }
        }catch(Exception e){
            log.error("从Redis同步订阅信息失败", e);
        }
    }

    @Override
    public boolean subscribeKline(String symbol, String interval){
        try{
            if(symbol == null || interval == null){
                log.warn("订阅K线数据失败: 交易对或时间间隔为空");
                return false;
            }

            String key = generateSubscriptionKey(symbol, interval);

            // 添加到内存缓存
            subscriptionMap.computeIfAbsent(symbol, k -> new HashSet<>()).add(interval);

            // 添加到Redis缓存
            redisTemplate.opsForSet().add(KLINE_SUBSCRIPTION_KEY, key);

            // 发布订阅事件
            eventPublisher.publishEvent(new KlineSubscriptionEvent(
                this,
                symbol,
                interval,
                KlineSubscriptionEvent.EventType.SUBSCRIBE
            ));

            log.info("已订阅K线数据: {} {}", symbol, interval);
            return true;
        }catch(Exception e){
            log.error("订阅K线数据失败: {} {}, 错误: {}", symbol, interval, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> batchSubscribeKline(String symbol, List<String> intervals){
        if(symbol == null || CollectionUtils.isEmpty(intervals)){
            return Collections.emptyList();
        }

        List<String> successList = new ArrayList<>();
        for(String interval: intervals){
            if(subscribeKline(symbol, interval)){
                successList.add(interval);
            }
        }

        return successList;
    }

    @Override
    public boolean unsubscribeKline(String symbol, String interval){
        try{
            if(symbol == null || interval == null){
                log.warn("取消订阅K线数据失败: 交易对或时间间隔为空");
                return false;
            }

            String key = generateSubscriptionKey(symbol, interval);

            // 从内存缓存移除
            Set<String> intervals = subscriptionMap.get(symbol);
            if(intervals != null){
                intervals.remove(interval);
                if(intervals.isEmpty()){
                    subscriptionMap.remove(symbol);
                }
            }

            // 从Redis缓存移除
            redisTemplate.opsForSet().remove(KLINE_SUBSCRIPTION_KEY, key);

            // 发布取消订阅事件
            eventPublisher.publishEvent(new KlineSubscriptionEvent(
                this,
                symbol,
                interval,
                KlineSubscriptionEvent.EventType.UNSUBSCRIBE
            ));

            log.info("已取消订阅K线数据: {} {}", symbol, interval);
            return true;
        }catch(Exception e){
            log.error("取消订阅K线数据失败: {} {}, 错误: {}", symbol, interval, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> batchUnsubscribeKline(String symbol, List<String> intervals){
        if(symbol == null || CollectionUtils.isEmpty(intervals)){
            return Collections.emptyList();
        }

        List<String> successList = new ArrayList<>();
        for(String interval: intervals){
            if(unsubscribeKline(symbol, interval)){
                successList.add(interval);
            }
        }

        return successList;
    }

    @Override
    public boolean cacheKlineData(Candlestick candlestick){
        if(candlestick == null){
            return false;
        }

        try{
            String symbol = candlestick.getSymbol();
            String interval = candlestick.getIntervalVal();

            if(symbol == null || interval == null){
                log.warn("缓存K线数据失败: 交易对或时间间隔为空");
                return false;
            }

            String cacheKey = generateCacheKey(symbol, interval);

            // 获取当前缓存的K线数据
            String cachedDataJson = redisTemplate.opsForValue().get(cacheKey);
            List<Candlestick> cachedKlineData = new ArrayList<>();

            if(cachedDataJson != null && ! cachedDataJson.isEmpty()){
                cachedKlineData = objectMapper.readValue(cachedDataJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Candlestick.class));

                // 查找是否存在相同时间的K线数据
                boolean updated = false;
                LocalDateTime candlestickOpenTime = candlestick.getOpenTime();

                for(int i = 0;i < cachedKlineData.size();i++){
                    Candlestick existingCandle = cachedKlineData.get(i);
                    LocalDateTime existingOpenTime = existingCandle.getOpenTime();
                    if(existingOpenTime != null && candlestickOpenTime != null &&
                        existingOpenTime.equals(candlestickOpenTime)){
                        // 替换已存在的K线数据
                        cachedKlineData.set(i, candlestick);
                        updated = true;
                        break;
                    }
                }

                // 如果不存在相同时间的K线数据，则添加到列表中
                if(! updated){
                    cachedKlineData.add(candlestick);

                    // 按时间排序
                    cachedKlineData.sort((c1, c2) -> {
                        LocalDateTime time1 = c1.getOpenTime();
                        LocalDateTime time2 = c2.getOpenTime();
                        if(time1 == null && time2 == null) return 0;
                        if(time1 == null) return - 1;
                        if(time2 == null) return 1;
                        return time1.compareTo(time2);
                    });
                }
            }else{
                // 如果缓存中没有数据，则直接添加
                cachedKlineData.add(candlestick);
            }

            // 将更新后的K线数据列表转为JSON字符串并保存到缓存
            String updatedKlineDataJson = objectMapper.writeValueAsString(cachedKlineData);
            redisTemplate.opsForValue().set(cacheKey, updatedKlineDataJson, KLINE_CACHE_DURATION);

            log.info("已缓存单条K线数据: {} {}, 时间: {}", symbol, interval, candlestick.getOpenTime());
            return true;
        }catch(JsonProcessingException e){
            log.error("缓存K线数据失败 - JSON序列化/反序列化错误: {}, 错误: {}",
                candlestick.getSymbol(), e.getMessage(), e);
            return false;
        }catch(Exception e){
            log.error("缓存K线数据失败: {}, 错误: {}",
                candlestick.getSymbol(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int batchCacheKlineData(List<Candlestick> candlesticks){
        if(CollectionUtils.isEmpty(candlesticks)){
            return 0;
        }

        int successCount = 0;

        // 按照交易对和时间间隔分组
        Map<String,Map<String,List<Candlestick>>> groupedData = new HashMap<>();

        for(Candlestick candlestick: candlesticks){
            if(candlestick == null) continue;

            String symbol = candlestick.getSymbol();
            String interval = candlestick.getIntervalVal();

            if(symbol == null || interval == null) continue;

            groupedData
                .computeIfAbsent(symbol, k -> new HashMap<>())
                .computeIfAbsent(interval, k -> new ArrayList<>())
                .add(candlestick);
        }

        // 分组缓存
        for(Map.Entry<String,Map<String,List<Candlestick>>> symbolEntry: groupedData.entrySet()){
            String symbol = symbolEntry.getKey();
            Map<String,List<Candlestick>> intervalMap = symbolEntry.getValue();

            for(Map.Entry<String,List<Candlestick>> intervalEntry: intervalMap.entrySet()){
                String interval = intervalEntry.getKey();
                List<Candlestick> data = intervalEntry.getValue();

                try{
                    String cacheKey = generateCacheKey(symbol, interval);

                    // 获取当前缓存的K线数据
                    String cachedDataJson = redisTemplate.opsForValue().get(cacheKey);
                    List<Candlestick> cachedKlineData = new ArrayList<>();

                    if(cachedDataJson != null && ! cachedDataJson.isEmpty()){
                        cachedKlineData = objectMapper.readValue(cachedDataJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Candlestick.class));
                    }

                    // 创建一个Map用于快速查找已有的K线数据（按开盘时间索引）
                    Map<LocalDateTime,Integer> existingDataMap = new HashMap<>();
                    for(int i = 0;i < cachedKlineData.size();i++){
                        LocalDateTime openTime = cachedKlineData.get(i).getOpenTime();
                        if(openTime != null){
                            existingDataMap.put(openTime, i);
                        }
                    }

                    // 处理新的K线数据
                    for(Candlestick kline: data){
                        LocalDateTime openTime = kline.getOpenTime();
                        if(openTime == null) continue;

                        Integer existingIndex = existingDataMap.get(openTime);

                        if(existingIndex != null){
                            // 更新已存在的K线数据
                            cachedKlineData.set(existingIndex, kline);
                        }else{
                            // 添加新的K线数据
                            cachedKlineData.add(kline);
                        }

                        successCount++;
                    }

                    // 按时间排序
                    cachedKlineData.sort((c1, c2) -> {
                        LocalDateTime time1 = c1.getOpenTime();
                        LocalDateTime time2 = c2.getOpenTime();
                        if(time1 == null && time2 == null) return 0;
                        if(time1 == null) return - 1;
                        if(time2 == null) return 1;
                        return time1.compareTo(time2);
                    });

                    // 将更新后的K线数据列表转为JSON字符串并保存到缓存
                    String updatedKlineDataJson = objectMapper.writeValueAsString(cachedKlineData);
                    redisTemplate.opsForValue().set(cacheKey, updatedKlineDataJson, KLINE_CACHE_DURATION);

                    log.debug("已批量缓存K线数据: {} {}, 数据条数: {}", symbol, interval, data.size());
                }catch(Exception e){
                    log.error("批量缓存K线数据失败: {} {}, 错误: {}", symbol, interval, e.getMessage(), e);
                }
            }
        }

        return successCount;
    }

    @Override
    public List<CandlestickEntity> getLatestKlineData(String symbol, String interval, int limit){
        List<CandlestickEntity> allData = getKlineData(symbol, interval,limit);

        if(CollectionUtils.isEmpty(allData)){
            return Collections.emptyList();
        }

        // 按时间倒序排序
        allData.sort((c1, c2) -> {
            LocalDateTime time1 = c1.getOpenTime();
            LocalDateTime time2 = c2.getOpenTime();
            if(time1 == null && time2 == null) return 0;
            if(time1 == null) return 1; // null 放在后面
            if(time2 == null) return - 1;
            return time2.compareTo(time1); // 降序
        });

        // 返回指定数量的最新数据
        return allData.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<CandlestickEntity> getHistoricalKlineData(String symbol, String interval, Long startTime, Long endTime, Integer limit){
        List<CandlestickEntity> allData = getKlineData(symbol, interval,limit);

        if(CollectionUtils.isEmpty(allData)){
            return Collections.emptyList();
        }

        // 筛选时间范围内的数据
        List<CandlestickEntity> filteredData = allData.stream()
            .filter(kline -> {
                LocalDateTime openTime = kline.getOpenTime();
                if(openTime == null) return false;

                // 将LocalDateTime转换为long进行比较
                long openTimeMillis = openTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return (startTime == null || openTimeMillis >= startTime) &&
                    (endTime == null || openTimeMillis <= endTime);
            })
            .collect(Collectors.toList());

        // 按时间升序排序
        filteredData.sort((c1, c2) -> {
            LocalDateTime time1 = c1.getOpenTime();
            LocalDateTime time2 = c2.getOpenTime();
            if(time1 == null && time2 == null) return 0;
            if(time1 == null) return - 1;
            if(time2 == null) return 1;
            return time1.compareTo(time2);
        });

        // 如果指定了limit，则返回指定数量的数据
        if(limit != null && limit > 0 && filteredData.size() > limit){
            return filteredData.subList(0, limit);
        }

        return filteredData;
    }

    @Override
    public boolean isKlineSubscribed(String symbol, String interval){
        if(symbol == null || interval == null){
            return false;
        }

        // 优先从内存中检查
        Set<String> intervals = subscriptionMap.get(symbol);
        if(intervals != null && intervals.contains(interval)){
            return true;
        }

        // 从Redis检查（防止服务重启后内存状态丢失）
        String key = generateSubscriptionKey(symbol, interval);
        Boolean isMember = redisTemplate.opsForSet().isMember(KLINE_SUBSCRIPTION_KEY, key);

        // 如果Redis中存在但内存中不存在，则更新内存
        if(Boolean.TRUE.equals(isMember) && (intervals == null || ! intervals.contains(interval))){
            subscriptionMap.computeIfAbsent(symbol, k -> new HashSet<>()).add(interval);
        }

        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public Map<String,List<String>> getAllSubscribedKlines(){
        Map<String,Set<String>> subscriptions = getAllSubscriptions();

        // 转换Set为List
        Map<String,List<String>> result = new HashMap<>();
        for(Map.Entry<String,Set<String>> entry: subscriptions.entrySet()){
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return result;
    }

    @Override
    public List<String> getSubscribedIntervals(String symbol){
        try{
            if(symbol == null){
                return Collections.emptyList();
            }

            // 优先从内存中获取
            Set<String> intervals = subscriptionMap.get(symbol);
            if(intervals != null){
                return new ArrayList<>(intervals);
            }

            // 如果内存中没有，则从Redis获取完整订阅信息
            Map<String,List<String>> allSubscriptions = getAllSubscribedKlines();
            return allSubscriptions.getOrDefault(symbol, Collections.emptyList());
        }catch(Exception e){
            log.error("获取交易对 {} 订阅的K线间隔失败: {}", symbol, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getAllSubscribedSymbols(){
        return getAllSubscriptions().keySet();
    }

    @Override
    public Map<String,Set<String>> getAllSubscribedSymbolsWithIntervals(){
        return getAllSubscriptions();
    }

    @Override
    public boolean clearKlineCache(String symbol, String interval){
        try{
            if(symbol == null || interval == null){
                log.warn("清除K线缓存失败: 交易对或时间间隔为空");
                return false;
            }

            String cacheKey = generateCacheKey(symbol, interval);
            Boolean deleted = redisTemplate.delete(cacheKey);

            if(Boolean.TRUE.equals(deleted)){
                log.info("已清除K线缓存: {} {}", symbol, interval);
                return true;
            }else{
                log.warn("清除K线缓存失败或缓存不存在: {} {}", symbol, interval);
                return false;
            }
        }catch(Exception e){
            log.error("清除K线缓存失败: {} {}, 错误: {}", symbol, interval, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void initDefaultKlineSubscriptions(){
        log.info("初始化默认K线订阅");

        // 默认订阅的交易对和时间间隔
        Set<String> members = redisTemplate.opsForSet().members(KLINE_SUBSCRIPTION_KEY);
        for(String member: members){
            String[] split = member.split(":");
            String symbol = split[0];
            String interval = split[1];
            batchSubscribeKline(symbol, Collections.singletonList(interval));
        }

//        // 批量订阅
//        for (String symbol : defaultSymbols) {
//            List<String> successIntervals = batchSubscribeKline(symbol, defaultIntervals);
//            log.info("为交易对 {} 订阅K线间隔: {}", symbol, successIntervals);
//        }
    }

    @Override
    /**
     * 获取K线数据的辅助方法（内部使用）
     */
    public List<CandlestickEntity> getKlineData(String symbol, String interval, int klineLimit){
        try{
            if(symbol == null || interval == null){
                return Collections.emptyList();
            }

            String cacheKey = generateCacheKey(symbol, interval);

            // 从Redis缓存获取K线数据
            String cachedDataJson = redisTemplate.opsForValue().get(cacheKey);

            if(cachedDataJson == null || cachedDataJson.isEmpty()){
                return Collections.emptyList();
            }

            // 将JSON字符串转为K线数据列表
            List<CandlestickEntity> candlesticks = objectMapper.readValue(cachedDataJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CandlestickEntity.class));
            if(candlesticks.size() < klineLimit){
                throw new IllegalStateException("k线数量不足阈值 " + klineLimit);
            }
            return candlesticks;
        }catch(JsonProcessingException e){
            log.error("获取K线数据失败 - JSON反序列化错误: {} {}, 错误: {}", symbol, interval, e.getMessage(), e);
            return Collections.emptyList();
        }catch(Exception e){
            log.error("获取K线数据失败: {} {}, 错误: {}", symbol, interval, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 从Redis获取所有订阅信息
     */
    private Map<String,Set<String>> getAllSubscriptions(){
        Map<String,Set<String>> result = new HashMap<>();

        // 从Redis中获取所有订阅
        Set<String> subscriptionKeys = redisTemplate.opsForSet().members(KLINE_SUBSCRIPTION_KEY);

        if(subscriptionKeys != null){
            for(String key: subscriptionKeys){
                String[] parts = key.split(":");
                if(parts.length == 2){
                    String symbol = parts[0];
                    String interval = parts[1];
                    result.computeIfAbsent(symbol, k -> new HashSet<>()).add(interval);
                }
            }

            // 更新内存中的订阅状态
            subscriptionMap.clear();
            subscriptionMap.putAll(result);
        }

        return result;
    }

    /**
     * 生成Redis缓存Key
     */
    private String generateCacheKey(String symbol, String interval){
        return KLINE_CACHE_KEY_PREFIX + symbol + ":" + interval;
    }

    /**
     * 生成订阅标识Key
     */
    private String generateSubscriptionKey(String symbol, String interval){
        return symbol + ":" + interval;
    }
}
