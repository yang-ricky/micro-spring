package org.microspring.core.exception;

/**
 * 当Bean创建过程中发生错误时抛出此异常
 */
public class BeanCreationException extends RuntimeException {
    private final String beanName;

    public BeanCreationException(String beanName, String message) {
        super("Error creating bean [" + beanName + "]: " + message);
        this.beanName = beanName;
    }

    public BeanCreationException(String beanName, String message, Throwable cause) {
        super("Error creating bean [" + beanName + "]: " + message, cause);
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
} 