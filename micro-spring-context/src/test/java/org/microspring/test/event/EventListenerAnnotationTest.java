package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.*;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class EventListenerAnnotationTest {
    
    @Test
    public void testEventListenerAnnotation() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.event");
        
        MyEventHandler handler = context.getBean(MyEventHandler.class);
        assertNotNull("MyEventHandler should be found", handler);
        
        context.publishEvent(new CustomEvent("test"));
        context.publishEvent(new ContextStartedEvent(context));
        
        List<String> receivedEvents = handler.getReceivedEvents();
        assertEquals(2, receivedEvents.size());
        assertTrue(receivedEvents.contains("Custom event received"));
        assertTrue(receivedEvents.contains("Context started event received"));
    }
    
    static class CustomEvent extends ApplicationEvent {
        public CustomEvent(Object source) {
            super(source);
        }
    }

} 