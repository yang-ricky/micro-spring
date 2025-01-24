package org.microspring.test.configuration;

public class PrototypeBean {
    private static int instanceCount = 0;
    
    public PrototypeBean() {
        instanceCount++;
    }
    
    public static int getInstanceCount() {
        return instanceCount;
    }
    
    public static void resetInstanceCount() {
        instanceCount = 0;
    }
} 