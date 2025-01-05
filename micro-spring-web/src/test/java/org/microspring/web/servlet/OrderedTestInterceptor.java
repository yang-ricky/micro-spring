package org.microspring.web.servlet;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.microspring.stereotype.Component;
import org.microspring.web.HandlerInterceptor;

@Component
public class OrderedTestInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) {
        TestInterceptor.executionOrder.add("preHandle1");
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, Object result) {
        TestInterceptor.executionOrder.add("postHandle1");
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        TestInterceptor.executionOrder.add("afterCompletion1");
    }
} 