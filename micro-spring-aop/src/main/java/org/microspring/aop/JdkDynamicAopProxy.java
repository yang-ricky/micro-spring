package org.microspring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK-based AopProxy implementation that uses JDK dynamic proxies.
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

    private final Object target;
    private final List<MethodInterceptor> interceptors;

    public JdkDynamicAopProxy(Object target, List<MethodInterceptor> interceptors) {
        this.target = target;
        this.interceptors = interceptors;
    }

    @Override
    public Object getProxy() {
        return getProxy(target.getClass().getClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?>[] interfaces = target.getClass().getInterfaces();
        return Proxy.newProxyInstance(classLoader, interfaces, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Create method invocation with the interceptor chain
        ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(
            target, method, args, interceptors
        );
        
        // Start the interceptor chain
        return invocation.proceed();
    }
} 