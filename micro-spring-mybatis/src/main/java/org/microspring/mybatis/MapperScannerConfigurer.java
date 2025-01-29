package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.stereotype.Component;
import org.microspring.core.BeanFactoryPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.BeanDefinition;

import java.io.File;
import java.net.URL;

@Component
public class MapperScannerConfigurer implements BeanFactoryPostProcessor {
    
    private SqlSessionFactory sqlSessionFactory;
    private String basePackage;

    @Override
    public void postProcessBeanFactory(DefaultBeanFactory beanFactory) {
        try {
            // 获取 SqlSessionFactory
            this.sqlSessionFactory = (SqlSessionFactory) beanFactory.getBean("sqlSessionFactory");
            
            if (basePackage != null && !basePackage.isEmpty()) {
                String[] packages = basePackage.split(",");
                for (String pkg : packages) {
                    // 检查并注册到 MyBatis Configuration
                    boolean shouldSkip = false;
                    for (Class<?> type : sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()) {
                        if (type.getName().startsWith(pkg.trim())) {
                            shouldSkip = true;
                            break;
                        }
                    }
                    
                    if (!shouldSkip) {
                        sqlSessionFactory.getConfiguration().addMappers(pkg.trim());
                        // 然后扫描并注册到 Spring 容器中
                        scanMappers(pkg.trim(), beanFactory);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanMappers(String packageToScan, DefaultBeanFactory beanFactory) throws Exception {
        String path = packageToScan.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url != null) {
            File dir = new File(url.getFile());
            if (dir.exists() && dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageToScan + "." + file.getName().replace(".class", "");
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isInterface()) {
                            processClass(clazz, beanFactory);
                        }
                    }
                }
            }
        }
    }

    private void processClass(Class<?> mapperInterface, DefaultBeanFactory beanFactory) {
        String beanName = getBeanName(mapperInterface);
        
        // 检查是否已经注册到 Spring 容器
        BeanDefinition existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
        if (existingBeanDefinition != null) {
            return;
        }
        
        DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(MapperFactoryBean.class);
        beanDefinition.addConstructorArg(new ConstructorArg(null, mapperInterface, Class.class));
        beanDefinition.addPropertyValue(new PropertyValue("sqlSessionFactory", sqlSessionFactory, SqlSessionFactory.class));
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    private String getBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
    
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
    
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getBasePackage() {
        return this.basePackage;
    }
} 