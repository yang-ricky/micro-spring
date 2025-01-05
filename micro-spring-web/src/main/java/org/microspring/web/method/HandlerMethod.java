package org.microspring.web.method;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.microspring.web.annotation.RequestBody;
import org.microspring.web.annotation.PathVariable;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import org.microspring.web.annotation.ExceptionHandler;

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
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVar = parameter.getAnnotation(PathVariable.class);
                args[i] = resolvePathVariable(request, pathVar.value(), parameter.getType());
            }
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
    
    private Object resolvePathVariable(HttpServletRequest request, String name, Class<?> paramType) {
        String uri = request.getRequestURI();
        String[] pathSegments = uri.split("/");
        
        // 获取方法上的路径
        String methodPath = "";
        if (method.isAnnotationPresent(org.microspring.web.annotation.GetMapping.class)) {
            methodPath = method.getAnnotation(org.microspring.web.annotation.GetMapping.class).value();
        }
        
        String[] patternSegments = methodPath.split("/");
        
        // 从后往前匹配，因为变量通常在路径末尾
        int uriIndex = pathSegments.length - 1;
        int patternIndex = patternSegments.length - 1;
        
        while (uriIndex >= 0 && patternIndex >= 0) {
            if (patternSegments[patternIndex].startsWith("{") && 
                patternSegments[patternIndex].endsWith("}")) {
                String varName = patternSegments[patternIndex].substring(1, patternSegments[patternIndex].length() - 1);
                if (varName.equals(name)) {
                    String value = pathSegments[uriIndex];
                    try {
                        if (paramType == Long.class) {
                            return Long.parseLong(value);
                        }
                        return value;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                            String.format("Failed to convert path variable '%s' to type %s", 
                            name, paramType.getSimpleName())
                        );
                    }
                }
            }
            uriIndex--;
            patternIndex--;
        }
        
        return null;
    }
    
    public Object invokeAndHandle(HttpServletRequest request) throws Exception {
        try {
            return invoke(request);
        } catch (Exception e) {
            // 获取实际的异常（如果是InvocationTargetException）
            Throwable ex = e instanceof java.lang.reflect.InvocationTargetException ? 
                e.getCause() : e;
                
            // 查找异常处理方法
            Method exceptionHandler = findExceptionHandler(ex.getClass());
            if (exceptionHandler != null) {
                exceptionHandler.setAccessible(true);
                return exceptionHandler.invoke(bean, ex);
            }
            throw e;
        }
    }
    
    private Method findExceptionHandler(Class<?> exceptionClass) {
        // 查找所有带有@ExceptionHandler注解的方法
        Method bestMatch = null;
        Class<?> bestType = null;
        
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
                for (Class<? extends Throwable> exceptionType : annotation.value()) {
                    if (exceptionType.isAssignableFrom(exceptionClass)) {
                        // 找到匹配的处理器，检查是否是最具体的匹配
                        if (bestType == null || bestType.isAssignableFrom(exceptionType)) {
                            bestMatch = method;
                            bestType = exceptionType;
                        }
                    }
                }
            }
        }
        
        return bestMatch;
    }
} 