package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.adapter.MethodInvocationAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Interceptor for handling After advice.
 */
public class AfterAdviceInterceptor implements MethodInterceptor {
    private final Object aspectInstance;
    private final Method adviceMethod;

    public AfterAdviceInterceptor(Object aspectInstance, Method adviceMethod) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.adviceMethod.setAccessible(true);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            // First proceed with the remaining interceptor chain
            Object result = invocation.proceed();
            return result;
        } catch (Throwable ex) {
            // Get the original exception if it's wrapped in InvocationTargetException
            Throwable targetException = (ex instanceof InvocationTargetException) 
                ? ((InvocationTargetException) ex).getTargetException() 
                : ex;
            throw targetException;
        } finally {
            // Execute after advice with adapted JoinPoint
            adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation));
        }
    }
} 