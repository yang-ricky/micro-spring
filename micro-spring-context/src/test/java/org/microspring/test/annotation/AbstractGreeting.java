package org.microspring.test.annotation;

public abstract class AbstractGreeting {
    public abstract String greet();
    
    public String getGreetingWithTime() {
        return greet() + " at " + System.currentTimeMillis();
    }
} 