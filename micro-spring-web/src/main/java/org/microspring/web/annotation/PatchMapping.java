package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * PATCH 请求映射的快捷注解
 * 相当于 @RequestMapping(method = RequestMethod.PATCH)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.PATCH)
public @interface PatchMapping {
    /**
     * 请求路径
     */
    String value() default "";
} 