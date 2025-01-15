package org.microspring.test.event;

import org.microspring.context.event.ApplicationEvent;

public class CustomEvent extends ApplicationEvent {
    public CustomEvent(Object source) {
        super(source);
    }
} 