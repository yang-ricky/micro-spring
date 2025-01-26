package org.microspring.beans.factory;

/**
 * 工厂Bean接口，用于创建其他Bean的实例
 */
public interface FactoryBean<T> {
    /**
     * 获取Bean实例
     */
    T getObject() throws Exception;
    
    /**
     * 获取Bean类型
     */
    Class<?> getObjectType();
    
    /**
     * 是否是单例
     */
    boolean isSingleton();
} 