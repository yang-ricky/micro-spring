package org.microspring.test;

import org.microspring.beans.factory.annotation.Lazy;
import org.microspring.stereotype.Component;

@Component("lazyComponent")
@Lazy
public class LazyComponent {
    public LazyComponent() {
        System.out.println("LazyComponent is being created");
    }
    
    public String getMessage() {
        return "Hello from LazyComponent";
    }
} 