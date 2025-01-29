package org.microspring.mybatis.config;

import org.microspring.core.BeanDefinition;
import org.microspring.core.BeanFactoryPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.mybatis.MapperFactoryBean;
import org.microspring.mybatis.annotation.Mapper;
import org.microspring.stereotype.Component;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class MapperAnnotationBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    @Override
    public void postProcessBeanFactory(DefaultBeanFactory beanFactory) {
        
        // 扫描所有类，查找带有 @Mapper 注解的接口
        try {
            List<Class<?>> classes = scanPackage("org.microspring.mybatis");
            for (Class<?> clazz : classes) {
                // 检查是否是接口并且有 @Mapper 注解
                if (clazz.isInterface() && clazz.isAnnotationPresent(Mapper.class)) {
                    
                    String mapperBeanName = getBeanName(clazz);
                    
                    // 检查是否已经注册
                    BeanDefinition existingBeanDefinition = beanFactory.getBeanDefinition(mapperBeanName);
                    if (existingBeanDefinition != null) {
                        continue;
                    }
                    
                    // 创建并注册 MapperFactoryBean
                    DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(MapperFactoryBean.class);
                    beanDefinition.addConstructorArg(new ConstructorArg(null, clazz, Class.class));
                    beanDefinition.addPropertyValue(new PropertyValue("sqlSessionFactory", "sqlSessionFactory", SqlSessionFactory.class, true));
                    beanDefinition.setScope("singleton");
                    beanDefinition.setInitMethodName("afterPropertiesSet");
                    
                    beanFactory.registerBeanDefinition(mapperBeanName, beanDefinition);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    private List<Class<?>> scanPackage(String basePackage) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        
        if (url != null) {
            File dir = new File(url.getFile());
            if (dir.exists() && dir.isDirectory()) {
                scanDirectory(dir, basePackage, classes);
            }
        }
        return classes;
    }
    
    private void scanDirectory(File dir, String basePackage, List<Class<?>> classes) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // 递归扫描子目录
                scanDirectory(file, basePackage + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().replace(".class", "");
                try {
                    classes.add(Class.forName(className));
                } catch (Throwable e) {
                    // 忽略无法加载的类
                }
            }
        }
    }
    
    private String getBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
} 