package com.pickyboy.interviewcodex.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存失效注解
 * <p>
 * 标记在方法上（通常是写操作，如update/delete），
 * 在方法成功执行后，会自动删除指定的缓存。
 *
 * @author pickyboy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {

    /**
     * 缓存场景/Key前缀，必须与 @AutoCache 中的 scene 保持一致
     */
    String scene();

    /**
     * SpEL 表达式，用于动态计算 Key。
     * 通常是更新或删除对象的 ID。
     */
    String keyExpression();

    /**
     * 是否为批量操作，默认为 false
     * 当为 true 时，keyExpression 应该返回一个 List<Long> 类型的ID列表
     */
    boolean isBatch() default false;

    /**
     * 状态列表，用于支持多key模式
     * 例如题库缓存有两种状态：["true", "false"]
     * 如果不指定，则只清除基础key
     */
    String[] statuses() default {};
}