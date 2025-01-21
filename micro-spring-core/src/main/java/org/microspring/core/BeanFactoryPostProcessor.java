package org.microspring.core;

/**
 * 允许自定义修改应用程序上下文的bean定义
 * 在所有bean定义加载完成后，但在bean实例化之前调用
 */
public interface BeanFactoryPostProcessor {
    /**
     * 在bean定义加载完成后修改应用程序上下文的内部bean定义注册表
     * 所有bean定义都将被加载，但还没有bean被实例化
     * 这允许覆盖或添加属性，甚至可以初始化bean
     *
     * @param beanFactory bean工厂
     */
    void postProcessBeanFactory(DefaultBeanFactory beanFactory);
} 