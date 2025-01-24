package org.microspring.test.xml;

public class BeanFactory {
    private Class<?> targetClass;
    
    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }
    
    public Object getObject() throws Exception {
        return targetClass.newInstance();
    }
} 