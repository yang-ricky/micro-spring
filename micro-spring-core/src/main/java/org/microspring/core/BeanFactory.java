package org.microspring.core;

/**
 * Bean工厂的顶层接口
 */
public interface BeanFactory {
    
    /**
     * 获取Bean实例
     * @param name bean的名称
     * @return bean实例
     */
    Object getBean(String name);
    
    /**
     * 获取指定类型的Bean实例
     * @param name bean的名称
     * @param requiredType 期望的bean类型
     * @return bean实例
     */
    <T> T getBean(String name, Class<T> requiredType);
    
    /**
     * 检查是否包含指定的Bean
     * @param name bean的名称
     * @return 如果包含返回true，否则返回false
     */
    boolean containsBean(String name);
} 