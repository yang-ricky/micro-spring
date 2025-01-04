package org.microspring.aop;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.aop.advice.LogAdvice;
import org.microspring.aop.annotation.Loggable;
import org.microspring.aop.support.LoggingBeanPostProcessor;
import static org.junit.Assert.*;

public class AopProxyTest {
    
    public interface ITestService {
        String doSomething();
        void doWithException() throws Exception;
    }
    
    @Loggable
    public static class TestService implements ITestService {
        @Override
        public String doSomething() {
            return "Hello from TestService";
        }
        
        @Override
        public void doWithException() throws Exception {
            throw new Exception("Test Exception");
        }
    }
    
    @Loggable
    public static class NoInterfaceService {
        public String doSomething() {
            return "Hello from NoInterfaceService";
        }
        
        public void doWithException() throws Exception {
            throw new Exception("Test Exception from NoInterfaceService");
        }
    }
    
    @Test
    public void testAopProxy() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册LogAdvice
        DefaultBeanDefinition logAdviceDef = new DefaultBeanDefinition(LogAdvice.class);
        beanFactory.registerBeanDefinition("logAdvice", logAdviceDef);
        
        // 使用带参构造函数创建LoggingBeanPostProcessor
        beanFactory.addBeanPostProcessor(new LoggingBeanPostProcessor(beanFactory));
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(TestService.class);
        beanFactory.registerBeanDefinition("testService", bd);
        
        ITestService service = (ITestService) beanFactory.getBean("testService");
        assertNotNull(service);
        
        // 测试正常方法调用
        String result = service.doSomething();
        assertEquals("Hello from TestService", result);
        
        // 测试异常情况
        try {
            service.doWithException();
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("Test Exception", e.getMessage());
        }
    }
    
    @Test
    public void testCglibProxy() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.addBeanPostProcessor(new LoggingBeanPostProcessor(beanFactory));
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(NoInterfaceService.class);
        beanFactory.registerBeanDefinition("noInterfaceService", bd);
        
        NoInterfaceService service = (NoInterfaceService) beanFactory.getBean("noInterfaceService");
        assertNotNull(service);
        
        // 测试正常方法调用
        String result = service.doSomething();
        assertEquals("Hello from NoInterfaceService", result);
        
        // 测试异常情况
        try {
            service.doWithException();
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("Test Exception from NoInterfaceService", e.getMessage());
        }
    }
} 