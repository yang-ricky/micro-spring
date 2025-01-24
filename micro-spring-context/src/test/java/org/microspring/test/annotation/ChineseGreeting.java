package org.microspring.test.annotation;

import org.microspring.stereotype.Component;

@Component("chineseGreeting")
public class ChineseGreeting extends AbstractGreeting {
    @Override
    public String greet() {
        return "你好";
    }
} 