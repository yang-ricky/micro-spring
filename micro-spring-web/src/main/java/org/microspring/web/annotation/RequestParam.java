package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * 标注方法参数应该从请求参数中解析
 * 例如: /users?id=123 中的 id
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    /**
     * 参数名称
     */
    String value() default "";
    
    /**
     * 参数是否必需
     */
    boolean required() default true;
    
    /**
     * 默认值
     */
    String defaultValue() default "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
} 