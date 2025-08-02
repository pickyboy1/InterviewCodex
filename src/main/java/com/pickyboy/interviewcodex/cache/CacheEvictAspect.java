package com.pickyboy.interviewcodex.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 缓存失效切面
 *
 * @author pickyboy
 */
@Aspect
@Slf4j
public class CacheEvictAspect {

    private final CacheUtils cacheUtils;

    /**
     * 构造函数注入
     * @param cacheUtils 缓存工具类
     */
    public CacheEvictAspect(CacheUtils cacheUtils) {
        this.cacheUtils = cacheUtils;
    }

    /**
     * 在方法成功返回后执行
     * @param joinPoint a ProceedingJoinPoint
     * @param cacheEvict the cache evict
     */
    @AfterReturning(pointcut = "@annotation(cacheEvict)", returning = "result")
    public void evictCache(JoinPoint joinPoint, CacheEvict cacheEvict, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取状态列表
        List<String> statuses = cacheEvict.statuses().length > 0 ?
            Arrays.asList(cacheEvict.statuses()) : null;

        if (cacheEvict.isBatch()) {
            // 批量缓存清除
            evictBatchCache(cacheEvict, method, joinPoint.getArgs(), statuses);
        } else {
            // 单个缓存清除
            evictSingleCache(cacheEvict, method, joinPoint.getArgs(), statuses);
        }
    }

    /**
     * 单个缓存清除
     */
    private void evictSingleCache(CacheEvict cacheEvict, Method method, Object[] args, List<String> statuses) {
        Object keyResult = parseSpelKeyObject(cacheEvict.keyExpression(), method, args);

        if (statuses != null && !statuses.isEmpty()) {
            // 支持多状态的缓存清除
            cacheUtils.evictCacheWithStatus(cacheEvict.scene(), keyResult, statuses);
        } else {
            // 普通单个缓存清除
            cacheUtils.evictCache(cacheEvict.scene(), keyResult);
        }
    }

    /**
     * 批量缓存清除
     */
    private void evictBatchCache(CacheEvict cacheEvict, Method method, Object[] args, List<String> statuses) {
        Object keyResult = parseSpelKeyObject(cacheEvict.keyExpression(), method, args);

        if (!(keyResult instanceof List)) {
            log.warn("Batch cache evict expects a List but got: {}", keyResult.getClass());
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object> idList = (List<Object>) keyResult;

        if (statuses != null && !statuses.isEmpty()) {
            // 支持多状态的批量缓存清除
            cacheUtils.batchEvictCacheWithStatus(cacheEvict.scene(), idList, statuses);
        } else {
            // 普通批量缓存清除
            cacheUtils.batchEvictCache(cacheEvict.scene(), idList);
        }
    }

    private Object parseSpelKeyObject(String keyExpression, Method method, Object[] args) {
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
        return expression.getValue(context);
    }
}