package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import org.microspring.web.servlet.AdviceTestController.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(400)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "validation_error");
        return response;
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(404)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "not_found");
        return response;
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(500)
    public Map<String, String> handleException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", ex.getMessage());
        return response;
    }
} 