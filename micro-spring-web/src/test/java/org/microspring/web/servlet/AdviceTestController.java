package org.microspring.web.servlet;

import org.microspring.web.annotation.*;

@RestController
@RequestMapping("/advice")
public class AdviceTestController {
    
    @GetMapping("/validation")
    public void throwValidationError() {
        throw new IllegalArgumentException("Invalid input");
    }
    
    @GetMapping("/notfound")
    public void throwNotFound() {
        throw new ResourceNotFoundException("Resource not found");
    }
    
    @GetMapping("/error")
    public void throwError() {
        throw new RuntimeException("Something went wrong");
    }
    
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
} 