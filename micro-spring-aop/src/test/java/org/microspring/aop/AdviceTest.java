package org.microspring.aop;

import org.junit.Test;
import org.microspring.aop.annotation.*;
import org.microspring.aop.interceptor.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AdviceTest {

    interface TestService {
        String doSomething(String input);
        void throwException() throws Exception;
    }

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

        List<MethodInterceptor> interceptors = new ArrayList<>();
        
        interceptors.add(new AfterAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterAdvice", JoinPoint.class)));
            
        interceptors.add(new BeforeAdviceInterceptor(aspect, 
            TestAspect.class.getMethod("beforeAdvice", JoinPoint.class)));
            
        interceptors.add(new AfterReturningAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterReturningAdvice", JoinPoint.class, Object.class), "result"));
            
        interceptors.add(new AroundAdviceInterceptor(aspect,
            TestAspect.class.getMethod("aroundAdvice", ProceedingJoinPoint.class)));


        AopProxy proxy = new JdkDynamicAopProxy(target, interceptors);
        TestService proxyObject = (TestService) proxy.getProxy();

        String result = proxyObject.doSomething("World");
        assertEquals("Hello, World", result);


        List<String> executionOrder = aspect.getExecutionOrder();
        assertEquals("before", executionOrder.get(0));
        assertEquals("around-before", executionOrder.get(1));
        assertEquals("around-after", executionOrder.get(2));
        assertEquals("afterReturning", executionOrder.get(3));
        assertEquals("after", executionOrder.get(4));


        executionOrder.clear();


        List<MethodInterceptor> exceptionInterceptors = new ArrayList<>();
        

        exceptionInterceptors.add(new AfterAdviceInterceptor(aspect,
            TestAspect.class.getMethod("afterAdvice", JoinPoint.class)));

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