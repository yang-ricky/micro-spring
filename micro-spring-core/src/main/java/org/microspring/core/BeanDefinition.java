package org.microspring.core;

import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.List;
import java.lang.reflect.Method;
import org.microspring.core.type.PrimaryMetadata;

/**
 * Bean定义信息的抽象
 */
public interface BeanDefinition extends PrimaryMetadata {
    
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
    void setInitMethodName(String initMethodName);
    
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
    
    /**
     * 是否延迟初始化
     */
    boolean isLazyInit();
    
    /**
     * 设置是否延迟初始化
     */
    void setLazyInit(boolean lazyInit);
    
    /**
     * 获取销毁方法名
     */
    String getDestroyMethodName();
    
    /**
     * 设置销毁方法名
     */
    void setDestroyMethodName(String destroyMethodName);

    /**
     * 获取工厂方法
     */
    default Method getFactoryMethod() {
        return null;
    }

    /**
     * 获取工厂Bean的类型
     */
    default Class<?> getFactoryBeanClass() {
        return null;
    }

    void setPrimary(boolean primary);
} 