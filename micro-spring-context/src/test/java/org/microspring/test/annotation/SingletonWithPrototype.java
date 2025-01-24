package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Scope;

@Component
@Scope("singleton")
public class SingletonWithPrototype {
    @Autowired
    private TestPrototypeBean prototypeBean;
    
    public int getPrototypeCount() {
        return prototypeBean.increment();
    }
} 