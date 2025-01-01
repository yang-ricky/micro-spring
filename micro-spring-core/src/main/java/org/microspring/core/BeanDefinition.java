package org.microspring.core;

import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.List;

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
    
    /**
     * 获取构造器参数
     */
    List<ConstructorArg> getConstructorArgs();
    
    /**
     * 获取属性值
     */
    List<PropertyValue> getPropertyValues();
    
    /**
     * 添加构造器参数
     */
    void addConstructorArg(ConstructorArg arg);
    
    /**
     * 添加属性值
     */
    void addPropertyValue(PropertyValue propertyValue);
} 