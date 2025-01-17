package org.microspring.context.service;

import org.microspring.stereotype.Component;

@Component("messageService2")
public class MessageServiceImpl2 implements MessageService {
    @Override
    public String getMessage() {
        return "Message from Service 2";
    }
} 