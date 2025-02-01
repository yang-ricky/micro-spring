package org.microspring.aop;

import java.lang.reflect.Method;


public interface JoinPoint {

    Method getMethod();


    Object getTarget();


    Object[] getArgs();


    String getSignature();
} 