package org.microspring.core;

public class DefaultBeanDefinition implements BeanDefinition {
    private Class<?> beanClass;
    private String scope = "singleton";
    private String initMethodName;

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
} 