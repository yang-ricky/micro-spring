package org.microspring.web.servlet;

import java.util.Set;

public class MethodNotAllowedException extends RuntimeException {
    private final String method;
    private final Set<String> allowedMethods;
    
    public MethodNotAllowedException(String method, Set<String> allowedMethods) {
        super("Request method '" + method + "' not supported");
        this.method = method;
        this.allowedMethods = allowedMethods;
    }
    
    public String getMethod() {
        return method;
    }
    
    public Set<String> getAllowedMethods() {
        return allowedMethods;
    }
} 