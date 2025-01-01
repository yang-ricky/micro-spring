package org.microspring.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory implements BeanFactory {
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public Object getBean(String name) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        if (beanDefinition == null) {
            throw new RuntimeException("No bean named '" + name + "' is defined");
        }
        
        if (beanDefinition.isSingleton()) {
            Object singleton = singletonObjects.get(name);
            if (singleton == null) {
                singleton = createBean(beanDefinition);
                singletonObjects.put(name, singleton);
            }
            return singleton;
        }
        
        return createBean(beanDefinition);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = getBean(name);
        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw new RuntimeException("Bean named '" + name + "' is not of required type '" + requiredType.getName() + "'");
        }
        return (T) bean;
    }
    
    private Object createBean(BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        try {
            Object instance = beanClass.getDeclaredConstructor().newInstance();
            System.out.println("[BeanFactory] Creating bean: " + beanClass.getSimpleName());
            
            // 如果有初始化方法，调用它
            String initMethodName = beanDefinition.getInitMethodName();
            if (initMethodName != null && !initMethodName.isEmpty()) {
                beanClass.getMethod(initMethodName).invoke(instance);
            }
            
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error creating bean with class '" + beanClass.getName() + "'", e);
        }
    }
} 