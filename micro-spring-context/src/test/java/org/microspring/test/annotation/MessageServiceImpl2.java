package org.microspring.test.annotation;

import org.microspring.stereotype.Component;

@Component("messageService2")
public class MessageServiceImpl2 implements MessageService {
    @Override
    public String getMessage() {
        return "Message from Implementation 2";
    }
} 