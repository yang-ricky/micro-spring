package org.microspring.context;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.test.LazyComponent;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import org.microspring.stereotype.Component;

public class LazyAnnotationTest {
    @Test
    public void testAnnotationLazyInit() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            System.out.println("Before context initialization");
            AnnotationConfigApplicationContext context = 
                new AnnotationConfigApplicationContext("org.microspring.test");
            System.out.println("Context initialized");
            
            // 验证在获取bean之前没有创建消息
            String outputBeforeGetBean = outContent.toString();
            assertFalse("LazyComponent should not be created before getBean()", 
                outputBeforeGetBean.contains("LazyComponent is being created"));
            
            // 获取bean
            LazyComponent lazyBean = (LazyComponent) context.getBean("lazyComponent");
            
            // 验证在获取bean之后有创建消息
            String outputAfterGetBean = outContent.toString();
            assertTrue("LazyComponent should be created after getBean()", 
                outputAfterGetBean.contains("LazyComponent is being created"));
            
            assertNotNull("LazyComponent should not be null", lazyBean);
            assertEquals("Hello from LazyComponent", lazyBean.getMessage());
            
            // 验证第二次获取是否返回同一个实例
            LazyComponent secondInstance = (LazyComponent) context.getBean("lazyComponent");
            assertSame("Should return the same instance", lazyBean, secondInstance);

        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    public void testGetAllBeansWithAnnotation() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.microspring.test");
        
        // 先检查普通的getBeansWithAnnotation
        Map<String, Object> nonLazyBeans = context.getBeansWithAnnotation(Component.class);
        assertFalse("LazyComponent should not be in non-lazy beans", nonLazyBeans.containsKey("lazyComponent"));
        assertTrue("NonLazyBeans should contain other components", nonLazyBeans.size() > 0);
        
        // 再检查getAllBeansWithAnnotation
        Map<String, Object> allBeans = context.getAllBeansWithAnnotation(Component.class);
        assertTrue("LazyComponent should be in all beans", allBeans.containsKey("lazyComponent"));
        assertTrue("AllBeans should contain more beans than nonLazyBeans", allBeans.size() > nonLazyBeans.size());
        
        // 验证懒加载bean确实被包含了
        Object lazyBean = allBeans.get("lazyComponent");
        assertNotNull("LazyComponent should be instantiated", lazyBean);
        assertTrue("LazyComponent should be of correct type", lazyBean instanceof LazyComponent);
        
        // 验证非懒加载的bean也都包含在内
        for (String beanName : nonLazyBeans.keySet()) {
            assertTrue("AllBeans should contain all non-lazy beans", allBeans.containsKey(beanName));
            assertSame("Should be the same instance", nonLazyBeans.get(beanName), allBeans.get(beanName));
        }
    }
} 