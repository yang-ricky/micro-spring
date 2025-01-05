package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * 标注需要进行参数校验
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Valid {
} 