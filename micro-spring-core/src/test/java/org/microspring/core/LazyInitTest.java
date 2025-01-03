package org.microspring.core;

import org.junit.Test;
import org.microspring.core.io.XmlBeanDefinitionReader;

import static org.junit.Assert.*;

public class LazyInitTest {
    
    public static class LazyBean {
        public LazyBean() {
            System.out.println("LazyBean constructor called");
        }
    }
    
    public static class EagerBean {
        public EagerBean() {
            System.out.println("EagerBean constructor called");
        }
    }
    
    @Test
    public void testLazyInit() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册一个lazy-init的bean
        DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(LazyBean.class);
        beanDefinition.setLazyInit(true);
        beanFactory.registerBeanDefinition("lazyBean", beanDefinition);
        
        // 此时不应该创建bean
        System.out.println("Before getBean call");
        
        // 第一次获取时才创建
        Object lazyBean = beanFactory.getBean("lazyBean");
        assertNotNull(lazyBean);
        assertTrue(lazyBean instanceof LazyBean);
        
        // 再次获取应该是同一个实例
        Object sameLazyBean = beanFactory.getBean("lazyBean");
        assertSame(lazyBean, sameLazyBean);
    }

    @Test
    public void testXmlLazyInit() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions("lazyContext.xml");
        
        System.out.println("Context initialized");
        // 此时应该只看到EagerBean的创建日志
        
        System.out.println("Before getting lazyBean");
        Object lazyBean = beanFactory.getBean("lazyBean");
        // 这时才应该看到LazyBean的创建日志
        
        assertNotNull(lazyBean);
        assertTrue(lazyBean instanceof LazyBean);
    }
} 