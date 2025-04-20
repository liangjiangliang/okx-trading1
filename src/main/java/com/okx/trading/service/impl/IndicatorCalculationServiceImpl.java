package com.okx.trading.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.service.IndicatorCalculationService;
import com.okx.trading.util.TechnicalIndicatorUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 指标计算服务实现类
 * 负责实时计算K线数据的技术指标，并存储到Redis中
 */
@Service
@RequiredArgsConstructor
public class IndicatorCalculationServiceImpl implements IndicatorCalculationService, CommandLineRunner{

    private static final Logger logger = LoggerFactory.getLogger(IndicatorCalculationServiceImpl.class);

    private final RedisTemplate<String,Object> redisTemplate;

    // 指标计算参数常量
    private static final int DEFAULT_SCALE = 8;
    private static final int MACD_FAST_PERIOD = 12;
    private static final int MACD_SLOW_PERIOD = 26;
    private static final int MACD_SIGNAL_PERIOD = 9;
    private static final int RSI_DEFAULT_PERIOD = 14;
    private static final int BOLL_DEFAULT_PERIOD = 20;
    private static final double BOLL_DEFAULT_MULTIPLIER = 2.0;
    private static final int KDJ_DEFAULT_PERIOD = 9;

    // Redis键前缀
    private static final String SOURCE_KLINE_PREFIX = "coin-rt-kline:";
    private static final String TARGET_INDICATOR_PREFIX = "coin-rt-indicator:";
    private static final String INDICATOR_SUBSCRIPTION_KEY = "kline:subscriptions";

    // 线程池
    @Qualifier("indicatorCalculateScheduler")
    @Autowired
    private ScheduledExecutorService scheduler;
    private RedisMessageListenerContainer listenerContainer;

    // 订阅映射表，key为交易对，value为时间间隔集合
    private final Map<String,Set<String>> subscriptionMap = new ConcurrentHashMap<>();

//    @PostConstruct
//    public void init(){
//        scheduler = Executors.newScheduledThreadPool(2);
//        logger.info("指标计算服务初始化完成");
//    }

    @PreDestroy
    public void cleanup(){
        stopService();
    }

    @Override
    public void run(String... args){
        startService();
    }

    @Override
    public void startService(){
        try{

            // 设置定期任务，定期检查新订阅
            scheduler.scheduleAtFixedRate(
                this :: checkNewSubscriptions,
                0,
                1,
                TimeUnit.SECONDS
            );
        }catch(Exception e){
            logger.error("启动指标计算服务失败", e);
        }
    }

