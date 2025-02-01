package org.microspring.aop;

import java.lang.reflect.Method;

/**
 * Description of a method invocation in the AOP framework.
 * A method invocation is a joinpoint and can be intercepted by an interceptor.
 */
public interface MethodInvocation {

    /**
     * Get the method being called.
     * @return the method being called
     */
    Method getMethod();

    /**
     * Get the target object being called.
     * @return the target object being called
     */
    Object getTarget();

    /**
     * Get the arguments as an array object.
     * @return the argument array
     */
    Object[] getArguments();

    /**
     * Proceed with the invocation chain.
     * @return the return value of the method invocation
     * @throws Throwable if the method invocation throws an exception
     */
    Object proceed() throws Throwable;
} 