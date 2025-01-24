package org.microspring.test.circular;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class CircularFieldC {
    @Autowired
    private CircularFieldA a;
    
    public CircularFieldA getA() {
        return a;
    }
} 