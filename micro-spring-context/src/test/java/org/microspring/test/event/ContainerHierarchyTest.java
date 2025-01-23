package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.*;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.core.annotation.Order;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class ContainerHierarchyTest {
    static List<String> parentEvents = new ArrayList<>();
    static List<String> childEvents = new ArrayList<>();
    
    @Test
    public void testEventPropagation() {
        // 创建父容器
        AnnotationConfigApplicationContext parentContext = new AnnotationConfigApplicationContext();
        ApplicationListener<ApplicationEvent> parentListener = new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                parentEvents.add("Parent:" + event.getClass().getSimpleName());
            }
        };
        parentContext.addApplicationListener(parentListener);
        parentContext.refresh();
        
        // 创建子容器
        AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
        ApplicationListener<ContextStartedEvent> childListener = new ApplicationListener<ContextStartedEvent>() {
            @Override
            public void onApplicationEvent(ContextStartedEvent event) {
                childEvents.add("Child:" + event.getClass().getSimpleName());
            }
        };
        childContext.setParent(parentContext);
        childContext.addApplicationListener(childListener);
        childContext.refresh();
        
        // 清空事件列表
        parentEvents.clear();
        childEvents.clear();
        
        
        // 从子容器发布事件
        childContext.publishEvent(new ContextStartedEvent(childContext));

        
        // 从父容器发布事件
        parentContext.publishEvent(new ContextRefreshedEvent(parentContext));
        
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

    @Test
    public void testCompleteEventPropagation() {
        // 创建容器层次结构：grandParent -> parent -> child
        AnnotationConfigApplicationContext grandParentContext = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext parentContext = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
        
        // 设置层次关系
        parentContext.setParent(grandParentContext);
        childContext.setParent(parentContext);
        
        // 清空事件列表
        List<String> grandParentEvents = new ArrayList<>();
        List<String> parentEvents = new ArrayList<>();
        List<String> childEvents = new ArrayList<>();
        
        // 为每个容器注册不同类型的事件监听器
        grandParentContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                grandParentEvents.add("GrandParent:" + event.getClass().getSimpleName());
            }
        });
        
        parentContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                parentEvents.add("Parent:" + event.getClass().getSimpleName());
            }
        });
        
        childContext.addApplicationListener(new ApplicationListener<ContextStartedEvent>() {
            @Override
            public void onApplicationEvent(ContextStartedEvent event) {
                childEvents.add("Child:" + event.getClass().getSimpleName());
            }
        });
        
        // 刷新所有容器
        grandParentContext.refresh();
        parentContext.refresh();
        childContext.refresh();
        
        // 清空所有容器的事件列表
        grandParentEvents.clear();
        parentEvents.clear();
        childEvents.clear();
        
        // 测试场景1：子容器事件向上传播
        childContext.publishEvent(new ContextStartedEvent(childContext));
        assertEquals(1, childEvents.size());
        assertEquals(1, parentEvents.size());
        assertEquals(1, grandParentEvents.size());
        
        // 测试场景2：父容器事件不向下传播
        parentContext.publishEvent(new ContextRefreshedEvent(parentContext));
        assertEquals(1, childEvents.size());  // 子容器不应该收到
        assertEquals(2, parentEvents.size());
        assertEquals(2, grandParentEvents.size());
        
        // 测试场景3：祖父容器事件也不向下传播
        grandParentContext.publishEvent(new ContextClosedEvent(grandParentContext));
        assertEquals(1, childEvents.size());
        assertEquals(2, parentEvents.size());
        assertEquals(3, grandParentEvents.size());
        
        // 测试场景4：验证事件源信息正确性
        assertTrue(childEvents.get(0).endsWith("ContextStartedEvent"));
        assertTrue(parentEvents.get(0).endsWith("ContextStartedEvent"));
        assertTrue(parentEvents.get(1).endsWith("ContextRefreshedEvent"));
        assertTrue(grandParentEvents.get(2).endsWith("ContextClosedEvent"));
        
        // 关闭容器
        childContext.close();
        parentContext.close();
        grandParentContext.close();
    }

    @Test
    public void testEventOrderInHierarchy() {
        AnnotationConfigApplicationContext parentContext = new AnnotationConfigApplicationContext();
        AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
        childContext.setParent(parentContext);
        
        List<String> executionOrder = new ArrayList<>();
        
        // 注册带优先级的监听器
        parentContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Order(1)
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                executionOrder.add("parent-1");
            }
        });
        
        parentContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Order(2)
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                executionOrder.add("parent-2");
            }
        });
        
        childContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Order(1)
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                executionOrder.add("child-1");
            }
        });
        
        // 发布事件并验证顺序
        childContext.publishEvent(new ContextStartedEvent(childContext));
        
        assertEquals("child-1", executionOrder.get(0));
        assertEquals("parent-1", executionOrder.get(1));
        assertEquals("parent-2", executionOrder.get(2));
    }
} 