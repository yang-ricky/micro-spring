package org.microspring.web.servlet;

import org.junit.Before;
import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InterceptorTest {
    
    private DispatcherServlet servlet;
    private HttpServletResponse response;
    private StringWriter stringWriter;
    private PrintWriter writer;
    
    @Before
    public void setup() throws Exception {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.registerBeanDefinition("testController", 
            new DefaultBeanDefinition(TestController.class));
        beanFactory.registerBeanDefinition("testInterceptor", 
            new DefaultBeanDefinition(TestInterceptor.class));
        
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        servlet = new DispatcherServlet(context);
        servlet.init();
        
        response = mock(HttpServletResponse.class);
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        TestInterceptor.clearExecutionOrder();
    }
    
    @Test
    public void testInterceptorOrder() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/hello");
        when(request.getMethod()).thenReturn("GET");
        
        servlet.service(request, response);
        writer.flush();
        
        // 验证拦截器执行顺序
        assertEquals(3, TestInterceptor.executionOrder.size());
        assertEquals("preHandle", TestInterceptor.executionOrder.get(0));
        assertEquals("postHandle", TestInterceptor.executionOrder.get(1));
        assertEquals("afterCompletion", TestInterceptor.executionOrder.get(2));
    }
    
    @Test
    public void testInterceptorBlocking() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/blocked");
        when(request.getMethod()).thenReturn("GET");
        
        servlet.service(request, response);
        
        // 验证请求被拦截
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertEquals(2, TestInterceptor.executionOrder.size());
        assertEquals("preHandle", TestInterceptor.executionOrder.get(0));
        assertEquals("afterCompletion", TestInterceptor.executionOrder.get(1));
    }
    
    @Test
    public void testMultipleInterceptors() throws Exception {
        // 添加第二个拦截器
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.registerBeanDefinition("testController", 
            new DefaultBeanDefinition(TestController.class));
        beanFactory.registerBeanDefinition("testInterceptor2", 
            new DefaultBeanDefinition(SecondTestInterceptor.class));
        beanFactory.registerBeanDefinition("testInterceptor1", 
            new DefaultBeanDefinition(OrderedTestInterceptor.class));
        
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        servlet = new DispatcherServlet(context);
        servlet.init();
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/hello");
        when(request.getMethod()).thenReturn("GET");
        
        servlet.service(request, response);
        writer.flush();
        
        // 验证执行顺序
        List<String> executionOrder = TestInterceptor.executionOrder;
        assertEquals(6, executionOrder.size());
        assertEquals("preHandle2", executionOrder.get(0));  // 第一个拦截器的preHandle
        assertEquals("preHandle1", executionOrder.get(1));  // 第二个拦截器的preHandle
        assertEquals("postHandle2", executionOrder.get(2)); // 第二个拦截器的postHandle
        assertEquals("postHandle1", executionOrder.get(3)); // 第一个拦截器的postHandle
        assertEquals("afterCompletion2", executionOrder.get(4)); // 第二个拦截器的afterCompletion
        assertEquals("afterCompletion1", executionOrder.get(5)); // 第一个拦截器的afterCompletion
    }
    
    @Test
    public void testInterceptorWithException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/error");
        when(request.getMethod()).thenReturn("GET");
        
        try {
            servlet.service(request, response);
            fail("Should throw exception");
        } catch (ServletException expected) {
            // 验证即使发生异常，afterCompletion 也会被调用
            List<String> executionOrder = TestInterceptor.executionOrder;
            assertEquals(2, executionOrder.size());
            assertEquals("preHandle", executionOrder.get(0));
            assertEquals("afterCompletion", executionOrder.get(1));
            // postHandle 不应该被调用，因为有异常发生
            assertFalse(executionOrder.contains("postHandle"));
        }
    }
    
    @Test
    public void testInterceptorChainBreak() throws Exception {
        // 添加两个拦截器，第一个返回 false
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.registerBeanDefinition("testController", 
            new DefaultBeanDefinition(TestController.class));
        beanFactory.registerBeanDefinition("interceptor1", 
            new DefaultBeanDefinition(BlockingInterceptor.class));
        beanFactory.registerBeanDefinition("interceptor2", 
            new DefaultBeanDefinition(TestInterceptor.class));
        
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        servlet = new DispatcherServlet(context);
        servlet.init();
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/hello");
        when(request.getMethod()).thenReturn("GET");
        
        servlet.service(request, response);
        
        // 验证第二个拦截器没有被执行
        List<String> executionOrder = TestInterceptor.executionOrder;
        assertTrue(executionOrder.contains("preHandle1"));
        assertFalse(executionOrder.contains("preHandle2"));
        assertTrue(executionOrder.contains("afterCompletion1"));
        assertFalse(executionOrder.contains("afterCompletion2"));
    }
    
    @Test
    public void testNoInterceptors() throws Exception {
        // 创建一个没有注册任何拦截器的 context
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.registerBeanDefinition("testController", 
            new DefaultBeanDefinition(TestController.class));
        
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        servlet = new DispatcherServlet(context);
        servlet.init();
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/hello");
        when(request.getMethod()).thenReturn("GET");
        
        servlet.service(request, response);
        writer.flush();
        
        // 验证请求能正常处理
        verify(response).setContentType("text/plain;charset=UTF-8");
        assertEquals("Hello, MVC!", stringWriter.toString());
    }
} 