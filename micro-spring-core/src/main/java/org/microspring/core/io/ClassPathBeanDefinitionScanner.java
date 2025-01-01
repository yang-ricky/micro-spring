package org.microspring.core.io;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.core.BeanDefinition;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;

public class ClassPathBeanDefinitionScanner {
    
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
            
            // 检查是否有Component注解
            Component component = clazz.getAnnotation(Component.class);
            if (component != null) {
                BeanDefinition bd = new BeanDefinition() {
                    @Override
                    public Class<?> getBeanClass() {
                        return clazz;
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
                        return null;
                    }

                    @Override
                    public List<ConstructorArg> getConstructorArgs() {
                        return new ArrayList<>();
                    }

                    @Override
                    public List<PropertyValue> getPropertyValues() {
                        List<PropertyValue> propertyValues = new ArrayList<>();
                        // 处理@Autowired注解的字段
                        for (Field field : clazz.getDeclaredFields()) {
                            Autowired autowired = field.getAnnotation(Autowired.class);
                            if (autowired != null) {
                                PropertyValue pv = new PropertyValue(field.getName(), null, field.getType());
                                propertyValues.add(pv);
                            }
                        }
                        return propertyValues;
                    }

                    @Override
                    public void addConstructorArg(ConstructorArg arg) {
                    }

                    @Override
                    public void addPropertyValue(PropertyValue propertyValue) {
                    }
                };
                beanDefinitions.add(bd);
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
} 