package org.microspring.web.servlet;

import org.microspring.stereotype.Component;
import org.microspring.web.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SecondTestInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) {
        TestInterceptor.executionOrder.add("preHandle2");
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, Object result) {
        TestInterceptor.executionOrder.add("postHandle2");
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        TestInterceptor.executionOrder.add("afterCompletion2");
    }
} 