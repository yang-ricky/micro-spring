package org.microspring.test.annotation;

import org.microspring.stereotype.Component;

@Component("englishGreeting")
public class EnglishGreeting extends AbstractGreeting {
    @Override
    public String greet() {
        return "Hello";
    }
} 