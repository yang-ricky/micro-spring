package org.microspring.aop;


public interface MethodInterceptor {
    

    Object invoke(MethodInvocation invocation) throws Throwable;
} 