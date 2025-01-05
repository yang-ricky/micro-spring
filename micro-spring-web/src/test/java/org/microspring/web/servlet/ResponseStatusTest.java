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

public class ResponseStatusTest {
    
    private DispatcherServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private HttpServletResponse response;
    
    @Before
    public void setup() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        DefaultBeanDefinition controllerDef = new DefaultBeanDefinition(ResponseStatusTestController.class);
        beanFactory.registerBeanDefinition("statusController", controllerDef);
        
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
    public void testMethodLevelStatus() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/status/created");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setStatus(201);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"message\":\"Resource created\"}", stringWriter.toString());
    }
    
    @Test
    public void testExceptionHandlerStatus() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/status/notfound");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setStatus(404);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"error\":\"Item not found\",\"type\":\"not_found\"}", 
                    stringWriter.toString());
    }
    
    @Test
    public void testExceptionLevelStatus() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/status/forbidden");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setStatus(403);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"error\":\"Access denied\"}", stringWriter.toString());
    }
} 