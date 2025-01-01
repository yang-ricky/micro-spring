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

import org.microspring.core.io.BeanDefinitionHolder;
import org.microspring.core.io.XmlBeanDefinitionReader;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.aware.BeanNameAware;
import org.microspring.beans.factory.annotation.Autowired;

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
        try {
            // 1. 处理构造器注入
            Object bean;
            List<ConstructorArg> constructorArgs = bd.getConstructorArgs();
            if (!constructorArgs.isEmpty()) {
                Class<?>[] paramTypes = new Class<?>[constructorArgs.size()];
                Object[] paramValues = new Object[constructorArgs.size()];
                for (int i = 0; i < constructorArgs.size(); i++) {
                    ConstructorArg arg = constructorArgs.get(i);
                    paramTypes[i] = arg.getType();
                    paramValues[i] = arg.isRef() ? 
                        getBean(arg.getRef()) : arg.getValue();
                }
                Constructor<?> constructor = bd.getBeanClass().getConstructor(paramTypes);
                bean = constructor.newInstance(paramValues);
            } else {
                bean = bd.getBeanClass().newInstance();
            }

            // 2. 处理属性注入
            for (PropertyValue pv : bd.getPropertyValues()) {
                String name = pv.getName();
                Object value = pv.getValue();
                
                if (value instanceof Map) {
                    value = handleMapValue((Map<?, ?>)value);
                } else if (value instanceof List) {
                    value = handleListValue((List<?>)value);
                } else if (pv.isRef()) {
                    value = getBean((String)value);
                }
                
                Field field = bd.getBeanClass().getDeclaredField(name);
                field.setAccessible(true);
                field.set(bean, value);
            }
            
            // 3. 处理@Autowired注解
            for (Field field : bd.getBeanClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    Object value = getBean(field.getType());
                    field.set(bean, value);
                }
            }
            
            // 4. 处理Aware回调
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware)bean).setBeanName(beanName);
            }
            
            // 5. 调用初始化方法
            String initMethodName = bd.getInitMethodName();
            if (initMethodName != null && !initMethodName.isEmpty()) {
                Method initMethod = bd.getBeanClass().getDeclaredMethod(initMethodName);
                initMethod.invoke(bean);
            }
            
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Error creating bean: " + beanName, e);
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
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader();
        List<BeanDefinitionHolder> holders = reader.loadBeanDefinitions(xmlPath);
        for (BeanDefinitionHolder holder : holders) {
            registerBeanDefinition(holder.getBeanName(), holder.getBeanDefinition());
        }
    }

    private Object createBeanInstance(BeanDefinition bd) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        List<ConstructorArg> constructorArgs = bd.getConstructorArgs();
        
        if (constructorArgs.isEmpty()) {
            return beanClass.getDeclaredConstructor().newInstance();
        }
        
        // 处理构造器参数
        Class<?>[] paramTypes = new Class<?>[constructorArgs.size()];
        Object[] paramValues = new Object[constructorArgs.size()];
        
        for (int i = 0; i < constructorArgs.size(); i++) {
            ConstructorArg arg = constructorArgs.get(i);
            paramTypes[i] = arg.getType();
            paramValues[i] = arg.isRef() ? getBean(arg.getRef()) : arg.getValue();
        }
        
        Constructor<?> constructor = beanClass.getDeclaredConstructor(paramTypes);
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

    public <T> T getBean(Class<T> requiredType) {
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition bd = beanDefinitionMap.get(beanName);
            if (requiredType.isAssignableFrom(bd.getBeanClass())) {
                return (T) getBean(beanName);
            }
        }
        throw new RuntimeException("No bean of type '" + requiredType.getName() + "' is defined");
    }
} 