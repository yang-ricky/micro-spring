package org.microspring.web.method;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.microspring.web.annotation.RequestBody;
import org.microspring.web.annotation.ResponseStatus;
import org.microspring.web.annotation.PathVariable;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import org.microspring.web.annotation.ExceptionHandler;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.microspring.web.annotation.RequestParam;

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
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                args[i] = resolveRequestParam(request, requestParam, parameter.getType());
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
    
    private Object resolveRequestParam(HttpServletRequest request, RequestParam annotation, 
            Class<?> paramType) {
        String paramName = annotation.value();
        String paramValue = request.getParameter(paramName);
        
        if (paramValue == null) {
            if (annotation.required() && annotation.defaultValue().equals(
                "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                throw new IllegalArgumentException(
                    String.format("Required parameter '%s' is not present", paramName));
            }
            if (!annotation.defaultValue().equals(
                "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                paramValue = annotation.defaultValue();
            }
        }
        
        try {
            if (paramType == String.class) {
                return paramValue;
            } else if (paramType == Integer.class || paramType == int.class) {
                return Integer.parseInt(paramValue);
            } else if (paramType == Long.class || paramType == long.class) {
                return Long.parseLong(paramValue);
            } else if (paramType == Boolean.class || paramType == boolean.class) {
                return Boolean.parseBoolean(paramValue);
            }
            // 可以添加更多类型的转换
            throw new IllegalArgumentException(
                String.format("Unsupported parameter type: %s", paramType.getName()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Failed to convert parameter '%s' to type %s", 
                paramName, paramType.getSimpleName()));
        }
    }
    
    public Object invokeAndHandle(HttpServletRequest request, HttpServletResponse response, 
            List<Object> globalExceptionHandlers) throws Exception {
        try {
            Object result = invoke(request);
            // 检查方法上的 @ResponseStatus
            if (method.isAnnotationPresent(ResponseStatus.class)) {
                ResponseStatus status = method.getAnnotation(ResponseStatus.class);
                response.setStatus(status.value());
            }
            return result;
        } catch (Exception e) {
            // 获取实际的异常
            Throwable ex = e instanceof java.lang.reflect.InvocationTargetException ? 
                e.getCause() : e;
                
            // 首先检查异常类上的 @ResponseStatus
            if (ex.getClass().isAnnotationPresent(ResponseStatus.class)) {
                ResponseStatus status = ex.getClass().getAnnotation(ResponseStatus.class);
                response.setStatus(status.value());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", ex.getMessage());
                return errorResponse;
            }
            
            // 然后尝试使用本地异常处理器
            Method localHandler = findExceptionHandler(bean.getClass(), ex.getClass());
            if (localHandler != null) {
                return handleException(localHandler, ex, response);
            }
            
            // 最后尝试使用全局异常处理器
            for (Object handler : globalExceptionHandlers) {
                Method globalHandler = findExceptionHandler(handler.getClass(), ex.getClass());
                if (globalHandler != null) {
                    return handleException(handler, globalHandler, ex, response);
                }
            }
            
            // 如果没有找到任何处理器，重新抛出异常
            throw e;
        }
    }
    
    private Object handleException(Method handler, Throwable ex, HttpServletResponse response) 
            throws Exception {
        handler.setAccessible(true);
        Object result = handler.invoke(bean, ex);
        if (handler.isAnnotationPresent(ResponseStatus.class)) {
            ResponseStatus status = handler.getAnnotation(ResponseStatus.class);
            response.setStatus(status.value());
        }
        return result;
    }
    
    private Object handleException(Object handler, Method method, Throwable ex, 
            HttpServletResponse response) throws Exception {
        method.setAccessible(true);
        Object result = method.invoke(handler, ex);
        if (method.isAnnotationPresent(ResponseStatus.class)) {
            ResponseStatus status = method.getAnnotation(ResponseStatus.class);
            response.setStatus(status.value());
        }
        return result;
    }
    
    private Method findExceptionHandler(Class<?> handlerClass, Class<? extends Throwable> exceptionClass) {
        Method bestMatch = null;
        Class<?> bestType = null;
        
        for (Method method : handlerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
                for (Class<? extends Throwable> exceptionType : annotation.value()) {
                    if (exceptionType.isAssignableFrom(exceptionClass)) {
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