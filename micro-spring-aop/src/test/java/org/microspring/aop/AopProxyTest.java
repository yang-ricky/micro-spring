package org.microspring.aop;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.aop.advice.LogAdvice;
import org.microspring.aop.annotation.Loggable;
import org.microspring.aop.support.LoggingBeanPostProcessor;
import org.microspring.aop.interceptor.LoggingMethodInterceptor;
import java.util.Collections;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
        // 捕获控制台输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            
            // 注册LogAdvice
            DefaultBeanDefinition logAdviceDef = new DefaultBeanDefinition(LogAdvice.class);
            beanFactory.registerBeanDefinition("logAdvice", logAdviceDef);
            
            beanFactory.addBeanPostProcessor(new LoggingBeanPostProcessor(beanFactory));
            
            DefaultBeanDefinition bd = new DefaultBeanDefinition(TestService.class);
            beanFactory.registerBeanDefinition("testService", bd);
            
            ITestService service = (ITestService) beanFactory.getBean("testService");
            assertNotNull(service);
            
            // 测试正常方法调用
            String result = service.doSomething();
            assertEquals("Hello from TestService", result);
            
            String output = outputStream.toString();
            assertTrue("Should log before method execution", 
                output.contains("[LogAdvice] Before method: doSomething"));
            assertTrue("Should log after method execution", 
                output.contains("[LogAdvice] After method: doSomething"));
            
            outputStream.reset();
            
            // 测试异常情况
            try {
                service.doWithException();
                fail("Should throw exception");
            } catch (Exception e) {
                assertEquals("Test Exception", e.getMessage());
                output = outputStream.toString();
                assertTrue("Should log before method execution", 
                    output.contains("[LogAdvice] Before method: doWithException"));
                assertTrue("Should log exception", 
                    output.contains("[LogAdvice] Exception in method: doWithException"));
            }
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    public void testCglibProxy() {
        // 捕获控制台输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            
            // 注册LogAdvice
            DefaultBeanDefinition logAdviceDef = new DefaultBeanDefinition(LogAdvice.class);
            beanFactory.registerBeanDefinition("logAdvice", logAdviceDef);
            
            beanFactory.addBeanPostProcessor(new LoggingBeanPostProcessor(beanFactory));
            
            DefaultBeanDefinition bd = new DefaultBeanDefinition(NoInterfaceService.class);
            beanFactory.registerBeanDefinition("noInterfaceService", bd);
            
            NoInterfaceService service = (NoInterfaceService) beanFactory.getBean("noInterfaceService");
            assertNotNull(service);
            
            // 测试正常方法调用
            String result = service.doSomething();
            assertEquals("Hello from NoInterfaceService", result);
            
            String output = outputStream.toString();
            assertTrue("Should log before method execution", 
                output.contains("[LogAdvice(CGLIB)] Before method: doSomething"));
            assertTrue("Should log after method execution", 
                output.contains("[LogAdvice(CGLIB)] After method: doSomething"));
            
            outputStream.reset();
            
            // 测试异常情况
            try {
                service.doWithException();
                fail("Should throw exception");
            } catch (Exception e) {
                assertEquals("Test Exception from NoInterfaceService", e.getMessage());
                output = outputStream.toString();
                assertTrue("Should log before method execution", 
                    output.contains("[LogAdvice(CGLIB)] Before method: doWithException"));
                assertTrue("Should log exception", 
                    output.contains("[LogAdvice(CGLIB)] Exception in method: doWithException"));
            }
        } finally {
            System.setOut(originalOut);
        }
    }

    // Test interface
    interface Calculator {
        int add(int a, int b);
    }

    // Test implementation
    static class SimpleCalculator implements Calculator {
        @Override
        public int add(int a, int b) {
            return a + b;
        }
    }

    @Test
    public void testProxyWithLoggingInterceptor() {
        // Create target object
        Calculator target = new SimpleCalculator();
        
        // Create proxy with logging interceptor
        AopProxy proxy = new JdkDynamicAopProxy(
            target, 
            Collections.singletonList(new LoggingMethodInterceptor())
        );
        
        // Get proxy object
        Calculator proxyObject = (Calculator) proxy.getProxy();
        
        // Execute method - should log before and after
        int result = proxyObject.add(5, 3);
        
        // Verify result
        assertEquals(8, result);
    }
} 