package org.microspring.aop;

/**
 * Base interface for AOP proxy objects.
 * Provides a unified interface for JDK dynamic proxies and CGLIB proxies.
 */
public interface AopProxy {
    
    /**
     * Create a new proxy object.
     * @return the proxy object
     */
    Object getProxy();

    /**
     * Create a new proxy object using the given class loader.
     * @param classLoader the class loader to create the proxy with
     * @return the proxy object
     */
    Object getProxy(ClassLoader classLoader);
} 