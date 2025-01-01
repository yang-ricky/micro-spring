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
    
    @Component
    public static class ServiceWithConstructor {
        private final ServiceA serviceA;
        private final ServiceB serviceB;

        @Autowired  // 构造器注入
        public ServiceWithConstructor(ServiceA serviceA, 
                                    @Qualifier("serviceB") ServiceB serviceB) {
            this.serviceA = serviceA;
            this.serviceB = serviceB;
        }

        public String getMessageFromBoth() {
            return serviceA.getMessage() + " & " + serviceB.getMessageFromA();
        }
    }
    
    @Component
    public static class MessageServiceWithSetter {
        private ServiceA serviceA;
        private ServiceB serviceB;
        
        // 标准的 setter 方法注入
        @Autowired
        public void setServiceA(ServiceA serviceA) {
            this.serviceA = serviceA;
        }
        
        @Autowired
        @Qualifier("serviceB")  // 使用限定符指定具体的 serviceB
        public void setServiceB(ServiceB serviceB) {
            this.serviceB = serviceB;
        }
        
        public String getCombinedMessages() {
            return serviceA.getMessage() + " and " + serviceB.getMessageFromA();
        }
    }
    
    @Component
    public static class MessageServiceWithMethod {
        private ServiceA serviceA;
        private ServiceB serviceB;
        
        // 普通方法注入，一次注入多个依赖
        @Autowired
        public void initializeServices(ServiceA serviceA, 
                                     @Qualifier("serviceB") ServiceB serviceB) {
            this.serviceA = serviceA;
            this.serviceB = serviceB;
            // 这里可以添加一些初始化逻辑
        }
        
        public String getMessages() {
            return "Standard: " + serviceA.getMessage() + 
                   ", From B: " + serviceB.getMessageFromA() + 
                   ", Specific: " + serviceB.getMessageFromSpecificA();
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
    
    @Test
    public void testConstructorInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        ServiceWithConstructor service = context.getBean(ServiceWithConstructor.class);
        assertNotNull("Service should be found", service);
        assertEquals("Messages should be combined correctly",
            "Hello from ServiceA & Hello from ServiceA", 
            service.getMessageFromBoth());
    }
    
    @Test
    public void testSetterInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        MessageServiceWithSetter setterService = context.getBean(MessageServiceWithSetter.class);
        assertNotNull("MessageServiceWithSetter should be found", setterService);
        assertEquals("Hello from ServiceA and Hello from ServiceA", 
                    setterService.getCombinedMessages());
    }
    
    @Test
    public void testMethodInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        MessageServiceWithMethod methodService = context.getBean(MessageServiceWithMethod.class);
        assertNotNull("MessageServiceWithMethod should be found", methodService);
        assertEquals("Standard: Hello from ServiceA, " + 
                    "From B: Hello from ServiceA, " + 
                    "Specific: Hello from Specific ServiceA", 
                    methodService.getMessages());
    }
} 