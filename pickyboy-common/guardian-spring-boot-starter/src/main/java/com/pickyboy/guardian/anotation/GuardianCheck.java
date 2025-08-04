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
     * ip,自动获取ip
     * userId,自动获取用户ID
     * @return SpEL 表达式字符串或其他Key表达式
     */
    String key();


    /**
     * 频率计算的时间窗口大小。
     * <p>
     * <b>关键改动</b>: 默认值为 -1 (哨兵值)。
     * 如果为 -1，框架将使用 application.yml 中配置的全局默认值 (guardian.default-window-size)。
     *
     * @return 时间数值
     */
    int windowSize() default -1;

    /**
     * 时间窗口的单位。
     * 默认为分钟 (MINUTES)。
     *
     * @return TimeUnit 枚举
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;



    Rule[] rules() default {};


    /**
     * 失败时的错误消息，默认使用配置文件中的默认消息
     * @return 错误消息
     */
    String errorMessage() default "";
}
