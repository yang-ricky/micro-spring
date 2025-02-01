package org.microspring.aop;


public interface AopProxy {

    Object getProxy();


    Object getProxy(ClassLoader classLoader);
} 