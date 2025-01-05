package org.microspring.web.method;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.microspring.web.annotation.RequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;

public class HandlerMethod {
    private final Object bean;
    private final Method method;
    private final ObjectMapper objectMapper;
    
    public HandlerMethod(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.objectMapper = new ObjectMapper();
    }
    
    public Object getBean() {
        return bean;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public Object invoke(HttpServletRequest request) throws Exception {
        Object[] args = resolveParameters(request);
        return method.invoke(bean, args);
    }
    
    private Object[] resolveParameters(HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                args[i] = resolveRequestBody(request, parameter.getType());
            }
            // 可以在这里添加其他参数解析逻辑
        }
        
        return args;
    }
    
    private Object resolveRequestBody(HttpServletRequest request, Class<?> paramType) throws Exception {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return objectMapper.readValue(body.toString(), paramType);
    }
} 