package com.pickyboy.interviewcodex.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.pickyboy.interviewcodex.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;


/**
 * 自动缓存切面
 *
 * @author pickyboy
 */
@Aspect
//@Component
@Slf4j
public class AutoCacheAspect {

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    public AutoCacheAspect(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }


    @Around("@annotation(autoCache)")
    public Object cache(ProceedingJoinPoint joinPoint, AutoCache autoCache) throws Throwable {
        // 1. 构造缓存键
        String cacheKey = generateCacheKey(autoCache, joinPoint);

        // 2. 先尝试从缓存获取
        // 包括L1和L2缓存
        Object cachedValue = getCachedValue(cacheKey, joinPoint, autoCache);
        // 缓存命中
        if (cachedValue != null) {
            // 缓存空结果,不是真的缓存null,而是空值占位符,真的null可能只是键不存在
            // 检查是否为空值标识
            if (isNullPlaceholder(cachedValue)) {
                // 拿到缓存空值,返回null,避免缓存穿透
                log.debug("Cache hit with null value for key: {}", cacheKey);
                return null;
            }
            log.debug("Cache hit for key: {}", cacheKey);
            return cachedValue;
        }

        // 3. 缓存未命中，处理缓存击穿防护
        if (autoCache.enableBreakdownProtection()) {
            return executeWithBreakdownProtection(cacheKey, joinPoint, autoCache);
        } else {
            // 直接执行原方法
            log.debug("Cache miss for key: {}, executing original method", cacheKey);
            Object result = joinPoint.proceed();
            cacheResult(cacheKey, result, autoCache);
            return result;
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(AutoCache autoCache, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String key = parseSpelKey(autoCache.keyExpression(), method, joinPoint.getArgs());
        return autoCache.scene() + "::" + key;
    }

    /**
     * 获取缓存值
     */
    private Object getCachedValue(String cacheKey, ProceedingJoinPoint joinPoint, AutoCache autoCache) {
        // 1. L1 本地缓存查询
        // isHotKey方法会上报一次key,用于统计
        if(autoCache.enableL1()) {
            if (JdHotKeyStore.isHotKey(cacheKey)) {
                Object localCacheResult = JdHotKeyStore.get(cacheKey);
                if (localCacheResult != null) {
                    log.info("L1 Cache Hit: {}", cacheKey);
                    return localCacheResult;
                }
            }
        }

        // 2. L2 分布式缓存查询
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String jsonValue = bucket.get();
        if (jsonValue != null) {
            log.info("L2 Cache Hit: {}", cacheKey);
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Type genericReturnType = method.getGenericReturnType();

            Object redisResult = deserialize(jsonValue, genericReturnType);
            if (redisResult != null) {
                // L2缓存命中,尝试智能设置本地缓存
                if(autoCache.enableL1()) {
                JdHotKeyStore.smartSet(cacheKey, redisResult);
                }
                return redisResult;
            }
        }

        return null;
    }

    /**
     * 带缓存击穿防护的执行方法
     */
    private Object executeWithBreakdownProtection(String cacheKey, ProceedingJoinPoint joinPoint, AutoCache autoCache) throws Throwable {
        String lockKey = "lock::" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取分布式锁,避免某个热点key过期,大量请求同时访问数据库
            boolean acquired = lock.tryLock(3000, 10000, TimeUnit.MILLISECONDS);

            if (acquired) {
                try {
                // 获取锁成功，再次检查缓存（双重检查,防止获取锁之前缓存被其他线程写入,不经判断就执行原方法,访问数据库）
                Object doubleCheckResult = getCachedValue(cacheKey, joinPoint, autoCache);
                if (doubleCheckResult != null) {
                    if (isNullPlaceholder(doubleCheckResult)) {
                        log.debug("Double check: Cache hit with null value for key: {}", cacheKey);
                        return null;
                    }
                    log.debug("Double check: Cache hit for key: {}", cacheKey);
                    return doubleCheckResult;
                }

                    // 缓存仍然不存在，执行原方法
                    log.debug("Lock acquired for key: {}, executing original method", cacheKey);
                    Object result = joinPoint.proceed();
                    cacheResult(cacheKey, result, autoCache);
                    return result;

                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，进行重试
                return retryWithBackoff(cacheKey, joinPoint, autoCache, 3);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cache lock interrupted for key: {}", cacheKey);
            // 降级执行原方法
            Object result = joinPoint.proceed();
            cacheResult(cacheKey, result, autoCache);
            return result;
        }
    }

    /**
     * 重试获取缓存，带退避策略
     */
    private Object retryWithBackoff(String cacheKey, ProceedingJoinPoint joinPoint, AutoCache autoCache, int maxRetry) throws Throwable {
        for (int i = 0; i < maxRetry; i++) {
            try {
                // 别的线程先拿到锁,执行完后会写入缓存,这里先等待一会拿到锁的线程执行,之后尽量从缓存获取
                Thread.sleep(100 + i * 50); // 退避策略：100ms, 150ms, 200ms

                            // 重新尝试从缓存获取
            Object retryResult = getCachedValue(cacheKey, joinPoint, autoCache);
            if (retryResult != null) {
                if (isNullPlaceholder(retryResult)) {
                    log.debug("Retry {}: Cache hit with null value for key: {}", i + 1, cacheKey);
                    return null;
                }
                log.debug("Retry {}: Cache hit for key: {}", i + 1, cacheKey);
                return retryResult;
            }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 所有重试都失败，降级执行原方法
        // 已经尝试从缓存获取了好几次,还是没拿到,说明拿到锁的线程可能出了问题,不再继续尝试从缓存获取,直接执行原方法
        log.warn("All retries failed for key: {}, executing original method", cacheKey);
        Object result = joinPoint.proceed();
        cacheResult(cacheKey, result, autoCache);
        return result;
    }

        /**
     * 缓存结果，包括null值的防穿透处理
     */
    private void cacheResult(String cacheKey, Object result, AutoCache autoCache) {
        if (result == null) {
            // 缓存空值，设置较短的过期时间防止缓存穿透
            String nullPlaceholder = createNullPlaceholder();
            int nullTtl = autoCache.nullTtl() > 0 ? autoCache.nullTtl() : 60; // 默认60秒

            redissonClient.getBucket(cacheKey).set(nullPlaceholder, nullTtl, TimeUnit.SECONDS);
            log.debug("Cached null value for key: {} with TTL: {}s", cacheKey, nullTtl);
        } else {
            // 缓存正常值（序列化后存储）
            String jsonToCache = serialize(result);
            int ttl = autoCache.expireTime();
            redissonClient.getBucket(cacheKey).set(jsonToCache, ttl, TimeUnit.SECONDS);
            log.debug("Cached result for key: {} with TTL: {}s", cacheKey, ttl);

            // 仅在启用L1缓存时设置本地缓存
            if (autoCache.enableL1()) {
                JdHotKeyStore.smartSet(cacheKey, result);
            }
        }
    }

    /**
     * 创建空值占位符
     */
    private String createNullPlaceholder() {
        return "NULL_PLACEHOLDER_" + System.currentTimeMillis();
    }

    /**
     * 检查是否为空值占位符
     */
    private boolean isNullPlaceholder(Object value) {
        return value instanceof String && ((String) value).startsWith("NULL_PLACEHOLDER_");
    }

    // --- 辅助方法 ---

    private String parseSpelKey(String keyExpression, Method method, Object[] args) {
        SpelExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(keyExpression);
        EvaluationContext context = new StandardEvaluationContext();
        StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();
        String[] parameterNames = discoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        return String.valueOf(expression.getValue(context));
    }

    // 序列化方法现在变得非常简单
    private String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new AutoCacheException("Serialization failed", e);
        }
    }

    // 反序列化方法也变得非常通用
    private Object deserialize(String jsonValue, Type returnType) {
        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            JavaType javaType = typeFactory.constructType(returnType);
            return objectMapper.readValue(jsonValue, javaType);
        } catch (Exception e) {
            log.error("Deserialization failed for key", e);
            return null;
        }
    }
}
