package org.microspring.mybatis.config;

import org.microspring.context.annotation.ImportBeanDefinitionRegistrar;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.mybatis.MapperFactoryBean;
import org.microspring.mybatis.annotation.Mapper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapperAnnotationRegistrar implements ImportBeanDefinitionRegistrar {
    
    @Override
    public void registerBeanDefinitions(Class<?> importingClass, DefaultBeanFactory beanFactory) {
        System.out.println("=== MapperAnnotationRegistrar.registerBeanDefinitions ===");
        System.out.println("Processing package: " + importingClass.getPackage().getName());
        
        // 扫描同包下的所有类
        String basePackage = importingClass.getPackage().getName();
        try {
            List<Class<?>> classes = scanPackage(basePackage);
            System.out.println("Found " + classes.size() + " classes in package");
            
            for (Class<?> clazz : classes) {
                System.out.println("Checking class: " + clazz.getName());
                System.out.println("Is interface: " + clazz.isInterface());
                System.out.println("Has @Mapper: " + clazz.isAnnotationPresent(Mapper.class));
                
                if (clazz.isInterface() && clazz.isAnnotationPresent(Mapper.class)) {
                    String beanName = getBeanName(clazz);
                    System.out.println("Registering mapper with name: " + beanName);
                    
                    DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(MapperFactoryBean.class);
                    beanDefinition.addConstructorArg(new ConstructorArg(null, clazz, Class.class));
                    beanDefinition.addPropertyValue(new PropertyValue("sqlSessionFactory", "sqlSessionFactory", String.class));
                    
                    beanFactory.registerBeanDefinition(beanName, beanDefinition);
                    System.out.println("Successfully registered mapper: " + beanName);
                }
            }
        } catch (Exception e) {
            System.out.println("Error scanning package: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== End MapperAnnotationRegistrar.registerBeanDefinitions ===");
    }
    
    private List<Class<?>> scanPackage(String basePackage) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        
        if (url != null) {
            File dir = new File(url.getFile());
            if (dir.exists() && dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = basePackage + "." + file.getName().replace(".class", "");
                        classes.add(Class.forName(className));
                    }
                }
            }
        }
        return classes;
    }
    
    private String getBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
} 