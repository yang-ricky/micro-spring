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
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import javax.annotation.Resource;

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
import org.microspring.beans.factory.annotation.Value;
import org.microspring.beans.factory.FactoryBean;
import org.microspring.core.env.Environment;

public class DefaultBeanFactory implements BeanFactory {
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
    
    private boolean closed = false;
    
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    
    private Environment environment;

    public void removeBeanDefinition(String beanName) {
        this.beanDefinitionMap.remove(beanName);
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

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
    protected <T> T doGetBean(String name, Class<T> requiredType){
        try {
            // 1. 先尝试从缓存获取(1级、2级、3级)
            Object bean = getSingleton(name, true);
            if (bean != null) {
                // 若 bean 已存在，则可进行类型检查
                if (bean instanceof FactoryBean) {
                    try {
                        FactoryBean<?> factoryBean = (FactoryBean<?>) bean;
                        bean = factoryBean.getObject();
                    } catch (Exception e) {
                        throw new BeanCreationException(name, "FactoryBean threw exception on object creation", e);
                    }
                }
                
                if (requiredType != null && !requiredType.isInstance(bean)) {
                    throw new BeanCreationException(name, 
                        "Bean is not of required type " + requiredType.getName());
                }
                return (T) bean;
            }

            // 2. beanDefinition
            BeanDefinition beanDefinition = getBeanDefinition(name);
            if (beanDefinition == null) {
                throw new NoSuchBeanDefinitionException(name);
            }

            // 3. 创建Bean(单例或原型)
            if (beanDefinition.isSingleton()) {
                bean = createSingleton(name, () -> createBean(name, beanDefinition));
            } else {
                bean = createBean(name, beanDefinition);
            }

            // 4. 处理 FactoryBean
            if (bean instanceof FactoryBean) {
                try {
                    FactoryBean<?> factoryBean = (FactoryBean<?>) bean;
                    bean = factoryBean.getObject();
                } catch (Exception e) {
                    throw new BeanCreationException(name, "FactoryBean threw exception on object creation", e);
                }
            }

            // 5. 类型检查
            if (requiredType != null && !requiredType.isInstance(bean)) {
                throw new BeanCreationException(name, 
                    "Bean is not of required type " + requiredType.getName());
            }
            return (T) bean;
        } catch (CircularDependencyException e) {
            // 循环依赖异常直接抛出，不包装
            throw e;
        } catch (Exception e) {
            if (!(e instanceof BeanCreationException || e instanceof NoSuchBeanDefinitionException)) {
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
            Object rawBean = createBeanInstance(beanName, bd);

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
            throw e;
        } catch (CircularDependencyException e) {
            throw e;
        } catch (Exception e) {
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
        // 1. 先从一级缓存找
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                // 2. 从二级缓存找
                singletonObject = this.earlySingletonObjects.get(beanName);
                if (singletonObject == null && allowEarlyReference) {
                    // 3. 从三级缓存找
                    ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        // 从三级缓存获取，然后放入二级缓存
                        singletonObject = singletonFactory.getObject();
                        this.earlySingletonObjects.put(beanName, singletonObject);
                        this.singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return singletonObject;
    }

    private void invokeInitMethod(String beanName, Object bean, BeanDefinition bd) {
        String initMethodName = bd.getInitMethodName();
        if (initMethodName != null && !initMethodName.isEmpty()) {
            try {
                Method initMethod = bd.getBeanClass().getDeclaredMethod(initMethodName);
                initMethod.setAccessible(true);
                initMethod.invoke(bean);
            } catch (NoSuchMethodException e) {
                throw new BeanCreationException(beanName, 
                    "Init method [" + initMethodName + "] not found");
            } catch (Exception e) {
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
                if (pv.isRef()) {
                    Object ref = pv.getRef();
                    if (ref instanceof String && isInCreation((String) ref)) {
                        throw new CircularDependencyException(
                            "Cannot resolve circular reference between prototype beans: " + 
                            bd.getBeanClass().getSimpleName() + " and " + ref);
                    }
                }
            }
        }

        // 1. 处理 PropertyValue 注入
        for (PropertyValue pv : bd.getPropertyValues()) {
            Field field = bean.getClass().getDeclaredField(pv.getName());
            field.setAccessible(true);
                
            Object value;
            if (pv.isRef()) {
                Object ref = pv.getRef();
                if (ref instanceof String) {
                    // 处理单个引用
                    String refName = (String) ref;
                    try {
                        value = getSingleton(refName, true);
                        if (value == null) {
                            value = doGetBean(refName, null);
                        }
                        
                        // 处理 FactoryBean (为了MyBatis)
                        if (value instanceof FactoryBean) {
                            try {
                                value = ((FactoryBean<?>) value).getObject();
                            } catch (Exception e) {
                                throw new BeanCreationException(refName, "Failed to get object from FactoryBean", e);
                            }
                        }
                    } catch (Exception e) {
                        if (e instanceof CircularDependencyException) {
                            throw e;
                        }
                        throw new CircularDependencyException(
                            "Error injecting reference '" + refName + "'", e);
                    }
                } else if (ref instanceof List) {
                    // 处理List类型的引用
                    List<?> refList = (List<?>) ref;
                    List<Object> resolvedList = new ArrayList<>();
                    for (Object item : refList) {
                        if (item instanceof String) {
                            String refName = (String) item;
                            Object refValue = getSingleton(refName, true);
                            if (refValue == null) {
                                refValue = doGetBean(refName, null);
                            }
                            resolvedList.add(refValue);
                        } else {
                            resolvedList.add(item);
                        }
                    }
                    value = resolvedList;
                } else if (ref instanceof Map) {
                    // 处理Map类型的引用
                    Map<?, ?> refMap = (Map<?, ?>) ref;
                    Map<Object, Object> resolvedMap = new HashMap<>();
                    for (Map.Entry<?, ?> entry : refMap.entrySet()) {
                        Object key = entry.getKey();
                        Object val = entry.getValue();
                        if (val instanceof String) {
                            String refName = (String) val;
                            Object refValue = getSingleton(refName, true);
                            if (refValue == null) {
                                refValue = doGetBean(refName, null);
                            }
                            resolvedMap.put(key, refValue);
                        } else {
                            resolvedMap.put(key, val);
                        }
                    }
                    value = resolvedMap;
                } else {
                    value = ref;
                }
            } else {
                value = pv.getValue();
            }
                        
            field.set(bean, value);
                }
                
        // 2. 处理 @Autowired 注解的字段注入
        for (Field field : bd.getBeanClass().getDeclaredFields()) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                field.setAccessible(true);
                
                Class<?> fieldType = field.getType();
                
                // 处理集合类型
                if (List.class.isAssignableFrom(fieldType)) {
                    // 获取List的泛型类型
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        Class<?> elementType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                        List<Object> matchingBeans = getBeansByType(elementType);
                        field.set(bean, matchingBeans);
                    } else {
                        // 如果没有泛型参数，设置空列表
                        field.set(bean, new ArrayList<>());
                    }
                    continue;
                }
                
                // 处理Map类型
                if (Map.class.isAssignableFrom(fieldType)) {
                    // 获取Map的泛型类型
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType paramType = (ParameterizedType) genericType;
                        Type[] typeArgs = paramType.getActualTypeArguments();
                        // 只处理 Map<String, SomeType> 类型
                        if (typeArgs[0] == String.class) {
                            Class<?> valueType = (Class<?>) typeArgs[1];
                            Map<String, Object> matchingBeans = new HashMap<>();
                            // 获取所有匹配类型的bean
                            for (String name : getBeanDefinitionNames()) {
                                BeanDefinition beanDef = getBeanDefinition(name);
                                if (valueType.isAssignableFrom(beanDef.getBeanClass())) {
                                    matchingBeans.put(name, getBean(name));
                                }
                            }
                            field.set(bean, matchingBeans);
                        }
                    } else {
                        // 如果没有泛型参数，设置空Map
                        field.set(bean, new HashMap<>());
                    }
                    continue;
            }
                
                // 处理普通类型
                String refName;
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                if (qualifier != null) {
                    refName = qualifier.value();
                } else {
                    // 使用字段类型作为依赖的bean名称
                    refName = Character.toLowerCase(fieldType.getSimpleName().charAt(0)) 
                             + fieldType.getSimpleName().substring(1);
        }

                            try {
                    // 先尝试从缓存中获取
                    Object value = getSingleton(refName, true);
                    if (value == null) {
                        value = doGetBean(refName, fieldType);
                    }
                    field.set(bean, value);
                            } catch (Exception e) {
                    if (e instanceof CircularDependencyException) {
                        throw e;
                    }
                        throw new CircularDependencyException(
                        "Error injecting autowired field '" + field.getName() + "'", e);
                }
                            }
                        }

        // 2.1 处理 @Resource 注解的字段注入
        for (Field field : bd.getBeanClass().getDeclaredFields()) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                
                // 处理集合类型
                if (List.class.isAssignableFrom(fieldType)) {
                    // 获取List的泛型类型
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        Class<?> elementType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                        List<Object> matchingBeans = getBeansByType(elementType);
                        field.set(bean, matchingBeans);
                    } else {
                        // 如果没有泛型参数，设置空列表
                        field.set(bean, new ArrayList<>());
                    }
                    continue;
                }
                
