package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.ApplicationEvent;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class OrderedEventTest {
    
    static List<String> executionOrder = new ArrayList<>();
    
    @Test
    public void testListenerOrder() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        // 添加监听器，故意打乱顺序
        context.addApplicationListener(new ThirdListener());  // Order(3)
        context.addApplicationListener(new FirstListener());  // Order(1)
        context.addApplicationListener(new SecondListener()); // Order(2)
        context.addApplicationListener(new DefaultListener()); // 无Order注解
        
        executionOrder.clear();
        
        // 发布事件
        context.publishEvent(new CustomOrderedEvent(this));
        
        // 验证执行顺序
        assertEquals("First", executionOrder.get(0));
        assertEquals("Second", executionOrder.get(1));
        assertEquals("Third", executionOrder.get(2));
        assertEquals("Default", executionOrder.get(3));
        
        context.close();
    }
    
    static class CustomOrderedEvent extends ApplicationEvent {
        public CustomOrderedEvent(Object source) {
            super(source);
        }
    }
    
    @Order(1)
    static class FirstListener implements ApplicationListener<CustomOrderedEvent> {
        @Override
        public void onApplicationEvent(CustomOrderedEvent event) {
            executionOrder.add("First");
        }
    }
    
    @Order(2)
    static class SecondListener implements ApplicationListener<CustomOrderedEvent> {
        @Override
        public void onApplicationEvent(CustomOrderedEvent event) {
            executionOrder.add("Second");
        }
    }
    
    @Order(3)
    static class ThirdListener implements ApplicationListener<CustomOrderedEvent> {
        @Override
        public void onApplicationEvent(CustomOrderedEvent event) {
            executionOrder.add("Third");
        }
    }
    
    static class DefaultListener implements ApplicationListener<CustomOrderedEvent> {
        @Override
        public void onApplicationEvent(CustomOrderedEvent event) {
            executionOrder.add("Default");
        }
    }
} 