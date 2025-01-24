package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component
public class Car implements Vehicle {
    @Override
    public String getType() {
        return "car";
    }
} 