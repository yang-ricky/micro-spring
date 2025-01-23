package org.microspring.context.annotation;

import java.lang.annotation.*;

/**
 * 标识一个bean为首选bean。当有多个相同类型的bean时，被@Primary标注的bean将被优先考虑。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
} 