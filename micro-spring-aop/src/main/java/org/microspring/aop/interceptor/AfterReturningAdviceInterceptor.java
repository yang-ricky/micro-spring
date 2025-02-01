package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.adapter.MethodInvocationAdapter;

import java.lang.reflect.Method;

/**
 * Interceptor for handling AfterReturning advice.
 */
public class AfterReturningAdviceInterceptor implements MethodInterceptor {
    private final Object aspectInstance;
    private final Method adviceMethod;
    private final String returningParameterName;

    public AfterReturningAdviceInterceptor(Object aspectInstance, Method adviceMethod, String returningParameterName) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.returningParameterName = returningParameterName;
        this.adviceMethod.setAccessible(true); // Make the method accessible
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object returnValue = invocation.proceed();
        
        // Execute after returning advice with adapted JoinPoint and return value if parameter name is specified
        if (returningParameterName != null && !returningParameterName.isEmpty()) {
            adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation), returnValue);
        } else {
            adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation));
        }
        
        return returnValue;
    }
} 