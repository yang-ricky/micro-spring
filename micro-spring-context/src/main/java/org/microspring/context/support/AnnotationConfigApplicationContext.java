package org.microspring.context.support;

import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.core.BeanFactoryPostProcessor;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.beans.factory.annotation.Lazy;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.stereotype.Component;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.event.ContextRefreshedEvent;
import org.microspring.context.event.EventListenerMethodProcessor;
import org.microspring.context.event.SimpleApplicationEventPublisher;
import org.microspring.stereotype.Service;
import org.microspring.stereotype.Repository;
import org.microspring.context.scope.ScopeManager;
import org.microspring.context.scope.ObjectFactory;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.context.annotation.Bean;
import org.microspring.context.annotation.Configuration;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.context.annotation.Primary;
import javax.annotation.Resource;
import org.microspring.context.annotation.Import;
import org.microspring.context.annotation.ImportBeanDefinitionRegistrar;
import org.microspring.context.annotation.ImportSelector;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class AnnotationConfigApplicationContext extends AbstractApplicationContext {
    private String basePackage;
    private final ScopeManager scopeManager = new ScopeManager();
    
    public AnnotationConfigApplicationContext() {
        super();
    }
    
    public AnnotationConfigApplicationContext(String basePackage) {
        super();
        this.basePackage = basePackage;
        refresh();
    }

    /**
     * 注册一个配置类
     * @param configClass 要注册的配置类
     */
    public void register(Class<?> configClass) {
        if (configClass.isAnnotationPresent(Configuration.class)) {
            processConfigurationClass(configClass);
            
            // 调用 BeanFactoryPostProcessor
            invokeBeanFactoryPostProcessors();
            
            // 初始化非延迟加载的单例bean
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                if (bd.isSingleton() && !bd.isLazyInit()) {
                    getBean(beanName);
                }
            }
        } else {
            registerBean(configClass);
        }
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public String getApplicationName() {
        return "AnnotationConfigApplicationContext";
    }

    @Override
    public void refresh() {
        if (basePackage != null) {
            // 1. 扫描组件
            scanPackages(basePackage);
        }
            
        // 2. 调用 BeanFactoryPostProcessor
        invokeBeanFactoryPostProcessors();
            
        // 3. 注册 BeanPostProcessor 和监听器
        super.refresh();
        
        // 4. 只初始化非延迟加载的单例bean
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (bd.isSingleton() && !bd.isLazyInit()) {
                getBean(beanName);
            }
        }
        
        // 5. 发布刷新完成事件
        publishEvent(new ContextRefreshedEvent(this));
    }

    private void invokeBeanFactoryPostProcessors() {
        System.out.println("Invoking BeanFactoryPostProcessors...");
        // 获取所有 BeanFactoryPostProcessor 类型的 bean 定义
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (BeanFactoryPostProcessor.class.isAssignableFrom(bd.getBeanClass())) {
                System.out.println("Found BeanFactoryPostProcessor: " + beanName);
                BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) getBean(beanName);
                postProcessor.postProcessBeanFactory(beanFactory);
            }
        }
    }

    protected void scanPackages(String... basePackages) {
        try {
            for (String basePackage : basePackages) {
                String path = basePackage.replace('.', '/');
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                
                // 获取类路径下的资源
                Enumeration<URL> resources = classLoader.getResources(path);

                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if (resource != null) {
                        File directory = new File(resource.getFile());
                        if (directory.exists()) {
                            scanDirectory(directory, basePackage);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error scanning packages: " + e.getMessage(), e);
        }
    }

    private void scanDirectory(File directory, String basePackage) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, basePackage + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    try {
                        String className = basePackage + "." + 
                            file.getName().substring(0, file.getName().length() - 6);
                        Class<?> clazz = Class.forName(className);
                        
                        // 跳过注解类
                        if (clazz.isAnnotation()) {
                            continue;
                        }
                        
                        // 检查是否有构造型注解
                        Component componentAnn = clazz.getAnnotation(Component.class);
                        Service serviceAnn = clazz.getAnnotation(Service.class);
                        Repository repositoryAnn = clazz.getAnnotation(Repository.class);
                        Configuration configAnn = clazz.getAnnotation(Configuration.class);
                        
                        if (componentAnn != null || serviceAnn != null || 
                            repositoryAnn != null || configAnn != null) {
                            
                            // 如果是配置类，处理其中的@Bean方法
                            if (configAnn != null) {
                                processConfigurationClass(clazz);
                            } else {
                                registerBean(clazz);
                            }
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void processConfigurationClass(Class<?> configClass) {
        // 首先注册配置类本身
        registerBean(configClass);
        
        // 处理@Import注解
        Import importAnn = configClass.getAnnotation(Import.class);
        if (importAnn != null) {
            for (Class<?> importedClass : importAnn.value()) {
                if (ImportSelector.class.isAssignableFrom(importedClass)) {
                    // 如果是ImportSelector的实现类，创建实例并调用selectImports方法
                    try {
                        ImportSelector selector = (ImportSelector) importedClass.getDeclaredConstructor().newInstance();
                        String[] importClassNames = selector.selectImports(configClass);
                        for (String className : importClassNames) {
                            try {
                                Class<?> selectedClass = Class.forName(className);
                                if (selectedClass.isAnnotationPresent(Configuration.class)) {
                                    processConfigurationClass(selectedClass);
                                } else {
                                    registerBean(selectedClass);
                                }
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException("Could not load class: " + className, e);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to process ImportSelector: " + importedClass, e);
                    }
                } else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(importedClass)) {
                    // 如果是ImportBeanDefinitionRegistrar的实现类，创建实例并调用registerBeanDefinitions方法
                    try {
                        ImportBeanDefinitionRegistrar registrar = (ImportBeanDefinitionRegistrar) importedClass.getDeclaredConstructor().newInstance();
                        registrar.registerBeanDefinitions(configClass, beanFactory);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to process ImportBeanDefinitionRegistrar: " + importedClass, e);
                    }
                } else if (importedClass.isAnnotationPresent(Configuration.class)) {
                    // 如果是配置类，递归处理
                    processConfigurationClass(importedClass);
                } else {
                    // 如果是普通类，注册为bean
                    registerBean(importedClass);
                }
            }
        }
        
        // 处理所有带有@Bean注解的方法
        for (Method method : configClass.getDeclaredMethods()) {
            Bean beanAnn = method.getAnnotation(Bean.class);
            if (beanAnn != null) {
                String beanName = determineBeanName(beanAnn, method);
                registerBeanMethod(beanName, method, configClass);
            }
        }
    }

    private String determineBeanName(Bean beanAnn, Method method) {
        // 如果@Bean注解指定了名称，使用指定的名称
        String[] names = beanAnn.value();
        if (names.length > 0 && !names[0].isEmpty()) {
            return names[0];
        }
        // 否则使用方法名作为bean名称
        return method.getName();
    }

    private void registerBeanMethod(String beanName, Method method, Class<?> configClass) {
        BeanDefinition bd = new BeanDefinition() {
            private boolean lazyInit = false;
            private String initMethodName;
            private String destroyMethodName;
            private boolean primary = false;
            private final List<PropertyValue> propertyValues = new ArrayList<>();
            private final List<ConstructorArg> constructorArgs = new ArrayList<>();
            private Method factoryMethod = method;  // 设置工厂方法
            private Class<?> factoryBeanClass = configClass;  // 设置工厂bean类
            
            @Override
            public Class<?> getBeanClass() {
                return method.getReturnType();
            }
            
            @Override
            public String getScope() {
                Scope scope = method.getAnnotation(Scope.class);
                return scope != null ? scope.value() : "singleton";
            }
            
            @Override
            public boolean isSingleton() {
                return "singleton".equals(getScope());
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
            public List<PropertyValue> getPropertyValues() {
                return propertyValues;
            }
            
            @Override
            public void addPropertyValue(PropertyValue pv) {
                propertyValues.add(pv);
            }
            
            @Override
            public List<ConstructorArg> getConstructorArgs() {
                return constructorArgs;
            }
            
            @Override
            public void addConstructorArg(ConstructorArg arg) {
                constructorArgs.add(arg);
            }
            
            @Override
            public boolean isPrimary() {
                return primary;
            }
            
            @Override
            public void setPrimary(boolean primary) {
                this.primary = primary;
            }

            @Override
            public Method getFactoryMethod() {
                return factoryMethod;
            }

            @Override
            public Class<?> getFactoryBeanClass() {
                return factoryBeanClass;
            }
        };
        
        // 检查并设置@Primary注解
        Primary primaryAnn = method.getAnnotation(Primary.class);
        if (primaryAnn != null) {
            bd.setPrimary(true);
        }
        
        // 处理@Bean注解的initMethod和destroyMethod属性
        Bean beanAnn = method.getAnnotation(Bean.class);
        if (beanAnn != null) {
            String initMethod = beanAnn.initMethod();
            if (!initMethod.isEmpty()) {
                bd.setInitMethodName(initMethod);
            }
            String destroyMethod = beanAnn.destroyMethod();
            if (!destroyMethod.equals("(inferred)")) {
                bd.setDestroyMethodName(destroyMethod);
            }
        }
        
        // 处理工厂方法的参数
        if (method.getParameterCount() > 0) {
            Class<?>[] paramTypes = method.getParameterTypes();
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                Value valueAnn = parameters[i].getAnnotation(Value.class);
                
                // 如果参数有@Value注解
                if (valueAnn != null) {
                    String value = valueAnn.value();
                    ConstructorArg arg = new ConstructorArg(null, value, paramType);
                    bd.addConstructorArg(arg);
                }
                // 如果是基本类型或String，但没有@Value注解
                else if (paramType.isPrimitive() || paramType == String.class || 
                    (paramType.getName().startsWith("java.lang") && paramType != Class.class)) {
                    ConstructorArg arg = new ConstructorArg(null, null, paramType);
                    bd.addConstructorArg(arg);
                } else {
                    // 对于引用类型，设置ref为bean名称
                    String refName = Character.toLowerCase(paramType.getSimpleName().charAt(0)) + 
                                   paramType.getSimpleName().substring(1);
                    ConstructorArg arg = new ConstructorArg(refName, null, paramType);
                    bd.addConstructorArg(arg);
                }
            }
        }
        
        beanFactory.registerBeanDefinition(beanName, bd);
    }

    private void registerBean(Class<?> clazz) {
        String beanName = null;
        
        // 检查 @Service 注解
        Service serviceAnn = clazz.getAnnotation(Service.class);
        if (serviceAnn != null) {
            beanName = serviceAnn.value();
        }
        
        // 检查 @Repository 注解
        if (beanName == null || beanName.isEmpty()) {
            Repository repositoryAnn = clazz.getAnnotation(Repository.class);
            if (repositoryAnn != null) {
                beanName = repositoryAnn.value();
            }
        }
        
        // 检查 @Component 注解
        if (beanName == null || beanName.isEmpty()) {
            Component componentAnn = clazz.getAnnotation(Component.class);
            if (componentAnn != null) {
                beanName = componentAnn.value();
            }
        }
        
        // 如果没有指定bean名称，使用类名首字母小写作为bean名称
        if (beanName == null || beanName.isEmpty()) {
            beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0)) + 
                      clazz.getSimpleName().substring(1);
        }
        
        // 创建并注册BeanDefinition
        BeanDefinition bd = new BeanDefinition() {
            private boolean lazyInit = false;
            private String initMethodName;
            private String destroyMethodName;
            private boolean primary = false;
            private final List<PropertyValue> propertyValues = new ArrayList<>();
            private final List<ConstructorArg> constructorArgs = new ArrayList<>();
            
            @Override
            public Class<?> getBeanClass() {
                return clazz;
            }
            
            @Override
            public String getScope() {
                Scope scope = clazz.getAnnotation(Scope.class);
                return scope != null ? scope.value() : "singleton";
            }
            
            @Override
            public boolean isSingleton() {
                return "singleton".equals(getScope());
            }
            
            @Override
            public String getInitMethodName() {
                return this.initMethodName;
            }
            
            @Override
            public void setInitMethodName(String initMethodName) {
                this.initMethodName = initMethodName;
            }
            
            @Override
            public String getDestroyMethodName() {
                return this.destroyMethodName;
            }
            
            @Override
            public void setDestroyMethodName(String destroyMethodName) {
                this.destroyMethodName = destroyMethodName;
            }
            
            @Override
            public List<ConstructorArg> getConstructorArgs() {
                return this.constructorArgs;
            }
            
            @Override
            public List<PropertyValue> getPropertyValues() {
                return this.propertyValues;
            }
            
            @Override
            public void addConstructorArg(ConstructorArg arg) {
                this.constructorArgs.add(arg);
            }
            
            @Override
            public void addPropertyValue(PropertyValue propertyValue) {
                this.propertyValues.add(propertyValue);
            }
            
            @Override
            public boolean isLazyInit() {
                Lazy lazy = clazz.getAnnotation(Lazy.class);
                return lazy != null;
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

        // 检查并设置@Primary注解
        Primary primaryAnn = clazz.getAnnotation(Primary.class);
        if (primaryAnn != null) {
            bd.setPrimary(true);
        }
        
        // 处理字段注入的依赖关系
        for (Field field : clazz.getDeclaredFields()) {
            // 处理 @Autowired 注解
            if (field.isAnnotationPresent(Autowired.class)) {
                String propertyName = field.getName();
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                Class<?> fieldType = field.getType();

                // 跳过集合类型的字段，让 DefaultBeanFactory 处理
                if (List.class.isAssignableFrom(fieldType) || 
                    Map.class.isAssignableFrom(fieldType)) {
                    continue;
                }
                
                // 如果有@Qualifier注解，使用指定的名称
                // 否则使用类型对应的默认bean名称（类名首字母小写）
                String refName;
                if (qualifier != null) {
                    refName = qualifier.value();
                } else {
                    String targetClassName = fieldType.getSimpleName();
                    refName = Character.toLowerCase(targetClassName.charAt(0)) + 
                             targetClassName.substring(1);
                }
                
                PropertyValue pv = new PropertyValue(propertyName, refName, fieldType, true);
                bd.addPropertyValue(pv);
            }
            
            // 处理 @Resource 注解
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                String propertyName = field.getName();
                Class<?> fieldType = field.getType();

                // 跳过集合类型的字段，让 DefaultBeanFactory 处理
                if (List.class.isAssignableFrom(fieldType) || 
                    Map.class.isAssignableFrom(fieldType)) {
                    continue;
                }
                
                // 获取引用的bean名称
                String refName = resource.name();
                if (refName.isEmpty()) {
                    // 如果没有指定name，使用字段名
                    refName = propertyName;
                }
                
                PropertyValue pv = new PropertyValue(propertyName, refName, fieldType, true);
                bd.addPropertyValue(pv);
            }
        }
        
        // 处理setter方法注入的依赖关系
        for (Method method : clazz.getDeclaredMethods()) {
            // 处理 @Autowired 注解
            if (method.isAnnotationPresent(Autowired.class)) {
                if (method.getName().startsWith("set") && 
                    method.getParameterCount() == 1) {
                    
                    String propertyName = method.getName().substring(3);
                    propertyName = Character.toLowerCase(propertyName.charAt(0)) + 
                                 propertyName.substring(1);
                    
                    Class<?> paramType = method.getParameterTypes()[0];
                    
                    // 跳过集合类型的参数，让 DefaultBeanFactory 处理
                    if (List.class.isAssignableFrom(paramType) || 
                        Map.class.isAssignableFrom(paramType)) {
                        continue;
                    }
                    
                    Qualifier qualifier = method.getAnnotation(Qualifier.class);
                    
                    // 如果有@Qualifier注解，使用指定的名称
                    // 否则使用类型对应的默认bean名称（类名首字母小写）
                    String refName;
                    if (qualifier != null) {
                        refName = qualifier.value();
                    } else {
                        String targetClassName = paramType.getSimpleName();
                        refName = Character.toLowerCase(targetClassName.charAt(0)) + 
                                 targetClassName.substring(1);
                    }
                    
                    PropertyValue pv = new PropertyValue(propertyName, refName, paramType, true);
                    bd.addPropertyValue(pv);
                }
            }
            
            // 处理 @Resource 注解
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null && 
                method.getName().startsWith("set") && 
                method.getParameterCount() == 1) {
                
                String propertyName = method.getName().substring(3);
                propertyName = Character.toLowerCase(propertyName.charAt(0)) + 
                             propertyName.substring(1);
                
                Class<?> paramType = method.getParameterTypes()[0];
                
                // 跳过集合类型的参数，让 DefaultBeanFactory 处理
                if (List.class.isAssignableFrom(paramType) || 
                    Map.class.isAssignableFrom(paramType)) {
                    continue;
                }
                
                // 获取引用的bean名称
                String refName = resource.name();
                if (refName.isEmpty()) {
                    // 如果没有指定name，使用属性名
                    refName = propertyName;
                }
                
                PropertyValue pv = new PropertyValue(propertyName, refName, paramType, true);
                bd.addPropertyValue(pv);
            }
        }
        
        beanFactory.registerBeanDefinition(beanName, bd);
    }

    protected void registerEventListenerProcessor() {
        // 直接创建并使用 EventListenerMethodProcessor
        EventListenerMethodProcessor processor = new EventListenerMethodProcessor(this);
        processor.processEventListenerMethods();
    }

    // 添加这个方法以支持测试用例
    public DefaultBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public Object getBean(String name) {
        BeanDefinition bd = beanFactory.getBeanDefinition(name);
        String scope = bd.getScope();
        
        // 对于 request 作用域
        if (Scope.REQUEST.equals(scope)) {
            HttpServletRequest request = scopeManager.getCurrentRequest();
            if (request != null) {
                return scopeManager.getBean(name, scope, (ObjectFactory) () -> super.getBean(name));
            }
        }
        
        // 对于 session 作用域
        if (Scope.SESSION.equals(scope)) {
            HttpSession session = scopeManager.getCurrentSession();
            if (session != null) {
                return scopeManager.getBean(name, scope, (ObjectFactory) () -> super.getBean(name));
            }
        }
        
        // 其他作用域使用父类的 getBean 方法
        return super.getBean(name);
    }

    public ScopeManager getScopeManager() {
        return this.scopeManager;
    }
} 