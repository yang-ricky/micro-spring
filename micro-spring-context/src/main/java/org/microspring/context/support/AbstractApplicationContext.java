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
import org.microspring.context.event.Async;
import org.microspring.core.annotation.Order;
import org.microspring.context.event.ApplicationEventMulticaster;
import org.microspring.context.event.SimpleApplicationEventMulticaster;
import org.microspring.context.event.EventListenerMethodProcessor;
import org.microspring.context.event.SmartApplicationListener;
import org.microspring.core.BeanPostProcessor;

import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

public abstract class AbstractApplicationContext implements ApplicationContext {
    protected final DefaultBeanFactory beanFactory;
    protected final ValueResolver valueResolver;
    private final List<ApplicationListener<?>> applicationListeners = new ArrayList<>();
    private final ApplicationEventMulticaster eventMulticaster;
    private final ExecutorService eventExecutor = Executors.newFixedThreadPool(4);
    
    private ApplicationContext parent;
    
    public AbstractApplicationContext() {
        this.beanFactory = new DefaultBeanFactory();
        this.valueResolver = new DefaultValueResolver(beanFactory);
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setTaskExecutor(eventExecutor);
        this.eventMulticaster = multicaster;
    }
    
    public AbstractApplicationContext(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.valueResolver = new DefaultValueResolver(beanFactory);
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setTaskExecutor(eventExecutor);
        this.eventMulticaster = multicaster;
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
        // 先从 beanFactory 获取 bean
        Object bean = beanFactory.getBean(name);
        // 处理 @Value 注解
        injectDependencies(bean);
        return bean;
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        // 先从 beanFactory 获取 bean
        T bean = beanFactory.getBean(name, requiredType);
        // 处理 @Value 注解
        injectDependencies(bean);
        return bean;
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) {
        // 先从 beanFactory 获取 bean
        T bean = beanFactory.getBean(requiredType);
        // 处理 @Value 注解
        injectDependencies(bean);
        return bean;
    }
    
    protected void injectDependencies(Object bean) {
        Class<?> clazz = bean.getClass();
        
        // 1. 处理 @Value 注解
        for (Field field : clazz.getDeclaredFields()) {
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
                        args[i] = beanFactory.getBean(qualifier.value());
                    } else {
                        args[i] = beanFactory.getBean(paramTypes[i]);
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
        
        // 关闭线程池
        eventExecutor.shutdown();
        try {
            if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                eventExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            eventExecutor.shutdownNow();
        }
    }
    
    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return beanFactory.getBeanNamesForAnnotation(annotationType);
    }
    
    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        Map<String, Object> result = new HashMap<>();
        String[] beanNames = getBeanNamesForAnnotation(annotationType);
        