                if (Map.class.isAssignableFrom(fieldType)) {
                    // 获取Map的泛型类型
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType paramType = (ParameterizedType) genericType;
                        Type[] typeArgs = paramType.getActualTypeArguments();
                        // 只处理 Map<String, SomeType> 类型
                        if (typeArgs[0] == String.class) {
                            Class<?> valueType = (Class<?>) typeArgs[1];
                            Map<String, Object> matchingBeans = new HashMap<>();
                            // 获取所有匹配类型的bean
                            for (String name : getBeanDefinitionNames()) {
                                BeanDefinition beanDef = getBeanDefinition(name);
                                if (valueType.isAssignableFrom(beanDef.getBeanClass())) {
                                    matchingBeans.put(name, getBean(name));
                                }
                            }
                            field.set(bean, matchingBeans);
                            }
                    } else {
                        // 如果没有泛型参数，设置空Map
                        field.set(bean, new HashMap<>());
                    }
                    continue;
                }
                
                // 处理普通类型
                // 1. 首先按name查找
                String refName = resource.name();
                if (refName.isEmpty()) {
                    // 如果没有指定name，使用字段名
                    refName = field.getName();
                }
                
                try {
                    Object value = null;
                    // 先尝试按名称查找
                    if (containsBean(refName)) {
                        value = getSingleton(refName, true);
                        if (value == null) {
                            value = doGetBean(refName, fieldType);
                        }
                    }
                    
                    // 2. 如果按名称没找到，降级为按类型查找
                    if (value == null) {
                        value = getBean(fieldType);
                    }
                    
                    field.set(bean, value);
                    } catch (Exception e) {
                        if (e instanceof CircularDependencyException) {
                            throw e;
                        }
                        throw new CircularDependencyException(
                        "Error injecting resource field '" + field.getName() + "'", e);
                }
            }
        }

        // 3. 处理方法注入
        for (Method method : bd.getBeanClass().getDeclaredMethods()) {
            // 处理 @Resource 注解
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null && method.getParameterCount() == 1) {
                method.setAccessible(true);
                Class<?> paramType = method.getParameterTypes()[0];
                
                // 处理集合类型
                if (List.class.isAssignableFrom(paramType)) {
                    // 获取泛型类型
                    Type genericType = method.getGenericParameterTypes()[0];
                    if (genericType instanceof ParameterizedType) {
                        Type elementType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                        if (elementType instanceof Class) {
                            List<Object> matchingBeans = getBeansByType((Class<?>) elementType);
                            method.invoke(bean, matchingBeans);
                    }
                } else {
                        // 如果没有泛型参数，注入空列表
                        method.invoke(bean, new ArrayList<>());
                    }
                    continue;
                }
                
                if (Map.class.isAssignableFrom(paramType)) {
                    // 获取泛型类型
                    Type genericType = method.getGenericParameterTypes()[0];
                    if (genericType instanceof ParameterizedType) {
                        Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                        if (typeArgs.length == 2 && typeArgs[0] == String.class && typeArgs[1] instanceof Class) {
                            Class<?> valueType = (Class<?>) typeArgs[1];
                            Map<String, Object> matchingBeans = new HashMap<>();
                            for (String name : getBeanDefinitionNames()) {
                                BeanDefinition beanDef = getBeanDefinition(name);
                                if (valueType.isAssignableFrom(beanDef.getBeanClass())) {
                                    matchingBeans.put(name, getBean(name));
                                }
                            }
                            method.invoke(bean, matchingBeans);
                }
            } else {
                        // 如果没有泛型参数，注入空Map
                        method.invoke(bean, new HashMap<>());
                    }
                    continue;
            }
                
                // 处理普通类型
                // 1. 首先按name查找
                String refName = resource.name();
                if (refName.isEmpty()) {
                    // 如果没有指定name，使用方法名去掉"set"后的部分
                    if (method.getName().startsWith("set")) {
                        refName = method.getName().substring(3);
                        refName = Character.toLowerCase(refName.charAt(0)) + refName.substring(1);
                    } else {
                        // 如果不是setter方法，使用参数类型的首字母小写作为bean名称
                        refName = Character.toLowerCase(paramType.getSimpleName().charAt(0)) + 
                                 paramType.getSimpleName().substring(1);
                    }
                }
                
                try {
                    Object value = null;
                    // 先尝试按名称查找
                    if (containsBean(refName)) {
                        value = getSingleton(refName, true);
                        if (value == null) {
                            value = doGetBean(refName, paramType);
                        }
                    }
                    
                    // 2. 如果按名称没找到，降级为按类型查找
                    if (value == null) {
                        value = getBean(paramType);
                    }
                    
                    method.invoke(bean, value);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject resource method: " + method.getName(), e);
                }
            }
        }

        // 4. 处理 @Value 注解
        for (Method method : bd.getBeanClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Value.class)) {
                Value valueAnnotation = method.getAnnotation(Value.class);
                String value = valueAnnotation.value();
                
                // 确保是setter方法
                if (method.getName().startsWith("set") && 
                    method.getParameterCount() == 1 && 
                    method.getReturnType() == void.class) {
                    
                    method.setAccessible(true);
                    Class<?> paramType = method.getParameterTypes()[0];
                    Object convertedValue = convertValue(value, paramType);
                    method.invoke(bean, convertedValue);
                }
            }
        }
    }

    public void loadBeanDefinitions(String xmlPath) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);
        reader.loadBeanDefinitions(xmlPath);
    }

    protected Object createBeanInstance(String targetBeanName, BeanDefinition bd) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        
        // 1. 如果有工厂方法，优先使用工厂方法创建
        Method factoryMethod = bd.getFactoryMethod();
        if (factoryMethod != null) {
            try {
                Class<?> factoryBeanClass = bd.getFactoryBeanClass();
                
                // 获取或创建工厂bean实例
                Object factoryBean = getBean(factoryBeanClass);

                // 处理工厂方法的参数
                if (factoryMethod.getParameterCount() > 0) {
                    List<ConstructorArg> constructorArgs = bd.getConstructorArgs();
                    Object[] args = new Object[factoryMethod.getParameterCount()];
                    
                    for (int i = 0; i < args.length; i++) {
                        ConstructorArg arg = constructorArgs.get(i);
                        if (arg.isRef()) {
                            // 如果是引用类型，从容器中获取bean
                            args[i] = getBean((String)arg.getRef());
                        } else {
                            // 如果是值类型，进行类型转换
                            Object value = arg.getValue();
                            if (value != null) {
                                Class<?> paramType = factoryMethod.getParameterTypes()[i];
                                args[i] = convertValue((String)value, paramType);
                            } else {
                                Class<?> paramType = factoryMethod.getParameterTypes()[i];
                                if (paramType == String.class) {
                                    args[i] = "";  // 默认空字符串
                                } else if (paramType == Integer.class || paramType == int.class) {
                                    args[i] = 0;   // 默认0
                                } else if (paramType == Long.class || paramType == long.class) {
                                    args[i] = 0L;  // 默认0L
                                } else if (paramType == Double.class || paramType == double.class) {
                                    args[i] = 0.0; // 默认0.0
                                } else if (paramType == Boolean.class || paramType == boolean.class) {
                                    args[i] = false; // 默认false
                                }
                            }
                        }
                    }
                    return factoryMethod.invoke(factoryBean, args);
                } else {
                    return factoryMethod.invoke(factoryBean);
                }
            } catch (Exception e) {
                throw new BeanCreationException(beanClass.getName(), 
                    "Failed to invoke factory method [" + factoryMethod.getName() + "]", e);
            }
        }
        
        // 2. 如果没有工厂方法，使用构造函数创建
        List<ConstructorArg> constructorArgs = bd.getConstructorArgs();
        
        // 获取所有构造函数
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        
        // 首先尝试找到带有 @Autowired 注解的构造函数
        Constructor<?> autowiredConstructor = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                autowiredConstructor = constructor;
                break;
            }
        }
        
        // 如果找到了带 @Autowired 的构造函数，使用它
        if (autowiredConstructor != null) {

            Class<?>[] paramTypes = autowiredConstructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            
            // 获取构造函数的参数
            Parameter[] parameters = autowiredConstructor.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Class<?> paramType = paramTypes[i];
                
                // 检查是否有 @Qualifier 注解
                Qualifier qualifier = param.getAnnotation(Qualifier.class);
                Object argValue;
                
                if (List.class.isAssignableFrom(paramType)) {
                    // 处理 List 类型参数
                    Type paramGenericType = param.getParameterizedType();
                    if (paramGenericType instanceof ParameterizedType) {
                        Type elementType = ((ParameterizedType) paramGenericType).getActualTypeArguments()[0];
                        if (elementType instanceof Class) {
                            argValue = getBeansByType((Class<?>) elementType);
                        } else {
                            argValue = new ArrayList<>();
                        }
                    } else {
                        argValue = new ArrayList<>();
                    }
                } else if (Map.class.isAssignableFrom(paramType)) {
                    // 处理 Map 类型参数
                    Type paramGenericType = param.getParameterizedType();
                    if (paramGenericType instanceof ParameterizedType) {
                        Type[] typeArgs = ((ParameterizedType) paramGenericType).getActualTypeArguments();
                        if (typeArgs.length == 2 && typeArgs[0] == String.class && typeArgs[1] instanceof Class) {
                            Class<?> valueType = (Class<?>) typeArgs[1];
                            Map<String, Object> matchingBeans = new HashMap<>();
                            for (String name : getBeanDefinitionNames()) {
                                BeanDefinition beanDef = getBeanDefinition(name);
                                if (valueType.isAssignableFrom(beanDef.getBeanClass())) {
                                    matchingBeans.put(name, getBean(name));
                                }
                            }
                            argValue = matchingBeans;
                        } else {
                            argValue = new HashMap<>();
                        }
                    } else {
                        argValue = new HashMap<>();
                    }
                } else if (qualifier != null) {
                    // 如果有@Qualifier，按名称查找
                    String beanName = qualifier.value();
                    argValue = getBean(beanName);
                } else {
                    // 如果没有@Qualifier，按类型查找
                    try {
                        argValue = getBean(paramType);
                    } catch (RuntimeException e) {
                        // 如果按类型查找失败，尝试使用默认的命名规则
                        String defaultBeanName = Character.toLowerCase(paramType.getSimpleName().charAt(0)) + 
                                               paramType.getSimpleName().substring(1);
                        argValue = getBean(defaultBeanName);
                    }
                }
                args[i] = argValue;
            }
            
            autowiredConstructor.setAccessible(true);
            return autowiredConstructor.newInstance(args);
        }
        
        // 如果没有找到 @Autowired 构造函数，但有构造函数参数，使用匹配的构造函数
        if (!constructorArgs.isEmpty()) {
            // 检查构造器循环依赖
            for (ConstructorArg arg : constructorArgs) {
                if (arg.isRef() && isInCreation(arg.getRef())) {
                    System.err.println("[CircularDependencyException] Circular dependency detected through constructor argument: " + arg.getRef());
                    throw new CircularDependencyException(
                        "Circular dependency detected through constructor argument: " + arg.getRef());
                }
            }
            
            // 获取构造器参数
            Object[] args = new Object[constructorArgs.size()];
            
            for (int i = 0; i < constructorArgs.size(); i++) {
                ConstructorArg constructorArg = constructorArgs.get(i);
                if (constructorArg.isRef()) {
                    args[i] = getBean(constructorArg.getRef());
                } else {
                    // 如果是值类型，直接使用值
                    Object value = constructorArg.getValue();
                    if (value != null) {
                        args[i] = value;
                    } else {
                        // 只有在没有显式设置值且不是引用的情况下才使用默认值
                        Constructor<?> constructor = constructors[0];
                        Class<?> paramType = constructor.getParameterTypes()[i];
                        args[i] = getDefaultValue(paramType);
                    }
                }
            }
            
            try {
                // 查找匹配的构造器
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterCount() == args.length) {
                        Class<?>[] paramTypes = constructor.getParameterTypes();
                        boolean matches = true;
                        for (int i = 0; i < paramTypes.length; i++) {
                            Object arg = args[i];
                            if (arg != null) {
                                Class<?> paramType = paramTypes[i];
                                Class<?> argType = arg.getClass();
                                
                                // 处理基本类型和包装类型的匹配
                                if (paramType.isPrimitive()) {
                                    // 如果参数是基本类型，检查arg是否是对应的包装类型
                                    if (!isAssignableFromPrimitive(argType, paramType)) {
                                        matches = false;
                                        break;
                                    }
                                } else if (argType.isPrimitive()) {
                                    // 如果arg是基本类型，检查参数是否是对应的包装类型
                                    if (!isAssignableFromPrimitive(paramType, argType)) {
                                        matches = false;
                                        break;
                                    }
                                } else if (!paramType.isAssignableFrom(argType)) {
                                    // 尝试类型转换
                                    if (paramType == String.class) {
                                        args[i] = arg.toString();
                                    } else if ((paramType == Integer.class || paramType == int.class) && arg instanceof String) {
                                        args[i] = Integer.parseInt((String) arg);
                                    } else if ((paramType == Long.class || paramType == long.class) && arg instanceof String) {
                                        args[i] = Long.parseLong((String) arg);
                                    } else if ((paramType == Double.class || paramType == double.class) && arg instanceof String) {
                                        args[i] = Double.parseDouble((String) arg);
                                    } else if ((paramType == Boolean.class || paramType == boolean.class) && arg instanceof String) {
                                        args[i] = Boolean.parseBoolean((String) arg);
                                    } else {
                                        matches = false;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (matches) {
                            constructor.setAccessible(true);
                            try {
                                return constructor.newInstance(args);
                            } catch (Exception e) {
                                throw e;
                            }
                        }
                    }
                }
                throw new CircularDependencyException(
                    "No suitable constructor found for " + beanClass.getName());
            } catch (SecurityException e) {
                throw new CircularDependencyException(
                    "Error creating instance of " + beanClass.getName(), e);
            }
        }
        
        // 3. 如果没有构造器参数，尝试使用带有@Value注解的构造器
        for (Constructor<?> constructor : constructors) {
            Parameter[] parameters = constructor.getParameters();
            boolean hasValueAnnotation = false;
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(Value.class)) {
                    hasValueAnnotation = true;
                    break;
                }
            }
            
            if (hasValueAnnotation) {
                constructor.setAccessible(true);
                Object[] args = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    Value valueAnnotation = parameter.getAnnotation(Value.class);
                    if (valueAnnotation != null) {
                        String value = valueAnnotation.value();
                        Class<?> paramType = parameter.getType();
                        args[i] = convertValue(value, paramType);
                    }
                }
                try {
                    return constructor.newInstance(args);
                } catch (Exception e) {
                    throw new BeanCreationException(beanClass.getName(), 
                        "Failed to instantiate bean with @Value constructor", e);
                }
            }
        }
        
        // 4. 最后尝试使用无参构造器
        try {
            Constructor<?> constructor = beanClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new CircularDependencyException(
                "No suitable constructor found for " + beanClass.getName(), e);
        }
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException("Unsupported type for @Value conversion: " + targetType);
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
        List<String> primaryBeans = new ArrayList<>();
        String exactMatch = null;
        
        // 先检查普通 bean
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition bd = beanDefinitionMap.get(beanName);
            if (requiredType.isAssignableFrom(bd.getBeanClass())) {
                matchingBeans.add(beanName);
                if (bd.isPrimary()) {
                    primaryBeans.add(beanName);
                }
                if (bd.getBeanClass() == requiredType) {
                    exactMatch = beanName;
                }
            }
        }
        
        // 如果没找到普通 bean，检查 FactoryBean(是为集成mybatis)
        if (matchingBeans.isEmpty()) {
            for (String beanName : beanDefinitionMap.keySet()) {
                BeanDefinition bd = beanDefinitionMap.get(beanName);
                if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
                    try {
                        FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(beanName);
                        if (requiredType.isAssignableFrom(factoryBean.getObjectType())) {
                            matchingBeans.add(beanName);
                            if (bd.isPrimary()) {
                                primaryBeans.add(beanName);
                            }
                            if (factoryBean.getObjectType() == requiredType) {
                                exactMatch = beanName;
                            }
                        }
                    } catch (Exception e) {
                        // 忽略异常，继续检查其他 bean
                        continue;
                    }
                }
            }
        }
        
        if (matchingBeans.isEmpty()) {
            throw new NoSuchBeanDefinitionException("No bean of type '" + requiredType.getName() + "' is defined");
        }
        
        // 如果有多个@Primary标注的bean，抛出异常
        if (primaryBeans.size() > 1) {
            throw new NoSuchBeanDefinitionException("Multiple primary beans found for type '" + 
                requiredType.getName() + "': " + primaryBeans);
        }
        
        // 优先返回@Primary标注的bean
        if (primaryBeans.size() == 1) {
            String beanName = primaryBeans.get(0);
            Object bean = getBean(beanName);
            if (bean instanceof FactoryBean) {
                try {
                    return (T) ((FactoryBean<?>) bean).getObject();
                } catch (Exception e) {
                    throw new BeanCreationException(beanName, "Failed to get object from FactoryBean", e);
                }
            }
            return (T) bean;
        }
        
        // 其次返回精确匹配的bean
        if (exactMatch != null) {
            Object bean = getBean(exactMatch);
            if (bean instanceof FactoryBean) {
                try {
                    return (T) ((FactoryBean<?>) bean).getObject();
                } catch (Exception e) {
                    throw new BeanCreationException(exactMatch, "Failed to get object from FactoryBean", e);
                }
            }
            return (T) bean;
        }
        
        // 如果只有一个匹配的bean，返回它
        if (matchingBeans.size() == 1) {
            String beanName = matchingBeans.get(0);
            Object bean = getBean(beanName);
            if (bean instanceof FactoryBean) {
                try {
                    return (T) ((FactoryBean<?>) bean).getObject();
                } catch (Exception e) {
                    throw new BeanCreationException(beanName, "Failed to get object from FactoryBean", e);
                }
            }
            return (T) bean;
        }
        
        // 如果有多个匹配但没有Primary标注，抛出异常
        throw new NoSuchBeanDefinitionException("Multiple beans found for type '" + requiredType.getName() 
            + "' and none is marked as primary: " + matchingBeans);
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
                throw new BeanCreationException(bd.getBeanClass().getSimpleName(), 
                    "Destroy method [" + destroyMethodName + "] not found");
            } catch (Exception e) {
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

    protected void registerBean(Class<?> beanClass) {
        String beanName = Character.toLowerCase(beanClass.getSimpleName().charAt(0)) + 
                         beanClass.getSimpleName().substring(1);
        
        // 创建BeanDefinition
        BeanDefinition bd = new BeanDefinition() {
            private boolean lazyInit = false;
            private String initMethodName;
            private String destroyMethodName;
            private boolean primary = false;
            private final List<PropertyValue> propertyValues = new ArrayList<>();
            private final List<ConstructorArg> constructorArgs = new ArrayList<>();
            
            @Override
            public Class<?> getBeanClass() {
                return beanClass;
            }
            
            @Override
            public String getScope() {
                return "singleton";
            }
            
            @Override
            public boolean isSingleton() {
                return true;
            }
            
            @Override
            public String getInitMethodName() {
                return initMethodName;
            }
            
            @Override
            public void setInitMethodName(String initMethodName) {
                this.initMethodName = initMethodName;
            }
            
            @Override
            public String getDestroyMethodName() {
                return destroyMethodName;
            }
            
            @Override
            public void setDestroyMethodName(String destroyMethodName) {
                this.destroyMethodName = destroyMethodName;
            }
            
            @Override
            public List<ConstructorArg> getConstructorArgs() {
                return constructorArgs;
            }
            
            @Override
            public List<PropertyValue> getPropertyValues() {
                return propertyValues;
            }
            
            @Override
            public void addConstructorArg(ConstructorArg arg) {
                constructorArgs.add(arg);
            }
            
            @Override
            public void addPropertyValue(PropertyValue propertyValue) {
                propertyValues.add(propertyValue);
            }
            
            @Override
            public boolean isLazyInit() {
                return lazyInit;
            }
            
            @Override
            public void setLazyInit(boolean lazyInit) {
                this.lazyInit = lazyInit;
            }

            @Override
            public boolean isPrimary() {
                return primary;
            }

            @Override
            public void setPrimary(boolean primary) {
                this.primary = primary;
            }
        };
        
        registerBeanDefinition(beanName, bd);
    }

    private boolean isAssignableFromPrimitive(Class<?> from, Class<?> to) {
        if (from.isPrimitive()) {
            if (to.isPrimitive()) {
                return from == to;
            } else {
                return from == boolean.class && to == Boolean.class ||
                       from == char.class && to == Character.class ||
                       from == byte.class && to == Byte.class ||
                       from == short.class && to == Short.class ||
                       from == int.class && to == Integer.class ||
                       from == long.class && to == Long.class ||
                       from == float.class && to == Float.class ||
                       from == double.class && to == Double.class;
            }
        }
        return false;
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == String.class) {
            return "";
        } else if (type == Integer.class || type == int.class) {
            return 0;
        } else if (type == Long.class || type == long.class) {
            return 0L;
        } else if (type == Double.class || type == double.class) {
            return 0.0;
        } else if (type == Boolean.class || type == boolean.class) {
            return false;
        } else if (type == Float.class || type == float.class) {
            return 0.0f;
        } else if (type == Short.class || type == short.class) {
            return (short) 0;
        } else if (type == Byte.class || type == byte.class) {
            return (byte) 0;
        } else if (type == Character.class || type == char.class) {
            return '\u0000';
        }
        return null;
    }

    public List<Object> getBeansByType(Class<?> type) {
        List<Object> result = new ArrayList<>();
        for (String name : getBeanDefinitionNames()) {
            BeanDefinition bd = getBeanDefinition(name);
            if (type.isAssignableFrom(bd.getBeanClass())) {
                result.add(getBean(name));
            }
        }
        return result;
    }

    /**
     * 直接注册一个单例对象
     * @param beanName bean的名称
     * @param singletonObject 单例对象
     */
    public void registerSingleton(String beanName, Object singletonObject) {
        if (beanName == null || beanName.isEmpty()) {
            throw new IllegalArgumentException("Bean name must not be null or empty");
        }
        if (singletonObject == null) {
            throw new IllegalArgumentException("Singleton object must not be null");
        }
        if (this.singletonObjects.containsKey(beanName)) {
            throw new IllegalStateException("Could not register singleton object [" + singletonObject + 
                "] under bean name '" + beanName + "': there is already object [" + 
                this.singletonObjects.get(beanName) + "] bound");
        }
        this.singletonObjects.put(beanName, singletonObject);
    }
} 