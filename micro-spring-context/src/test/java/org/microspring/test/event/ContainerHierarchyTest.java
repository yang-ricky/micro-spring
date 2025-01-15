package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.*;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;

public class ContainerHierarchyTest {
    static List<String> parentEvents = new ArrayList<>();
    static List<String> childEvents = new ArrayList<>();
    
    @Test
    public void testEventPropagation() {
        // 创建父容器
        AnnotationConfigApplicationContext parentContext = new AnnotationConfigApplicationContext();
        System.out.println("\nRegistering parent container listener...");
        ApplicationListener<ApplicationEvent> parentListener = new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                System.out.println("Parent container received event: " + event.getClass().getSimpleName());
                parentEvents.add("Parent:" + event.getClass().getSimpleName());
            }
        };
        parentContext.addApplicationListener(parentListener);
        System.out.println("Parent listener registered: " + parentListener.getClass().getName());
        parentContext.refresh();
        
        // 创建子容器
        AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
        System.out.println("\nRegistering child container listener...");
        ApplicationListener<ContextStartedEvent> childListener = new ApplicationListener<ContextStartedEvent>() {
            @Override
            public void onApplicationEvent(ContextStartedEvent event) {
                System.out.println("Child container received event: " + event.getClass().getSimpleName() + 
                                 " from " + event.getSource().getClass().getSimpleName());
                childEvents.add("Child:" + event.getClass().getSimpleName());
            }
        };
        childContext.setParent(parentContext);
        childContext.addApplicationListener(childListener);
        System.out.println("Child listener registered: " + childListener.getClass().getName());
        childContext.refresh();
        
        // 清空事件列表
        System.out.println("\nClearing event lists after refresh...");
        parentEvents.clear();
        childEvents.clear();
        
        System.out.println("\nStarting event propagation test...");
        System.out.println("Initial parent events: " + parentEvents);
        System.out.println("Initial child events: " + childEvents);
        
        // 从子容器发布事件
        System.out.println("\nPublishing ContextStartedEvent from child container...");
        childContext.publishEvent(new ContextStartedEvent(childContext));
        System.out.println("Child events after ContextStartedEvent: " + childEvents);
        System.out.println("Parent events after ContextStartedEvent: " + parentEvents);
        
        // 从父容器发布事件
        System.out.println("\nPublishing ContextRefreshedEvent from parent container...");
        parentContext.publishEvent(new ContextRefreshedEvent(parentContext));
        System.out.println("Child events after ContextRefreshedEvent: " + childEvents);
        System.out.println("Parent events after ContextRefreshedEvent: " + parentEvents);
        
        // 验证事件传播
        assertEquals("Child events size should be 1", 1, childEvents.size());
        assertEquals("First child event should be ContextStartedEvent", 
                    "Child:ContextStartedEvent", childEvents.get(0));
        assertEquals("Parent events size should be 2", 2, parentEvents.size());
        assertEquals("First parent event should be ContextStartedEvent", 
                    "Parent:ContextStartedEvent", parentEvents.get(0));
        assertEquals("Second parent event should be ContextRefreshedEvent", 
                    "Parent:ContextRefreshedEvent", parentEvents.get(1));
        
        parentContext.close();
        childContext.close();
    }
} 