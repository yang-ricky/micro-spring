package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

@Component
public class ServiceWithConstructor {
    private final ServiceA serviceA;
    private final ServiceB serviceB;

    @Autowired  // 构造器注入
    public ServiceWithConstructor(ServiceA serviceA, 
                                @Qualifier("serviceB") ServiceB serviceB) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
    }

    public String getMessageFromBoth() {
        return serviceA.getMessage() + " & " + serviceB.getMessageFromA();
    }
} 