package org.microspring.test.collection;

import org.microspring.stereotype.Component;

@Component
public class Rabbit implements Pet {
    @Override
    public String getName() {
        return "rabbit";
    }
} 