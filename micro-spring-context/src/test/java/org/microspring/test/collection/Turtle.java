package org.microspring.test.collection;

import org.microspring.stereotype.Component;

@Component
public class Turtle implements Pet {
    @Override
    public String getName() {
        return "turtle";
    }
} 