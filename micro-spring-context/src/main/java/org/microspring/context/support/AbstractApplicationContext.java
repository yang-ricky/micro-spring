package org.microspring.context.support;

import org.microspring.beans.factory.annotation.Value;
import org.microspring.context.ApplicationContext;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.core.io.ClassPathBeanDefinitionScanner;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.context.event.ApplicationEvent;
import org.microspring.context.event.ApplicationEventPublisher;
import org.microspring.context.event.SimpleApplicationEventPublisher;
import org.microspring.context.event.ContextRefreshedEvent;
import org.microspring.context.event.ApplicationListener;

import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public abstract class AbstractApplicationContext implements ApplicationContext {
    protected final DefaultBeanFactory beanFactory;
    protected final ValueResolver valueResolver;
    private final List<ApplicationListener<?>> applicationListeners = new ArrayList<>();
    
    public AbstractApplicationContext() {
        this.beanFactory = new DefaultBeanFactory();
        this.valueResolver = new DefaultValueResolver(beanFactory);
    }
    
    public AbstractApplicationContext(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.valueResolver = new DefaultValueResolver(beanFactory);
    }
    
    @Override
    public abstract String getApplicationName();
    
    @Override
    public long getStartupDate() {
        return System.currentTimeMillis();
    }
    
    @Override
    public boolean containsBean(String name) {
        return beanFactory.getBeanDefinition(name) != null;
    }
    
    protected void scanPackages(String... basePackages) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
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
    public <T> T getBean(Class<T> requiredType) {
        T bean = beanFactory.getBean(requiredType);
        injectDependencies(bean);
        return bean;
    }
    
    protected void injectDependencies(Object bean) {
        Class<?> clazz = bean.getClass();
        
        // 1. 处理字段注入
        for (Field field : clazz.getDeclaredFields()) {
            // 现有的字段注入代码保持不变
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
            
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                Object valueToInject;
                
                if (qualifier != null) {
                    valueToInject = getBean(qualifier.value());
                } else {
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
        
        // 2. 处理方法注入（包括setter方法和普通方法）
        for (Method method : clazz.getDeclaredMethods()) {
            Autowired autowired = method.getAnnotation(Autowired.class);
            if (autowired != null) {
                Class<?>[] paramTypes = method.getParameterTypes();
                Annotation[][] paramAnnotations = method.getParameterAnnotations();
                Object[] args = new Object[paramTypes.length];
                
                // 处理每个参数
                for (int i = 0; i < paramTypes.length; i++) {
                    Qualifier qualifier = null;
                    // 查找参数上的 @Qualifier 注解
                    for (Annotation ann : paramAnnotations[i]) {
                        if (ann instanceof Qualifier) {
                            qualifier = (Qualifier) ann;
                            break;
                        }
                    }
                    
                    if (qualifier != null) {
                        args[i] = getBean(qualifier.value());
                    } else {
                        args[i] = getBean(paramTypes[i]);
                    }
                }
                
                try {
                    method.setAccessible(true);
                    method.invoke(bean, args);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject method: " + method, e);
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
    
    public void close() {
        System.out.println("Closing application context...");
        // 遍历所有单例bean，调用销毁方法
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (bd.isSingleton()) {
                Object bean = getBean(beanName);
                String destroyMethodName = bd.getDestroyMethodName();
                if (destroyMethodName != null && !destroyMethodName.isEmpty()) {
                    try {
                        Method destroyMethod = bean.getClass().getDeclaredMethod(destroyMethodName);
                        destroyMethod.setAccessible(true);
                        destroyMethod.invoke(bean);
                    } catch (Exception e) {
                        throw new RuntimeException("Error invoking destroy method on bean: " + beanName, e);
                    }
                }
            }
        }
    }
    
    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return beanFactory.getBeanNamesForAnnotation(annotationType);
    }
    
    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        Map<String, Object> result = new HashMap<>();
        Set<String> beanNames = getBeanFactory().getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            Object bean = getBean(beanName);
            if (bean.getClass().isAnnotationPresent(annotationType)) {
                result.put(beanName, bean);
            }
        }
        
        return result;
    }
    
    protected DefaultBeanFactory getBeanFactory() {
        return this.beanFactory;
    }
    
    @Override
    public void refresh() {
        
        // 注册监听器
        registerListeners();
        
        // 发布刷新完成事件
        publishEvent(new ContextRefreshedEvent(this));
    }
    
    protected void registerListeners() {
        String[] listenerNames = getBeanNamesForType(ApplicationListener.class);
        for (String listenerName : listenerNames) {
            ApplicationListener<?> listener = (ApplicationListener<?>) getBean(listenerName);
            addApplicationListener(listener);
        }
    }
    
    @Override
    public void publishEvent(ApplicationEvent event) {
        for (ApplicationListener<?> listener : getApplicationListeners(event)) {
            invokeListener(listener, event);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void invokeListener(ApplicationListener listener, ApplicationEvent event) {
        listener.onApplicationEvent(event);
    }
    
    private List<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event) {
        List<ApplicationListener<?>> allListeners = new ArrayList<>();
        for (ApplicationListener<?> listener : applicationListeners) {
            if (supportsEvent(listener, event)) {
                allListeners.add(listener);
            }
        }
        return allListeners;
    }
    
    private boolean supportsEvent(ApplicationListener<?> listener, ApplicationEvent event) {
        return true; // 简化版实现，实际应该检查泛型类型
    }
    
    public void addApplicationListener(ApplicationListener<?> listener) {
        if (listener != null) {
            this.applicationListeners.add(listener);
        }
    }
    
    public String[] getBeanNamesForType(Class<?> type) {
        Set<String> result = new HashSet<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (type.isAssignableFrom(bd.getBeanClass())) {
                result.add(beanName);
            }
        }
        return result.toArray(new String[0]);
    }
} 