package org.microspring.core;

import org.junit.Test;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class MicroSpringCoreTest {
    
    @Test
    public void testMicroSpringStartup() {
        System.out.println("Micro Spring Core 启动");
        
        // 创建BeanFactory实例
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 创建一个简单的BeanDefinition
        BeanDefinition beanDefinition = new BeanDefinition() {
            @Override
            public Class<?> getBeanClass() {
                return String.class;
            }
            
            @Override
            public String getScope() {
                return "singleton";
            }
            
            @Override
            public boolean isSingleton() {
                return true;
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
                // 测试用例不需要实现
            }
            
            @Override
            public void addPropertyValue(PropertyValue propertyValue) {
                // 测试用例不需要实现
            }
        };
        
        // 注册BeanDefinition
        beanFactory.registerBeanDefinition("testBean", beanDefinition);
        
        assertTrue(true);
    }
} 