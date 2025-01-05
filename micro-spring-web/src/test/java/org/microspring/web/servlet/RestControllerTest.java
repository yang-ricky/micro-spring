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

public class RestControllerTest {
    
    private DispatcherServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private HttpServletResponse response;
    
    @Before
    public void setup() throws Exception {
        // Setup
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        DefaultBeanDefinition restControllerDef = new DefaultBeanDefinition(TestRestController.class);
        beanFactory.registerBeanDefinition("testRestController", restControllerDef);
        
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
    public void testRequestMapping() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("[{\"name\":\"John\",\"age\":25},{\"name\":\"Jane\",\"age\":24}]", 
                    stringWriter.toString());
    }
    
    @Test
    public void testGetMapping() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/user");
        when(request.getMethod()).thenReturn("GET");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"name\":\"John\",\"age\":25}", stringWriter.toString());
    }
    
    @Test
    public void testPostMapping() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/user");
        when(request.getMethod()).thenReturn("POST");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"name\":\"New User\",\"age\":30}", stringWriter.toString());
    }
    
    @Test
    public void testPutMapping() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/user");
        when(request.getMethod()).thenReturn("PUT");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"name\":\"Updated User\",\"age\":35}", stringWriter.toString());
    }
    
    @Test
    public void testDeleteMapping() throws Exception {
        // Mock request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/user/1");
        when(request.getMethod()).thenReturn("DELETE");

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"message\":\"User deleted successfully\"}", stringWriter.toString());
    }
    
    @Test
    public void testMethodNotAllowed() throws Exception {
        // Mock request with wrong HTTP method
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/user/1");
        when(request.getMethod()).thenReturn("PATCH");  // 使用未支持的方法

        // Execute
        servlet.service(request, response);
        writer.flush();

        // Verify
        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
} 