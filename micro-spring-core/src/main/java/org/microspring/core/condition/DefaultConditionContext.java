package org.microspring.core.condition;

import org.microspring.core.DefaultBeanFactory;

public class DefaultConditionContext implements ConditionContext {
    private final DefaultBeanFactory beanFactory;
    
    public DefaultConditionContext(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public String getEnvironment(String key) {
        return System.getProperty(key);
    }
    
    @Override
    public DefaultBeanFactory getBeanFactory() {
        return beanFactory;
    }
} 