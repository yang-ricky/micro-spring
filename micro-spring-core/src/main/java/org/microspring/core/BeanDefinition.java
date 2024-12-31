package org.microspring.core;

/**
 * Bean定义信息的抽象
 */
public interface BeanDefinition {
    
    /**
     * 获取Bean的Class类型
     */
    Class<?> getBeanClass();
    
    /**
     * 获取Bean的作用域
     */
    String getScope();
    
    /**
     * 是否是单例
     */
    boolean isSingleton();
    
    /**
     * 获取初始化方法名
     */
    String getInitMethodName();
} 