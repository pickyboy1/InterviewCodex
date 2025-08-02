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
    public Object process(ProceedingJoinPoint pjp, AutoCache autoCache) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Type genericReturnType = method.getGenericReturnType();


        String key = parseSpelKey(autoCache.keyExpression(), method, pjp.getArgs());
        String cacheKey = autoCache.scene() + "::" + key;

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

        // 获取失败，转向redis获取
        // 2. L2 分布式缓存查询
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String jsonValue = bucket.get();
        if (jsonValue != null) {
            log.info("L2 Cache Hit: {}", cacheKey);
            Object redisResult = deserialize(jsonValue, genericReturnType);
            if (redisResult != null) {
                // 尝试智能设置本地缓存
                JdHotKeyStore.smartSet(cacheKey, redisResult);
                return redisResult;
            }
        }

        // 获取失败，转向数据库获取
        // 3. 缓存未命中，执行原始方法
        log.info("Cache Miss: {}. Executing original method.", cacheKey);
        Object dbResult = pjp.proceed();

        // 4. 回填缓存
        if (dbResult != null) {
            String jsonToCache = serialize(dbResult);
            bucket.set(jsonToCache, autoCache.expireTime(), TimeUnit.SECONDS);
            log.info("Set L2 Cache: {}", cacheKey);
            JdHotKeyStore.smartSet(cacheKey, dbResult);
        }

        return dbResult;
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
