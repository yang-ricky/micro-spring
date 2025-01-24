package org.microspring.test.annotation;

import org.microspring.stereotype.Component;

@Component
public class ServiceA {
    private String message = "Hello from ServiceA";
    public String getMessage() {
        return message;
    }
} 