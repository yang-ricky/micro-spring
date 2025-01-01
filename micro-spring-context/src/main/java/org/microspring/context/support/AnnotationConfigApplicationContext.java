package org.microspring.context.support;

import org.microspring.core.BeanDefinition;
import org.microspring.core.annotation.Component;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class AnnotationConfigApplicationContext extends AbstractApplicationContext {
    private final String basePackage;
    
    public AnnotationConfigApplicationContext(String basePackage) {
        super();
        this.basePackage = basePackage;
        refresh();
    }

    @Override
    public String getApplicationName() {
        return "AnnotationConfigApplicationContext";
    }

    @Override
    public void refresh() {
        // 1. 扫描组件
        scanPackages(basePackage);
        
        // 2. 初始化所有单例bean
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (bd.isSingleton()) {
                getBean(beanName);  // 触发bean的创建和初始化
            }
        }
    }

    private void scanPackages(String basePackage) {
        try {
            String packagePath = basePackage.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            // 获取包对应的资源URL
            java.net.URL resource = classLoader.getResource(packagePath);
            if (resource == null) {
                throw new RuntimeException("Package " + basePackage + " not found");
            }
            
            java.io.File directory = new java.io.File(resource.getFile());
            if (directory.exists()) {
                // 扫描目录下的所有class文件
                for (File file : directory.listFiles()) {
                    if (file.getName().endsWith(".class")) {
                        String className = basePackage + '.' + 
                            file.getName().substring(0, file.getName().length() - 6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(org.microspring.core.annotation.Component.class)) {
                                registerBean(clazz);
                            } else if (clazz.isAnnotationPresent(org.microspring.stereotype.Component.class)) {
                                registerBean(clazz);
                            }
                        } catch (ClassNotFoundException e) {
                            // 忽略无法加载的类
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning package: " + basePackage, e);
        }
    }

    private void registerBean(Class<?> clazz) {
        String beanName;
        // 分别尝试获取两个包下的Component注解
        org.microspring.core.annotation.Component coreComponent = 
            clazz.getAnnotation(org.microspring.core.annotation.Component.class);
        org.microspring.stereotype.Component stereotypeComponent = 
            clazz.getAnnotation(org.microspring.stereotype.Component.class);
        
        if (coreComponent != null) {
            beanName = coreComponent.value();
        } else if (stereotypeComponent != null) {
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
                return null;
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
        };
        
        // 注册BeanDefinition
        beanFactory.registerBeanDefinition(beanName, bd);
    }
} 