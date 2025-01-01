package org.microspring.context.support;

import org.microspring.beans.factory.annotation.Value;
import org.microspring.context.ApplicationContext;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.core.io.ClassPathBeanDefinitionScanner;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;

public abstract class AbstractApplicationContext implements ApplicationContext {
    protected DefaultBeanFactory beanFactory;
    protected ValueResolver valueResolver;
    
    public AbstractApplicationContext() {
        this.beanFactory = new DefaultBeanFactory();
        this.valueResolver = new DefaultValueResolver();
    }
    
    @Override
    public Object getBean(String name) {
        Object bean = beanFactory.getBean(name);
        injectDependencies(bean);
        return bean;
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T bean = beanFactory.getBean(name, requiredType);
        injectDependencies(bean);
        return bean;
    }
    
    @Override
    public long getStartupDate() {
        return System.currentTimeMillis();
    }
    
    @Override
    public boolean containsBean(String name) {
        return beanFactory.getBeanDefinition(name) != null;
    }
    
    protected void scanPackages(String... basePackages) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner();
        for (String basePackage : basePackages) {
            List<BeanDefinition> beanDefinitions = scanner.scan(basePackage);
            for (BeanDefinition bd : beanDefinitions) {
                String beanName = generateBeanName(bd.getBeanClass());
                beanFactory.registerBeanDefinition(beanName, bd);
            }
        }
    }
    
    private String generateBeanName(Class<?> beanClass) {
        String shortClassName = beanClass.getSimpleName();
        if (beanClass.getEnclosingClass() != null) {
            shortClassName = shortClassName.substring(shortClassName.lastIndexOf('$') + 1);
        }
        return Character.toLowerCase(shortClassName.charAt(0)) + shortClassName.substring(1);
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) {
        T bean = beanFactory.getBean(requiredType);
        injectDependencies(bean);
        return bean;
    }
    
    protected void injectDependencies(Object bean) {
        Class<?> clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            // 处理 @Value 注解
            Value valueAnn = field.getAnnotation(Value.class);
            if (valueAnn != null) {
                String expression = valueAnn.value();
                Object resolvedValue = valueResolver.resolveValue(expression);
                try {
                    field.setAccessible(true);
                    Object convertedValue = convertValueIfNecessary(field.getType(), resolvedValue);
                    field.set(bean, convertedValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to inject @Value: " + expression, e);
                }
                continue;
            }
            
            // 处理 @Autowired 注解
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                Object valueToInject;
                
                if (qualifier != null) {
                    // 如果有 @Qualifier，通过名称获取 bean
                    valueToInject = getBean(qualifier.value());
                } else {
                    // 如果没有 @Qualifier，通过类型获取 bean
                    valueToInject = getBean(field.getType());
                }
                
                try {
                    field.setAccessible(true);
                    field.set(bean, valueToInject);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to inject field: " + field, e);
                }
            }
        }
    }
    
    private Object convertValueIfNecessary(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }
        
        // 如果类型已经匹配，直接返回
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // String -> 基本类型的转换
        String strValue = value.toString();
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(strValue);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(strValue);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(strValue);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(strValue);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(strValue);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(strValue);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(strValue);
        }
        if (targetType == char.class || targetType == Character.class) {
            return strValue.length() > 0 ? strValue.charAt(0) : '\0';
        }
        
        // 如果没有合适的转换，返回原值
        return value;
    }
} 