package org.microspring.test.primary;

import org.microspring.context.annotation.Primary;
import org.microspring.stereotype.Component;

@Component
@Primary
public class Dog implements Animal {
    @Override
    public String getName() {
        return "dog";
    }
} 