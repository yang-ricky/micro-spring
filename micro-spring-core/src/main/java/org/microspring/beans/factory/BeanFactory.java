package org.microspring.beans.factory;

import org.microspring.beans.BeanDefinition;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanFactory {
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    public Object getBean(String beanName) {
        // 先从单例池中获取
        Object singleton = singletonObjects.get(beanName);
        if (singleton != null) {
            return singleton;
        }

        // 获取bean定义
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new RuntimeException("No bean named '" + beanName + "' is defined");
        }

        // 创建bean实例
        try {
            singleton = beanDefinition.getBeanClass().newInstance();
            singletonObjects.put(beanName, singleton);
            return singleton;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating bean with name '" + beanName + "'", e);
        }
    }
} 