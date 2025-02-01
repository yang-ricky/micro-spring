package org.microspring.aop;

/**
 * Interface for method interception. Implementations can perform custom behavior
 * before and after the method invocation.
 */
public interface MethodInterceptor {
    
    /**
     * Implement this method to perform extra treatments before and after the invocation.
     * @param invocation the method invocation joinpoint
     * @return the result of the call to the method invocation
     * @throws Throwable if the interceptors or the target object throws an exception
     */
    Object invoke(MethodInvocation invocation) throws Throwable;
} 