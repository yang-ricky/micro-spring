package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

@Component
public class MessageServiceWithMethod {
    private ServiceA serviceA;
    private ServiceB serviceB;
    
    // 普通方法注入，一次注入多个依赖
    @Autowired
    public void initializeServices(ServiceA serviceA, 
                                 @Qualifier("serviceB") ServiceB serviceB) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        // 这里可以添加一些初始化逻辑
    }
    
    public String getMessages() {
        return "Standard: " + serviceA.getMessage() + 
               ", From B: " + serviceB.getMessageFromA() + 
               ", Specific: " + serviceB.getMessageFromSpecificA();
    }
} 