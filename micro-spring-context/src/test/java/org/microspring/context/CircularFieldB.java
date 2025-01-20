package org.microspring.context;

import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.stereotype.Component;

@Component
public class CircularFieldB {
    @Autowired
    private CircularFieldC c;
    
    public CircularFieldC getC() {
        return c;
    }
} 