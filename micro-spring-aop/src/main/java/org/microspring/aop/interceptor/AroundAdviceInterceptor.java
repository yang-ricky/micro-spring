package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.ProceedingJoinPoint;
import org.microspring.aop.ReflectiveMethodInvocation;

import java.lang.reflect.Method;

/**
 * Interceptor for handling Around advice.
 */
public class AroundAdviceInterceptor implements MethodInterceptor {
    private final Object aspectInstance;
    private final Method adviceMethod;

    public AroundAdviceInterceptor(Object aspectInstance, Method adviceMethod) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.adviceMethod.setAccessible(true); // Make the method accessible
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // Create ProceedingJoinPoint from MethodInvocation
        ProceedingJoinPoint pjp = new MethodInvocationProceedingJoinPoint(invocation);
        // Execute around advice with ProceedingJoinPoint
        return adviceMethod.invoke(aspectInstance, pjp);
    }

    /**
     * Adapts MethodInvocation to ProceedingJoinPoint interface
     */
    private static class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint {
        private final MethodInvocation methodInvocation;

        public MethodInvocationProceedingJoinPoint(MethodInvocation methodInvocation) {
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

        @Override
        public Object proceed() throws Throwable {
            return methodInvocation.proceed();
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            // Create a new MethodInvocation with the new arguments
            return new ReflectiveMethodInvocation(
                methodInvocation.getTarget(),
                methodInvocation.getMethod(),
                args,
                ((ReflectiveMethodInvocation) methodInvocation).getInterceptors()
            ).proceed();
        }
    }
} 