package org.microspring.web.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    /**
     * 请求路径
     */
    String value() default "";
    
    /**
     * 请求方法
     */
    RequestMethod[] method() default {};
} 