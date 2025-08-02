package com.pickyboy.interviewcodex.cache.config;

import com.pickyboy.interviewcodex.cache.CacheUtils;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 *
 * @author pickyboy
 */
@Configuration
public class CacheConfig {

    /**
     * 缓存工具类Bean
     * @param redissonClient Redis客户端
     * @return CacheUtils实例
     */
    @Bean
    public CacheUtils cacheUtils(RedissonClient redissonClient) {
        return new CacheUtils(redissonClient);
    }
}