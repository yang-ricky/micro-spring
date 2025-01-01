package org.microspring.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class XmlBeanDefinitionReaderTest {
    
    public static class ServiceBean {
        private String name = "service";
        
        public void init() {
            System.out.println("Initializing " + name);
        }
        
        public String getName() {
            return name;
        }
    }
    
    @Test
    public void testLoadXmlConfig() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.loadBeanDefinitions("application-context.xml");
        
        ServiceBean serviceBean = beanFactory.getBean("serviceBean", ServiceBean.class);
        assertNotNull(serviceBean);
        assertEquals("service", serviceBean.getName());
    }
} 