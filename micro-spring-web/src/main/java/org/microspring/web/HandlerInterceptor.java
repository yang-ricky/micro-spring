package org.microspring.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HandlerInterceptor {
    /**
     * 在处理器执行前调用
     * @return true 继续执行，false 中断执行
     */
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) throws Exception {
        return true;
    }
    
    /**
     * 在处理器执行后调用
     */
    default void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, Object result) throws Exception {
    }
    
    /**
     * 在请求完成后调用(包括视图渲染后)
     */
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
    }
} 