package org.microspring.core;

import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class DefaultBeanDefinition implements BeanDefinition {
    private final Class<?> beanClass;
    private String scope = "singleton";
    private boolean lazyInit = false;
    private String initMethodName;
    private String destroyMethodName;
    private boolean primary = false;
    private final List<PropertyValue> propertyValues = new ArrayList<>();
    private final List<ConstructorArg> constructorArgs = new ArrayList<>();
    private Method factoryMethod;
    private Class<?> factoryBeanClass;

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

    @Override
    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    @Override
    public String getDestroyMethodName() {
        return this.destroyMethodName;
    }

    @Override
    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }

    @Override
    public List<ConstructorArg> getConstructorArgs() {
        return this.constructorArgs;
    }

    @Override
    public List<PropertyValue> getPropertyValues() {
        return this.propertyValues;
    }

    @Override
    public void addConstructorArg(ConstructorArg arg) {
        this.constructorArgs.add(arg);
    }

    @Override
    public void addPropertyValue(PropertyValue propertyValue) {
        this.propertyValues.add(propertyValue);
    }

    @Override
    public boolean isLazyInit() {
        return this.lazyInit;
    }

    @Override
    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    @Override
    public Method getFactoryMethod() {
        return this.factoryMethod;
    }

    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    @Override
    public Class<?> getFactoryBeanClass() {
        return this.factoryBeanClass;
    }

    public void setFactoryBeanClass(Class<?> factoryBeanClass) {
        this.factoryBeanClass = factoryBeanClass;
    }

    @Override
    public boolean isPrimary() {
        return this.primary;
    }

    @Override
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
} 