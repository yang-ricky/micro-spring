package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.stereotype.Component;
import org.microspring.core.BeanFactoryPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.PropertyValue;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.beans.factory.InitializingBean;

import java.io.File;
import java.net.URL;

@Component
public class MapperScannerConfigurer implements BeanFactoryPostProcessor, InitializingBean {
    
    private SqlSessionFactory sqlSessionFactory;
    private String basePackage = "org.microspring.mybatis.test.mapper";

    @Override
    public void postProcessBeanFactory(DefaultBeanFactory beanFactory) {
        try {
            scanMappers(beanFactory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanMappers(DefaultBeanFactory beanFactory) throws Exception {
        String path = basePackage.replace('.', '/');
        
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url != null) {
            File dir = new File(url.getFile());
            if (dir.exists() && dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = basePackage + "." + file.getName().replace(".class", "");
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
        
        // 创建 MapperFactoryBean 的 BeanDefinition
        DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(MapperFactoryBean.class);
        
        // 设置构造函数参数，而不是属性注入
        beanDefinition.addConstructorArg(new ConstructorArg(null, mapperInterface, Class.class));
        
        // 设置 sqlSessionFactory 属性
        beanDefinition.addPropertyValue(new PropertyValue("sqlSessionFactory", "sqlSessionFactory", SqlSessionFactory.class, true));
        
        // 注册 BeanDefinition
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    private String getBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.sqlSessionFactory == null) {
            throw new IllegalArgumentException("Property 'sqlSessionFactory' is required");
        }
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
    
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
} 