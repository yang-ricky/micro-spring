package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component
public class Apple implements Fruit {
    @Override
    public String getName() {
        return "apple";
    }
} 