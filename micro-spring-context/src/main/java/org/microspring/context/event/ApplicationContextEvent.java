package org.microspring.context.event;

import org.microspring.context.ApplicationContext;

/**
 * 所有应用上下文事件的基类
 */
public abstract class ApplicationContextEvent extends ApplicationEvent {
    public ApplicationContextEvent(ApplicationContext source) {
        super(source);
    }
    
    public ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }
} 