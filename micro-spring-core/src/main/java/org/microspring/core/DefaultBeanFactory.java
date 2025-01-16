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
import java.util.HashSet;

import org.microspring.core.io.XmlBeanDefinitionReader;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.exception.CircularDependencyException;
import org.microspring.core.aware.BeanNameAware;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.core.aware.BeanFactoryAware;
import org.microspring.core.exception.BeanCreationException;
import org.microspring.core.exception.NoSuchBeanDefinitionException;

public class DefaultBeanFactory implements BeanFactory {
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
    
    private boolean closed = false;
    
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public Object getBean(String name) {
        BeanDefinition bd = getBeanDefinition(name);
        if (bd == null) {
            throw new NoSuchBeanDefinitionException(name);
        }
        
        if (bd.isSingleton()) {
            // 对于单例，先检查缓存
            Object singleton = singletonObjects.get(name);
            if (singleton != null) {
                return singleton;
            }
            // 如果缓存中没有，创建并缓存
            singleton = createBean(name, bd);
            singletonObjects.put(name, singleton);
            return singleton;
        }
        
        // 非单例(原型)每次都创建新的
        return createBean(name, bd);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return doGetBean(name, requiredType);
    }

    @SuppressWarnings("unchecked")
    protected <T> T doGetBean(String name, Class<T> requiredType) {
        try {
            // 1. 先尝试从缓存获取(1级、2级、3级)
            Object bean = getSingleton(name, true);
            if (bean != null) {
                // 若 bean 已存在，则可进行类型检查
                if (requiredType != null && !requiredType.isInstance(bean)) {
                    System.err.println("[BeanCreationException] Bean [" + name + "] is not of required type " + requiredType.getName());
                    throw new BeanCreationException(name, 
                        "Bean is not of required type " + requiredType.getName());
                }
                return (T) bean;
            }

            // 2. beanDefinition
            BeanDefinition beanDefinition = getBeanDefinition(name);
            if (beanDefinition == null) {
                System.err.println("[NoSuchBeanDefinitionException] No bean named '" + name + "' is defined");
                throw new NoSuchBeanDefinitionException(name);
            }

            // 3. 创建Bean(单例或原型)
            if (beanDefinition.isSingleton()) {
                bean = createSingleton(name, () -> createBean(name, beanDefinition));
            } else {
                bean = createBean(name, beanDefinition);
            }

            // 4. 类型检查
            if (requiredType != null && !requiredType.isInstance(bean)) {
                System.err.println("[BeanCreationException] Bean [" + name + "] is not of required type " + requiredType.getName());
                throw new BeanCreationException(name, 
                    "Bean is not of required type " + requiredType.getName());
            }
            return (T) bean;
        } catch (CircularDependencyException e) {
            // 循环依赖异常直接抛出，不包装
            throw e;
        } catch (Exception e) {
            if (!(e instanceof BeanCreationException || e instanceof NoSuchBeanDefinitionException)) {
                System.err.println("[BeanCreationException] Failed to get bean [" + name + "]");
                throw new BeanCreationException(name, "Failed to get bean", e);
            }
            throw e;
        }
    }

