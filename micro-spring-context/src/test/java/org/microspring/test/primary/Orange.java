package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component
public class Orange implements Fruit {
    @Override
    public String getName() {
        return "orange";
    }
} 