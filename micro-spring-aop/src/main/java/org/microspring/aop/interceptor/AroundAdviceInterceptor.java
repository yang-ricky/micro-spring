package org.microspring.aop.interceptor;

import org.microspring.aop.MethodInterceptor;
import org.microspring.aop.MethodInvocation;
import org.microspring.aop.ProceedingJoinPoint;
import org.microspring.aop.ReflectiveMethodInvocation;

import java.lang.reflect.Method;


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
        ProceedingJoinPoint pjp = new MethodInvocationProceedingJoinPoint(invocation);
        return adviceMethod.invoke(aspectInstance, pjp);
    }

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
            return new ReflectiveMethodInvocation(
                methodInvocation.getTarget(),
                methodInvocation.getMethod(),
                args,
                ((ReflectiveMethodInvocation) methodInvocation).getInterceptors()
            ).proceed();
        }
    }
} 