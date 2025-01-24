package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component
public class Bird implements Animal {
    @Override
    public String getName() {
        return "bird";
    }
} 