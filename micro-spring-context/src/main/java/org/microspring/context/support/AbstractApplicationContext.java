package org.microspring.context.support;

import org.microspring.context.ApplicationContext;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.core.io.ClassPathBeanDefinitionScanner;

import java.util.List;

public abstract class AbstractApplicationContext implements ApplicationContext {
    protected final DefaultBeanFactory beanFactory;
    protected final long startupDate;
    
    public AbstractApplicationContext() {
        this.beanFactory = new DefaultBeanFactory();
        this.startupDate = System.currentTimeMillis();
    }
    
    @Override
    public Object getBean(String name) {
        return beanFactory.getBean(name);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return beanFactory.getBean(name, requiredType);
    }
    
    @Override
    public long getStartupDate() {
        return startupDate;
    }
    
    @Override
    public boolean containsBean(String name) {
        return beanFactory.getBeanDefinition(name) != null;
    }
    
    protected void scanPackages(String... basePackages) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner();
        for (String basePackage : basePackages) {
            List<BeanDefinition> beanDefinitions = scanner.scan(basePackage);
            for (BeanDefinition bd : beanDefinitions) {
                String beanName = generateBeanName(bd.getBeanClass());
                beanFactory.registerBeanDefinition(beanName, bd);
            }
        }
    }
    
    private String generateBeanName(Class<?> beanClass) {
        String shortClassName = beanClass.getSimpleName();
        if (beanClass.getEnclosingClass() != null) {
            shortClassName = shortClassName.substring(shortClassName.lastIndexOf('$') + 1);
        }
        return Character.toLowerCase(shortClassName.charAt(0)) + shortClassName.substring(1);
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) {
        // 遍历所有BeanDefinition，找到匹配的类型
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (requiredType.isAssignableFrom(bd.getBeanClass())) {
                return (T) getBean(beanName);
            }
        }
        throw new RuntimeException("No bean of type '" + requiredType.getName() + "' is defined");
    }
} 