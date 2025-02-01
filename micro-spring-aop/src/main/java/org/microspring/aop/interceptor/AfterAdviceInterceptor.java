package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.adapter.MethodInvocationAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


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
            Object result = invocation.proceed();
            return result;
        } catch (Throwable ex) {
            Throwable targetException = (ex instanceof InvocationTargetException) 
                ? ((InvocationTargetException) ex).getTargetException() 
                : ex;
            throw targetException;
        } finally {
            adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation));
        }
    }
} 