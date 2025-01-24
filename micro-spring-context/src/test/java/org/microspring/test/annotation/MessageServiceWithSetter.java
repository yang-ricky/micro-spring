package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

@Component
public class MessageServiceWithSetter {
    private ServiceA serviceA;
    private ServiceB serviceB;
    
    // 标准的 setter 方法注入
    @Autowired
    public void setServiceA(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
    
    @Autowired
    @Qualifier("serviceB")  // 使用限定符指定具体的 serviceB
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
    
    public String getCombinedMessages() {
        return serviceA.getMessage() + " and " + serviceB.getMessageFromA();
    }
} 