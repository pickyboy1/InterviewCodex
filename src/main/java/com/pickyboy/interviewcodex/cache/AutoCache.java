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

    /**
     * 空值TTL（秒），用于防止缓存穿透
     * @return
     */
    public int nullTtl() default 60; // 默认1分钟

    /**
     * 是否启用缓存击穿防护（分布式锁）(热点key过期,大量请求同时访问数据库)
     * @return
     */
    public boolean enableBreakdownProtection() default true;

    /**
     * 随机过期时间范围（秒）
     * 用于防止缓存雪崩，在基础过期时间上添加 [0, randomExpireRange] 的随机值
     * 默认为0，表示不使用随机过期时间
     *
     * 例如：expireTime=3600, randomExpireRange=300
     * 实际过期时间将在 [3600, 3900] 秒之间随机
     *
     * @return 随机过期时间的最大值（秒）
     */
    public int randomExpireRange() default 0;

    /**
     * 是否启用L1缓存（本地缓存）
     * @return
     */
    boolean enableL1() default true;
}
