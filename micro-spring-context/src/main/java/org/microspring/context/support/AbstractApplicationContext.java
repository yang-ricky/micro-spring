package org.microspring.context.support;

import org.microspring.context.ApplicationContext;
import org.microspring.core.DefaultBeanFactory;

public abstract class AbstractApplicationContext implements ApplicationContext {
    protected final DefaultBeanFactory beanFactory;
    protected final long startupDate;
    
    public AbstractApplicationContext() {
        this.beanFactory = new DefaultBeanFactory();
        this.startupDate = System.currentTimeMillis();
    }
    
    @Override
    public Object getBean(String name) {
        return beanFactory.getBean(name);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return beanFactory.getBean(name, requiredType);
    }
    
    @Override
    public long getStartupDate() {
        return startupDate;
    }
    
    @Override
    public boolean containsBean(String name) {
        return beanFactory.getBeanDefinition(name) != null;
    }
} 