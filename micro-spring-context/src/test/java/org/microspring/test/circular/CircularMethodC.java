package org.microspring.test.circular;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

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