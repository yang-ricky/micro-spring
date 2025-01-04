package org.microspring.core.exception;

/**
 * 当发现循环依赖且无法解决时抛出此异常
 */
public class CircularDependencyException extends RuntimeException {
    
    public CircularDependencyException(String message) {
        super(message);
    }
    
    public CircularDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
} 