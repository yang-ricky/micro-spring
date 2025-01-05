package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * DELETE 请求映射的快捷注解
 * 相当于 @RequestMapping(method = RequestMethod.DELETE)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.DELETE)
public @interface DeleteMapping {
    /**
     * 请求路径
     */
    String value() default "";
} 