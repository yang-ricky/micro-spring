package org.microspring.context.event;

import java.util.ArrayList;
import java.util.List;

public class SimpleApplicationEventPublisher implements ApplicationEventPublisher {
    private final List<ApplicationListener<?>> listeners = new ArrayList<>();

    @Override
    public void publishEvent(ApplicationEvent event) {
        for (ApplicationListener<?> listener : getApplicationListeners(event)) {
            invokeListener(listener, event);
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeListener(ApplicationListener listener, ApplicationEvent event) {
        listener.onApplicationEvent(event);
    }

    private List<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event) {
        List<ApplicationListener<?>> allListeners = new ArrayList<>();
        for (ApplicationListener<?> listener : listeners) {
            if (supportsEvent(listener, event)) {
                allListeners.add(listener);
            }
        }
        return allListeners;
    }

    private boolean supportsEvent(ApplicationListener<?> listener, ApplicationEvent event) {
        return true; // 简化版实现，实际应该检查泛型类型
    }

    public void addApplicationListener(ApplicationListener<?> listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }
} 