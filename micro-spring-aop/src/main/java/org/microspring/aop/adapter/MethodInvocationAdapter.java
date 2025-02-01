package org.microspring.aop.adapter;

import org.microspring.aop.JoinPoint;
import org.microspring.aop.MethodInvocation;

import java.lang.reflect.Method;

/**
 * Adapter class that adapts MethodInvocation to JoinPoint interface
 */
public class MethodInvocationAdapter implements JoinPoint {
    private final MethodInvocation methodInvocation;

    public MethodInvocationAdapter(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }

    @Override
    public Method getMethod() {
        return methodInvocation.getMethod();
    }

    @Override
    public Object getTarget() {
        return methodInvocation.getTarget();
    }

    @Override
    public Object[] getArgs() {
        return methodInvocation.getArguments();
    }

    @Override
    public String getSignature() {
        Method method = getMethod();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
} 