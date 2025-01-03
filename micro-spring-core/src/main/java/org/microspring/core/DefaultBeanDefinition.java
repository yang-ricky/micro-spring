package org.microspring.core;

import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.ArrayList;
import java.util.List;

public class DefaultBeanDefinition implements BeanDefinition {
    private Class<?> beanClass;
    private String scope = "singleton";
    private String initMethodName;
    private final List<ConstructorArg> constructorArgs = new ArrayList<>();
    private final List<PropertyValue> propertyValues = new ArrayList<>();
    private boolean lazyInit = false;  // 默认不延迟加载

    public DefaultBeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    @Override
    public String getScope() {
        return this.scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean isSingleton() {
        return "singleton".equals(this.scope);
    }

    @Override
    public String getInitMethodName() {
        return this.initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    @Override
    public List<ConstructorArg> getConstructorArgs() {
        return constructorArgs;
    }

    @Override
    public List<PropertyValue> getPropertyValues() {
        return propertyValues;
    }

    @Override
    public void addConstructorArg(ConstructorArg arg) {
        constructorArgs.add(arg);
    }

    @Override
    public void addPropertyValue(PropertyValue propertyValue) {
        propertyValues.add(propertyValue);
    }

    @Override
    public boolean isLazyInit() {
        return this.lazyInit;
    }

    @Override
    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }
} 