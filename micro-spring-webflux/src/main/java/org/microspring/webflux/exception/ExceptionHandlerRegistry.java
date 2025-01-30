package org.microspring.webflux.exception;

import org.microspring.web.annotation.ExceptionHandler;
import org.microspring.web.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Registry for exception handlers from @RestControllerAdvice classes
 */
public class ExceptionHandlerRegistry {
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> handlers = new HashMap<>();

    /**
     * Register exception handlers from a @RestControllerAdvice class
     */
    public void registerExceptionHandler(Object bean) {
        Class<?> beanClass = bean.getClass();
        if (!beanClass.isAnnotationPresent(RestControllerAdvice.class)) {
            return;
        }

        // Scan methods for @ExceptionHandler
        for (Method method : beanClass.getMethods()) {
            ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
            if (annotation != null) {
                registerExceptionHandlerMethod(bean, method, annotation);
            }
        }
    }

    private void registerExceptionHandlerMethod(Object bean, Method method, ExceptionHandler annotation) {
        // Get exception types from annotation
        Class<? extends Throwable>[] exceptionTypes = annotation.value();
        if (exceptionTypes.length == 0) {
            // If no exception types specified, try to get from method parameters
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length > 0 && Throwable.class.isAssignableFrom(paramTypes[0])) {
                exceptionTypes = new Class[]{(Class<? extends Throwable>) paramTypes[0]};
            }
        }

        // Register handler for each exception type
        for (Class<? extends Throwable> exceptionType : exceptionTypes) {
            if (handlers.containsKey(exceptionType)) {
                throw new IllegalStateException(
                    "Duplicate exception handler for " + exceptionType.getName());
            }
            handlers.put(exceptionType, new ExceptionHandlerMethod(bean, method));
        }
    }

    /**
     * Find the most specific handler for an exception
     */
    public Optional<ExceptionHandlerMethod> findHandler(Throwable ex) {
        Class<?> exceptionClass = ex.getClass();
        // Try to find exact match first
        ExceptionHandlerMethod handler = handlers.get(exceptionClass);
        if (handler != null) {
            return Optional.of(handler);
        }

        // Try to find handler for superclass
        for (Map.Entry<Class<? extends Throwable>, ExceptionHandlerMethod> entry : handlers.entrySet()) {
            if (entry.getKey().isAssignableFrom(exceptionClass)) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    /**
     * Wrapper class for exception handler methods
     */
    public static class ExceptionHandlerMethod {
        private final Object bean;
        private final Method method;

        public ExceptionHandlerMethod(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        @SuppressWarnings("unchecked")
        public Mono<Object> invoke(Throwable ex) {
            try {
                Object result = method.invoke(bean, ex);
                if (result instanceof Mono) {
                    return (Mono<Object>) result;
                }
                return Mono.just(result);
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Failed to invoke exception handler", e));
            }
        }
    }
} 