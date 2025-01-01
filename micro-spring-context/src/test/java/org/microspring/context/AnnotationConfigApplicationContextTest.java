package org.microspring.context;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.stereotype.Component;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;

public class AnnotationConfigApplicationContextTest {
    
    @Component
    public static class ServiceA {
        private String message = "Hello from ServiceA";
        public String getMessage() {
            return message;
        }
    }
    
    @Component("serviceB")
    @Scope("prototype")
    public static class ServiceB {
        @Autowired
        private ServiceA serviceA;
        
        @Autowired
        @Qualifier("specificBean")
        private ServiceA specificServiceA;
        
        public String getMessageFromA() {
            return serviceA.getMessage();
        }
        
        public String getMessageFromSpecificA() {
            return specificServiceA.getMessage();
        }
    }
    
    @Component("specificBean")
    public static class SpecificServiceA extends ServiceA {
        @Override
        public String getMessage() {
            return "Hello from Specific ServiceA";
        }
    }
    
    @Test
    public void testAnnotationBasedContainer() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        // 测试基本的组件扫描
        ServiceA serviceA = context.getBean(ServiceA.class);
        assertNotNull("ServiceA should be found", serviceA);
        assertEquals("Hello from ServiceA", serviceA.getMessage());
        
        // 测试@Qualifier注解
        ServiceB serviceB = context.getBean("serviceB", ServiceB.class);
        assertNotNull("ServiceB should be found", serviceB);
        assertEquals("Hello from ServiceA", serviceB.getMessageFromA());
        assertEquals("Hello from Specific ServiceA", serviceB.getMessageFromSpecificA());
        
        // 测试@Scope("prototype")
        ServiceB anotherB = context.getBean("serviceB", ServiceB.class);
        assertNotSame("Prototype beans should be different", serviceB, anotherB);
    }
} 