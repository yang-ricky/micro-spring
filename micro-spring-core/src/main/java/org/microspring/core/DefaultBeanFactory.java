package org.microspring.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory implements BeanFactory {
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public Object getBean(String name) {
        return null; // 暂时返回null，在任务2中实现具体逻辑
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return null; // 暂时返回null，在任务2中实现具体逻辑
    }
} 