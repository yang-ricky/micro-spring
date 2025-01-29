package org.microspring.webflux;

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
    public Mono<Object> invoke(Object... args) {
        try {
            // 检查方法参数
            Parameter[] parameters = method.getParameters();
            Object[] actualArgs = new Object[parameters.length];
            
            // 如果方法没有参数，使用空数组
            if (parameters.length == 0) {
                actualArgs = new Object[0];
            } 
            // 如果方法有一个参数且是 ReactiveServerRequest 类型
            else if (parameters.length == 1 && parameters[0].getType().equals(ReactiveServerRequest.class)) {
                actualArgs[0] = args[0]; // 传入 request 参数
            }
            // 其他情况抛出异常
            else {
                throw new IllegalArgumentException("Method " + method.getName() + 
                    " must have either no parameters or a single ReactiveServerRequest parameter");
            }

            Object result = method.invoke(bean, actualArgs);
            if (result instanceof Mono) {
                return (Mono<Object>) result;
            }
            return Mono.just(result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return Mono.error(new RuntimeException("Failed to invoke handler method", e));
        }
    }
} 