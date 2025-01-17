package org.microspring.context.service;

import org.microspring.stereotype.Component;

@Component("messageService1")
public class MessageServiceImpl1 implements MessageService {
    @Override
    public String getMessage() {
        return "Message from Service 1";
    }
} 