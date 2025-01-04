package org.microspring.core.condition;

import org.microspring.core.DefaultBeanFactory;

public interface ConditionContext {
    String getEnvironment(String key);
    DefaultBeanFactory getBeanFactory();
} 