package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Scope;

@Component
@Scope("prototype")
public class PrototypeWithSingleton {
    @Autowired
    private TestSingletonBean singletonBean;
    
    public int getSingletonCount() {
        return singletonBean.increment();
    }
} 