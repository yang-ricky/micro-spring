package org.microspring.core;

/**
 * Bean实例化感知处理器接口
 * 用于在Bean实例化过程中提供更细粒度的控制，特别是在处理循环依赖时
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    
    /**
     * 在 Bean 实例化后，但在属性填充之前获取早期引用
     */
    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }
} 