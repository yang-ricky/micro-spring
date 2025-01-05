package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * PUT 请求映射的快捷注解
 * 相当于 @RequestMapping(method = RequestMethod.PUT)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.PUT)
public @interface PutMapping {
    /**
     * 请求路径
     */
    String value() default "";
} 