        for (String beanName : beanNames) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (!bd.isLazyInit()) {
                // 统一使用 getBean 方法
                result.put(beanName, getBean(beanName));
            }
        }
        return result;
    }

    public Map<String, Object> getAllBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        Map<String, Object> result = new HashMap<>();
        
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (bd.getBeanClass().isAnnotationPresent(annotationType)) {
                // 统一使用 getBean 方法
                result.put(beanName, getBean(beanName));
            }
        }
        return result;
    }
    
    protected DefaultBeanFactory getBeanFactory() {
        return this.beanFactory;
    }
    
    @Override
    public void refresh() {
        // 添加调试日志
        registerBeanPostProcessors(beanFactory);
        
        // 注册监听器
        registerListeners();
        
        // 发布刷新完成事件
        publishEvent(new ContextRefreshedEvent(this));
    }
    
    protected void registerListeners() {
        // 处理所有的事件监听器（包括@EventListener注解和ApplicationListener接口）
        EventListenerMethodProcessor processor = new EventListenerMethodProcessor(this);
        processor.processEventListenerMethods();
    }
    
    @Override
    public void publishEvent(ApplicationEvent event) {
        
        List<ApplicationListener<?>> currentListeners = getApplicationListeners(event);
        
        for (ApplicationListener<?> listener : currentListeners) {
            invokeListener(listener, event);
        }
        
        if (parent != null) {
            parent.publishEvent(event);
        } 
    }
    
    @SuppressWarnings("unchecked")
    private void invokeListener(ApplicationListener listener, ApplicationEvent event) {
        if (listener instanceof SmartApplicationListener) {
            // SmartApplicationListener 直接调用，类型检查已经在 supportsEvent 中完成
            listener.onApplicationEvent(event);
            return;
        }

        final Method onApplicationEventMethod;
        try {
            // 获取监听器的泛型类型
            Class<?> eventType = getEventType(listener);
            // 使用监听器声明的事件类型而不是具体的事件类型
            onApplicationEventMethod = listener.getClass().getMethod("onApplicationEvent", eventType);
            onApplicationEventMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        if (onApplicationEventMethod.isAnnotationPresent(Async.class)) {
            // 异步处理
            eventExecutor.submit(() -> {
                try {
                    onApplicationEventMethod.invoke(listener, event);
                } catch (Exception e) {
                    // 异步处理中的异常需要特别处理
                    System.err.println("Error processing event asynchronously: " + e.getMessage());
                }
            });
        } else {
            // 同步处理
            try {
                onApplicationEventMethod.invoke(listener, event);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking listener", e);
            }
        }
    }
    
    private List<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event) {        
        List<ApplicationListener<?>> allListeners = new ArrayList<>();
        for (ApplicationListener<?> listener : applicationListeners) {
            if (supportsEvent(listener, event)) {
                allListeners.add(listener);
            }
        }
        
        // 根据@Order注解和SmartApplicationListener接口排序
        allListeners.sort((l1, l2) -> {
            int p1 = getListenerOrder(l1);
            int p2 = getListenerOrder(l2);
            return Integer.compare(p1, p2);
        });
        
        return allListeners;
    }
    
    private boolean supportsEvent(ApplicationListener<?> listener, ApplicationEvent event) {
        
        // 如果是SmartApplicationListener，使用其自定义的类型检查逻辑
        if (listener instanceof SmartApplicationListener) {
            SmartApplicationListener smartListener = (SmartApplicationListener) listener;
            return smartListener.supportsEventType(event.getClass()) && 
                   smartListener.supportsSourceType(event.getSource().getClass());
        }

        // 获取监听器的泛型类型
        Class<?> eventType = getEventType(listener);
        
        // 检查事件类型的继承关系
        Class<?> currentClass = event.getClass();
        while (currentClass != null) {
            currentClass = currentClass.getSuperclass();
        }
        
        // 检查事件是否是监听器要监听的类型或其子类
        boolean supports = eventType != null && eventType.isAssignableFrom(event.getClass());
        
        return supports;
    }
    
    /**
     * 获取监听器的事件泛型类型
     */
    private Class<?> getEventType(ApplicationListener<?> listener) {
        
        // SmartApplicationListener 不需要获取泛型类型
        if (listener instanceof SmartApplicationListener) {
            return ApplicationEvent.class;
        }
        
        // 先检查接口
        Type[] genericInterfaces = listener.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) type;
                if (paramType.getRawType().equals(ApplicationListener.class)) {
                    Type[] typeArguments = paramType.getActualTypeArguments();
                    if (typeArguments != null && typeArguments.length > 0) {
                        Type typeArgument = typeArguments[0];
                        if (typeArgument instanceof Class) {
                            return (Class<?>) typeArgument;
                        }
                    }
                }
            }
        }
        
        // 如果接口没找到，再检查父类
        Type genericSuperclass = listener.getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericSuperclass;
            Type[] typeArguments = paramType.getActualTypeArguments();
            if (typeArguments != null && typeArguments.length > 0) {
                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class) {
                    return (Class<?>) typeArgument;
                }
            }
        }
        
        throw new IllegalStateException("Could not find event type for listener: " + listener);
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
    
    @Override
    public void setParent(ApplicationContext parent) {
        this.parent = parent;
    }
    
    @Override
    public ApplicationContext getParent() {
        return this.parent;
    }
    
    private int getListenerOrder(ApplicationListener<?> listener) {
        if (listener instanceof SmartApplicationListener) {
            return ((SmartApplicationListener) listener).getOrder();
        }
        Order order = listener.getClass().getAnnotation(Order.class);
        return order != null ? order.value() : Integer.MAX_VALUE;
    }
    
    protected void registerBeanPostProcessors(DefaultBeanFactory beanFactory) {
        String[] postProcessorNames = getBeanNamesForType(BeanPostProcessor.class);
        
        for (String ppName : postProcessorNames) {
            BeanPostProcessor pp = (BeanPostProcessor) beanFactory.getBean(ppName);
            beanFactory.addBeanPostProcessor(pp);
        }
    }
} 