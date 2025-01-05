package org.microspring.web.servlet;

import org.microspring.context.ApplicationContext;
import org.microspring.web.method.HandlerMethod;
import org.microspring.web.servlet.handler.RequestMappingHandlerMapping;
import org.microspring.web.annotation.ResponseBody;
import org.microspring.web.annotation.RestController;
import org.microspring.web.annotation.RestControllerAdvice;
import org.microspring.web.HandlerInterceptor;
import org.microspring.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class DispatcherServlet extends HttpServlet {
    
    private ApplicationContext applicationContext;
    private HandlerMapping handlerMapping;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Object> globalExceptionHandlers;
    private List<HandlerInterceptor> interceptors = new ArrayList<>();
    
    public DispatcherServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void init() throws ServletException {
        this.handlerMapping = new RequestMappingHandlerMapping(applicationContext);
        initGlobalExceptionHandlers();
        
        // 初始化拦截器
        Map<String, Object> interceptorBeans = 
            applicationContext.getBeansWithAnnotation(Component.class);
        for (Object bean : interceptorBeans.values()) {
            if (bean instanceof HandlerInterceptor) {
                interceptors.add((HandlerInterceptor) bean);
            }
        }
    }
    
    private void initGlobalExceptionHandlers() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RestControllerAdvice.class);
        this.globalExceptionHandlers = new ArrayList<>(beans.values());
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HandlerMethod handlerMethod = null;
        Exception handlerException = null;
        
        try {
            // 先获取 handler，即使路径不存在也要获取
            try {
                handlerMethod = handlerMapping.getHandler(request);
            } catch (MethodNotAllowedException e) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            
            // 执行所有拦截器的 preHandle
            if (!applyPreHandle(request, response, handlerMethod)) {
                return;  // 如果有拦截器返回 false，直接返回
            }
            
            // 在拦截器执行后再检查 handler 是否存在
            if (handlerMethod == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Object result = handlerMethod.invokeAndHandle(request, response, globalExceptionHandlers);
            
            // 执行所有拦截器的 postHandle
            applyPostHandle(request, response, handlerMethod, result);
            
            if (result != null) {
                if (result instanceof String && !isResponseBody(handlerMethod)) {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write((String) result);
                } else {
                    response.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(response.getWriter(), result);
                }
            }
            
        } catch (Exception ex) {
            handlerException = ex;
            if (ex instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) ex;
            }
            throw new ServletException("Error invoking handler method", ex);
        } finally {
            // 执行所有拦截器的 afterCompletion
            triggerAfterCompletion(request, response, handlerMethod, handlerException);
        }
    }
    
    private boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) throws Exception {
        for (HandlerInterceptor interceptor : interceptors) {
            if (!interceptor.preHandle(request, response, handler)) {
                return false;
            }
        }
        return true;
    }
    
    private void applyPostHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, Object result) throws Exception {
        for (HandlerInterceptor interceptor : interceptors) {
            interceptor.postHandle(request, response, handler, result);
        }
    }
    
    private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        for (HandlerInterceptor interceptor : interceptors) {
            try {
                interceptor.afterCompletion(request, response, handler, ex);
            } catch (Throwable e) {
                System.err.println("HandlerInterceptor.afterCompletion threw exception: " + e);
            }
        }
    }
    
    private boolean isResponseBody(HandlerMethod handlerMethod) {
        // 检查方法上是否有 @ResponseBody
        if (handlerMethod.getMethod().isAnnotationPresent(ResponseBody.class)) {
            return true;
        }
        // 检查类上是否有 @ResponseBody 或 @RestController
        Class<?> controllerClass = handlerMethod.getBean().getClass();
        return controllerClass.isAnnotationPresent(ResponseBody.class) ||
               controllerClass.isAnnotationPresent(RestController.class);
    }
    
    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }
} 