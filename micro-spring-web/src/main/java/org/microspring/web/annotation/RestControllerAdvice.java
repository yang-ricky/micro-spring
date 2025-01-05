package org.microspring.web.annotation;

import java.lang.annotation.*;
import org.microspring.stereotype.Component;

/**
 * 标注类为全局异常处理器
 * 可以处理所有控制器抛出的异常
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@ResponseBody
public @interface RestControllerAdvice {
    /**
     * 指定要处理的包名
     */
    String[] basePackages() default {};

    /**
     * 指定要处理的注解类型
     */
    Class<? extends Annotation>[] annotations() default {};

    /**
     * 指定要处理的目标类
     */
    Class<?>[] assignableTypes() default {};
} 