package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.ApplicationContext;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.context.event.ApplicationEvent;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.event.ContextRefreshedEvent;

import static org.junit.Assert.assertTrue;

public class ApplicationEventTest {

    @Test
    public void testContextRefreshedEvent() {
        TestEventListener listener = new TestEventListener();
        
        // 先添加监听器，再创建上下文
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.addApplicationListener(listener);
        
        // 设置扫描路径并刷新上下文
        context.setBasePackage("org.microspring.test.event");
        context.refresh();
        
        assertTrue("ContextRefreshedEvent should be received", listener.refreshEventReceived);
    }

    static class TestEventListener implements ApplicationListener<ContextRefreshedEvent> {
        private boolean refreshEventReceived = false;

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            this.refreshEventReceived = true;
        }
    }
} 