package org.microspring.core;

import org.junit.Test;
import org.microspring.core.io.XmlBeanDefinitionReader;

import static org.junit.Assert.*;

public class LazyInitTest {
    
    public static class LazyBean {
        public LazyBean() {
        }
    }
    
    public static class EagerBean {
        public EagerBean() {
        }
    }
    
    @Test
    public void testLazyInit() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册一个lazy-init的bean
        DefaultBeanDefinition beanDefinition = new DefaultBeanDefinition(LazyBean.class);
        beanDefinition.setLazyInit(true);
        beanFactory.registerBeanDefinition("lazyBean", beanDefinition);
        
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

        Object lazyBean = beanFactory.getBean("lazyBean");
        // 这时才应该看到LazyBean的创建日志
        
        assertNotNull(lazyBean);
        assertTrue(lazyBean instanceof LazyBean);
    }
} 