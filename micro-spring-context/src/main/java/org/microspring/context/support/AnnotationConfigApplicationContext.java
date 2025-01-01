package org.microspring.context.support;

import org.microspring.core.BeanDefinition;

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
} 