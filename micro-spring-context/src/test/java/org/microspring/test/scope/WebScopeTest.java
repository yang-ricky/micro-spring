package org.microspring.test.scope;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.context.scope.ScopeManager;
import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Scope;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebScopeTest {
    
    @Mock
    private HttpServletRequest request1;
    
    @Mock
    private HttpServletRequest request2;
    
    @Mock
    private HttpSession session;
    
    private AnnotationConfigApplicationContext context;
    private ScopeManager scopeManager;
    
    @Component
    @Scope("request")
    public static class RequestScopedBean {
        private int count = 0;
        public int increment() {
            return ++count;
        }
    }
    
    @Component
    @Scope("session")
    public static class SessionScopedBean {
        private int count = 0;
        public int increment() {
            return ++count;
        }
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // 配置 mock 对象
        when(request1.getSession()).thenReturn(session);
        when(request2.getSession()).thenReturn(session);
        when(request1.getRequestedSessionId()).thenReturn("request1");
        when(request2.getRequestedSessionId()).thenReturn("request2");
        when(session.getId()).thenReturn("session1");
        
        // 创建并配置 context
        context = new AnnotationConfigApplicationContext();
        scopeManager = context.getScopeManager();
        
        // 设置 request 和 session，以便在扫描和初始化时使用
        scopeManager.setCurrentRequest(request1);
        scopeManager.setCurrentSession(session);
        
        context.setBasePackage(getClass().getPackage().getName());
        context.refresh();
        
        // 清理，让每个测试方法自己设置需要的 request 和 session
        scopeManager.setCurrentRequest(null);
        scopeManager.setCurrentSession(null);
    }
    
    @Test
    public void testRequestScope() {
        // 模拟第一个请求
        scopeManager.setCurrentRequest(request1);
        RequestScopedBean bean1 = (RequestScopedBean) context.getBean("requestScopedBean");
        assertEquals(1, bean1.increment());
        assertEquals(2, bean1.increment());
        
        // 模拟第二个请求
        scopeManager.setCurrentRequest(request2);
        RequestScopedBean bean2 = (RequestScopedBean) context.getBean("requestScopedBean");
        assertEquals(1, bean2.increment());  // 新的请求，应该是新的实例
        
        // 清理
        scopeManager.setCurrentRequest(null);
    }
    
    @Test
    public void testSessionScope() {
        try {
            // 设置 request 和 session
            scopeManager.setCurrentRequest(request1);
            scopeManager.setCurrentSession(session);
            
            // 模拟同一会话的两个不同请求
            SessionScopedBean bean1 = (SessionScopedBean) context.getBean("sessionScopedBean");
            assertEquals(1, bean1.increment());
            
            // 切换到另一个请求，但是同一个 session
            scopeManager.setCurrentRequest(request2);
            SessionScopedBean bean2 = (SessionScopedBean) context.getBean("sessionScopedBean");
            assertEquals(2, bean2.increment());  // 同一会话，应该是同一个实例
            
        } finally {
            // 清理
            scopeManager.setCurrentRequest(null);
            scopeManager.setCurrentSession(null);
        }
    }
    
    @Test
    public void testSessionScopedBeanConsistency() {
        try {
            // 必须同时设置 request 和 session
            scopeManager.setCurrentRequest(request1);
            scopeManager.setCurrentSession(session);
            
            // 通过不同方式获取 session 作用域的 bean
            SessionScopedBean bean1 = (SessionScopedBean) context.getBean("sessionScopedBean");
            SessionScopedBean bean2 = (SessionScopedBean) context.getBeansWithAnnotation(Component.class)
                .get("sessionScopedBean");
            
            // 在同一个 session 中应该是同一个实例
            assertSame("Session scoped beans should be the same within same session", bean1, bean2);
            
        } finally {
            // 清理
            scopeManager.setCurrentRequest(null);
            scopeManager.setCurrentSession(null);
        }
    }
} 