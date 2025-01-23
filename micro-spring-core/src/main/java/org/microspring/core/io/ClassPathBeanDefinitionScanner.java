package org.microspring.core.io;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.core.BeanDefinition;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.stereotype.Component;
import org.microspring.core.annotation.Conditional;
import org.microspring.core.condition.Condition;
import org.microspring.core.condition.ConditionContext;
import org.microspring.core.condition.DefaultConditionContext;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.annotation.Annotation;


public class ClassPathBeanDefinitionScanner {
    private final DefaultBeanFactory beanFactory;
    private final ConditionContext conditionContext;
    
    public ClassPathBeanDefinitionScanner(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.conditionContext = new DefaultConditionContext(beanFactory);
    }
    
    public List<BeanDefinition> scan(String basePackage) {
        List<BeanDefinition> beanDefinitions = new ArrayList<>();
        try {
            String path = basePackage.replace('.', '/');
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url != null) {
                File dir = new File(url.getFile());
                if (dir.exists() && dir.isDirectory()) {
                    for (File file : dir.listFiles()) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = basePackage + '.' + file.getName().substring(0, file.getName().length() - 6);
                            processClass(className, beanDefinitions);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning package: " + basePackage, e);
        }
        return beanDefinitions;
    }

    private void processClass(String className, List<BeanDefinition> beanDefinitions) {
        try {
            Class<?> clazz = Class.forName(className);
            // 只跳过非静态内部类
            if ((clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) 
                || clazz.isLocalClass() 
                || clazz.isAnonymousClass()) {
                return;
            }
            
            Component component = clazz.getAnnotation(Component.class);
            if (component != null) {
                // 获取bean名称
                String beanName = component.value();
                if (beanName.isEmpty()) {
                    beanName = toLowerFirstCase(clazz.getSimpleName());
                }
                
                // 检查条件注解
                boolean shouldRegister = true;
                Conditional conditional = clazz.getAnnotation(Conditional.class);
                if (conditional != null) {
                    for (Class<? extends Condition> conditionClass : conditional.value()) {
                        try {
                            Condition condition = conditionClass.getDeclaredConstructor().newInstance();
                            if (!condition.matches(conditionContext)) {
                                shouldRegister = false;
                                break;
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to evaluate condition", e);
                        }
                    }
                }

                if (shouldRegister) {
                    BeanDefinition bd = createBeanDefinition(clazz);
                    beanDefinitions.add(bd);
                    beanFactory.registerBeanDefinition(beanName, bd);
                    System.out.println("[INFO] Bean " + clazz.getSimpleName() + " is loaded");
                }
            }
            
            // 处理内部类
            for (Class<?> innerClass : clazz.getDeclaredClasses()) {
                if (innerClass.isAnnotationPresent(Component.class)) {
                    processClass(innerClass.getName(), beanDefinitions);
                }
            }
        } catch (ClassNotFoundException e) {
            // 忽略无法加载的类
        }
    }

    private BeanDefinition createBeanDefinition(Class<?> beanClass) {
        return new BeanDefinition() {
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
    }

    private String toLowerFirstCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char[] chars = str.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }
} 