package org.microspring.web.servlet;

import org.microspring.context.ApplicationContext;
import org.microspring.web.method.HandlerMethod;
import org.microspring.web.servlet.handler.RequestMappingHandlerMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {
    
    private ApplicationContext applicationContext;
    private HandlerMapping handlerMapping;
    
    public DispatcherServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void init() throws ServletException {
        this.handlerMapping = new RequestMappingHandlerMapping(applicationContext);
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HandlerMethod handlerMethod = handlerMapping.getHandler(request);
        
        if (handlerMethod == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        try {
            Object result = handlerMethod.getMethod().invoke(handlerMethod.getBean());
            response.getWriter().write(String.valueOf(result));
        } catch (Exception e) {
            throw new ServletException("Error invoking handler method", e);
        }
    }
} 