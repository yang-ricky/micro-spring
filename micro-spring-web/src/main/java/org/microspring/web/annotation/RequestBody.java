package org.microspring.web.annotation;

import java.lang.annotation.*;

/**
 * 标注方法参数应该从请求体中解析
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
} 