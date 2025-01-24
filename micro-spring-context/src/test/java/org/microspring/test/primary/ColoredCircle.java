package org.microspring.test.primary;

import org.microspring.stereotype.Component;

@Component
public class ColoredCircle extends Circle {
    @Override
    public String getShape() {
        return "colored-circle";
    }
} 