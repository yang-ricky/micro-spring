package org.microspring.beans;

public class BeanDefinition {
    private String beanClassName;
    private Class<?> beanClass;
    
    public BeanDefinition(String beanClassName) {
        this.beanClassName = beanClassName;
        try {
            this.beanClass = Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find class [" + beanClassName + "]");
        }
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }
} 