package org.microspring.context.event;

import org.microspring.core.annotation.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class SimpleApplicationEventMulticaster implements ApplicationEventMulticaster {
    
    private final List<ApplicationListener<?>> applicationListeners = new ArrayList<>();
    private Executor taskExecutor; // 用于异步事件处理

    public void setTaskExecutor(Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        if (listener != null) {
            this.applicationListeners.add(listener);
        }
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {
        this.applicationListeners.remove(listener);
    }

    @Override
    public void removeAllListeners() {
        this.applicationListeners.clear();
    }

    @Override
    public void multicastEvent(final ApplicationEvent event) {
        List<ApplicationListener<?>> listeners = getApplicationListeners(event);
        
        for (ApplicationListener<?> listener : listeners) {
            if (shouldHandleAsynchronously(listener)) {
                if (taskExecutor != null) {
                    taskExecutor.execute(() -> invokeListener(listener, event));
                } else {
                    invokeListener(listener, event);
                }
            } else {
                invokeListener(listener, event);
            }
        }
    }

    protected List<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event) {
        List<ApplicationListener<?>> allListeners = new ArrayList<>();
        for (ApplicationListener<?> listener : this.applicationListeners) {
            if (supportsEvent(listener, event)) {
                allListeners.add(listener);
            }
        }
        
        // 根据@Order注解排序
        allListeners.sort((l1, l2) -> {
            Order order1 = l1.getClass().getAnnotation(Order.class);
            Order order2 = l2.getClass().getAnnotation(Order.class);
            int p1 = order1 != null ? order1.value() : Integer.MAX_VALUE;
            int p2 = order2 != null ? order2.value() : Integer.MAX_VALUE;
            return Integer.compare(p1, p2);
        });
        
        return allListeners;
    }

    @SuppressWarnings("unchecked")
    protected void invokeListener(ApplicationListener listener, ApplicationEvent event) {
        try {
            if (supportsEvent(listener, event)) {
                listener.onApplicationEvent(event);
            } 
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to invoke event listener", e);
        }
    }

    protected boolean supportsEvent(ApplicationListener<?> listener, ApplicationEvent event) {
        Class<?> eventType = getEventType(listener);

        if (eventType == null) {
            return false;
        }

        boolean supports = eventType.isAssignableFrom(event.getClass());
        
        if (supports) {
            Class<?> currentClass = event.getClass();
            while (currentClass != null) {
                currentClass = currentClass.getSuperclass();
            }
        }
        
        return supports;
    }

    protected Class<?> getEventType(ApplicationListener<?> listener) {
        Type[] genericInterfaces = listener.getClass().getGenericInterfaces();
        
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) type;
                if (ApplicationListener.class.equals(paramType.getRawType())) {
                    Type[] typeArguments = paramType.getActualTypeArguments();
                    if (typeArguments != null && typeArguments.length > 0) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }
        System.out.println("No event type found, returning null");
        return null;
    }

    private boolean shouldHandleAsynchronously(ApplicationListener<?> listener) {
        try {
            return listener.getClass()
                .getMethod("onApplicationEvent", ApplicationEvent.class)
                .isAnnotationPresent(Async.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
} 