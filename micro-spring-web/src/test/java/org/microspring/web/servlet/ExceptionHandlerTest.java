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

public class ExceptionHandlerTest {
    
    private DispatcherServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private HttpServletResponse response;
    
    @Before
    public void setup() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        DefaultBeanDefinition controllerDef = new DefaultBeanDefinition(ExceptionHandlerTestController.class);
        beanFactory.registerBeanDefinition("exceptionController", controllerDef);
        
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
    public void testCustomExceptionHandler() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/error");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals(
            "{\"error\":\"Something went wrong\",\"type\":\"custom_error\"}", 
            stringWriter.toString()
        );
    }
    
    @Test
    public void testRuntimeExceptionHandler() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/test/error2");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals(
            "{\"error\":\"Runtime error occurred\",\"type\":\"runtime_error\"}", 
            stringWriter.toString()
        );
    }
} 