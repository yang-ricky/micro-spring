package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.ApplicationContext;
import org.microspring.context.event.*;
import org.microspring.core.annotation.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.annotation.Annotation;

import static org.junit.Assert.*;

public class ApplicationEventMulticasterTest {

    // 测试事件类
    static class TestEvent extends ApplicationEvent {
        public TestEvent(Object source) {
            super(source);
        }
    }

    // 测试监听器
    static class TestListener implements ApplicationListener<TestEvent> {
        private final List<String> events = new ArrayList<>();
        
        @Override
        public void onApplicationEvent(TestEvent event) {
            events.add("Event received: " + event.getClass().getSimpleName());
        }
        
        public List<String> getEvents() {
            return events;
        }
    }

    @Test
    public void testBasicEventHandling() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        TestListener listener = new TestListener();
        
        // 添加监听器
        multicaster.addApplicationListener(listener);
        
        // 发布事件
        TestEvent event = new TestEvent(this);
        multicaster.multicastEvent(event);
        
        // 验证
        assertEquals(1, listener.getEvents().size());
        assertEquals("Event received: TestEvent", listener.getEvents().get(0));
    }

    @Test
    public void testOrderedEventHandling() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        List<String> executionOrder = new ArrayList<>();
        
        // 注册多个带顺序的监听器
        ApplicationListener<TestEvent> listener1 = new ApplicationListener<TestEvent>() {
            @Order(2)
            @Override
            public void onApplicationEvent(TestEvent event) {
                executionOrder.add("listener1");
            }
        };
        
        ApplicationListener<TestEvent> listener2 = new ApplicationListener<TestEvent>() {
            @Order(1)
            @Override
            public void onApplicationEvent(TestEvent event) {
                executionOrder.add("listener2");
            }
        };
        
        // 先注册优先级低的，再注册优先级高的，验证排序是否生效
        multicaster.addApplicationListener(listener2);  // Order(1)
        multicaster.addApplicationListener(listener1);  // Order(2)
        
        // 发布事件
        multicaster.multicastEvent(new TestEvent(this));
        
        // 验证执行顺序
        assertEquals("listener2", executionOrder.get(0));
        assertEquals("listener1", executionOrder.get(1));
    }

    @Test
    public void testAsyncEventHandling() throws InterruptedException {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = new ArrayList<>();
        
        // 设置执行器
        multicaster.setTaskExecutor(new Executor() {
            @Override
            public void execute(Runnable command) {
                new Thread(() -> {
                    command.run();
                    threadNames.add(Thread.currentThread().getName());
                    latch.countDown();
                }, "async-executor").start();
            }
        });
        
        // 创建异步监听器
        ApplicationListener<TestEvent> asyncListener = new ApplicationListener<TestEvent>() {
            @Async
            @Override
            public void onApplicationEvent(TestEvent event) {
                try {
                    Thread.sleep(100); // 模拟异步处理
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        
        multicaster.addApplicationListener(asyncListener);
        multicaster.multicastEvent(new TestEvent(this));
        
        // 等待异步处理完成
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(threadNames.get(0).startsWith("async-executor"));
    }

    @Test
    public void testRemoveListener() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        TestListener listener = new TestListener();
        
        multicaster.addApplicationListener(listener);
        multicaster.removeApplicationListener(listener);
        
        multicaster.multicastEvent(new TestEvent(this));
        
        assertTrue(listener.getEvents().isEmpty());
    }

    @Test
    public void testEventTypeMatching() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        List<String> events = new ArrayList<>();
        
        ApplicationContext mockContext = new MockApplicationContext();
        
        System.out.println("\n=== Setting up listeners ===");
        class StartedEventListener implements ApplicationListener<ContextStartedEvent> {
            @Override
            public void onApplicationEvent(ContextStartedEvent event) {
                System.out.println("StartedListener received event: " + event.getClass().getSimpleName());
                events.add("ContextStartedEvent");
                System.out.println("Current events after StartedListener: " + events);
            }
        }
        
        class GenericEventListener implements ApplicationListener<ApplicationEvent> {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                System.out.println("GenericListener received event: " + event.getClass().getSimpleName());
                events.add("ApplicationEvent-" + event.getClass().getSimpleName());
                System.out.println("Current events after GenericListener: " + events);
            }
        }
        
        System.out.println("Adding startedListener");
        multicaster.addApplicationListener(new StartedEventListener());
        System.out.println("Adding genericListener");
        multicaster.addApplicationListener(new GenericEventListener());
        
        System.out.println("\n=== Publishing events ===");
        System.out.println("Publishing ContextStartedEvent");
        multicaster.multicastEvent(new ContextStartedEvent(mockContext));
        
        System.out.println("\nEvents after first event: " + events);
        
        System.out.println("\nPublishing ContextRefreshedEvent");
        ContextRefreshedEvent refreshEvent = new ContextRefreshedEvent(mockContext);
        multicaster.multicastEvent(refreshEvent);
        
        System.out.println("\nEvents after second event: " + events);
        
        System.out.println("\n=== Verification ===");
        System.out.println("Final events list: " + events);
        assertEquals(3, events.size());  // startedListener处理一次，genericListener处理两次（一次处理ContextStartedEvent，一次处理ContextRefreshedEvent）
        assertTrue(events.contains("ContextStartedEvent"));
        assertTrue(events.contains("ApplicationEvent-ContextStartedEvent"));
        assertTrue(events.contains("ApplicationEvent-ContextRefreshedEvent"));
    }
} 