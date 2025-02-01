package org.microspring.context.annotation;

import org.microspring.core.type.AnnotatedTypeMetadata;

/**
 * 条件接口，用于条件化地注册 Bean
 */
public interface Condition {
    /**
     * 判断条件是否匹配
     *
     * @param context 条件上下文
     * @param metadata 注解元数据
     * @return 如果条件匹配返回 true
     */
    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
} 