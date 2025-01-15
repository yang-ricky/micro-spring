package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.*;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class SmartListenerTest {
    static List<String> events = new ArrayList<>();
    
    // 自定义事件源
    static class CustomEventSource {}
    
    // 自定义事件
    static class CustomEvent extends ApplicationEvent {
        public CustomEvent(Object source) {
            super(source);
        }
    }
    
    static class SmartEventListener implements SmartApplicationListener {
        @Override
        public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
            // 只支持CustomEvent
            return CustomEvent.class.isAssignableFrom(eventType);
        }
        
        @Override
        public boolean supportsSourceType(Class<?> sourceType) {
            // 只支持CustomEventSource和AnnotationConfigApplicationContext
            return CustomEventSource.class.isAssignableFrom(sourceType) ||
                   AnnotationConfigApplicationContext.class.isAssignableFrom(sourceType);
        }
        
        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            events.add("Smart:" + event.getClass().getSimpleName() + 
                      " from " + event.getSource().getClass().getSimpleName());
        }
        
        @Override
        public int getOrder() {
            return 0;  // 最高优先级
        }
    }
    
    @Test
    public void testSmartListener() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        // 注册监听器
        context.addApplicationListener(new SmartEventListener());
        context.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                events.add("Normal:" + event.getClass().getSimpleName());
            }
        });
        
        events.clear();
        
        // 只发送一个事件，会被两个监听器处理
        context.publishEvent(new CustomEvent(new CustomEventSource()));  // Smart和Normal监听器 (+2)
        
        assertEquals(2, events.size());  // 一个事件被两个监听器处理
        assertTrue(events.get(0).startsWith("Smart:"));
        assertTrue(events.get(1).startsWith("Normal:"));
        
        context.close();
    }
} 