package org.microspring.core.io;

import org.microspring.core.BeanDefinition;

public class BeanDefinitionHolder {
    private final String beanName;
    private final BeanDefinition beanDefinition;
    
    public BeanDefinitionHolder(String beanName, BeanDefinition beanDefinition) {
        this.beanName = beanName;
        this.beanDefinition = beanDefinition;
    }
    
    public String getBeanName() {
        return beanName;
    }
    
    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }
} 