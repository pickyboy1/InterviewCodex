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
}
