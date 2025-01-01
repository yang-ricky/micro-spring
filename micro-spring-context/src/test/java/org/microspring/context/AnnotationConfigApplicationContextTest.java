package org.microspring.context;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.core.annotation.Component;
import static org.junit.Assert.*;
import org.microspring.context.support.AnnotationConfigApplicationContext;

public class AnnotationConfigApplicationContextTest {
    
    @Component
    public static class ServiceA {
        private String message = "Hello from ServiceA";
        
        public String getMessage() {
            return message;
        }
    }
    
    @Component
    public static class ServiceB {
        @Autowired
        private ServiceA serviceA;
        
        public String getMessageFromA() {
            return serviceA.getMessage();
        }
    }
    
    @Test
    public void testComponentScan() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.microspring.context");
        
        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);
        
        assertNotNull(serviceA);
        assertNotNull(serviceB);
        assertEquals("Hello from ServiceA", serviceB.getMessageFromA());
    }
} 