package org.microspring.context;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.stereotype.Component;

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