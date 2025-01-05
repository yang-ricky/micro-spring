package org.microspring.web.servlet;

import org.microspring.context.ApplicationContext;
import org.microspring.web.method.HandlerMethod;
import org.microspring.web.servlet.handler.RequestMappingHandlerMapping;
import org.microspring.web.annotation.ResponseBody;
import org.microspring.web.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {
    
    private ApplicationContext applicationContext;
    private HandlerMapping handlerMapping;
    
    public DispatcherServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void init() throws ServletException {
        this.handlerMapping = new RequestMappingHandlerMapping(applicationContext);
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
            
            Object result = handlerMethod.getMethod().invoke(handlerMethod.getBean());
            
            // 检查是否需要 JSON 响应
            if (isResponseBody(handlerMethod)) {
                response.setContentType("application/json;charset=UTF-8");
                String jsonResult = convertToJson(result);
                response.getWriter().write(jsonResult);
            } else {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(String.valueOf(result));
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
        // 这里可以使用 Jackson 或其他 JSON 库
        // 为了简单演示，我们先用简单的方式
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        }
        // TODO: 实现更完整的 JSON 转换
        return obj.toString();
    }
} 