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

public class RestControllerAdviceTest {
    
    private DispatcherServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private HttpServletResponse response;
    
    @Before
    public void setup() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.registerBeanDefinition("adviceController", 
            new DefaultBeanDefinition(AdviceTestController.class));
        beanFactory.registerBeanDefinition("globalHandler", 
            new DefaultBeanDefinition(GlobalExceptionHandler.class));
        
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
    public void testValidationError() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/advice/validation");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setStatus(400);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals(
            "{\"error\":\"Invalid input\",\"type\":\"validation_error\"}", 
            stringWriter.toString()
        );
    }
    
    @Test
    public void testResourceNotFound() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/advice/notfound");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setStatus(404);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals(
            "{\"error\":\"Resource not found\",\"type\":\"not_found\"}", 
            stringWriter.toString()
        );
    }
    
    @Test
    public void testGlobalError() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/advice/error");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setStatus(500);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals(
            "{\"error\":\"Internal Server Error\",\"message\":\"Something went wrong\"}", 
            stringWriter.toString()
        );
    }
} 