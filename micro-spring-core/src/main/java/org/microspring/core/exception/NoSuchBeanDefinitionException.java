package org.microspring.core.exception;

/**
 * 当找不到对应的Bean定义时抛出此异常
 */
public class NoSuchBeanDefinitionException extends RuntimeException {
    private final String beanName;

    public NoSuchBeanDefinitionException(String beanName) {
        super("No bean named '" + beanName + "' is defined");
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
} 