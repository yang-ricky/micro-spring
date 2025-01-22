package org.microspring.aop;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.aop.advice.LogAdvice;
import org.microspring.aop.support.AspectBeanPostProcessor;
import org.microspring.aop.annotation.Loggable;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

public class AspectTest {
    
    public interface ITestService {
        String doSomething();
    }
    
    @Loggable
    public static class TestService implements ITestService {
        @Override
        public String doSomething() {
            return "done";
        }
    }
    
    @Aspect(order = 2)
    public static class LoggingAspect extends LogAdvice {
        @Override
        public void before(Method method, Object[] args) {
            System.out.println("[LoggingAspect] Before method: " + method.getName());
        }
        
        @Override
        public void afterReturning(Method method, Object result) {
            System.out.println("[LoggingAspect] After method: " + method.getName());
        }
        
        @Override
        public void afterThrowing(Method method, Exception ex) {
            System.out.println("[LoggingAspect] Exception in method: " + method.getName());
        }
    }
    
    @Aspect(order = 1)
    public static class PerformanceAspect extends LogAdvice {
        private long startTime;
        
        @Override
        public void before(Method method, Object[] args) {
            startTime = System.currentTimeMillis();
            System.out.println("[Performance] Starting method: " + method.getName());
        }
        
        @Override
        public void afterReturning(Method method, Object result) {
            long endTime = System.currentTimeMillis();
            System.out.println("[Performance] Method: " + method.getName() + " took " + (endTime - startTime) + "ms");
        }
    }
    
    @Test
    public void testMultipleAspects() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            
            // 注册切面处理器
            beanFactory.addBeanPostProcessor(new AspectBeanPostProcessor(beanFactory));
            
            // 注册两个切面
            DefaultBeanDefinition loggingAspectDef = new DefaultBeanDefinition(LoggingAspect.class);
            beanFactory.registerBeanDefinition("loggingAspect", loggingAspectDef);
            
            DefaultBeanDefinition performanceAspectDef = new DefaultBeanDefinition(PerformanceAspect.class);
            beanFactory.registerBeanDefinition("performanceAspect", performanceAspectDef);
            
            // 先获取切面实例，确保它们被初始化
            beanFactory.getBean("loggingAspect");
            beanFactory.getBean("performanceAspect");
            
            // 注册被代理的服务
            DefaultBeanDefinition serviceDef = new DefaultBeanDefinition(TestService.class);
            beanFactory.registerBeanDefinition("testService", serviceDef);
            
            // 获取并调用服务
            ITestService service = (ITestService) beanFactory.getBean("testService");
            service.doSomething();
            
            String output = outputStream.toString();
            System.err.println("Actual output:\n" + output); // 使用err流输出调试信息
            
            // 验证切面执行顺序：Logging切面在Performance切面之后
            assertTrue("Should contain Performance aspect log", 
                output.contains("[Performance] Starting method"));
            assertTrue("Should contain Logging aspect log", 
                output.contains("[LoggingAspect] Before method"));
            assertTrue("Should contain method execution result", 
                output.contains("[LoggingAspect] After method"));
            assertTrue("Should contain performance measurement", 
                output.contains("[Performance] Method"));
            
            // 验证执行顺序
            int perfStart = output.indexOf("[Performance] Starting method");
            int logBefore = output.indexOf("[LoggingAspect] Before method");
            int logAfter = output.indexOf("[LoggingAspect] After method");
            int perfEnd = output.indexOf("[Performance] Method");
            
            assertTrue("Performance aspect should start first", perfStart >= 0);
            assertTrue("Logging aspect should follow", logBefore > perfStart);
            assertTrue("Logging aspect should complete", logAfter > logBefore);
            assertTrue("Performance aspect should end last", perfEnd > logAfter);
            
        } finally {
            System.setOut(originalOut);
        }
    }
} 