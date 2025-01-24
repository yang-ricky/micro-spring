package org.microspring.test.collection;

import org.microspring.stereotype.Component;

@Component
public class Hamster implements Pet {
    @Override
    public String getName() {
        return "hamster";
    }
} 