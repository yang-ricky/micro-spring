package org.microspring.context.event;

import java.lang.reflect.Method;

public class ApplicationListenerMethodAdapter implements ApplicationListener<ApplicationEvent> {
    private final Object target;
    private final Method method;
    private final Class<?> eventType;

    public ApplicationListenerMethodAdapter(Object target, Method method) {
        this.target = target;
        this.method = method;
        this.eventType = determineEventType(method);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        try {
            method.invoke(target, event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke event listener method", e);
        }
    }

    private Class<?> determineEventType(Method method) {
        // 首先检查@EventListener注解中指定的类型
        EventListener ann = method.getAnnotation(EventListener.class);
        if (ann != null && ann.value().length > 0) {
            return ann.value()[0];
        }
        
        // 如果注解没有指定类型，则使用方法参数类型
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new IllegalStateException("Event listener method must have exactly one parameter");
        }
        return paramTypes[0];
    }

    public Class<?> getEventType() {
        return eventType;
    }
} 