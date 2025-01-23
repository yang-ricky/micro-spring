package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.Async;
import org.microspring.context.event.ApplicationEvent;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.support.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertTrue;

public class AsyncEventTest {
    @Test
    public void testAsyncEventHandling() throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        AsyncEventListener asyncListener = new AsyncEventListener();
        SyncEventListener syncListener = new SyncEventListener();
        
        context.addApplicationListener(asyncListener);
        context.addApplicationListener(syncListener);
        
        // 发布事件
        CustomEvent event = new CustomEvent(this);
        context.publishEvent(event);
        
        // 等待异步处理完成
        Thread.sleep(1000);
        
        assertTrue(asyncListener.isEventProcessed());
        assertTrue(syncListener.isEventProcessed());
        
        context.close();
    }
    
    static class CustomEvent extends ApplicationEvent {
        public CustomEvent(Object source) {
            super(source);
        }
    }
    
    static class AsyncEventListener implements ApplicationListener<CustomEvent> {
        private boolean eventProcessed = false;
        
        @Async
        @Override
        public void onApplicationEvent(CustomEvent event) {
            // 模拟耗时操作
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            eventProcessed = true;
        }
        
        public boolean isEventProcessed() {
            return eventProcessed;
        }
    }
    
    static class SyncEventListener implements ApplicationListener<CustomEvent> {
        private boolean eventProcessed = false;
        
        @Override
        public void onApplicationEvent(CustomEvent event) {
            eventProcessed = true;
        }
        
        public boolean isEventProcessed() {
            return eventProcessed;
        }
    }
} 