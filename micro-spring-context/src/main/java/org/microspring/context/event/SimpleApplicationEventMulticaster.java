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
        System.out.println("\n=== Multicasting event: " + event.getClass().getSimpleName() + " ===");
        List<ApplicationListener<?>> listeners = getApplicationListeners(event);
        System.out.println("Total listeners: " + applicationListeners.size());
        System.out.println("Matching listeners: " + listeners.size());
        
        for (ApplicationListener<?> listener : listeners) {
            System.out.println("\nProcessing listener: " + listener.getClass().getSimpleName());
            if (shouldHandleAsynchronously(listener)) {
                System.out.println("Handling asynchronously");
                if (taskExecutor != null) {
                    taskExecutor.execute(() -> invokeListener(listener, event));
                } else {
                    System.out.println("No task executor, falling back to sync processing");
                    invokeListener(listener, event);
                }
            } else {
                System.out.println("Handling synchronously");
                invokeListener(listener, event);
            }
        }
        System.out.println("=== Event multicasting completed ===\n");
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
            System.out.println("Attempting to invoke listener: " + listener.getClass().getSimpleName());
            if (supportsEvent(listener, event)) {
                System.out.println("Listener supports event, invoking...");
                listener.onApplicationEvent(event);
            } else {
                System.out.println("Listener does not support event, skipping");
            }
        } catch (Exception e) {
            System.err.println("Error invoking listener: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to invoke event listener", e);
        }
    }

    protected boolean supportsEvent(ApplicationListener<?> listener, ApplicationEvent event) {
        System.out.println("\nChecking if listener " + listener.getClass().getSimpleName() + 
                          " supports event " + event.getClass().getSimpleName());
        Class<?> eventType = getEventType(listener);
        System.out.println("Listener event type: " + eventType);

        if (eventType == null) {
            System.out.println("No event type found for listener, returning false");
            return false;
        }

        boolean supports = eventType.isAssignableFrom(event.getClass());
        System.out.println("Checking if " + event.getClass() + " is assignable to " + eventType);
        System.out.println("Supports event: " + supports);
        
        if (supports) {
            System.out.println("Event class hierarchy:");
            Class<?> currentClass = event.getClass();
            while (currentClass != null) {
                System.out.println(" - " + currentClass.getName());
                currentClass = currentClass.getSuperclass();
            }
        }
        
        return supports;
    }

    protected Class<?> getEventType(ApplicationListener<?> listener) {
        System.out.println("\nGetting event type for listener: " + listener.getClass().getSimpleName());
        Type[] genericInterfaces = listener.getClass().getGenericInterfaces();
        System.out.println("Generic interfaces: " + Arrays.toString(genericInterfaces));
        
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) type;
                System.out.println("Found parameterized type: " + paramType);
                System.out.println("Raw type: " + paramType.getRawType());
                if (ApplicationListener.class.equals(paramType.getRawType())) {
                    Type[] typeArguments = paramType.getActualTypeArguments();
                    System.out.println("Type arguments: " + Arrays.toString(typeArguments));
                    if (typeArguments != null && typeArguments.length > 0) {
                        System.out.println("Returning event type: " + typeArguments[0]);
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