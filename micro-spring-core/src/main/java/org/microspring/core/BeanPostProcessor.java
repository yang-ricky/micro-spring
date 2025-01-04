package org.microspring.core;

/**
 * Bean后置处理器接口
 * 允许自定义修改新bean实例的工厂钩子
 */
public interface BeanPostProcessor {

    /**
     * 在bean初始化之前应用此BeanPostProcessor
     * 
     * @param bean bean实例
     * @param beanName bean名称
     * @return 要使用的bean实例，原始实例或包装实例
     */
    Object postProcessBeforeInitialization(Object bean, String beanName);

    /**
     * 在bean初始化之后应用此BeanPostProcessor
     * 
     * @param bean bean实例
     * @param beanName bean名称
     * @return 要使用的bean实例，原始实例或包装实例
     */
    Object postProcessAfterInitialization(Object bean, String beanName);
} 