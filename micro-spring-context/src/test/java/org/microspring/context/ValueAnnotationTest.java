package org.microspring.context;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.stereotype.Component;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;

public class ValueAnnotationTest {
    
    @Component
    public static class ValueBean {
        @Value("${app.name:defaultName}")
        private String appName;
        
        @Value("#{2 + 3}")
        private int result;
        
        public String getAppName() { return appName; }
        public int getResult() { return result; }
    }
    
    @Test
    public void testValueAnnotation() {
        // 设置系统属性
        System.setProperty("app.name", "TestApp");
        
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.context");
        
        ValueBean bean = context.getBean(ValueBean.class);
        assertNotNull(bean);
        assertEquals("TestApp", bean.getAppName());
        assertEquals(5, bean.getResult());
    }
} 