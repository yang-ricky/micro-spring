package org.microspring.aop;

import java.lang.reflect.Method;

/**
 * Interface that provides access to the current join point during method invocation.
 */
public interface JoinPoint {
    /**
     * Get the method being called.
     */
    Method getMethod();

    /**
     * Get the target object being called.
     */
    Object getTarget();

    /**
     * Get the arguments passed to the method.
     */
    Object[] getArgs();

    /**
     * Get the signature (description) of this join point.
     */
    String getSignature();
} 