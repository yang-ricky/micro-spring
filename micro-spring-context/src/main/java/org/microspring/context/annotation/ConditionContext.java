package org.microspring.context.annotation;

import org.microspring.core.env.Environment;
import org.microspring.core.DefaultBeanFactory;

/**
 * 条件上下文，提供条件评估时需要的上下文信息
 */
public interface ConditionContext {

    /**
     * 获取 bean 定义注册表
     */
    DefaultBeanFactory getBeanFactory();

    /**
     * 获取环境配置
     */
    Environment getEnvironment();

    /**
     * 获取类加载器
     */
    ClassLoader getClassLoader();
} 