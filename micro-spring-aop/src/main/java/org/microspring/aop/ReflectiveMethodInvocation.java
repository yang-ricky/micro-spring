package org.microspring.aop;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Implementation of MethodInvocation that reflects an invocation of a method
 * on a target object with a chain of interceptors.
 */
public class ReflectiveMethodInvocation implements MethodInvocation {
    
    private final Object target;
    private final Method method;
    private final Object[] arguments;
    private final List<MethodInterceptor> interceptors;
    private int currentInterceptorIndex = -1;

    public ReflectiveMethodInvocation(Object target, Method method, Object[] arguments, 
                                    List<MethodInterceptor> interceptors) {
        this.target = target;
        this.method = method;
        this.arguments = arguments;
        this.interceptors = interceptors;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        // If we've reached the end of the interceptor chain, invoke the target method
        if (currentInterceptorIndex == interceptors.size() - 1) {
            return method.invoke(target, arguments);
        }
        
        // Get next interceptor and invoke it
        MethodInterceptor interceptor = interceptors.get(++currentInterceptorIndex);
        return interceptor.invoke(this);
    }
} 