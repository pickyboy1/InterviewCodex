package com.pickyboy.interviewcodex.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动缓存注解
 *
 * @author pickyboy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCache {

    /**
     * 缓存的场景
     *
     * @return
     */
    public String scene();


    /**
     * SPEL表达式: Spring Expression Language（Spring 表达式语言）
     * <pre>
     *     #id
     *     #insertResult.id
     * </pre>
     *
     * @return
     */
    public String keyExpression() default AutoCacheConstant.NONE_KEY;

    /**
     * 超时时间： 秒
     * 默认情况300s
     *
     * @return
     */
    public int expireTime() default AutoCacheConstant.DEFAULT_EXPIRE_TIME;

    boolean enableL1() default true;
}
