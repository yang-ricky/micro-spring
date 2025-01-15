package org.microspring.test.event;

import org.microspring.context.ApplicationContext;
import org.microspring.context.event.ApplicationEvent;
import java.lang.annotation.Annotation;
import java.util.Map;

public class MockApplicationContext implements ApplicationContext {
    @Override
    public String getApplicationName() { return "MockContext"; }
    
    @Override
    public long getStartupDate() { return System.currentTimeMillis(); }
    
    @Override
    public void refresh() {}
    
    @Override
    public void close() {}
    
    @Override
    public Object getBean(String name) { return null; }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) { return null; }
    
    @Override
    public <T> T getBean(Class<T> requiredType) { return null; }
    
    @Override
    public boolean containsBean(String name) { return false; }
    
    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) { return null; }
    
    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) { return new String[0]; }
    
    @Override
    public void setParent(ApplicationContext parent) {}
    
    @Override
    public ApplicationContext getParent() { return null; }
    
    @Override
    public void publishEvent(ApplicationEvent event) {}
} 