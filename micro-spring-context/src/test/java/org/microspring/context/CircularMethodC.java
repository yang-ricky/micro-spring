package org.microspring.context;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.stereotype.Component;

@Component
public class CircularMethodC {
    private CircularMethodA a;
    
    @Autowired
    public void setA(CircularMethodA a) {
        this.a = a;
    }
    
    public CircularMethodA getA() {
        return a;
    }
} 