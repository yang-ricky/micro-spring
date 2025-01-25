package org.microspring.mybatis.config;

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
        
        // 创建 MapperScannerConfigurer 的 BeanDefinition
        DefaultBeanDefinition definition = new DefaultBeanDefinition(MapperScannerConfigurer.class);
        
        // 设置属性
        String[] basePackages = mapperScan.basePackages().length > 0 ? 
            mapperScan.basePackages() : new String[]{mapperScan.value()};
        
        // 设置 basePackage 属性
        if (basePackages.length > 0) {
            String basePackage = String.join(",", basePackages);
            definition.getPropertyValues().add(
                new PropertyValue("basePackage", basePackage, String.class)
            );
        }
        
        // 设置 sqlSessionFactoryBean 属性
        String sqlSessionFactoryRef = mapperScan.sqlSessionFactoryRef();
        if (sqlSessionFactoryRef.isEmpty()) {
            sqlSessionFactoryRef = "sqlSessionFactory"; // 默认名称
        }
        definition.getPropertyValues().add(
            new PropertyValue("sqlSessionFactoryBean", sqlSessionFactoryRef, null, true)
        );
        
        // 注册 BeanDefinition
        String beanName = MapperScannerConfigurer.class.getName() + "#" + 
            Integer.toHexString(System.identityHashCode(definition));
        beanFactory.registerBeanDefinition(beanName, definition);
        
    }
} 