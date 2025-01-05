package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * 标注方法或异常类的响应状态码
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {
    /**
     * HTTP 状态码
     */
    int value() default 200;

    /**
     * 响应消息
     */
    String reason() default "";
} 