package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Scope;

@Component
@Scope("singleton")
public class TestSingletonBean {
    private int count = 0;
    public int increment() {
        return ++count;
    }
} 