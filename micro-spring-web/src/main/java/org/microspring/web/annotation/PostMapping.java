package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * POST 请求映射的快捷注解
 * 相当于 @RequestMapping(method = RequestMethod.POST)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.POST)
public @interface PostMapping {
    /**
     * 请求路径
     */
    String value() default "";
} 