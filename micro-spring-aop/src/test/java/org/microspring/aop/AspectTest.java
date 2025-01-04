package org.microspring.aop;

import org.junit.Test;
import org.microspring.aop.advice.LogAdvice;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import static org.junit.Assert.*;

public class AspectTest {
    
    public interface ITestService {
        String doSomething();
    }
    
    @Aspect
    public static class TestService implements ITestService {
        @Override
        public String doSomething() {
            System.out.println("TestService is doing something");
            return "done";
        }
    }
    
    @Test
    public void testAspectWithPointcut() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册切面
        DefaultBeanDefinition logAdviceDef = new DefaultBeanDefinition(LogAdvice.class);
        beanFactory.registerBeanDefinition("logAdvice", logAdviceDef);
        
        // 注册被代理的服务
        DefaultBeanDefinition serviceDef = new DefaultBeanDefinition(TestService.class);
        beanFactory.registerBeanDefinition("testService", serviceDef);
        
        // 验证切面是否生效
        ITestService proxy = (ITestService) beanFactory.getBean("testService");
        assertNotNull(proxy);
        
        String result = proxy.doSomething();
        assertEquals("done", result);
        // 应该看到before和after的日志输出
    }
} 