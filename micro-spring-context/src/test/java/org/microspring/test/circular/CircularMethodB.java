package org.microspring.test.circular;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class CircularMethodB {
    private CircularMethodC c;
    
    @Autowired
    public void setC(CircularMethodC c) {
        this.c = c;
    }
    
    public CircularMethodC getC() {
        return c;
    }
} 