package org.microspring.webflux;

import org.microspring.web.annotation.PathVariable;
import org.microspring.web.annotation.RequestHeader;
import org.microspring.web.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a handler method in a controller
 */
public class HandlerMethod {
    private final Object bean;
    private final Method method;
    private final Pattern pathPattern;
    private final Map<String, Integer> pathVariableIndexes;

    public HandlerMethod(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.pathVariableIndexes = new HashMap<>();
        this.pathPattern = buildPathPattern(method);
    }

    private Pattern buildPathPattern(Method method) {
        // Get method-level path
        String methodPath = method.getAnnotation(org.microspring.web.annotation.RequestMapping.class).value();
        
        // Get class-level path
        String classPath = "";
        Class<?> controllerClass = bean.getClass();
        if (controllerClass.isAnnotationPresent(org.microspring.web.annotation.RequestMapping.class)) {
            classPath = controllerClass.getAnnotation(org.microspring.web.annotation.RequestMapping.class).value();
        }
        
        // Combine paths
        String fullPath = classPath + methodPath;
        
        StringBuilder patternBuilder = new StringBuilder();
        int parameterIndex = 0;

        // Convert /users/{id}/posts/{postId} to regex pattern
        String[] segments = fullPath.split("/");
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            
            if (segment.startsWith("{") && segment.endsWith("}")) {
                String variableName = segment.substring(1, segment.length() - 1);
                pathVariableIndexes.put(variableName, parameterIndex++);
                patternBuilder.append("/([^/]+)");
            } else {
                patternBuilder.append("/").append(Pattern.quote(segment));
            }
        }

