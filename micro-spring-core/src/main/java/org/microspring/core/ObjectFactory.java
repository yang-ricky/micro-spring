package org.microspring.core;

/**
 * 用于延迟创建Bean实例的工厂接口
 * 主要用于解决循环依赖时创建早期引用
 */
public interface ObjectFactory<T> {
    /**
     * 获取对象实例
     * @return 对象实例
     */
    T getObject();
} 