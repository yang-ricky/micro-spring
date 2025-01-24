package org.microspring.test.circular;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class CircularMethodA {
    private CircularMethodB b;
    
    @Autowired
    public void setB(CircularMethodB b) {
        this.b = b;
    }
    
    public CircularMethodB getB() {
        return b;
    }
} 