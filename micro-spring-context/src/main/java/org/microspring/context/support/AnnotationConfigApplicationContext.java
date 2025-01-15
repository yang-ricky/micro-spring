package org.microspring.context.support;

import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.beans.factory.annotation.Lazy;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.stereotype.Component;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.event.ContextRefreshedEvent;
import org.microspring.context.event.EventListenerMethodProcessor;
import org.microspring.context.event.SimpleApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;

public class AnnotationConfigApplicationContext extends AbstractApplicationContext {
    private String basePackage;
    
    public AnnotationConfigApplicationContext() {
        super();
    }
    
    public AnnotationConfigApplicationContext(String basePackage) {
        super();
        this.basePackage = basePackage;
        refresh();
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
            System.out.println("Scanning package: " + basePackage);
            scanPackages(basePackage);
            
            // 2. 只初始化非延迟加载的单例bean
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                System.out.println("Found bean: " + beanName);
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                if (bd.isSingleton() && !bd.isLazyInit()) {
                    getBean(beanName);
                }
            }
        }

        // 3. 注册监听器
        System.out.println("Registering listeners");
        registerListeners();
        
        // 4. 发布刷新完成事件
        publishEvent(new ContextRefreshedEvent(this));
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
                    File directory = new File(resource.getFile());
                    if (directory.exists()) {
                        scanDirectory(directory, basePackage);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning packages", e);
        }
    }

    private void scanDirectory(File directory, String basePackage) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, basePackage + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String className = basePackage + "." + 
                        file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            registerBean(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        // 忽略找不到的类
                    }
                }
            }
        }
    }

    private void registerBean(Class<?> clazz) {
        String beanName;
        org.microspring.stereotype.Component stereotypeComponent = 
            clazz.getAnnotation(org.microspring.stereotype.Component.class);
        
        if (stereotypeComponent != null) {
            beanName = stereotypeComponent.value();
        } else {
            throw new RuntimeException("No @Component annotation found on class: " + clazz.getName());
        }
        
        if (beanName.isEmpty()) {
            // 如果没有指定bean名称，使用类名首字母小写作为bean名称
            beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0)) + 
                      clazz.getSimpleName().substring(1);
        }
        
        // 创建BeanDefinition
        BeanDefinition bd = new BeanDefinition() {
            private boolean lazyInit = false;
            private String initMethodName;
            private String destroyMethodName;
            
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
                return new ArrayList<>();
            }
            
            @Override
            public List<PropertyValue> getPropertyValues() {
                return new ArrayList<>();
            }
            
            @Override
            public void addConstructorArg(ConstructorArg arg) {
            }
            
            @Override
            public void addPropertyValue(PropertyValue propertyValue) {
            }
            
            @Override
            public boolean isLazyInit() {
                // 检查@Lazy注解
                Lazy lazy = clazz.getAnnotation(Lazy.class);
                return lazy != null;  // 如果有@Lazy注解，就返回true
            }
            
            @Override
            public void setLazyInit(boolean lazyInit) {
                this.lazyInit = lazyInit;
            }
        };
        
        // 注册BeanDefinition
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
} 