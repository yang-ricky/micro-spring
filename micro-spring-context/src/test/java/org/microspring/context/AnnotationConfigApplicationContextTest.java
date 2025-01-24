package org.microspring.context;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.test.annotation.*;
import static org.junit.Assert.*;

public class AnnotationConfigApplicationContextTest {
    
    @Test
    public void testBasicDependencyInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        ServiceA serviceA = context.getBean(ServiceA.class);
        assertNotNull("ServiceA should be found", serviceA);
        assertEquals("Hello from ServiceA", serviceA.getMessage());
        
        ServiceB serviceB = context.getBean("serviceB", ServiceB.class);
        assertNotNull("ServiceB should be found", serviceB);
        assertEquals("Hello from ServiceA", serviceB.getMessageFromA());
        assertEquals("Hello from Specific ServiceA", serviceB.getMessageFromSpecificA());
        
        ServiceB anotherB = context.getBean("serviceB", ServiceB.class);
        assertNotSame("Prototype beans should be different", serviceB, anotherB);
    }
    
    @Test
    public void testConstructorInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        ServiceWithConstructor service = context.getBean(ServiceWithConstructor.class);
        assertNotNull("Service should be found", service);
        assertEquals("Messages should be combined correctly",
            "Hello from ServiceA & Hello from ServiceA", 
            service.getMessageFromBoth());
    }
    
    @Test
    public void testSetterInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        MessageServiceWithSetter setterService = context.getBean(MessageServiceWithSetter.class);
        assertNotNull("MessageServiceWithSetter should be found", setterService);
        assertEquals("Hello from ServiceA and Hello from ServiceA", 
                    setterService.getCombinedMessages());
    }
    
    @Test
    public void testMethodInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        MessageServiceWithMethod methodService = context.getBean(MessageServiceWithMethod.class);
        assertNotNull("MessageServiceWithMethod should be found", methodService);
        assertEquals("Standard: Hello from ServiceA, " + 
                    "From B: Hello from ServiceA, " + 
                    "Specific: Hello from Specific ServiceA", 
                    methodService.getMessages());
    }

    @Test
    public void testScopeAnnotations() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        // 测试单例 bean
        TestSingletonBean singleton1 = context.getBean(TestSingletonBean.class);
        TestSingletonBean singleton2 = context.getBean(TestSingletonBean.class);
        
        assertEquals(1, singleton1.increment());
        assertEquals(2, singleton2.increment());  // 同一个实例，计数继续增加
        assertSame(singleton1, singleton2);  // 应该是同一个实例
        
        // 测试原型 bean
        TestPrototypeBean prototype1 = context.getBean(TestPrototypeBean.class);
        TestPrototypeBean prototype2 = context.getBean(TestPrototypeBean.class);
        
        assertEquals(1, prototype1.increment());
        assertEquals(1, prototype2.increment());  // 新实例，计数从1开始
        assertNotSame(prototype1, prototype2);  // 应该是不同的实例
    }

    @Test
    public void testSingletonWithPrototypeDependency() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        SingletonWithPrototype singleton1 = context.getBean(SingletonWithPrototype.class);
        SingletonWithPrototype singleton2 = context.getBean(SingletonWithPrototype.class);
        
        assertEquals(1, singleton1.getPrototypeCount());
        assertEquals(2, singleton2.getPrototypeCount());
        assertSame(singleton1, singleton2);
    }

    @Test
    public void testPrototypeWithSingletonDependency() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        PrototypeWithSingleton prototype1 = context.getBean(PrototypeWithSingleton.class);
        PrototypeWithSingleton prototype2 = context.getBean(PrototypeWithSingleton.class);
        
        assertEquals(1, prototype1.getSingletonCount());
        assertEquals(2, prototype2.getSingletonCount());
        assertNotSame(prototype1, prototype2);
    }

    // todo: 测试循环依赖
    // todo: 测试集合注入
    // List<MessageService> services
    // Map<String, MessageService> serviceMap
    
    @Test
    public void testInterfaceImplementations() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        MessageService service1 = context.getBean("messageService1", MessageService.class);
        MessageService service2 = context.getBean("messageService2", MessageService.class);
        
        assertEquals("Message from Implementation 1", service1.getMessage());
        assertEquals("Message from Implementation 2", service2.getMessage());
        assertNotSame(service1, service2);
    }
    
    @Test
    public void testAbstractClassImplementations() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        AbstractGreeting chineseGreeting = context.getBean("chineseGreeting", AbstractGreeting.class);
        AbstractGreeting englishGreeting = context.getBean("englishGreeting", AbstractGreeting.class);
        
        assertTrue(chineseGreeting.greet().startsWith("你好"));
        assertTrue(englishGreeting.greet().startsWith("Hello"));
        assertNotSame(chineseGreeting, englishGreeting);
    }
    
    @Test
    public void testCombinedGreetingService() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.annotation");
        
        GreetingService greetingService = context.getBean(GreetingService.class);
        String allMessages = greetingService.getAllMessages();
        
        assertTrue(allMessages.contains("你好"));
        assertTrue(allMessages.contains("Hello"));
        assertTrue(allMessages.contains("Message from Implementation 1"));
        assertTrue(allMessages.contains("Message from Implementation 2"));
    }
} 