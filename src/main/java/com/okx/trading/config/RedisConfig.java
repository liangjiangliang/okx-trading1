package com.okx.trading.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 配置RedisTemplate
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * key使用StringRedisSerializer
     * value使用GenericJackson2JsonRedisSerializer
     * hash的key也使用StringRedisSerializer
     * hash的value使用GenericJackson2JsonRedisSerializer
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

//        // 使用StringRedisSerializer来序列化和反序列化redis的key
//        template.setKeySerializer(new StringRedisSerializer());
//        // 使用GenericJackson2JsonRedisSerializer来序列化和反序列化redis的value值
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//
//        // Hash的key也采用StringRedisSerializer的序列化方式
//        template.setHashKeySerializer(new StringRedisSerializer());
//        // Hash的value使用GenericJackson2JsonRedisSerializer的序列化方式
//        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
//
        // 设置序列化方式
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();

        // 这一步关键：注册 Java8 时间模块！
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        serializer.setObjectMapper(mapper);
        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        template.afterPropertiesSet();

        return template;
    }
}
