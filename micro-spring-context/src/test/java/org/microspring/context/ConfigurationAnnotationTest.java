package org.microspring.context;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.test.configuration.*;

public class ConfigurationAnnotationTest {
    
    @Before
    public void setup() {
        // Reset the instance count before each test
        PrototypeBean.resetInstanceCount();
    }
    
    @Test
    public void testConfigurationAndBeanAnnotations() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        TestService testService = context.getBean("testService", TestService.class);
        assertNotNull("TestService should not be null", testService);
        assertEquals("served", testService.serve());
        
        TestRepository testRepository = context.getBean("testRepository", TestRepository.class);
        assertNotNull("TestRepository should not be null", testRepository);
        assertEquals("found", testRepository.find());
        
        TestController testController = context.getBean("testController", TestController.class);
        assertNotNull("TestController should not be null", testController);
        assertEquals("served", testController.handle());
    }
    
    @Test
    public void testBeanMethodDependencyInjection() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        TestController controller = context.getBean("testController", TestController.class);
        assertNotNull("Controller's testService dependency should be injected", controller.getTestService());
        assertEquals("served", controller.handle());
    }

    @Test
    public void testPrototypeScopeBean() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        PrototypeBean bean1 = context.getBean("prototypeBean", PrototypeBean.class);
        PrototypeBean bean2 = context.getBean("prototypeBean", PrototypeBean.class);
        
        assertNotNull("Prototype bean should not be null", bean1);
        assertNotNull("Prototype bean should not be null", bean2);
        assertNotSame("Prototype beans should be different instances", bean1, bean2);
        assertEquals("Should have created 2 instances", 2, PrototypeBean.getInstanceCount());
    }

    @Test
    public void testBeanLifecycle() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        LifecycleBean bean = context.getBean("lifecycleBean", LifecycleBean.class);
        assertTrue("Bean should be initialized", bean.isInitialized());
        
        context.close();
        assertTrue("Bean should be destroyed", bean.isDestroyed());
    }

    @Test
    public void testCustomBeanName() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        CustomNameBean bean = context.getBean("customName", CustomNameBean.class);
        assertNotNull("Should find bean with custom name", bean);
        
        try {
            context.getBean("customNameBean", CustomNameBean.class);
            fail("Should not find bean with default name");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testConfigurationClassAsBeanDefinition() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        TestConfig config = context.getBean(TestConfig.class);
        assertNotNull("Configuration class should be registered as bean", config);
    }

    @Test
    public void testBeanMethodWithStringParameter() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.configuration");
        
        Person person = context.getBean("basicValue", Person.class);
        
        assertNotNull("Person should not be null", person);
        assertEquals(Integer.valueOf(24), person.getAge());
        assertEquals("testmessage", person.getMessage());
    }
} 