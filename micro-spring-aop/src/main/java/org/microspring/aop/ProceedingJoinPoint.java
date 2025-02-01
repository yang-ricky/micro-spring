package org.microspring.aop;


public interface ProceedingJoinPoint extends JoinPoint {

    Object proceed() throws Throwable;

    Object proceed(Object[] args) throws Throwable;
} 