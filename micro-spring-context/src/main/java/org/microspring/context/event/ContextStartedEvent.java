package org.microspring.context.event;

import org.microspring.context.ApplicationContext;

public class ContextStartedEvent extends ApplicationContextEvent {
    public ContextStartedEvent(ApplicationContext source) {
        super(source);
    }
} 