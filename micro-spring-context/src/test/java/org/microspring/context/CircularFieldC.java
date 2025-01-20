package org.microspring.context;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.stereotype.Component;

@Component
public class CircularFieldC {
    @Autowired
    private CircularFieldA a;
    
    public CircularFieldA getA() {
        return a;
    }
} 