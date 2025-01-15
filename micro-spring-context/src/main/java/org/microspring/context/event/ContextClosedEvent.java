package org.microspring.context.event;

import org.microspring.context.ApplicationContext;

public class ContextClosedEvent extends ApplicationContextEvent {
    public ContextClosedEvent(ApplicationContext source) {
        super(source);
    }
} 