package org.microspring.context.annotation;

import java.lang.annotation.*;

/**
 * 指示组件在哪些 profile 被激活时才应该被注册。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Profile {
    /**
     * profile 的名称。
     * 如果配置了多个 profile，只要其中任何一个处于激活状态，该组件就会被注册。
     */
    String[] value();
} 