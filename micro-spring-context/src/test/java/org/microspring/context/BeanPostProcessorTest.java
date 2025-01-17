package org.microspring.context;

import org.junit.Test;
import org.microspring.core.BeanPostProcessor;
import org.microspring.stereotype.Component;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;

public class BeanPostProcessorTest {

    @Component
    public static class TestBean {
        private String message = "original";
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    @Component
    public static class TestBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            ///System.out.println("Before initialization of bean: " + beanName);
            return bean;
        }
        
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            //System.out.println("After initialization of bean: " + beanName);
            if (bean instanceof TestBean) {
                ((TestBean) bean).setMessage("processed");
            }
            return bean;
        }
    }
    
    @Test
    public void testBeanPostProcessorExecution() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        TestBean testBean = context.getBean(TestBean.class);
        assertEquals("processed", testBean.getMessage());
    }
} 