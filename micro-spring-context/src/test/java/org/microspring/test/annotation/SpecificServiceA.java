package org.microspring.test.annotation;

import org.microspring.stereotype.Component;

@Component("specificBean")
public class SpecificServiceA extends ServiceA {
    @Override
    public String getMessage() {
        return "Hello from Specific ServiceA";
    }
} 