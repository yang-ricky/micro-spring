package org.microspring.context;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.stereotype.Component;

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