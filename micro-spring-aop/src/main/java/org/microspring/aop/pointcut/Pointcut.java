package org.microspring.aop.pointcut;


public interface Pointcut {

    ClassFilter getClassFilter();


    MethodMatcher getMethodMatcher();

    Pointcut TRUE = TruePointcut.INSTANCE;
} 