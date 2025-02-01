package org.microspring.aop;

import java.lang.reflect.Method;


public interface MethodInvocation {


    Method getMethod();


    Object getTarget();


    Object[] getArguments();


    Object proceed() throws Throwable;
} 