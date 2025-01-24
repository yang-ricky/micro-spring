package org.microspring.test.circular;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

@Component
public class CircularFieldB {
    @Autowired
    private CircularFieldC c;
    
    public CircularFieldC getC() {
        return c;
    }
} 