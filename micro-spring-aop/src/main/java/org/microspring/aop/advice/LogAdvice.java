package org.microspring.aop.advice;

import java.lang.reflect.Method;

import org.microspring.aop.Aspect;

@Aspect
public class LogAdvice {
    
    public void before(Method method, Object[] args) {
        System.out.println("[LogAdvice] Before method: " + method.getName());
    }
    
    public void afterReturning(Method method, Object result) {
        System.out.println("[LogAdvice] After method: " + method.getName() + 
            ", result: " + result);
    }
    
    public void afterThrowing(Method method, Exception ex) {
        System.out.println("[LogAdvice] Exception in method: " + method.getName() + 
            ", error: " + ex.getMessage());
    }
} 