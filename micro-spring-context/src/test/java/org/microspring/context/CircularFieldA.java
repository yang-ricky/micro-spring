package org.microspring.context;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.stereotype.Component;

@Component
public class CircularFieldA {
    @Autowired
    private CircularFieldB b;
    
    public CircularFieldB getB() {
        return b;
    }
} 