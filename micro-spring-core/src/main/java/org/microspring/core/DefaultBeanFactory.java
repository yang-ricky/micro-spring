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

            // 4. 类型检查
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
            // BeanCreationException 直接抛出，不再包装
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
                if (pv.isRef() && isInCreation(pv.getRef())) {
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
                    // 先尝试从缓存中获取
                    value = getSingleton(refName, true);
                    if (value == null) {
                        value = doGetBean(refName, null);
                    }
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

        // 2. 处理 @Autowired 注解的字段注入
        for (Field field : bd.getBeanClass().getDeclaredFields()) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                field.setAccessible(true);
                
                // 获取依赖的bean名称
                String refName;
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                if (qualifier != null) {
                    refName = qualifier.value();
                } else {
                    // 使用字段类型作为依赖的bean名称
                    Class<?> fieldType = field.getType();
                    refName = Character.toLowerCase(fieldType.getSimpleName().charAt(0)) 
                             + fieldType.getSimpleName().substring(1);
                }
                
                try {
                    // 先尝试从缓存中获取
                    Object value = getSingleton(refName, true);
                    if (value == null) {
                        value = doGetBean(refName, field.getType());
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
                
                Object factoryBean = getBean(factoryBeanClass);
                
                // 处理工厂方法的参数
                if (factoryMethod.getParameterCount() > 0) {
                    List<ConstructorArg> constructorArgs = bd.getConstructorArgs();
                    Object[] args = new Object[factoryMethod.getParameterCount()];
                    
                    for (int i = 0; i < args.length; i++) {
                        ConstructorArg arg = constructorArgs.get(i);
                        if (arg.isRef()) {
                            // 如果是引用类型，从容器中获取bean
                            args[i] = getBean(arg.getRef());
                        } else {
                            // 如果是值类型，直接使用值
                            Object value = arg.getValue();
                            if (value != null) {
                                args[i] = value;
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
                e.printStackTrace();
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
                String beanName = qualifier != null ? qualifier.value() : 
                    Character.toLowerCase(paramType.getSimpleName().charAt(0)) + 
                    paramType.getSimpleName().substring(1);
                args[i] = getBean(beanName);
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
} 