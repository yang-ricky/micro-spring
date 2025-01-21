package org.microspring.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.microspring.stereotype.Component;
import org.microspring.mybatis.annotation.Mapper;
import org.microspring.beans.factory.InitializingBean;
import org.microspring.core.BeanFactoryPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
public class MapperScannerConfigurer implements BeanFactoryPostProcessor, InitializingBean {
    
    private SqlSessionFactoryBean sqlSessionFactoryBean;
    private SqlSessionFactory sqlSessionFactory;
    private final Map<String, Object> mapperProxies = new HashMap<>();
    private String basePackage = "org.microspring.mybatis";
    
    @Override
    public void postProcessBeanFactory(DefaultBeanFactory beanFactory) {
        try {
            scanMappers(basePackage, beanFactory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan mapper interfaces", e);
        }
    }
    
    private void scanMappers(String basePackage, DefaultBeanFactory beanFactory) {
        try {
            String path = basePackage.replace('.', '/');
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url != null) {
                File dir = new File(url.getFile());
                if (dir.exists() && dir.isDirectory()) {
                    for (File file : dir.listFiles()) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = basePackage + '.' + file.getName().substring(0, file.getName().length() - 6);
                            processClass(className, beanFactory);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning package: " + basePackage, e);
        }
    }
    
    private void processClass(String className, DefaultBeanFactory beanFactory) {
        try {
            Class<?> mapperInterface = Class.forName(className);
            if (mapperInterface.isInterface() && mapperInterface.isAnnotationPresent(Mapper.class)) {
                String beanName = generateBeanName(mapperInterface);
                DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(mapperInterface);
                beanDefinition.setScope("singleton");
                beanFactory.registerBeanDefinition(beanName, beanDefinition);
            }
        } catch (ClassNotFoundException e) {
            // 忽略无法加载的类
        }
    }
    
    private String generateBeanName(Class<?> mapperInterface) {
        String shortName = mapperInterface.getSimpleName();
        return Character.toLowerCase(shortName.charAt(0)) + shortName.substring(1);
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        this.sqlSessionFactory = sqlSessionFactoryBean.getObject();
        
        // 为所有注册的 Mapper 接口创建代理
        String path = basePackage.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url != null) {
            File dir = new File(url.getFile());
            if (dir.exists() && dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = basePackage + '.' + file.getName().substring(0, file.getName().length() - 6);
                        try {
                            Class<?> mapperInterface = Class.forName(className);
                            if (mapperInterface.isInterface() && mapperInterface.isAnnotationPresent(Mapper.class)) {
                                String beanName = generateBeanName(mapperInterface);
                                Object proxy = createMapperProxy(mapperInterface);
                                mapperProxies.put(beanName, proxy);
                            }
                        } catch (ClassNotFoundException e) {
                            // 忽略无法加载的类
                        }
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T createMapperProxy(Class<T> mapperInterface) {
        if (sqlSessionFactory == null) {
            throw new IllegalStateException("SqlSessionFactory must be set before creating mapper proxy");
        }
        
        sqlSessionFactory.getConfiguration().addMapper(mapperInterface);
        return sqlSessionFactory.getConfiguration().getMapper(mapperInterface, sqlSessionFactory.openSession());
    }
    
    public void setSqlSessionFactoryBean(SqlSessionFactoryBean sqlSessionFactoryBean) {
        this.sqlSessionFactoryBean = sqlSessionFactoryBean;
    }
    
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
    
    public Object getMapperProxy(String name) {
        return mapperProxies.get(name);
    }
} 