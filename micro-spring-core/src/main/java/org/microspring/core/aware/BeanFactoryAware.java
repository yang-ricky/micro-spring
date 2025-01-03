package org.microspring.core.aware;

import org.microspring.core.BeanFactory;

/**
 * 实现此接口的Bean可以获得对BeanFactory的引用
 */
public interface BeanFactoryAware {
    /**
     * 设置BeanFactory
     * @param beanFactory 创建该Bean的BeanFactory
     */
    void setBeanFactory(BeanFactory beanFactory);
} 