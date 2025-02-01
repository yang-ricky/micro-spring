package org.microspring.aop.interceptor;

import java.lang.reflect.Method;
import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;

/**
 * A simple logging interceptor that logs method entry and exit.
 */
public class LoggingMethodInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        String methodName = method.getName();
        
        System.out.println("Before method [" + methodName + "] execution");
        
        try {
            Object result = invocation.proceed();
            System.out.println("After method [" + methodName + "] execution with result: " + result);
            return result;
        } catch (Throwable ex) {
            System.out.println("Method [" + methodName + "] execution failed with exception: " + ex.getMessage());
            throw ex;
        }
    }
} 