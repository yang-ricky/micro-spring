package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/status")
public class ResponseStatusTestController {
    
    @GetMapping("/created")
    @ResponseStatus(201)
    public Map<String, String> createResource() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Resource created");
        return response;
    }
    
    @GetMapping("/notfound")
    @ResponseStatus(value = 404, reason = "Resource not found")
    public void throwNotFound() {
        throw new ResourceNotFoundException("Item not found");
    }
    
    @GetMapping("/forbidden")
    public void throwForbidden() {
        throw new ForbiddenException();
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(404)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "not_found");
        return response;
    }
    
    @ResponseStatus(403)
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException() {
            super("Access denied");
        }
    }
    
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
} 