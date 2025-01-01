package org.microspring.context.support;

import org.microspring.core.BeanDefinition;
import org.microspring.core.annotation.Component;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.ArrayList;
import java.util.List;

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

    private void registerBean(Class<?> beanClass) {
        Component component = beanClass.getAnnotation(Component.class);
        if (component != null) {
            String beanName = component.value();
            if (beanName.isEmpty()) {
                beanName = Character.toLowerCase(beanClass.getSimpleName().charAt(0)) 
                        + beanClass.getSimpleName().substring(1);
            }
            
            BeanDefinition bd = new BeanDefinition() {
                @Override
                public Class<?> getBeanClass() {
                    return beanClass;
                }
                
                @Override
                public String getScope() {
                    Scope scope = beanClass.getAnnotation(Scope.class);
                    return scope != null ? scope.value() : "singleton";
                }
                
                @Override
                public boolean isSingleton() {
                    return "singleton".equals(getScope());
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
                    return new ArrayList<>();
                }
                
                @Override
                public void addConstructorArg(ConstructorArg arg) {
                    // 注解配置不支持
                }
                
                @Override
                public void addPropertyValue(PropertyValue propertyValue) {
                    // 注解配置不支持
                }
            };
            
            beanFactory.registerBeanDefinition(beanName, bd);
        }
    }
} 