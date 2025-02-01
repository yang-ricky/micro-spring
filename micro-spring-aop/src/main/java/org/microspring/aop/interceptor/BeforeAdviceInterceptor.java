package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.adapter.MethodInvocationAdapter;

import java.lang.reflect.Method;

public class BeforeAdviceInterceptor implements MethodInterceptor {
    private final Object aspectInstance;
    private final Method adviceMethod;

    public BeforeAdviceInterceptor(Object aspectInstance, Method adviceMethod) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.adviceMethod.setAccessible(true);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        adviceMethod.invoke(aspectInstance, new MethodInvocationAdapter(invocation));
        return invocation.proceed();
    }
} 