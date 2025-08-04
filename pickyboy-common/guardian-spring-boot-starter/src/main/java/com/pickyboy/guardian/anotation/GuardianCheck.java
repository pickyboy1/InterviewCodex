package com.pickyboy.guardian.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Guardian 访问控制注解
 * <p>
 * 应用于方法上，以声明该方法需要进行频率检查和访问控制。
 *
 * @author pickyboy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardianCheck {

    /**
     * 业务场景名称，用于日志记录和区分不同的监控。
     * 例如："view_question", "post_comment"
     *
     * @return 场景名称
     */
    String scene();

    /**
     * 用于定位访问主体的 Key，支持 Spring Expression Language (SpEL)。
     * 例如：#userId, #request.getRemoteAddr(), #loginUser.id
     * 如果指定了keyProvider，此参数将作为额外参数传递给KeyProvider
     * @return SpEL 表达式字符串或其他Key表达式
     */
    String key();

    /**
     * 指定要使用的KeyProvider名称。
     * 可选值：
     * - "user": 自动获取用户ID
     * - "ip": 自动获取客户端IP地址
     * - "spel": SpEL表达式解析（默认）
     * - "auto": 根据key表达式自动选择
     * - 自定义Provider名称
     *
     * @return KeyProvider名称
     */
    String keyProvider() default "";

    /**
     * 计数器类型，默认使用配置文件中的默认类型
     * @return 计数器类型
     */
    String counterType() default "";

    /**
     * 频率计算的时间窗口大小。
     * 默认为 1。
     *
     * @return 时间数值
     */
    int windowSize() default 1;

    /**
     * 时间窗口的单位。
     * 默认为分钟 (MINUTES)。
     *
     * @return TimeUnit 枚举
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 过期时间（秒），默认为180秒
     * @return 过期时间
     */
    long expiration() default 180;


    Rule[] rules() default {};


    /**
     * 失败时的错误消息，默认使用配置文件中的默认消息
     * @return 错误消息
     */
    String errorMessage() default "";
}
