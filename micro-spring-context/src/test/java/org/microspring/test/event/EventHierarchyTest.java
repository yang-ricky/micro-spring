package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.*;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;

public class EventHierarchyTest {
    static List<String> events = new ArrayList<>();
    
    @Test
    public void testEventHierarchy() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        // 添加一个监听所有上下文事件的监听器
        context.addApplicationListener(new ApplicationListener<ApplicationContextEvent>() {
            @Override
            public void onApplicationEvent(ApplicationContextEvent event) {
                events.add("Context:" + event.getClass().getSimpleName());
            }
        });
        
        // 添加一个只监听刷新事件的监听器
        context.addApplicationListener(new ApplicationListener<ContextRefreshedEvent>() {
            @Override
            public void onApplicationEvent(ContextRefreshedEvent event) {
                events.add("Refresh:" + event.getClass().getSimpleName());
            }
        });
        
        events.clear();
        
        // 触发不同类型的事件
        context.publishEvent(new ContextStartedEvent(context));
        context.publishEvent(new ContextRefreshedEvent(context));
        context.publishEvent(new ContextClosedEvent(context));
    }
} 