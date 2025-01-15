package org.microspring.test.event;

import org.junit.Test;
import org.microspring.context.event.ApplicationEvent;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class CustomEventTest {
    static List<String> events = new ArrayList<>();
    
    // 自定义事件：用户注册事件
    static class UserRegisteredEvent extends ApplicationEvent {
        private final String username;
        
        public UserRegisteredEvent(Object source, String username) {
            super(source);
            this.username = username;
        }
        
        public String getUsername() {
            return username;
        }
    }
    
    // 自定义监听器：发送欢迎邮件
    static class WelcomeEmailListener implements ApplicationListener<UserRegisteredEvent> {
        @Override
        public void onApplicationEvent(UserRegisteredEvent event) {
            events.add("Welcome email sent to: " + event.getUsername());
        }
    }
    
    // 自定义监听器：记录日志
    static class UserRegistrationLogListener implements ApplicationListener<UserRegisteredEvent> {
        @Override
        public void onApplicationEvent(UserRegisteredEvent event) {
            events.add("User registration logged: " + event.getUsername());
        }
    }
    
    @Test
    public void testUserRegistrationEvent() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        // 注册监听器
        context.addApplicationListener(new WelcomeEmailListener());
        context.addApplicationListener(new UserRegistrationLogListener());
        
        events.clear();
        
        // 模拟用户注册
        context.publishEvent(new UserRegisteredEvent(this, "testUser"));
        
        // 验证事件处理
        assertEquals(2, events.size());
        assertTrue(events.get(0).contains("Welcome email sent to: testUser"));
        assertTrue(events.get(1).contains("User registration logged: testUser"));
        
        // 验证事件的解耦性：可以轻松添加新的监听器而不影响现有代码
        context.addApplicationListener(new ApplicationListener<UserRegisteredEvent>() {
            @Override
            public void onApplicationEvent(UserRegisteredEvent event) {
                events.add("Additional processing for: " + event.getUsername());
            }
        });
        
        events.clear();
        context.publishEvent(new UserRegisteredEvent(this, "anotherUser"));
        
        // 验证新增的监听器也能正常工作
        assertEquals(3, events.size());
        
        context.close();
    }
} 