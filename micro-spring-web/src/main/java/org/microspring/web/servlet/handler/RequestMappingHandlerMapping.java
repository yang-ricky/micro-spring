package org.microspring.web.servlet.handler;

import org.microspring.context.ApplicationContext;
import org.microspring.web.annotation.Controller;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RestController;
import org.microspring.web.context.WebApplicationContext;
import org.microspring.web.method.HandlerMethod;
import org.microspring.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RequestMappingHandlerMapping implements HandlerMapping {
    
    private final Map<String, HandlerMethod> handlerMethods = new HashMap<>();
    
    public RequestMappingHandlerMapping(ApplicationContext applicationContext) {
        if (!(applicationContext instanceof WebApplicationContext)) {
            throw new IllegalArgumentException(
                "ApplicationContext must be an instance of WebApplicationContext");
        }
        initHandlerMethods((WebApplicationContext) applicationContext);
    }
    
    private void initHandlerMethods(WebApplicationContext applicationContext) {
        // 获取所有带有 @Controller 或 @RestController 注解的 bean
        String[] controllerBeans = applicationContext.getBeanNamesForAnnotation(Controller.class);
        String[] restControllerBeans = applicationContext.getBeanNamesForAnnotation(RestController.class);
        
        // 合并两个数组
        String[] allBeans = new String[controllerBeans.length + restControllerBeans.length];
        System.arraycopy(controllerBeans, 0, allBeans, 0, controllerBeans.length);
        System.arraycopy(restControllerBeans, 0, allBeans, controllerBeans.length, restControllerBeans.length);
        
        for (String beanName : allBeans) {
            Object controller = applicationContext.getBean(beanName);
            Class<?> controllerClass = controller.getClass();
            
            // Get class-level mapping
            String baseUrl = "";
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                baseUrl = controllerClass.getAnnotation(RequestMapping.class).value();
            }
            
            // Get method-level mapping
            for (Method method : controllerClass.getMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    String methodUrl = method.getAnnotation(RequestMapping.class).value();
                    String fullUrl = baseUrl + methodUrl;
                    
                    handlerMethods.put(fullUrl, new HandlerMethod(controller, method));
                    System.out.println("[HandlerMapping] Mapped " + fullUrl + 
                        " -> " + controllerClass.getSimpleName() + "." + method.getName());
                }
            }
        }
    }
    
    @Override
    public HandlerMethod getHandler(HttpServletRequest request) {
        String lookupPath = request.getRequestURI().substring(request.getContextPath().length());
        return handlerMethods.get(lookupPath);
    }
} 