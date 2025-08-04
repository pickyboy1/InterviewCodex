package com.pickyboy.guardian.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rule {
    /**
     * 触发此规则的计数值（阈值）。
     */
    int count();

    /**
     * 达到阈值后，要执行的处置策略的 Bean 名称。
     */
    String[] strategy();

    String level();

    String description() default "";

    /**
     * 规则是否为持续触发模式。
     * - false (默认): 单次模式，仅在计数值正好等于 count 时触发。
     * - true: 持续模式，在计数值大于或等于 count 时都会触发。
     *
     * @return 是否持续触发
     */
    boolean continuous() default false;
}
