package org.microspring.webflux;

import org.microspring.web.annotation.RequestHeader;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Represents a handler method in a controller
 */
public class HandlerMethod {
    private final Object bean;
    private final Method method;

    public HandlerMethod(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Invoke the handler method
     */
    @SuppressWarnings("unchecked")
    public Mono<Object> invoke(ReactiveServerRequest request) {
        try {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            // First validate all headers to fail fast
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                RequestHeader headerAnn = param.getAnnotation(RequestHeader.class);
                if (headerAnn != null) {
                    // This will throw IllegalArgumentException if required header is missing
                    validateHeaderPresent(request, headerAnn, param);
                }
            }

            // Then resolve all parameters
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (param.getType().equals(ReactiveServerRequest.class)) {
                    args[i] = request;
                } else {
                    RequestHeader headerAnn = param.getAnnotation(RequestHeader.class);
                    if (headerAnn != null) {
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
            // Directly propagate IllegalArgumentException for header validation errors
            return Mono.error(e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return Mono.error(new RuntimeException("Failed to invoke handler method", e));
        }
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

        // This should never happen because we validate headers first
        throw new IllegalArgumentException("Required header '" + headerName + "' is not present");
    }
} 