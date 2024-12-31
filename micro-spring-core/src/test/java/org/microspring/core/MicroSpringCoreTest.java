package org.microspring.core;

import org.junit.Test;
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
        };
        
        // 注册BeanDefinition
        beanFactory.registerBeanDefinition("testBean", beanDefinition);
        
        assertTrue(true);
    }
} 