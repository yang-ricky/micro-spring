package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * 标注方法参数应该从URL路径中解析
 * 例如: /users/{id} 中的 {id}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    /**
     * URL路径变量的名称
     */
    String value() default "";
}