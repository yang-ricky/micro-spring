package org.microspring.core;

import java.util.List;
import java.util.Map;
import java.lang.annotation.Annotation;

public interface ListableBeanFactory extends BeanFactory {
    
    /**
     * 根据类型获取所有匹配的bean实例列表
     */
    <T> List<T> getBeansByType(Class<T> type);
    
    /**
     * 根据类型获取所有匹配的bean名称和实例的映射
     */
    <T> Map<String, T> getBeanNamesByType(Class<T> type);
    
    String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);
    
    void doAutowire(Object bean, BeanDefinition bd);
} 