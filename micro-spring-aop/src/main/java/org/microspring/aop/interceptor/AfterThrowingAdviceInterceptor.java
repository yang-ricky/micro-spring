package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.adapter.MethodInvocationAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Interceptor for handling AfterThrowing advice.
 */
public class AfterThrowingAdviceInterceptor implements MethodInterceptor {
    private final Object aspectInstance;
    private final Method adviceMethod;
    private final String throwingParameterName;

    public AfterThrowingAdviceInterceptor(Object aspectInstance, Method adviceMethod, String throwingParameterName) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.throwingParameterName = throwingParameterName;
        this.adviceMethod.setAccessible(true); // Make the method accessible
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            Object result = invocation.proceed();
            return result;
        } catch (Throwable ex) {
            // Get the original exception if it's wrapped in InvocationTargetException
            Throwable targetException = (ex instanceof InvocationTargetException) 
                ? ((InvocationTargetException) ex).getTargetException() 
                : ex;
            
            // Execute after throwing advice with adapted JoinPoint and exception
            if (throwingParameterName != null && !throwingParameterName.isEmpty()) {
                if (targetException instanceof Exception) {
                    adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation), targetException);
                }
            } else {
                adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation));
            }
            
            throw targetException;
        }
    }
} 