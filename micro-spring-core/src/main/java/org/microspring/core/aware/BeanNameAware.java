package org.microspring.core.aware;

/**
 * 实现此接口的Bean可以获得自己的beanName
 */
public interface BeanNameAware {
    void setBeanName(String name);
} 