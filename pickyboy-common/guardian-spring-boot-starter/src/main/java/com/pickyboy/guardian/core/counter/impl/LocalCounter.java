package com.pickyboy.guardian.core.counter.impl;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.pickyboy.guardian.config.GuardianProperties;
import com.pickyboy.guardian.core.counter.Counter;
import com.pickyboy.guardian.model.counter.CounterParam;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Caffeine 的本地内存计数器实现
 * <p>
 * 该实现通过内部维护一个 Map，为每种不同的时间窗口动态创建专属的 LoadingCache 实例，
 * 从而支持 @GuardianCheck 注解中灵活配置的过期时间。
 *
 * @author pickyboy
 */
@Slf4j
@Component
@ConditionalOnClass(Caffeine.class)
@ConditionalOnProperty(name = "guardian.default-counter-type", havingValue = GuardianConstants.COUNTER_TYPE_LOCAL)
public class LocalCounter implements Counter {

    // 全局最大缓存条目数，可以从配置文件中读取
    private static long maximumCacheSize;

    /**
     * 注入我们统一的配置属性 Bean。
     */
    @Resource
    private GuardianProperties guardianProperties;

    /**
     * 核心：使用 @PostConstruct 注解。
     * 这个方法会在 Spring 完成该 Bean 的所有依赖注入后，初始化之前被调用。
     * 这是初始化静态成员的最佳时机。
     */
    @PostConstruct
    public void init() {
        // 将从配置 Bean 中读取到的值，赋给静态成员
        maximumCacheSize = guardianProperties.getLocalCounterMaxSize();
        log.info("Guardian LocalCounter initialized with maximum size: {}", maximumCacheSize);
    }

    /**
     * 核心：使用 ConcurrentHashMap 来存储不同过期时间的缓存实例。
     * Key: 过期时间（秒）
     * Value: 对应过期时间的 LoadingCache 实例
     */
    private final ConcurrentHashMap<Long, LoadingCache<String, AtomicLong>> caches = new ConcurrentHashMap<>();

    @Override
    public long incrementAndGet(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return 0;
        }
        // 1. 根据参数获取对应的 LoadingCache 实例
        LoadingCache<String, AtomicLong> cache = getCacheFor(config);
        // 2. 构建包含 scene 和 key 的唯一缓存键
        String cacheKey = buildCacheKey(config);
        // 3. 从缓存中获取或创建 AtomicLong 计数器，并原子递增
        return cache.get(cacheKey).incrementAndGet();
    }

    @Override
    public long getCurrentCount(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return 0;
        }
        LoadingCache<String, AtomicLong> cache = getCacheFor(config);
        String cacheKey = buildCacheKey(config);
        // getIfPresent 不会触发加载，如果 Key 不存在则返回 null
        AtomicLong counter = cache.getIfPresent(cacheKey);
        return (counter != null) ? counter.get() : 0L;
    }

    @Override
    public void reset(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return;
        }
        LoadingCache<String, AtomicLong> cache = getCacheFor(config);
        String cacheKey = buildCacheKey(config);
        cache.invalidate(cacheKey);
    }

    @Override
    public String getType() {
        return GuardianConstants.COUNTER_TYPE_LOCAL;
    }


    /**
     * 根据传入的参数，获取或创建一个具有相应过期时间的 LoadingCache 实例。
     *
     * @param parameter 计数器参数
     * @return 配置好的 LoadingCache 实例
     */
    private LoadingCache<String, AtomicLong> getCacheFor(CounterParam parameter) {
        // 将时间窗口统一转换为秒，作为 Map 的 Key
        long durationInSeconds = parameter.getTimeUnit().toSeconds(parameter.getWindowSize());

        // 使用 computeIfAbsent 原子性地获取或创建缓存实例
        return caches.computeIfAbsent(durationInSeconds, duration -> {
            log.info("为 {} 秒的窗口创建新的 Caffeine 缓存实例", duration);
            return Caffeine.newBuilder()
                    .maximumSize(maximumCacheSize)
                    // 使用动态的过期时间
                    .expireAfterWrite(duration, TimeUnit.SECONDS)
                    // 定义加载逻辑：当 Key 不存在时，创建一个初始值为 0 的 AtomicLong
                    .build(key -> new AtomicLong(0));
        });
    }


    /**
     * 构建缓存键，确保每个场景下的每个key都是唯一的。
     *
     * @param config 计数器参数
     * @return 唯一的缓存键，格式如：view_question:user_123
     */
    private String buildCacheKey(CounterParam config) {
        // 将 scene 和 key 拼接，形成唯一的标识
        return config.getScene() + ":" + config.getKey();
    }

}