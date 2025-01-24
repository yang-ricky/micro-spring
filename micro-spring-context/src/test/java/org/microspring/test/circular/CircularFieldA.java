package org.microspring.test.circular;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class CircularFieldA {
    @Autowired
    private CircularFieldB b;
    
    public CircularFieldB getB() {
        return b;
    }
} 