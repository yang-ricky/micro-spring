package org.microspring.test.configuration;

import org.microspring.context.annotation.Bean;
import org.microspring.context.annotation.Configuration;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.beans.factory.annotation.Value;

@Configuration
public class TestConfig {
    
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

    @Bean
    public Person basicValue(@Value("testmessage") String message, @Value("24") Integer age) {
        return new Person(message, age);
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