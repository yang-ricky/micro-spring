package org.microspring.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import org.microspring.core.io.BeanDefinitionHolder;
import org.microspring.core.io.XmlBeanDefinitionReader;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.aware.BeanNameAware;

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
    
    private Object createBean(String beanName, BeanDefinition bd) {
        try {
            Object instance = createBeanInstance(bd);
            
            // 处理属性注入
            List<PropertyValue> propertyValues = bd.getPropertyValues();
            if (propertyValues != null) {
                for (PropertyValue pv : propertyValues) {
                    String propertyName = pv.getName();
                    Object value;
                    
                    if (pv.isRef()) {
                        value = getBean(pv.getRef());
                    } else {
                        Object rawValue = pv.getValue();
                        // 处理复杂类型
                        if (rawValue instanceof List) {
                            value = handleListValue((List<?>) rawValue);
                        } else if (rawValue instanceof Map) {
                            value = handleMapValue((Map<?, ?>) rawValue);
                        } else {
                            value = rawValue;
                        }
                    }
                    
                    if (value != null) {
                        String methodName = "set" + propertyName.substring(0, 1).toUpperCase() 
                            + propertyName.substring(1);
                        try {
                            // 获取setter方法的参数类型
                            Method[] methods = bd.getBeanClass().getMethods();
                            Method setter = null;
                            for (Method method : methods) {
                                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                                    setter = method;
                                    break;
                                }
                            }
                            
                            if (setter != null) {
                                // 如果需要类型转换，在这里处理
                                Class<?> paramType = setter.getParameterTypes()[0];
                                if (value instanceof List && paramType == List.class) {
                                    setter.invoke(instance, value);
                                } else if (value instanceof Map && paramType == Map.class) {
                                    setter.invoke(instance, value);
                                } else {
                                    setter.invoke(instance, value);
                                }
                            } else {
                                throw new RuntimeException("No setter method found for property: " + propertyName);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Error setting property '" + propertyName + "' for bean: " + beanName, e);
                        }
                    }
                }
            }
            
            // 处理Aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }
            
            return instance;
            
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
} 