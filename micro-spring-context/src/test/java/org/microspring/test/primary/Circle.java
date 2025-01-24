package org.microspring.test.primary;

import org.microspring.context.annotation.Primary;
import org.microspring.stereotype.Component;

@Component
@Primary
public class Circle implements Shape {
    @Override
    public String getShape() {
        return "circle";
    }
} 