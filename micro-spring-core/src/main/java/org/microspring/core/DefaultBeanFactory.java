package org.microspring.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;

import org.microspring.core.io.XmlBeanDefinitionReader;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.aware.BeanNameAware;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.core.aware.BeanFactoryAware;

public class DefaultBeanFactory implements BeanFactory {
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    private boolean closed = false;
    
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    
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
                singleton = createBean(name, beanDefinition);
                singletonObjects.put(name, singleton);
            }
            return singleton;
        }
        
        return createBean(name, beanDefinition);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = getBean(name);
        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw new RuntimeException("Bean named '" + name + "' is not of required type '" + requiredType.getName() + "'");
        }
        return (T) bean;
    }
    
    protected Object createBean(String beanName, BeanDefinition bd) {
        Object bean = null;
        try {
            bean = createBeanInstance(bd);
            populateBean(bean, bd);
            
            // 在Aware方法之前应用BeanPostProcessor
            bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
            if (bean != null) {
                invokeAwareMethods(beanName, bean);
                
                // 调用初始化方法
                String initMethodName = bd.getInitMethodName();
                if (initMethodName != null && !initMethodName.isEmpty()) {
                    Method initMethod = bean.getClass().getDeclaredMethod(initMethodName);
                    initMethod.setAccessible(true);
                    initMethod.invoke(bean);
                }
                
                // 在初始化之后应用BeanPostProcessor
                bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
            }
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Error creating bean with name '" + beanName + "'", e);
        }
    }

    private void invokeInitMethod(String beanName, Object bean, BeanDefinition bd) {
        String initMethodName = bd.getInitMethodName();
        if (initMethodName != null && !initMethodName.isEmpty()) {
            try {
                Method initMethod = bd.getBeanClass().getDeclaredMethod(initMethodName);
                initMethod.setAccessible(true);
                initMethod.invoke(bean);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking init method on bean: " + beanName, e);
            }
        }
    }
    
    public Map<String, Object> getSingletonObjects() {
        return this.singletonObjects;
    }

    protected void populateBean(Object bean, BeanDefinition bd) throws Exception {
        // 1. 处理 PropertyValue 注入
        for (PropertyValue pv : bd.getPropertyValues()) {
            Field field = bean.getClass().getDeclaredField(pv.getName());
            field.setAccessible(true);
            
            Object value;
            if (pv.isRef()) {
                value = getBean(pv.getRef());
            } else {
                value = pv.getValue();
            }
            
            field.set(bean, value);
        }

        // 2. 处理 @Autowired 字段注入
        for (Field field : bean.getClass().getDeclaredFields()) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                field.setAccessible(true);
                
                // 检查是否有 @Qualifier
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                Object value;
                
                if (qualifier != null) {
                    value = getBean(qualifier.value());
                } else {
                    value = getBean(field.getType());
                }
                
                field.set(bean, value);
            }
        }
    }
    
    private List<?> handleListValue(List<?> sourceList) {
        List<Object> targetList = new ArrayList<>();
        for (Object item : sourceList) {
            if (item instanceof String) {
                targetList.add(item);
            }
            // 可以添加其他类型的处理
        }
        return targetList;
    }
    
    private Map<?, ?> handleMapValue(Map<?, ?> sourceMap) {
        Map<Object, Object> targetMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                // 处理数字类型
                if (value.toString().matches("\\d+")) {
                    targetMap.put(key, Integer.parseInt((String) value));
                } else {
                    targetMap.put(key, value);
                }
            }
            // 可以添加其他类型的处理
        }
        return targetMap;
    }

    public void loadBeanDefinitions(String xmlPath) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);
        reader.loadBeanDefinitions(xmlPath);
    }

    protected Object createBeanInstance(BeanDefinition bd) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        
        // 1. 检查是否有构造器参数
        if (!bd.getConstructorArgs().isEmpty()) {
            return createBeanUsingConstructorArgs(bd);
        }
        
        // 2. 检查是否有@Autowired注解的构造器
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        Constructor<?> autowiredConstructor = null;
        
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                autowiredConstructor = constructor;
                break;
            }
        }
        
        // 3. 如果有@Autowired注解的构造器，使用它
        if (autowiredConstructor != null) {
            autowiredConstructor.setAccessible(true);
            Class<?>[] paramTypes = autowiredConstructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            
            // 获取构造器参数的限定符
            Annotation[][] paramAnnotations = autowiredConstructor.getParameterAnnotations();
            
            for (int i = 0; i < paramTypes.length; i++) {
                String qualifier = null;
                for (Annotation annotation : paramAnnotations[i]) {
                    if (annotation instanceof Qualifier) {
                        qualifier = ((Qualifier) annotation).value();
                        break;
                    }
                }
                
                // 根据类型和限定符获取依赖
                args[i] = qualifier != null ? 
                         getBean(qualifier) : 
                         getBean(paramTypes[i]);
            }
            
            return autowiredConstructor.newInstance(args);
        }
        
        // 4. 如果没有特殊要求，使用默认构造器
        try {
            Constructor<?> defaultConstructor = beanClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            return defaultConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No suitable constructor found for " + beanClass.getName(), e);
        }
    }

    private Object createBeanUsingConstructorArgs(BeanDefinition bd) throws Exception {
        List<ConstructorArg> args = bd.getConstructorArgs();
        Class<?>[] paramTypes = new Class<?>[args.size()];
        Object[] paramValues = new Object[args.size()];
        
        for (int i = 0; i < args.size(); i++) {
            ConstructorArg arg = args.get(i);
            if (arg.isRef()) {
                paramValues[i] = getBean(arg.getRef());
                paramTypes[i] = paramValues[i].getClass();
            } else {
                paramValues[i] = arg.getValue();
                paramTypes[i] = arg.getType();
            }
        }
        
        Constructor<?> constructor = bd.getBeanClass().getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(paramValues);
    }

    public BeanDefinition getBeanDefinition(String name) {
        return beanDefinitionMap.get(name);
    }

    @Override
    public boolean containsBean(String name) {
        return beanDefinitionMap.containsKey(name);
    }

    public Set<String> getBeanDefinitionNames() {
        return beanDefinitionMap.keySet();
    }

    @Override   
    public <T> T getBean(Class<T> requiredType) {
        List<String> matchingBeans = new ArrayList<>();
        String exactMatch = null;
        
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition bd = beanDefinitionMap.get(beanName);
            if (requiredType.isAssignableFrom(bd.getBeanClass())) {
                matchingBeans.add(beanName);
                if (bd.getBeanClass() == requiredType) {
                    exactMatch = beanName;
                }
            }
        }
        
        if (matchingBeans.isEmpty()) {
            throw new RuntimeException("No bean of type '" + requiredType.getName() + "' is defined");
        }
        
        // 优先返回精确匹配的bean
        if (exactMatch != null) {
            return (T) getBean(exactMatch);
        }
        
        // 如果只有一个匹配的bean，返回它
        if (matchingBeans.size() == 1) {
            return (T) getBean(matchingBeans.get(0));
        }
        
        // 如果有多个匹配但没有精确匹配，抛出异常
        throw new RuntimeException("Multiple beans found for type '" + requiredType.getName() 
            + "': " + matchingBeans);
    }

    public void close() {
        if (closed) {
            return;
        }
        
        // 遍历所有单例bean，调用destroy方法
        for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            BeanDefinition bd = getBeanDefinition(beanName);
            
            try {
                invokeDestroyMethod(bean, bd);
            } catch (Exception e) {
                throw new RuntimeException("Error destroying bean '" + beanName + "'", e);
            }
        }
        
        closed = true;
    }
    
    protected void invokeDestroyMethod(Object bean, BeanDefinition bd) {
        try {
            String destroyMethodName = bd.getDestroyMethodName();
            if (destroyMethodName != null && !destroyMethodName.isEmpty()) {
                Method destroyMethod = bd.getBeanClass().getDeclaredMethod(destroyMethodName);
                destroyMethod.setAccessible(true);
                destroyMethod.invoke(bean);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error invoking destroy method", e);
        }
    }

    private void invokeAwareMethods(String beanName, Object bean) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(this);
        }
    }

    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
    }

    private Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) {
        Object result = existingBean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object current = processor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                return null;
            }
            result = current;
        }
        return result;
    }

    private Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) {
        Object result = existingBean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return null;
            }
            result = current;
        }
        return result;
    }
} 