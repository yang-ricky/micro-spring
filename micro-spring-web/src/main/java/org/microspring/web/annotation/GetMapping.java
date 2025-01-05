package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * GET 请求映射的快捷注解
 * 相当于 @RequestMapping(method = RequestMethod.GET)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.GET)
public @interface GetMapping {
    /**
     * 请求路径
     */
    String value() default "";
} 