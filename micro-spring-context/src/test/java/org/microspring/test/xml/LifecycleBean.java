package org.microspring.test.xml;

public class LifecycleBean {
    private boolean initialized = false;
    private boolean destroyed = false;
    
    public void init() {
        initialized = true;
    }
    
    public void destroy() {
        destroyed = true;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
} 