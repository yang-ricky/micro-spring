package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Scope;

@Component
@Scope("prototype")
public class TestPrototypeBean {
    private int count = 0;
    public int increment() {
        return ++count;
    }
} 