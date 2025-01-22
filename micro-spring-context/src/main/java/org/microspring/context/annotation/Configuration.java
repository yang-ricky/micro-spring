package org.microspring.context.annotation;

import java.lang.annotation.*;
import org.microspring.stereotype.Component;

/**
 * 表示一个类声明了一个或多个@Bean方法
 * 可以被Spring容器处理以生成bean定义和服务请求
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    /**
     * 显式指定bean的名称
     */
    String value() default "";
} 