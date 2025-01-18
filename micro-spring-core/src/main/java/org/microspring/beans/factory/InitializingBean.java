package org.microspring.beans.factory;

/**
 * Bean初始化接口
 * 实现此接口的Bean会在所有属性设置完成后
 * 自动执行afterPropertiesSet方法
 */
public interface InitializingBean {
    /**
     * Bean属性设置完成后的初始化方法
     */
    void afterPropertiesSet() throws Exception;
} 