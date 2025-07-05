package com.okx.trading.service.impl;

import com.okx.trading.event.CoinSubscriptionEvent;
import com.okx.trading.model.entity.CandlestickEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.service.RedisCacheService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.okx.trading.constant.IndicatorInfo.*;

/**
 * Redis缓存服务实现类
 * 用于实时价格数据的缓存操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Data
public class RedisCacheServiceImpl implements RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 默认订阅的币种
     */
    private static final String[] DEFAULT_COINS = {"BTC-USDT", "ETH-USDT", "SOL-USDT"};


    @Override
    public void updateCoinPrice(String symbol, BigDecimal price) {
        try {
            // 存储价格到Redis的Hash结构中
            // HSET coin-rt-price BTC-USDT 价格
            redisTemplate.opsForHash().put(COIN_PRICE_KEY, symbol, price.toString());
            log.debug("更新币种 {} 实时价格: {}", symbol, price);
        } catch (Exception e) {
            log.error("更新币种实时价格到Redis失败: {}", e.getMessage(), e);
        }

    }

    @Override
    public void updateCandlestick(Candlestick candlestick) {
        try {
            String key = COIN_KLINE_PREFIX_KEY + candlestick.getSymbol() + ":" + candlestick.getIntervalVal();
            long openTime = candlestick.getOpenTime().atZone(ZoneId.of("UTC+8")).toInstant().toEpochMilli();
            double parseDouble = Double.parseDouble(String.valueOf(openTime));
            Set<Object> exist = redisTemplate.opsForZSet().rangeByScore(key, parseDouble, parseDouble);
            if (!exist.isEmpty()) {
                redisTemplate.opsForZSet().removeRangeByScore(key, parseDouble, parseDouble);
            }
            redisTemplate.opsForZSet().add(key, candlestick.toString(), parseDouble);
        } catch (Exception e) {
            log.error("更新币种实时K线到Redis失败: {} {},", candlestick, e.getMessage(), e);
        }

    }

    @Override
    public Map<String, BigDecimal> getAllCoinPrices() {
        try {
            // 获取所有币种价格
            // HGETALL coin-rt-price
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(COIN_PRICE_KEY);
            Map<String, BigDecimal> result = new HashMap<>(entries.size());

            // 转换类型
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String symbol = entry.getKey().toString();
                BigDecimal price = new BigDecimal(entry.getValue().toString());
                result.put(symbol, price);
            }

            return result;
        } catch (Exception e) {
            log.error("从Redis获取所有币种实时价格失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public BigDecimal getCoinPrice(String symbol) {
        try {
            // 获取指定币种价格
            // HGET coin-rt-price BTC-USDT
            Object value = redisTemplate.opsForHash().get(COIN_PRICE_KEY, symbol);
            if (value != null) {
                return new BigDecimal(value.toString());
            }
            return null;
        } catch (Exception e) {
            log.error("从Redis获取币种 {} 实时价格失败: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Set<String> getSubscribedCoins() {
        try {
            // 获取所有订阅的币种
            // SMEMBERS subscribe-coins
            Set<Object> members = redisTemplate.opsForSet().members(SUBSCRIBED_COINS_KEY);
            if (members == null || members.isEmpty()) {
                // 如果为空，初始化默认币种
                initDefaultSubscribedCoins();
                members = redisTemplate.opsForSet().members(SUBSCRIBED_COINS_KEY);
            }

            Set<String> result = new HashSet<>(members.size());
            for (Object member : members) {
                result.add(member.toString());
            }

            log.debug("获取订阅币种列表，共 {} 个", result.size());
            return result;
        } catch (Exception e) {
            log.error("从Redis获取订阅币种列表失败: {}", e.getMessage(), e);
            // 返回默认币种
            Set<String> defaultSet = new HashSet<>(DEFAULT_COINS.length);
            for (String coin : DEFAULT_COINS) {
                defaultSet.add(coin);
            }
            return defaultSet;
        }
    }

    @Override
    public boolean addSubscribedCoin(String symbol) {
        try {
            // 检查是否已在订阅列表中
            boolean isMember = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(SUBSCRIBED_COINS_KEY, symbol));

            // 添加订阅币种
            // SADD subscribe-coins BTC-USDT
            Long added = redisTemplate.opsForSet().add(SUBSCRIBED_COINS_KEY, symbol);
            boolean success = added != null && added > 0;

            if (success) {
                log.info("添加订阅币种: {}", symbol);
                // 仅当币种是新添加的（之前不存在于列表中）时才发布事件
                if (!isMember) {
                    log.debug("发布币种 {} 订阅事件", symbol);
                    eventPublisher.publishEvent(new CoinSubscriptionEvent(this, symbol, CoinSubscriptionEvent.EventType.SUBSCRIBE));
                } else {
                    log.debug("币种 {} 已存在于订阅列表中，不重复发布事件", symbol);
                }
            } else {
                log.debug("币种 {} 已在订阅列表中", symbol);
            }
            return true;
        } catch (Exception e) {
            log.error("添加订阅币种 {} 到Redis失败: {}", symbol, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeSubscribedCoin(String symbol) {
        try {
            // 检查是否存在于订阅列表中
            boolean isMember = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(SUBSCRIBED_COINS_KEY, symbol));

            if (!isMember) {
                log.debug("币种 {} 不在订阅列表中，无需移除", symbol);
                return true;
            }

            // 移除订阅币种
            // SREM subscribe-coins BTC-USDT
            Long removed = redisTemplate.opsForSet().remove(SUBSCRIBED_COINS_KEY, symbol);
            boolean success = removed != null && removed > 0;

            if (success) {
                log.info("移除订阅币种: {}", symbol);
                // 发布币种取消订阅事件
                log.debug("发布币种 {} 取消订阅事件", symbol);
                eventPublisher.publishEvent(new CoinSubscriptionEvent(this, symbol, CoinSubscriptionEvent.EventType.UNSUBSCRIBE));
            } else {
                log.debug("币种 {} 不在订阅列表中，或移除失败", symbol);
            }
            return true;
        } catch (Exception e) {
            log.error("从Redis移除订阅币种 {} 失败: {}", symbol, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void initDefaultSubscribedCoins() {
        try {
            // 检查是否已有订阅币种
            Long size = redisTemplate.opsForSet().size(SUBSCRIBED_COINS_KEY);
            if (size == null || size == 0) {
                // 添加默认订阅币种
                for (String coin : DEFAULT_COINS) {
                    redisTemplate.opsForSet().add(SUBSCRIBED_COINS_KEY, coin);
                }
                log.info("初始化默认订阅币种: {}", (Object) DEFAULT_COINS);
            }
        } catch (Exception e) {
            log.error("初始化默认订阅币种失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void setCache(String key, Object value, long timeoutMinutes) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutMinutes, TimeUnit.MINUTES);
            log.debug("设置缓存成功，key: {}, 过期时间: {} 分钟", key, timeoutMinutes);
        } catch (Exception e) {
            log.error("设置缓存失败，key: {}, error: {}", key, e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCache(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("缓存不存在或已过期，key: {}", key);
                return null;
            }
            log.debug("获取缓存成功，key: {}", key);
            return (T) value;
        } catch (Exception e) {
            log.error("获取缓存失败，key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteCache(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            boolean success = Boolean.TRUE.equals(deleted);
            if (success) {
                log.debug("删除缓存成功，key: {}", key);
            } else {
                log.debug("缓存不存在，key: {}", key);
            }
            return success;
        } catch (Exception e) {
            log.error("删除缓存失败，key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void batchAddKlineToSortedSet(String symbol, String interval, List<CandlestickEntity> candlesticks, long timeoutMinutes) {
        try {
            String key = String.format("%s%s:%s", COIN_NRT_KLINE_PREFIX_KEY, symbol, interval);

            // 批量添加到Sorted Set
            for (com.okx.trading.model.entity.CandlestickEntity candlestick : candlesticks) {
                if (candlestick.getOpenTime() != null) {
                    // 使用开盘时间的时间戳作为score
                    long openTimeTimestamp = candlestick.getOpenTime().atZone(java.time.ZoneId.of("UTC+8")).toInstant().toEpochMilli();
                    double score = (double) openTimeTimestamp;

                    // 将K线数据转换为JSON字符串作为value
                    String jsonValue = candlestick.toString();
                    // 添加新数据
                    redisTemplate.opsForZSet().addIfAbsent(key, candlestick.toString(), score);
                }
            }

            // 设置过期时间
            if (timeoutMinutes > 0) {
                redisTemplate.expire(key, Duration.ofMinutes(timeoutMinutes));
            }

            log.info("💾 批量添加 {} 条K线数据到Redis Sorted Set，key: {}, 过期时间: {} 分钟",
                    candlesticks.size(), key, timeoutMinutes);
        } catch (Exception e) {
            log.error("❌ 批量添加K线数据到Redis Sorted Set失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public java.util.Set<String> getKlineFromSortedSet(String symbol, String interval, double startScore, double endScore) {
        try {
            String key = String.format("%s%s:%s", COIN_NRT_KLINE_PREFIX_KEY, symbol, interval);

            // 按时间戳范围获取数据
            java.util.Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, startScore, endScore);

            java.util.Set<String> jsonStrings = new java.util.HashSet<>();
            if (result != null) {
                for (Object obj : result) {
                    jsonStrings.add(obj.toString());
                }
            }

            log.debug("从Redis Sorted Set获取K线数据，key: {}, 范围: {} - {}, 结果数量: {}",
                    key, startScore, endScore, jsonStrings.size());

            return jsonStrings;
        } catch (Exception e) {
            log.error("❌ 从Redis Sorted Set获取K线数据失败: {}", e.getMessage(), e);
            return new java.util.HashSet<>();
        }
    }

    @Override
    public boolean clearKlineSortedSet(String symbol, String interval) {
        try {
            String key = String.format("%s%s:%s", COIN_NRT_KLINE_PREFIX_KEY, symbol, interval);
            Boolean deleted = redisTemplate.delete(key);
            boolean success = Boolean.TRUE.equals(deleted);

            if (success) {
                log.info("🗑️ 清除K线Sorted Set成功，key: {}", key);
            } else {
                log.debug("K线Sorted Set不存在，key: {}", key);
            }

            return success;
        } catch (Exception e) {
            log.error("❌ 清除K线Sorted Set失败，key: {}{}, error: {}",
                    COIN_NRT_KLINE_PREFIX_KEY + symbol + interval, e.getMessage(), e);
            return false;
        }
    }
}
