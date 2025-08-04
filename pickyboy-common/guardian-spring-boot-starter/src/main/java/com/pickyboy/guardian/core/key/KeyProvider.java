package com.pickyboy.guardian.core.key;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Key生成器接口
 * 用于自定义限流Key的生成逻辑
 *
 * @author pickyboy
 */
public interface KeyProvider {

    /**
     * 生成限流Key
     *
     * @param joinPoint AOP切点信息
     * @param keyExpression 原始key表达式（可能为空）
     * @return 生成的限流Key
     */
    String generateKey(ProceedingJoinPoint joinPoint, String keyExpression);

    /**
     * 获取Provider名称
     *
     * @return Provider名称，用于注解中引用
     */
    String getName();

    /**
     * 获取Provider描述
     *
     * @return Provider描述信息
     */
    default String getDescription() {
        return getName();
    }

    /**
     * 是否支持该key表达式
     * 可以用于自动选择合适的provider
     *
     * @param keyExpression key表达式
     * @return 是否支持
     */
    default boolean supports(String keyExpression) {
        return true;
    }
}