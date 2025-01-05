package org.microspring.web.servlet;

import org.microspring.stereotype.Component;
import org.microspring.web.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestInterceptor implements HandlerInterceptor {
    public static final List<String> executionOrder = new ArrayList<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) {
        executionOrder.add("preHandle");
        // 测试请求拦截
        if (request.getRequestURI().contains("/blocked")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, Object result) {
        executionOrder.add("postHandle");
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        executionOrder.add("afterCompletion");
    }
    
    public static void clearExecutionOrder() {
        executionOrder.clear();
    }
} 