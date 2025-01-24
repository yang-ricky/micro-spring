package org.microspring.test.annotation;

import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

@Component
public class GreetingService {
    @Autowired
    @Qualifier("chineseGreeting")
    private AbstractGreeting chineseGreeting;
    
    @Autowired
    @Qualifier("englishGreeting")
    private AbstractGreeting englishGreeting;
    
    @Autowired
    @Qualifier("messageService1")
    private MessageService messageService1;
    
    @Autowired
    @Qualifier("messageService2")
    private MessageService messageService2;
    
    public String getAllMessages() {
        return String.join(" | ", 
            chineseGreeting.greet(),
            englishGreeting.greet(),
            messageService1.getMessage(),
            messageService2.getMessage()
        );
    }
} 