    /**
     * 核心：创建Bean的完整流程
     */
    public Object createBean(String beanName, BeanDefinition bd) {
        singletonsCurrentlyInCreation.add(beanName);
        try {
            // 1. 实例化原始对象
            Object rawBean = createBeanInstance(bd);

            // 2. 如果是单例 -> 提前放到三级缓存(存一个 ObjectFactory)
            if (bd.isSingleton()) {
                this.singletonFactories.put(beanName, () -> {
                    return getEarlyBeanReference(beanName, bd, rawBean);
                });
            }

            // 3. 填充属性 (依赖注入)
            populateBean(rawBean, bd);

            // 4. 初始化 (BPP before -> aware -> initMethod)
            Object bean = initializeBean(beanName, rawBean, bd);

            // 5. 如果已经创建了代理对象（从二级缓存中获取），则使用该代理对象
            if (bd.isSingleton()) {
                Object earlySingletonReference = this.earlySingletonObjects.get(beanName);
                if (earlySingletonReference != null) {
                    bean = earlySingletonReference;
                }
                // 放入一级缓存
                this.singletonObjects.put(beanName, bean);
                // 干掉三级和二级
                this.singletonFactories.remove(beanName);
                this.earlySingletonObjects.remove(beanName);
            }

            // 6. afterInitialization => 可能返回新代理
            bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);

            // 7. 如果 afterInit 又返回新的代理，则覆盖进一级缓存
            if (bd.isSingleton()) {
                this.singletonObjects.put(beanName, bean);
            }

            return bean;
        } catch (BeanCreationException e) {
            // BeanCreationException 直接抛出，不再包装
            throw e;
        } catch (CircularDependencyException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("[BeanCreationException] Failed to create bean [" + beanName + "]");
            throw new BeanCreationException(beanName, "Creation failed", e);
        } finally {
            singletonsCurrentlyInCreation.remove(beanName);
        }
    }

    protected Object initializeBean(String beanName, Object bean, BeanDefinition bd) {
        // 1. BPP before
        Object result = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        if (result == null) {
            return null;
        }
        bean = result;

        // 2. aware + initMethod
        invokeAwareMethods(beanName, bean);
        invokeInitMethod(beanName, bean, bd);

        // 不在这里放入 singletonObjects, 已移动到 createBean(...) 里

        return bean;
    }

    /**
     * 返回"早期Bean引用" - 如果有 InstantiationAwareBeanPostProcessor，就可能返回代理
     */
    protected Object getEarlyBeanReference(String beanName, BeanDefinition bd, Object bean) {
        Object exposedObject = bean;
        for (BeanPostProcessor bp : beanPostProcessors) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                exposedObject = ((InstantiationAwareBeanPostProcessor) bp)
                    .getEarlyBeanReference(exposedObject, beanName);
            }
        }
        return exposedObject;
    }

    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        Object bean = this.singletonObjects.get(beanName);
        if (bean == null && isInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                bean = this.earlySingletonObjects.get(beanName);
                if (bean == null && allowEarlyReference) {
                    ObjectFactory<?> factory = this.singletonFactories.get(beanName);
                    if (factory != null) {
                        bean = factory.getObject(); // => 可能是 代理
                        this.earlySingletonObjects.put(beanName, bean);
                        this.singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return bean;
    }

    protected void invokeInitMethod(String beanName, Object bean, BeanDefinition bd) {
        String initMethodName = bd.getInitMethodName();
        if (initMethodName != null && !initMethodName.isEmpty()) {
            try {
                Method initMethod = bd.getBeanClass().getDeclaredMethod(initMethodName);
                initMethod.setAccessible(true);
                initMethod.invoke(bean);
            } catch (NoSuchMethodException e) {
                System.err.println("[BeanCreationException] Init method [" + initMethodName + "] not found for bean [" + beanName + "]");
                throw new BeanCreationException(beanName, 
                    "Init method [" + initMethodName + "] not found");
            } catch (Exception e) {
                System.err.println("[BeanCreationException] Failed to invoke init method for bean [" + beanName + "]");
                throw new BeanCreationException(beanName, 
                    "Failed to invoke init method [" + initMethodName + "]", e);
            }
        }
    }
    
    public Map<String, Object> getSingletonObjects() {
        return this.singletonObjects;
    }

    public Map<String, Object> getEarlySingletonObjects() {
        return this.earlySingletonObjects;
    }

    public Map<String, ObjectFactory<?>> getSingletonFactories() {
        return this.singletonFactories;
    }

    protected void populateBean(Object bean, BeanDefinition bd) throws Exception {
        // 检查 prototype 作用域的循环依赖
        if (!bd.isSingleton()) {
            for (PropertyValue pv : bd.getPropertyValues()) {
                if (pv.isRef() && isInCreation(pv.getRef())) {
                    System.err.println("[CircularDependencyException] Cannot resolve circular reference between prototype beans");
                    throw new CircularDependencyException(
                        "Cannot resolve circular reference between prototype beans: " + 
                        bd.getBeanClass().getSimpleName() + " and " + pv.getRef());
                }
            }
        }

        // 1. 处理 PropertyValue 注入
        for (PropertyValue pv : bd.getPropertyValues()) {
            Field field = bean.getClass().getDeclaredField(pv.getName());
            field.setAccessible(true);
            
            Object value;
            if (pv.isRef()) {
                String refName = pv.getRef();
                try {
                    value = doGetBean(refName, null);
                } catch (Exception e) {
                    if (e instanceof CircularDependencyException) {
                        throw e;
                    }
                    throw new CircularDependencyException(
                        "Error injecting reference '" + refName + "'", e);
                }
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
                
                try {
                    if (qualifier != null) {
                        value = getBean(qualifier.value());
                    } else {
                        value = getBean(field.getType());
                    }
                    field.set(bean, value);
                } catch (Exception e) {
                    if (e instanceof CircularDependencyException) {
                        throw e;
                    }
                    throw new CircularDependencyException(
                        "Error autowiring field '" + field.getName() + "'", e);
                }
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
        
        // 如果有构造器参数
        if (!bd.getConstructorArgs().isEmpty()) {
            // 检查构造器循环依赖
            for (ConstructorArg arg : bd.getConstructorArgs()) {
                if (arg.isRef() && isInCreation(arg.getRef())) {
                    System.err.println("[CircularDependencyException] Circular dependency detected through constructor argument: " + arg.getRef());
                    throw new CircularDependencyException(
                        "Circular dependency detected through constructor argument: " + arg.getRef());
                }
            }
            
            // 获取构造器参数
            List<ConstructorArg> constructorArgs = bd.getConstructorArgs();
            Object[] args = new Object[constructorArgs.size()];
            
            for (int i = 0; i < constructorArgs.size(); i++) {
                ConstructorArg constructorArg = constructorArgs.get(i);
                if (constructorArg.isRef()) {
                    args[i] = getBean(constructorArg.getRef());
                } else {
                    args[i] = constructorArg.getValue();
                }
            }
            
            try {
                // 查找匹配的构造器
                Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterCount() == args.length) {
                        try {
                            constructor.setAccessible(true);
                            return constructor.newInstance(args);
                        } catch (Exception e) {
                            // 如果参数类型不匹配，继续尝试下一个构造器
                            continue;
                        }
                    }
                }
                throw new CircularDependencyException(
                    "No suitable constructor found for " + beanClass.getName());
            } catch (SecurityException e) {
                throw new CircularDependencyException(
                    "Error creating instance of " + beanClass.getName(), e);
            }
        } else {
            // XML 中没有配置构造器参数，但类可能有带参构造器
            Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
            if (constructors.length == 1) {
                Constructor<?> constructor = constructors[0];
                if (constructor.getParameterCount() > 0) {
                    // 有参数的构造器
                    constructor.setAccessible(true);
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        // 尝试获取依赖的 bean
                        args[i] = getBean(paramTypes[i]);
                    }
                    return constructor.newInstance(args);
                }
            }
            
            // 尝试使用无参构造器
            try {
                Constructor<?> constructor = beanClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new CircularDependencyException(
                    "No default constructor found for " + beanClass.getName(), e);
            }
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
            } catch (BeanCreationException e) {
                // BeanCreationException 直接抛出，不再包装
                throw e;
            } catch (Exception e) {
                System.err.println("[BeanCreationException] Failed to destroy bean [" + beanName + "]");
                throw new BeanCreationException(beanName, "Failed to destroy bean", e);
            }
        }
        
        closed = true;
    }
    
    protected void invokeDestroyMethod(Object bean, BeanDefinition bd) {
        String destroyMethodName = bd.getDestroyMethodName();
        if (destroyMethodName != null && !destroyMethodName.isEmpty()) {
            try {
                Method destroyMethod = bd.getBeanClass().getDeclaredMethod(destroyMethodName);
                destroyMethod.setAccessible(true);
                destroyMethod.invoke(bean);
            } catch (NoSuchMethodException e) {
                System.err.println("[BeanCreationException] Destroy method [" + destroyMethodName + "] not found");
                throw new BeanCreationException(bd.getBeanClass().getSimpleName(), 
                    "Destroy method [" + destroyMethodName + "] not found");
            } catch (Exception e) {
                System.err.println("[BeanCreationException] Failed to invoke destroy method");
                throw new BeanCreationException(bd.getBeanClass().getSimpleName(), 
                    "Failed to invoke destroy method [" + destroyMethodName + "]", e);
            }
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

    protected boolean isInCreation(String beanName) {
        return singletonsCurrentlyInCreation.contains(beanName);
    }

    protected Object createSingleton(String beanName, ObjectFactory<?> factory) {
        synchronized (this.singletonObjects) {
            if (!this.singletonObjects.containsKey(beanName)) {
                Object singleton = factory.getObject();
                this.singletonObjects.put(beanName, singleton);
                return singleton;
            }
            return this.singletonObjects.get(beanName);
        }
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            Class<?> beanClass = entry.getValue().getBeanClass();
            if (beanClass.isAnnotationPresent(annotationType)) {
                result.add(entry.getKey());
            }
        }
        return result.toArray(new String[0]);
    }

    public boolean containsSingleton(String name) {
        return singletonObjects.containsKey(name);
    }
} 