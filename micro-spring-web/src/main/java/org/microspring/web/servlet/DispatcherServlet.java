package org.microspring.web.servlet;

import org.microspring.context.ApplicationContext;
import org.microspring.web.method.HandlerMethod;
import org.microspring.web.servlet.handler.RequestMappingHandlerMapping;
import org.microspring.web.annotation.ResponseBody;
import org.microspring.web.annotation.RestController;
import org.microspring.web.annotation.RestControllerAdvice;

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
    
    public DispatcherServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void init() throws ServletException {
        this.handlerMapping = new RequestMappingHandlerMapping(applicationContext);
        initGlobalExceptionHandlers();
    }
    
    private void initGlobalExceptionHandlers() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RestControllerAdvice.class);
        this.globalExceptionHandlers = new ArrayList<>(beans.values());
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            HandlerMethod handlerMethod = handlerMapping.getHandler(request);
            if (handlerMethod == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Object result = handlerMethod.invokeAndHandle(request, response, globalExceptionHandlers);
            if (result != null) {
                if (result instanceof String && !isResponseBody(handlerMethod)) {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write((String) result);
                } else {
                    response.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(response.getWriter(), result);
                }
            }
        } catch (MethodNotAllowedException e) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } catch (Exception e) {
            throw new ServletException("Error invoking handler method", e);
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