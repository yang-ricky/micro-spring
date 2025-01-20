package org.microspring.core;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import static org.junit.Assert.*;

public class DefaultBeanFactoryTest {
    
    // 测试用的简单Bean类
    public static class TestBean {
        private String message = "First Bean";
        
        public String getMessage() {
            return message;
        }
    }
    
    // 循环依赖测试类 A
    public static class CircularA {
        @Autowired
        private CircularB b;
        
        public CircularB getB() {
            return b;
        }
    }
    
    // 循环依赖测试类 B
    public static class CircularB {
        @Autowired
        private CircularA a;
        
        public CircularA getA() {
            return a;
        }
    }
    
    // 字段注入测试类
    public static class ServiceA {
        private String message = "ServiceA";
        public String getMessage() {
            return message;
        }
    }
    
    public static class ServiceB {
        @Autowired
        private ServiceA serviceA;
        
        @Autowired
        @Qualifier("specificServiceA")
        private ServiceA specificServiceA;
        
        public ServiceA getServiceA() {
            return serviceA;
        }
        
        public ServiceA getSpecificServiceA() {
            return specificServiceA;
        }
    }
    
    public static class SpecificServiceA extends ServiceA {
        @Override
        public String getMessage() {
            return "SpecificServiceA";
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
    
    @Test
    public void testCircularDependency() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册循环依赖的bean
        DefaultBeanDefinition defA = new DefaultBeanDefinition(CircularA.class);
        DefaultBeanDefinition defB = new DefaultBeanDefinition(CircularB.class);
        beanFactory.registerBeanDefinition("circularA", defA);
        beanFactory.registerBeanDefinition("circularB", defB);
        
        // 获取bean实例
        CircularA a = beanFactory.getBean("circularA", CircularA.class);
        CircularB b = beanFactory.getBean("circularB", CircularB.class);
        
        // 验证循环依赖解决
        assertNotNull("CircularA should not be null", a);
        assertNotNull("CircularB should not be null", b);
        assertNotNull("B reference in A should not be null", a.getB());
        assertNotNull("A reference in B should not be null", b.getA());
        
        // 验证是否是相同的实例
        assertSame("Should get the same instance of CircularB", b, a.getB());
        assertSame("Should get the same instance of CircularA", a, b.getA());
        
        // 验证循环引用是否正确
        assertSame("Circular reference A->B->A should lead back to the same A", 
                  a, a.getB().getA());
        assertSame("Circular reference B->A->B should lead back to the same B", 
                  b, b.getA().getB());
    }
    
    @Test
    public void testFieldInjectionWithQualifier() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册所有需要的bean
        DefaultBeanDefinition defServiceA = new DefaultBeanDefinition(ServiceA.class);
        DefaultBeanDefinition defSpecificServiceA = new DefaultBeanDefinition(SpecificServiceA.class);
        DefaultBeanDefinition defServiceB = new DefaultBeanDefinition(ServiceB.class);
        
        beanFactory.registerBeanDefinition("serviceA", defServiceA);
        beanFactory.registerBeanDefinition("specificServiceA", defSpecificServiceA);
        beanFactory.registerBeanDefinition("serviceB", defServiceB);
        
        // 获取ServiceB实例
        ServiceB serviceB = beanFactory.getBean("serviceB", ServiceB.class);
        
        // 验证注入
        assertNotNull("ServiceB should not be null", serviceB);
        assertNotNull("Injected ServiceA should not be null", serviceB.getServiceA());
        assertNotNull("Injected SpecificServiceA should not be null", serviceB.getSpecificServiceA());
        
        // 验证消息
        assertEquals("ServiceA", serviceB.getServiceA().getMessage());
        assertEquals("SpecificServiceA", serviceB.getSpecificServiceA().getMessage());
        
        // 验证是否注入了正确的实例
        assertTrue("Should inject SpecificServiceA for qualified field", 
                  serviceB.getSpecificServiceA() instanceof SpecificServiceA);
    }
} 