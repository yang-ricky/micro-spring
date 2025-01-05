package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * 标注方法为异常处理器
 * 用于处理控制器中的特定异常
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {
    /**
     * 指定要处理的异常类型
     */
    Class<? extends Throwable>[] value() default {};
} 