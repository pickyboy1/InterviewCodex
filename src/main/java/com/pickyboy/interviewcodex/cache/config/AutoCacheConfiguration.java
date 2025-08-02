package com.pickyboy.interviewcodex.cache.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickyboy.interviewcodex.cache.AutoCacheAspect;
import com.pickyboy.interviewcodex.cache.CacheEvictAspect;
import com.pickyboy.interviewcodex.cache.CacheUtils;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoCacheConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AutoCacheAspect autoCacheAspect(RedissonClient redisson, ObjectMapper om){
        return new AutoCacheAspect(redisson,om);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheEvictAspect cacheEvictAspect(CacheUtils cacheUtils){
        return new CacheEvictAspect(cacheUtils);
    }
}
