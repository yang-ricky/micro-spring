package org.microspring.core.exception;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import static org.junit.Assert.*;

public class ExceptionHandlingTest {

    @Test
    public void testNoSuchBeanDefinitionException() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        try {
            beanFactory.getBean("nonExistentBean");
            fail("Should throw NoSuchBeanDefinitionException");
        } catch (NoSuchBeanDefinitionException e) {
            assertEquals("nonExistentBean", e.getBeanName());
            assertTrue(e.getMessage().contains("No bean named 'nonExistentBean' is defined"));
        }
    }

    @Test
    public void testBeanCreationExceptionForInvalidClass() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        try {
            // 使用一个不存在的类
            Class<?> nonExistentClass = Class.forName("org.microspring.NonExistentClass");
            DefaultBeanDefinition bd = new DefaultBeanDefinition(nonExistentClass);
            beanFactory.registerBeanDefinition("invalidBean", bd);
            fail("Should throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // 预期的异常
            assertTrue(e.getMessage().contains("org.microspring.NonExistentClass"));
        }
    }

    @Test
    public void testBeanCreationExceptionForInvalidInitMethod() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(TestBean.class);
        bd.setInitMethodName("nonExistentInitMethod");
        beanFactory.registerBeanDefinition("beanWithInvalidInit", bd);
        
        try {
            beanFactory.getBean("beanWithInvalidInit");
            fail("Should throw BeanCreationException");
        } catch (BeanCreationException e) {
            assertEquals("beanWithInvalidInit", e.getBeanName());
            assertTrue(e.getMessage().contains("Init method [nonExistentInitMethod] not found"));
        }
    }

    @Test
    public void testBeanCreationExceptionForInvalidDestroyMethod() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(TestBean.class);
        bd.setDestroyMethodName("nonExistentDestroyMethod");
        beanFactory.registerBeanDefinition("beanWithInvalidDestroy", bd);
        
        beanFactory.getBean("beanWithInvalidDestroy"); // 先创建Bean
        
        try {
            beanFactory.close();
            fail("Should throw BeanCreationException");
        } catch (BeanCreationException e) {
            assertTrue(e.getMessage().contains("Destroy method [nonExistentDestroyMethod] not found"));
        }
    }

    @Test
    public void testBeanCreationExceptionForTypeMismatch() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册一个String类型的Bean
        DefaultBeanDefinition bd = new DefaultBeanDefinition(String.class);
        beanFactory.registerBeanDefinition("stringBean", bd);
        
        try {
            // 尝试获取Integer类型
            beanFactory.getBean("stringBean", Integer.class);
            fail("Should throw BeanCreationException");
        } catch (BeanCreationException e) {
            assertEquals("stringBean", e.getBeanName());
            assertTrue(e.getMessage().contains("Bean is not of required type"));
        }
    }

    // 测试用的简单Bean类
    public static class TestBean {
        public TestBean() {}
    }
} 