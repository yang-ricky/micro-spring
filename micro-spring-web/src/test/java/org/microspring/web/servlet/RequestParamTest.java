package org.microspring.web.servlet;

import org.junit.Before;
import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RequestParamTest {
    
    private DispatcherServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private HttpServletResponse response;
    
    @Before
    public void setup() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.registerBeanDefinition("paramController", 
            new DefaultBeanDefinition(RequestParamTestController.class));
        
        AnnotationConfigWebApplicationContext context = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        context.refresh();
        
        servlet = new DispatcherServlet(context);
        servlet.init();

        // Mock response
        response = mock(HttpServletResponse.class);
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
    }
    
    @Test
    public void testRequiredParam() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/params/required");
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn("123");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"id\":123}", stringWriter.toString());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingRequiredParam() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/params/required");
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn(null);

        // Execute
        servlet.service(request, response);
    }
    
    @Test
    public void testOptionalParam() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/params/optional");
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("name")).thenReturn(null);

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"name\":null}", stringWriter.toString());
    }
    
    @Test
    public void testDefaultValue() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/params/default");
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("age")).thenReturn(null);

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"age\":18}", stringWriter.toString());
    }
    
    @Test
    public void testMultipleParams() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/params/multiple");
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn("123");
        when(request.getParameter("name")).thenReturn("John");
        when(request.getParameter("age")).thenReturn("25");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"name\":\"John\",\"id\":123,\"age\":25}", stringWriter.toString());
    }
} 