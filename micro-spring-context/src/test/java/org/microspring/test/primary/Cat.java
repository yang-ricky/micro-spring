package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component
public class Cat implements Animal {
    @Override
    public String getName() {
        return "cat";
    }
} 