        String pattern = "^" + patternBuilder.toString() + "$";
        return Pattern.compile(pattern);
    }

    public Method getMethod() {
        return method;
    }

    public boolean matches(String path) {
        boolean matches = pathPattern.matcher(path).matches();
        return matches;
    }

    private Map<String, String> extractPathVariables(String path) {
        Map<String, String> variables = new HashMap<>();
        Matcher matcher = pathPattern.matcher(path);
        
        if (matcher.matches()) {
            for (Map.Entry<String, Integer> entry : pathVariableIndexes.entrySet()) {
                String value = matcher.group(entry.getValue() + 1);
                variables.put(entry.getKey(), value);
            }
        }
        return variables;
    }

    /**
     * Invoke the handler method
     */
    @SuppressWarnings("unchecked")
    public Mono<Object> invoke(ReactiveServerRequest request) {
        try {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            
            // Extract path variables once
            String path = request.getUri().getPath();
            Map<String, String> pathVariables = extractPathVariables(path);

            // First validate all parameters
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                
                PathVariable pathVar = param.getAnnotation(PathVariable.class);
                RequestParam reqParam = param.getAnnotation(RequestParam.class);
                RequestHeader headerAnn = param.getAnnotation(RequestHeader.class);

                if (pathVar != null) {
                    // Find the correct path variable name using the same logic as resolution
                    String name = findPathVariableName(pathVariables, pathVar, param, i);
                    if (!pathVariables.containsKey(name)) {
                        throw new IllegalArgumentException("Path variable '" + name + "' not found");
                    }
                } else if (reqParam != null) {
                    validateRequestParam(request, reqParam, param);
                } else if (headerAnn != null) {
                    validateHeaderPresent(request, headerAnn, param);
                }
            }

            // Then resolve all parameters
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                
                if (param.getType().equals(ReactiveServerRequest.class)) {
                    args[i] = request;
                } else {
                    PathVariable pathVar = param.getAnnotation(PathVariable.class);
                    RequestParam reqParam = param.getAnnotation(RequestParam.class);
                    RequestHeader headerAnn = param.getAnnotation(RequestHeader.class);

                    if (pathVar != null) {
                        args[i] = resolvePathVariable(pathVariables, pathVar, param, i);
                    } else if (reqParam != null) {
                        args[i] = resolveRequestParam(request, reqParam, param);
                    } else if (headerAnn != null) {
                        args[i] = resolveHeaderValue(request, headerAnn, param);
                    } else {
                        throw new IllegalArgumentException("Unsupported parameter type: " + param.getType());
                    }
                }
            }

            Object result = method.invoke(bean, args);
            if (result instanceof Mono) {
                return (Mono<Object>) result;
            }
            return Mono.just(result);
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return Mono.error(new RuntimeException("Failed to invoke handler method", e));
        }
    }

    private String findPathVariableName(Map<String, String> pathVariables, PathVariable pathVar, Parameter param, int parameterIndex) {
        // First try the name from annotation
        String name = pathVar.value();
        
        // If annotation value is empty, try parameter name
        if (name.isEmpty()) {
            name = param.getName();
            // If we have a parameter name that starts with arg, try to find the path variable by position
            if (name.startsWith("arg")) {
                // Convert the parameter index to the corresponding path variable name
                for (Map.Entry<String, Integer> entry : pathVariableIndexes.entrySet()) {
                    if (entry.getValue() == parameterIndex) {
                        name = entry.getKey();
                        break;
                    }
                }
            }
        }
        return name;
    }

    private void validateRequestParam(ReactiveServerRequest request, RequestParam reqParam, Parameter param) {
        String paramName = reqParam.value();
        if (paramName.isEmpty()) {
            paramName = param.getName();
        }

        String paramValue = request.getUri().getQuery();
        Map<String, String> queryParams = parseQueryParams(paramValue);
        
        // Only validate if parameter is required and has no default value
        if (!queryParams.containsKey(paramName) && reqParam.required() && 
            reqParam.defaultValue().equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
            throw new IllegalArgumentException("Required parameter '" + paramName + "' is not present");
        }
    }

    private Object resolveRequestParam(ReactiveServerRequest request, RequestParam reqParam, Parameter param) {
        String paramName = reqParam.value();
        if (paramName.isEmpty()) {
            paramName = param.getName();
        }

        String paramValue = request.getUri().getQuery();
        Map<String, String> queryParams = parseQueryParams(paramValue);
        
        String value = queryParams.get(paramName);
        if (value == null) {
            // If parameter is not required or has default value, use default value
            if (!reqParam.required() || !reqParam.defaultValue().equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                value = reqParam.defaultValue();
                if (value.equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                    value = null;
                }
            } else {
                throw new IllegalArgumentException("Required parameter '" + paramName + "' is not present");
            }
        }

        return convertValue(value, param.getType());
    }

    private Object resolvePathVariable(Map<String, String> pathVariables, PathVariable pathVar, Parameter param, int parameterIndex) {
        String name = findPathVariableName(pathVariables, pathVar, param, parameterIndex);

        String value = pathVariables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Path variable '" + name + "' not found");
        }

        try {
            Object convertedValue = convertValue(value, param.getType());
            return convertedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to convert path variable '" + name + "' to type " + param.getType(), e);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }
        return params;
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new IllegalArgumentException("Cannot convert null to primitive type " + targetType);
            }
            return null;
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to convert value '" + value + "' to type " + targetType, e);
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + targetType);
    }

    private void validateHeaderPresent(ReactiveServerRequest request, RequestHeader headerAnn, Parameter param) {
        String headerName = headerAnn.value();
        if (headerName.isEmpty()) {
            headerName = param.getName();
        }

        String headerValue = request.getHeader(headerName);
        if (headerValue == null && headerAnn.required() && 
            headerAnn.defaultValue().equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
            throw new IllegalArgumentException("Required header '" + headerName + "' is not present");
        }
    }

    private String resolveHeaderValue(ReactiveServerRequest request, RequestHeader headerAnn, Parameter param) {
        String headerName = headerAnn.value();
        if (headerName.isEmpty()) {
            headerName = param.getName();
        }

        String headerValue = request.getHeader(headerName);
        if (headerValue != null) {
            return headerValue;
        }

        if (!headerAnn.required()) {
            String defaultValue = headerAnn.defaultValue();
            if (!defaultValue.equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                return defaultValue;
            }
            return null;
        }

        throw new IllegalArgumentException("Required header '" + headerName + "' is not present");
    }
} 