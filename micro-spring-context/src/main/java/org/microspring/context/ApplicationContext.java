package org.microspring.context;

import org.microspring.core.BeanFactory;

public interface ApplicationContext extends BeanFactory {
    void refresh();
    String getApplicationName();
    long getStartupDate();
    boolean containsBean(String name);
    <T> T getBean(Class<T> requiredType);
}