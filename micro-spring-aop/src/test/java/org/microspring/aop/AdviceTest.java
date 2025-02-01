package org.microspring.aop;

import org.junit.Test;
import org.microspring.aop.annotation.*;
import org.microspring.aop.interceptor.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AdviceTest {

    // Test service interface
    interface TestService {
        String doSomething(String input);
        void throwException() throws Exception;
    }

    // Test service implementation
    static class TestServiceImpl implements TestService {
        @Override
        public String doSomething(String input) {
            return "Hello, " + input;
        }

        @Override
        public void throwException() throws Exception {
            throw new Exception("Test exception");
        }
    }

    // Test aspect
    static class TestAspect {
        private final List<String> executionOrder = new ArrayList<>();

        @Before("execution(* doSomething(..))")
        public void beforeAdvice(JoinPoint joinPoint) {
            executionOrder.add("before");
        }

        @After("execution(* doSomething(..))")
        public void afterAdvice(JoinPoint joinPoint) {
            executionOrder.add("after");
        }

        @Around("execution(* doSomething(..))")
        public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
            executionOrder.add("around-before");
            Object result = pjp.proceed();
            executionOrder.add("around-after");
            return result;
        }

        @AfterReturning(value = "execution(* doSomething(..))", returning = "result")
        public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
            executionOrder.add("afterReturning");
        }

        @AfterThrowing(value = "execution(* throwException())", throwing = "ex")
        public void afterThrowingAdvice(JoinPoint joinPoint, Exception ex) {
            executionOrder.add("afterThrowing");
            assertEquals("Test exception", ex.getMessage());
        }

        public List<String> getExecutionOrder() {
            return executionOrder;
        }
    }

    @Test
    public void testAdviceTypes() throws Exception {
        TestService target = new TestServiceImpl();
        TestAspect aspect = new TestAspect();

        // Create interceptors in the correct order (from outer to inner)
        List<MethodInterceptor> interceptors = new ArrayList<>();
        
        // 1. After advice is the outermost (will execute last in finally)
        interceptors.add(new AfterAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterAdvice", JoinPoint.class)));
            
        // 2. Before advice executes before proceeding
        interceptors.add(new BeforeAdviceInterceptor(aspect, 
            TestAspect.class.getMethod("beforeAdvice", JoinPoint.class)));
            
        // 3. AfterReturning should execute before After but after Around
        interceptors.add(new AfterReturningAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterReturningAdvice", JoinPoint.class, Object.class), "result"));
            
        // 4. Around advice is the innermost (closest to target method)
        interceptors.add(new AroundAdviceInterceptor(aspect,
            TestAspect.class.getMethod("aroundAdvice", ProceedingJoinPoint.class)));

        // Create proxy
        AopProxy proxy = new JdkDynamicAopProxy(target, interceptors);
        TestService proxyObject = (TestService) proxy.getProxy();

        // Test normal method execution
        String result = proxyObject.doSomething("World");
        assertEquals("Hello, World", result);

        // Verify execution order
        List<String> executionOrder = aspect.getExecutionOrder();
        assertEquals("before", executionOrder.get(0));
        assertEquals("around-before", executionOrder.get(1));
        assertEquals("around-after", executionOrder.get(2));
        assertEquals("afterReturning", executionOrder.get(3));
        assertEquals("after", executionOrder.get(4));

        // Clear execution order for exception test
        executionOrder.clear();

        // Test exception handling
        List<MethodInterceptor> exceptionInterceptors = new ArrayList<>();
        
        // After advice should still be the outermost
        exceptionInterceptors.add(new AfterAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterAdvice", JoinPoint.class)));
        // AfterThrowing advice
        exceptionInterceptors.add(new AfterThrowingAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterThrowingAdvice", JoinPoint.class, Exception.class), "ex"));

        proxy = new JdkDynamicAopProxy(target, exceptionInterceptors);
        proxyObject = (TestService) proxy.getProxy();

        try {
            proxyObject.throwException();
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("Test exception", e.getMessage());
            assertTrue("Should contain afterThrowing in execution order", 
                executionOrder.contains("afterThrowing"));
            assertTrue("Should contain after in execution order", 
                executionOrder.contains("after"));
        }
    }
} 