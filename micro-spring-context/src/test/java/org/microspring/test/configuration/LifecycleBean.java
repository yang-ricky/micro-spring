package org.microspring.test.configuration;

public class LifecycleBean {
    private boolean initialized = false;
    private boolean destroyed = false;
    
    public void init() {
        initialized = true;
    }
    
    public void cleanup() {
        destroyed = true;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
} 