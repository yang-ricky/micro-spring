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

import org.microspring.core.io.BeanDefinitionHolder;
import org.microspring.core.io.XmlBeanDefinitionReader;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.aware.BeanNameAware;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

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
            Object bean = createBeanInstance(bd);
            
            // 处理字段注入
            populateBean(bean, bd);
            
            // 处理 Aware 回调
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(beanName);
            }
            
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Error creating bean: " + beanName, e);
        }
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
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader();
        List<BeanDefinitionHolder> holders = reader.loadBeanDefinitions(xmlPath);
        for (BeanDefinitionHolder holder : holders) {
            registerBeanDefinition(holder.getBeanName(), holder.getBeanDefinition());
        }
    }

    protected Object createBeanInstance(BeanDefinition bd) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        List<ConstructorArg> args = bd.getConstructorArgs();
        
        // 1. 如果有构造器参数，使用带参数的构造器
        if (!args.isEmpty()) {
            Class<?>[] paramTypes = new Class<?>[args.size()];
            Object[] paramValues = new Object[args.size()];
            
            for (int i = 0; i < args.size(); i++) {
                ConstructorArg arg = args.get(i);
                paramTypes[i] = arg.getType();
                paramValues[i] = arg.isRef() ? getBean(arg.getRef()) : arg.getValue();
            }
            
            Constructor<?> ctor = beanClass.getDeclaredConstructor(paramTypes);
            return ctor.newInstance(paramValues);
        }
        
        // 2. 查找带有@Autowired注解的构造器
        for (Constructor<?> ctor : beanClass.getDeclaredConstructors()) {
            if (ctor.isAnnotationPresent(Autowired.class)) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] paramValues = new Object[paramTypes.length];
                
                // 获取构造器参数的限定符
                Annotation[][] paramAnnotations = ctor.getParameterAnnotations();
                
                for (int i = 0; i < paramTypes.length; i++) {
                    String qualifier = null;
                    for (Annotation annotation : paramAnnotations[i]) {
                        if (annotation instanceof Qualifier) {
                            qualifier = ((Qualifier) annotation).value();
                            break;
                        }
                    }
                    
                    // 如果有@Qualifier注解，使用指定名称获取bean
                    if (qualifier != null) {
                        paramValues[i] = getBean(qualifier);
                    } else {
                        paramValues[i] = getBean(paramTypes[i]);
                    }
                }
                
                return ctor.newInstance(paramValues);
            }
        }
        
        // 3. 如果没有构造器参数和@Autowired注解，使用默认构造器
        return beanClass.getDeclaredConstructor().newInstance();
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
} 