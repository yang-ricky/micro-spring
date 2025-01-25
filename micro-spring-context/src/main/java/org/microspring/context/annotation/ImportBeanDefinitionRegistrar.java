package org.microspring.context.annotation;

import org.microspring.core.DefaultBeanFactory;

/**
 * 处理 @Import 注解导入的类的注册器接口
 * 实现该接口的类必须有一个无参构造函数
 * 
 * <p>该接口通常配合 @Import 注解使用，允许在运行时动态注册额外的bean定义
 * 例如：MyBatis的 @MapperScan 注解就是通过该接口来注册 Mapper 接口的代理对象
 * 
 * @see Import
 */
public interface ImportBeanDefinitionRegistrar {
    /**
     * 注册bean定义
     * 
     * @param importingClass 导入该注册器的类，通常是标注了 @Import 的配置类
     * @param beanFactory bean工厂，用于注册bean定义
     */
    void registerBeanDefinitions(Class<?> importingClass, DefaultBeanFactory beanFactory);
} 