    @Override
    public void stopService(){
        logger.info("停止指标计算服务...");
        if(scheduler != null && ! scheduler.isShutdown()){
            scheduler.shutdown();
            try{
                if(! scheduler.awaitTermination(5, TimeUnit.SECONDS)){
                    scheduler.shutdownNow();
                }
            }catch(InterruptedException e){
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if(listenerContainer != null){
            listenerContainer.stop();
        }

        logger.info("指标计算服务已停止");
    }

    @Override
    public boolean checkKlineContinuity(String symbol, String interval){
        try{
            logger.debug("检查K线数据连续性: {} {}", symbol, interval);
            String key = SOURCE_KLINE_PREFIX + symbol + ":" + interval;

            // 获取所有K线数据
            Set<Object> klineSet = redisTemplate.opsForZSet().range(key, 0, - 1);
            if(klineSet == null || klineSet.isEmpty()){
                logger.warn("未找到K线数据: {}", key);
                return false;
            }

            // 转换为List并按时间排序
            List<Candlestick> klineList = klineSet.stream()
                        .map(obj -> JSONObject.parseObject((String)obj, Candlestick.class))
                        .sorted(Comparator.comparing(candlestick -> candlestick.getOpenTime()))
                        .collect(Collectors.toList());

            if(klineList.size() < 2){
                logger.warn("K线数据不足，无法检查连续性: {}", key);
                return false;
            }

            // 计算预期的时间间隔
            Duration expectedInterval = getIntervalDuration(interval);
            if(expectedInterval == null){
                logger.warn("无法识别的时间间隔: {}", interval);
                return false;
            }

            // 检查每两个相邻K线之间的时间间隔
            boolean isContinuous = true;
            for(int i = 1;i < klineList.size();i++){
                LocalDateTime prevTime = klineList.get(i - 1).getOpenTime();
                LocalDateTime currTime = klineList.get(i).getOpenTime();
                Duration actualInterval = Duration.between(prevTime, currTime);

                // 允许1秒的误差
                long diffSeconds = Math.abs(actualInterval.getSeconds() - expectedInterval.getSeconds());
                if(diffSeconds > 1){
                    logger.warn("K线数据不连续: {} 在 {} 和 {} 之间, 预期间隔: {}, 实际间隔: {}秒",
                        key, prevTime, currTime, expectedInterval.getSeconds(), actualInterval.getSeconds());
                    isContinuous = false;
                    break;
                }
            }

            return isContinuous;
        }catch(Exception e){
            logger.error("检查K线数据连续性出错: {} {}", symbol, interval, e);
            return false;
        }
    }

    @Override
    public boolean calculateIndicators(String symbol, String interval){
        try{
            logger.debug("计算技术指标: {} {}", symbol, interval);
            String sourceKey = SOURCE_KLINE_PREFIX + symbol + ":" + interval;

            // 获取所有K线数据
            Set<Object> klineSet = redisTemplate.opsForZSet().range(sourceKey, 0, - 1);
            if(klineSet == null || klineSet.isEmpty()){
                logger.warn("未找到K线数据: {}", sourceKey);
                return false;
            }

            // 转换为List并按时间排序
            List<Candlestick> klineList = klineSet.stream()
                .map(obj -> JSONObject.parseObject((String)obj, Candlestick.class))
                .sorted(Comparator.comparing(candlestick -> candlestick.getOpenTime()))
                .collect(Collectors.toList());

            // 检查数据量是否足够
            if(klineList.size() < MACD_SLOW_PERIOD + MACD_SIGNAL_PERIOD){
                logger.warn("K线数据不足，无法计算指标: {}, 当前数据量: {}, 需要至少: {}",
                    sourceKey, klineList.size(), MACD_SLOW_PERIOD + MACD_SIGNAL_PERIOD);
                return false;
            }

            // 检查数据连续性
            if(! checkKlineContinuity(symbol, interval)){
                logger.warn("K线数据不连续，跳过指标计算: {} {}", symbol, interval);
                return false;
            }

            // 提取收盘价序列
            List<BigDecimal> closeList = klineList.stream()
                .map(candlestick -> candlestick.getClose())
                .collect(Collectors.toList());

            // 提取最高价和最低价序列，用于计算KDJ和布林带
            List<BigDecimal> highList = klineList.stream()
                .map(candlestick -> candlestick.getHigh())
                .collect(Collectors.toList());

            List<BigDecimal> lowList = klineList.stream()
                .map(candlestick -> candlestick.getLow())
                .collect(Collectors.toList());

            // 计算各种指标
            TechnicalIndicatorUtil.MACD macd = TechnicalIndicatorUtil.calculateMACD(
                closeList, MACD_FAST_PERIOD, MACD_SLOW_PERIOD, MACD_SIGNAL_PERIOD, DEFAULT_SCALE);

            List<BigDecimal> rsi = TechnicalIndicatorUtil.calculateRSI(
                closeList, RSI_DEFAULT_PERIOD, DEFAULT_SCALE);

            TechnicalIndicatorUtil.BollingerBands boll = TechnicalIndicatorUtil.calculateBollingerBands(
                closeList, BOLL_DEFAULT_PERIOD, BOLL_DEFAULT_MULTIPLIER, DEFAULT_SCALE);

            TechnicalIndicatorUtil.KDJ kdj = TechnicalIndicatorUtil.calculateKDJ(
                highList, lowList, closeList, KDJ_DEFAULT_PERIOD,
                new BigDecimal("0.666"), new BigDecimal("0.333"), DEFAULT_SCALE);

            // 获取最新的K线数据
            Candlestick latestKline = klineList.get(klineList.size() - 1);
            LocalDateTime latestTime = latestKline.getOpenTime();

            // 保存计算结果到Redis
            saveIndicatorToRedis(symbol, interval, "macd", macd, klineList);
            saveIndicatorToRedis(symbol, interval, "rsi", rsi, klineList);
            saveIndicatorToRedis(symbol, interval, "boll", boll, klineList);
            saveIndicatorToRedis(symbol, interval, "kdj", kdj, klineList);

            logger.info("成功计算并保存技术指标: {} {}", symbol, interval);
            return true;
        }catch(Exception e){
            logger.error("计算技术指标出错: {} {}", symbol, interval, e);
            return false;
        }
    }

    @Override
    public int batchCalculateIndicators(Map<String,List<String>> symbolIntervalMap){
        if(symbolIntervalMap == null || symbolIntervalMap.isEmpty()){
            return 0;
        }

        int successCount = 0;
        for(Map.Entry<String,List<String>> entry: symbolIntervalMap.entrySet()){
            String symbol = entry.getKey();
            List<String> intervals = entry.getValue();

            for(String interval: intervals){
                if(calculateIndicators(symbol, interval)){
                    successCount++;
                }
            }
        }

        return successCount;
    }

    @Override
    public Map<String,Object> getMACDIndicator(String symbol, String interval){
        try{
            String key = TARGET_INDICATOR_PREFIX + symbol + ":" + interval + ":macd";
            Map<Object,Object> rawData = redisTemplate.opsForHash().entries(key);

            if(rawData == null || rawData.isEmpty()){
                // 尝试重新计算
                if(calculateIndicators(symbol, interval)){
                    rawData = redisTemplate.opsForHash().entries(key);
                }

                if(rawData == null || rawData.isEmpty()){
                    return null;
                }
            }

            Map<String,Object> result = new HashMap<>();
            result.put("macdLine", rawData.get("macdLine"));
            result.put("signalLine", rawData.get("signalLine"));
            result.put("histogram", rawData.get("histogram"));
            result.put("timestamp", rawData.get("timestamp"));

            return result;
        }catch(Exception e){
            logger.error("获取MACD指标数据出错: {} {}", symbol, interval, e);
            return null;
        }
    }

    @Override
    public List<Double> getRSIIndicator(String symbol, String interval, int period){
        try{
            String key = TARGET_INDICATOR_PREFIX + symbol + ":" + interval + ":rsi";
            Map<Object,Object> rawData = redisTemplate.opsForHash().entries(key);

            if(rawData == null || rawData.isEmpty()){
                // 尝试重新计算
                if(calculateIndicators(symbol, interval)){
                    rawData = redisTemplate.opsForHash().entries(key);
                }

                if(rawData == null || rawData.isEmpty()){
                    return null;
                }
            }

            Object rsiValues = rawData.get("values");
            if(rsiValues instanceof List){
                List<?> list = (List<?>)rsiValues;
                return list.stream()
                    .filter(obj -> obj instanceof Number)
                    .map(obj -> ((Number)obj).doubleValue())
                    .collect(Collectors.toList());
            }

            return null;
        }catch(Exception e){
            logger.error("获取RSI指标数据出错: {} {}", symbol, interval, e);
            return null;
        }
    }

    @Override
    public Map<String,Object> getKDJIndicator(String symbol, String interval){
        try{
            String key = TARGET_INDICATOR_PREFIX + symbol + ":" + interval + ":kdj";
            Map<Object,Object> rawData = redisTemplate.opsForHash().entries(key);

            if(rawData == null || rawData.isEmpty()){
                // 尝试重新计算
                if(calculateIndicators(symbol, interval)){
                    rawData = redisTemplate.opsForHash().entries(key);
                }

                if(rawData == null || rawData.isEmpty()){
                    return null;
                }
            }

            Map<String,Object> result = new HashMap<>();
            result.put("kValues", rawData.get("kValues"));
            result.put("dValues", rawData.get("dValues"));
            result.put("jValues", rawData.get("jValues"));
            result.put("timestamp", rawData.get("timestamp"));

            return result;
        }catch(Exception e){
            logger.error("获取KDJ指标数据出错: {} {}", symbol, interval, e);
            return null;
        }
    }

    @Override
    public Map<String,Object> getBollingerBandsIndicator(String symbol, String interval){
        try{
            String key = TARGET_INDICATOR_PREFIX + symbol + ":" + interval + ":boll";
            Map<Object,Object> rawData = redisTemplate.opsForHash().entries(key);

            if(rawData == null || rawData.isEmpty()){
                // 尝试重新计算
                if(calculateIndicators(symbol, interval)){
                    rawData = redisTemplate.opsForHash().entries(key);
                }

                if(rawData == null || rawData.isEmpty()){
                    return null;
                }
            }

            Map<String,Object> result = new HashMap<>();
            result.put("upper", rawData.get("upper"));
            result.put("middle", rawData.get("middle"));
            result.put("lower", rawData.get("lower"));
            result.put("timestamp", rawData.get("timestamp"));

            return result;
        }catch(Exception e){
            logger.error("获取布林带指标数据出错: {} {}", symbol, interval, e);
            return null;
        }
    }

    @Override
    public boolean subscribeIndicatorCalculation(String symbol, String interval){
        if(symbol == null || interval == null){
            return false;
        }

        try{
            // 添加到内存缓存
            subscriptionMap.computeIfAbsent(symbol, k -> new HashSet<>()).add(interval);

            // 添加到Redis缓存
            String subscriptionKey = symbol + ":" + interval;
            redisTemplate.opsForSet().add(INDICATOR_SUBSCRIPTION_KEY, subscriptionKey);

            // 执行一次计算
            calculateIndicators(symbol, interval);

            // 设置订阅监听
            setupKlineSubscription(symbol, interval);

            logger.info("已订阅指标计算: {} {}", symbol, interval);
            return true;
        }catch(Exception e){
            logger.error("订阅指标计算失败: {} {}", symbol, interval, e);
            return false;
        }
    }

    @Override
    public boolean unsubscribeIndicatorCalculation(String symbol, String interval){
        if(symbol == null || interval == null){
            return false;
        }

        try{
            // 从内存缓存移除
            Set<String> intervals = subscriptionMap.get(symbol);
            if(intervals != null){
                intervals.remove(interval);
                if(intervals.isEmpty()){
                    subscriptionMap.remove(symbol);
                }
            }

            // 从Redis缓存移除
            String subscriptionKey = symbol + ":" + interval;
            redisTemplate.opsForSet().remove(INDICATOR_SUBSCRIPTION_KEY, subscriptionKey);

            logger.info("已取消订阅指标计算: {} {}", symbol, interval);
            return true;
        }catch(Exception e){
            logger.error("取消订阅指标计算失败: {} {}", symbol, interval, e);
            return false;
        }
    }

    @Override
    public Map<String,List<String>> getAllSubscribedIndicators(){
        try{
            // 从Redis加载最新订阅信息
            loadSubscriptionsFromRedis();

            // 转换为List格式返回
            Map<String,List<String>> result = new HashMap<>();
            for(Map.Entry<String,Set<String>> entry: subscriptionMap.entrySet()){
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            return result;
        }catch(Exception e){
            logger.error("获取订阅信息失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 执行初始指标计算
     */
    private void executeInitialCalculation(){
        if(subscriptionMap.isEmpty()){
            logger.info("没有订阅信息，跳过初始指标计算");
            return;
        }

        logger.info("开始执行初始指标计算...");
        int totalSubscriptions = 0;
        int successCount = 0;

        for(Map.Entry<String,Set<String>> entry: subscriptionMap.entrySet()){
            String symbol = entry.getKey();
            Set<String> intervals = entry.getValue();

            for(String interval: intervals){
                totalSubscriptions++;
                if(calculateIndicators(symbol, interval)){
                    successCount++;
                    setupKlineSubscription(symbol, interval);
                }
            }
        }

        logger.info("初始指标计算完成, 成功: {}/{}", successCount, totalSubscriptions);
    }

    /**
     * 从Redis加载订阅信息
     */
    private void loadSubscriptionsFromRedis(){
        try{
            Set<Object> subscriptionKeys = redisTemplate.opsForSet().members(INDICATOR_SUBSCRIPTION_KEY);

            if(subscriptionKeys != null && ! subscriptionKeys.isEmpty()){
                // 更新内存中的订阅状态
                Map<String,Set<String>> newSubscriptionMap = new HashMap<>();

                for(Object key: subscriptionKeys){
                    String subscriptionKey = key.toString();
                    String[] parts = subscriptionKey.split("-");
                    if(parts.length == 2){
                        String symbol = parts[0];
                        String interval = parts[1];
                        newSubscriptionMap.computeIfAbsent(symbol, k -> new HashSet<>()).add(interval);
                    }
                }

                // 更新内存中的订阅映射
                subscriptionMap.clear();
                subscriptionMap.putAll(newSubscriptionMap);

                logger.info("从Redis加载了{}个交易对的订阅信息", newSubscriptionMap.size());
            }
        }catch(Exception e){
            logger.error("从Redis加载订阅信息失败", e);
        }
    }

    /**
     * 检查新订阅并设置监听
     */
    private void checkNewSubscriptions(){
        try{
            logger.debug("启动指标计算服务...");

            // 从Redis加载订阅信息
//            loadSubscriptionsFromRedis();

            // 执行初始指标计算
//            executeInitialCalculation();

            Set<Object> subscriptionKeys = redisTemplate.opsForSet().members(INDICATOR_SUBSCRIPTION_KEY);

            if(subscriptionKeys == null || subscriptionKeys.isEmpty()){
                return;
            }

            int newSubscriptions = 0;
            for(Object key: subscriptionKeys){
                String subscriptionKey = key.toString();
                String[] parts = subscriptionKey.split(":");
                if(parts.length == 2){
                    String symbol = parts[0];
                    String interval = parts[1];

                    // 检查是否已在内存中
                    Set<String> intervals = subscriptionMap.get(symbol);
                    if(intervals == null || ! intervals.contains(interval)){
                        // 新订阅，添加到内存并设置监听
                        subscriptionMap.computeIfAbsent(symbol, k -> new HashSet<>()).add(interval);
                        setupKlineSubscription(symbol, interval);
                        calculateIndicators(symbol, interval);
                        newSubscriptions++;
                    }
                }
            }

            if(newSubscriptions > 0){
                logger.info("发现并处理了{}个新订阅", newSubscriptions);
            }
        }catch(Exception e){
            logger.error("检查新订阅出错", e);
        }
    }

    /**
     * 设置K线数据订阅监听
     */
    private void setupKlineSubscription(String symbol, String interval){
        if(listenerContainer == null){
            initMessageListenerContainer();
        }

        String channelPattern = SOURCE_KLINE_PREFIX + symbol + ":" + interval;
        logger.debug("设置K线数据订阅监听: {}", channelPattern);

        // 创建消息监听器
        MessageListenerAdapter listenerAdapter =
            new MessageListenerAdapter();
        listenerAdapter.setDelegate(new RedisMessageListener(symbol, interval));
        listenerAdapter.setDefaultListenerMethod("onMessage");
        listenerAdapter.setSerializer(new StringRedisSerializer());

        // 添加到监听容器
        listenerContainer.addMessageListener(
            listenerAdapter,
            new PatternTopic(channelPattern)
        );
    }

    /**
     * Redis消息监听器内部类
     */
    private class RedisMessageListener {
        private final String symbol;
        private final String interval;

        public RedisMessageListener(String symbol, String interval) {
            this.symbol = symbol;
            this.interval = interval;
        }

        public void onMessage(Object message, String pattern) {
            handleKlineUpdate(symbol, interval, message);
        }
    }

    /**
     * 初始化消息监听容器
     */
    private void initMessageListenerContainer(){
        logger.info("初始化Redis消息监听容器");
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        if(connectionFactory == null){
            throw new IllegalStateException("无法获取Redis连接工厂");
        }

        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();
    }

    /**
     * 处理K线数据更新
     */
    private void handleKlineUpdate(String symbol, String interval, Object message){
        try{
            logger.debug("收到K线数据更新: {} {}", symbol, interval);
            // 重新计算指标
            calculateIndicators(symbol, interval);
        }catch(Exception e){
            logger.error("处理K线数据更新出错: {} {}", symbol, interval, e);
        }
    }

    /**
     * 保存指标数据到Redis
     */
    private <T> void saveIndicatorToRedis(String symbol, String interval, String indicatorType, T indicatorData, List<Candlestick> klineList){
        String key = TARGET_INDICATOR_PREFIX + symbol + ":" + interval + ":" + indicatorType;
        Map<String,Object> dataMap = new HashMap<>();

        // 获取最新K线的时间戳
        Candlestick latestKline = klineList.get(klineList.size() - 1);
        long timestamp = latestKline.getOpenTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        dataMap.put("timestamp", timestamp);
        dataMap.put("symbol", symbol);
        dataMap.put("interval", interval);

        if(indicatorData instanceof TechnicalIndicatorUtil.MACD){
            TechnicalIndicatorUtil.MACD macd = (TechnicalIndicatorUtil.MACD)indicatorData;
            dataMap.put("macdLine", macd.getMacdLine());
            dataMap.put("signalLine", macd.getSignalLine());
            dataMap.put("histogram", macd.getHistogram());
        }else if(indicatorData instanceof List){
            // RSI等单值指标
            dataMap.put("values", indicatorData);
        }else if(indicatorData instanceof TechnicalIndicatorUtil.BollingerBands){
            TechnicalIndicatorUtil.BollingerBands boll = (TechnicalIndicatorUtil.BollingerBands)indicatorData;
            dataMap.put("upper", boll.getUpper());
            dataMap.put("middle", boll.getMiddle());
            dataMap.put("lower", boll.getLower());
        }else if(indicatorData instanceof TechnicalIndicatorUtil.KDJ){
            TechnicalIndicatorUtil.KDJ kdj = (TechnicalIndicatorUtil.KDJ)indicatorData;
            dataMap.put("kValues", kdj.getKValues());
            dataMap.put("dValues", kdj.getDValues());
            dataMap.put("jValues", kdj.getJValues());
        }

        // 保存到Redis
        redisTemplate.opsForHash().putAll(key, dataMap);
        redisTemplate.expire(key, Duration.ofDays(7)); // 设置7天过期
    }

    /**
     * 将时间间隔字符串转换为Duration对象
     */
    private Duration getIntervalDuration(String interval){
        // 支持的时间间隔格式：1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
        if(interval == null || interval.isEmpty()){
            return null;
        }

//        interval = interval.toUpperCase();
        try{
            if(interval.endsWith("M") ){
                // 月线，按30天计算
                int months = Integer.parseInt(interval.substring(0, interval.length() - 1));
                return Duration.ofDays(30 * months);
            }else if(interval.endsWith("W")){
                // 周线
                int weeks = Integer.parseInt(interval.substring(0, interval.length() - 1));
                return Duration.ofDays(7 * weeks);
            }else if(interval.endsWith("D")){
                // 日线
                int days = Integer.parseInt(interval.substring(0, interval.length() - 1));
                return Duration.ofDays(days);
            }else if(interval.endsWith("H")){
                // 小时线
                int hours = Integer.parseInt(interval.substring(0, interval.length() - 1));
                return Duration.ofHours(hours);
            }else if(interval.endsWith("m")){
                // 分钟线
                int minutes = Integer.parseInt(interval.substring(0, interval.length() - 1));
                return Duration.ofMinutes(minutes);
            }else if(interval.endsWith("s")){
                // 秒线
                int seconds = Integer.parseInt(interval.substring(0, interval.length() - 1));
                return Duration.ofSeconds(seconds);
            }
        }catch(NumberFormatException e){
            logger.warn("无法解析时间间隔: {}", interval);
        }

        return null;
    }
}
