package org.microspring.aop;

/**
 * Extension of the JoinPoint interface that adds the ability to control
 * the join point execution through proceed() method.
 */
public interface ProceedingJoinPoint extends JoinPoint {
    /**
     * Proceed with the next advice or target method invocation.
     * @return the result of proceeding
     * @throws Throwable if the invocation throws an exception
     */
    Object proceed() throws Throwable;

    /**
     * Proceed with the next advice or target method invocation with the specified arguments.
     * @param args the arguments to use for the invocation
     * @return the result of proceeding
     * @throws Throwable if the invocation throws an exception
     */
    Object proceed(Object[] args) throws Throwable;
} 