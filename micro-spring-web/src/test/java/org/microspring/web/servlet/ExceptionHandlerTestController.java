package org.microspring.web.servlet;

import org.microspring.web.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class ExceptionHandlerTestController {
    
    @GetMapping("/error")
    public void throwException() {
        throw new CustomException("Something went wrong");
    }
    
    @GetMapping("/error2")
    public void throwRuntimeException() {
        throw new RuntimeException("Runtime error occurred");
    }
    
    @ExceptionHandler(CustomException.class)
    public Map<String, String> handleCustomException(CustomException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "custom_error");
        return response;
    }
    
    @ExceptionHandler(RuntimeException.class)
    public Map<String, String> handleRuntimeException(RuntimeException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "runtime_error");
        return response;
    }
    
    public static class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }
} 