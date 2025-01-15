package org.microspring.test.event;

import org.microspring.stereotype.Component;
import org.microspring.context.event.EventListener;
import org.microspring.context.event.ContextStartedEvent;
import org.microspring.test.event.EventListenerAnnotationTest.CustomEvent;
import java.util.ArrayList;
import java.util.List;

@Component
public class MyEventHandler {
    private final List<String> receivedEvents = new ArrayList<>();
    
    @EventListener
    public void handleCustomEvent(CustomEvent event) {
        receivedEvents.add("Custom event received");
    }
    
    @EventListener
    public void handleContextStarted(ContextStartedEvent event) {
        receivedEvents.add("Context started event received");
    }
    
    public List<String> getReceivedEvents() {
        return receivedEvents;
    }
} 