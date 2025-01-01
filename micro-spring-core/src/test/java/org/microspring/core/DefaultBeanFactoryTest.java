package org.microspring.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class DefaultBeanFactoryTest {
    
    // 测试用的简单Bean类
    public static class TestBean {
        private String message = "First Bean";
        
        public String getMessage() {
            return message;
        }
    }
    
    @Test
    public void testSingletonBean() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册BeanDefinition
        DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(TestBean.class);
        beanFactory.registerBeanDefinition("testBean", beanDefinition);
        
        // 获取Bean实例
        TestBean bean1 = beanFactory.getBean("testBean", TestBean.class);
        TestBean bean2 = beanFactory.getBean("testBean", TestBean.class);
        
        // 验证单例
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertSame("Singleton beans should be the same instance", bean1, bean2);
        assertEquals("First Bean", bean1.getMessage());
    }
    
    @Test
    public void testPrototypeBean() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册prototype作用域的BeanDefinition
        DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(TestBean.class);
        beanDefinition.setScope("prototype");
        beanFactory.registerBeanDefinition("prototypeBean", beanDefinition);
        
        // 获取Bean实例
        TestBean bean1 = beanFactory.getBean("prototypeBean", TestBean.class);
        TestBean bean2 = beanFactory.getBean("prototypeBean", TestBean.class);
        
        // 验证prototype
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertNotSame("Prototype beans should be different instances", bean1, bean2);
    }
} 