package org.microspring.context.annotation;

import java.lang.annotation.*;

/**
 * 表示一个方法产生一个由Spring容器管理的bean
 * 通常与@Configuration一起使用
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    /**
     * bean的名称，如果不指定则默认使用方法名
     */
    String[] value() default {};

    /**
     * bean是否自动注入
     */
    boolean autowire() default true;

    /**
     * 初始化方法名
     */
    String initMethod() default "";

    /**
     * 销毁方法名
     */
    String destroyMethod() default "(inferred)";
} 