package org.microspring.mybatis.config;

import java.util.Arrays;

import org.microspring.context.annotation.ImportBeanDefinitionRegistrar;
import org.microspring.core.BeanDefinition;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.mybatis.MapperScannerConfigurer;
import org.microspring.mybatis.annotation.MapperScan;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.beans.PropertyValue;

public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar {
    
    @Override
    public void registerBeanDefinitions(Class<?> importingClass, DefaultBeanFactory beanFactory) {
        MapperScan mapperScan = importingClass.getAnnotation(MapperScan.class);
        if (mapperScan == null) {
            return;
        }
        
        // 创建 MapperScannerConfigurer 的实例
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        
        // 设置属性
        String[] basePackages = mapperScan.basePackages().length > 0 ? 
            mapperScan.basePackages() : new String[]{mapperScan.value()};

        // 设置 basePackage 属性
        if (basePackages.length > 0) {
            String basePackage = String.join(",", basePackages);
            configurer.setBasePackage(basePackage);
        }
        
        // 创建 BeanDefinition
        DefaultBeanDefinition definition = new DefaultBeanDefinition(MapperScannerConfigurer.class);
        
        // 添加属性值
        definition.getPropertyValues().add(
            new PropertyValue("basePackage", configurer.getBasePackage(), String.class)
        );
        
        String sqlSessionFactoryRef = mapperScan.sqlSessionFactoryRef();
        if (sqlSessionFactoryRef.isEmpty()) {
            sqlSessionFactoryRef = "sqlSessionFactory";
        }
        definition.getPropertyValues().add(
            new PropertyValue("sqlSessionFactory", sqlSessionFactoryRef, String.class, true)
        );
        
        // 注册 BeanDefinition
        String beanName = "mapperScannerConfigurer";
        beanFactory.registerBeanDefinition(beanName, definition);
    }
} 