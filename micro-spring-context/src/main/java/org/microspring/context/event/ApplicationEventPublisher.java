package org.microspring.context.event;

public interface ApplicationEventPublisher {
    void publishEvent(ApplicationEvent event);
} 