package org.microspring.test.annotation;

import org.microspring.stereotype.Component;

@Component("messageService1")
public class MessageServiceImpl1 implements MessageService {
    @Override
    public String getMessage() {
        return "Message from Implementation 1";
    }
} 