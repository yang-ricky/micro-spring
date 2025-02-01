package org.microspring.core.type;

import java.util.Map;

/**
 * 注解元数据接口
 */
public interface AnnotatedTypeMetadata {

    /**
     * 判断是否存在指定的注解
     */
    boolean isAnnotated(String annotationName);

    /**
     * 获取指定注解的属性
     */
    Map<String, Object> getAnnotationAttributes(String annotationName);
} 