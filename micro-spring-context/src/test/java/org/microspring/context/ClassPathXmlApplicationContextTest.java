package org.microspring.context;

import org.junit.Test;
import org.microspring.context.support.ClassPathXmlApplicationContext;
import static org.junit.Assert.*;

public class ClassPathXmlApplicationContextTest {
    
    public static class TestBean {
        private String name = "test";
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    @Test
    public void testApplicationContext() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        assertTrue(context.containsBean("testBean"));
        TestBean bean = context.getBean("testBean", TestBean.class);
        assertNotNull(bean);
        assertEquals("test", bean.getName());
        
        assertNotNull(context.getApplicationName());
        assertTrue(context.getStartupDate() > 0);
    }
} 