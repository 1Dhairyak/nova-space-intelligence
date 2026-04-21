package com.nova.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    /**
     * RedisTemplate for direct cache operations (e.g. storing ISS position).
     * Keys are plain Strings; values are JSON-serialised POJOs.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager with per-cache TTL policies.
     * Different data has different staleness tolerance:
     *   ISS     → 10s  (position changes every few seconds)
     *   weather → 5min (Kp index / flare data)
     *   neo     → 1hr  (asteroid feed doesn't change intra-day)
     *   launch  → 5min (countdown ticking; agency may update windows)
     *   apod    → 12hr (NASA updates once per day)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("iss",         defaults.entryTtl(Duration.ofSeconds(10)));
        cacheConfigs.put("weather",     defaults.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("neo",         defaults.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("launches",    defaults.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("apod",        defaults.entryTtl(Duration.ofHours(12)));
        cacheConfigs.put("missions",    defaults.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults.entryTtl(Duration.ofHours(1)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
