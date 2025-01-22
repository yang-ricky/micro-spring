package org.microspring.context;

import org.junit.Test;
import static org.junit.Assert.*;
import org.microspring.context.annotation.Bean;
import org.microspring.context.annotation.Configuration;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.context.support.AnnotationConfigApplicationContext;

public class ConfigurationAnnotationTest {
    
    @Configuration
    static class TestConfig {
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
        
        @Bean
        public TestRepository testRepository() {
            return new TestRepository();
        }
        
        @Bean
        public TestController testController(TestService testService) {
            return new TestController(testService);
        }

        //@Bean
        public Person basicValue() {
            return new Person("testmessage", 24);
        }

        static class Person {
            private final String message;
            private Integer age;
            
            public Person(String message, Integer age) {
                this.message = message;
                this.age = age;
            }
            
            public String getMessage() {
                return message;
            }

            public Integer getAge() {
                return  age;
            }
        }

        @Bean
        @Scope("prototype")
        public PrototypeBean prototypeBean() {
            return new PrototypeBean();
        }

        @Bean(initMethod = "init", destroyMethod = "cleanup")
        public LifecycleBean lifecycleBean() {
            return new LifecycleBean();
        }

        @Bean("customName")
        public CustomNameBean customNameBean() {
            return new CustomNameBean();
        }
    }
    
    static class TestService {
        public String serve() {
            return "served";
        }
    }
    
    static class TestRepository {
        public String find() {
            return "found";
        }
    }
    
    static class TestController {
        private final TestService testService;
        
        public TestController(TestService testService) {
            this.testService = testService;
        }
        
        public String handle() {
            return testService.serve();
        }
    }
    
    static class PrototypeBean {
        private static int instanceCount = 0;
        
        public PrototypeBean() {
            instanceCount++;
        }
        
        public static int getInstanceCount() {
            return instanceCount;
        }
    }
    
    static class LifecycleBean {
        private boolean initialized = false;
        private boolean destroyed = false;
        
        public void init() {
            initialized = true;
        }
        
        public void cleanup() {
            destroyed = true;
        }
        
        public boolean isInitialized() {
            return initialized;
        }
        
        public boolean isDestroyed() {
            return destroyed;
        }
    }
    
    static class CustomNameBean {}
    
    
    @Test
    public void testConfigurationAndBeanAnnotations() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
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
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        TestController controller = context.getBean("testController", TestController.class);
        assertNotNull("Controller's testService dependency should be injected", controller.testService);
        assertEquals("served", controller.handle());
    }

    @Test
    public void testPrototypeScopeBean() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
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
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        LifecycleBean bean = context.getBean("lifecycleBean", LifecycleBean.class);
        assertTrue("Bean should be initialized", bean.isInitialized());
        
        context.close();
        assertTrue("Bean should be destroyed", bean.isDestroyed());
    }

    @Test
    public void testCustomBeanName() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
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
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        TestConfig config = context.getBean(TestConfig.class);
        assertNotNull("Configuration class should be registered as bean", config);
    }

    @Test
    public void testBeanMethodWithStringParameter() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        //org.microspring.context.ConfigurationAnnotationTest.TestConfig.Person holder = context.getBean("basicValue", org.microspring.context.ConfigurationAnnotationTest.TestConfig.Person.class);
        //assertNotNull("StringHolder should not be null", holder);
    }
} 