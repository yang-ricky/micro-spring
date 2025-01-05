package org.microspring.web.servlet;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.web.context.support.AnnotationConfigWebApplicationContext;
import org.microspring.web.annotation.Controller;
import org.microspring.web.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class DispatcherServletTest {

    @Test
    public void testDispatchRequest() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册 TestController
        DefaultBeanDefinition testControllerDef = new DefaultBeanDefinition(TestController.class);
        beanFactory.registerBeanDefinition("testController", testControllerDef);
        
        // 创建 ApplicationContext 并使用已配置的 BeanFactory
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        DispatcherServlet servlet = new DispatcherServlet(context);
        servlet.init();

        // Mock request/response
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/hello");
        when(response.getWriter()).thenReturn(writer);

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        assertEquals("Hello, MVC!", stringWriter.toString());
    }

    @Test
    public void testRestController() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        DefaultBeanDefinition restControllerDef = new DefaultBeanDefinition(TestRestController.class);
        beanFactory.registerBeanDefinition("testRestController", restControllerDef);
        
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        DispatcherServlet servlet = new DispatcherServlet(context);
        servlet.init();

        // Mock request/response
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/user");
        when(response.getWriter()).thenReturn(writer);

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"name\":\"John\",\"age\":25}", stringWriter.toString());
    }
} 