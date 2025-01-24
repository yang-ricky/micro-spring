package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;
import org.microspring.beans.factory.annotation.Scope;

@Component("serviceB")
@Scope("prototype")
public class ServiceB {
    @Autowired
    private ServiceA serviceA;
    
    @Autowired
    @Qualifier("specificBean")
    private ServiceA specificServiceA;
    
    public String getMessageFromA() {
        return serviceA.getMessage();
    }
    
    public String getMessageFromSpecificA() {
        return specificServiceA.getMessage();
    }
} 