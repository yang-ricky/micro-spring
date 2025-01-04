package org.microspring.context;

import org.microspring.core.BeanFactory;

public interface ApplicationContext extends BeanFactory {
    /**
     * 获取应用程序名称
     */
    String getApplicationName();
    
    /**
     * 获取启动时间
     */
    long getStartupDate();
    
    /**
     * 刷新容器
     */
    void refresh();
    
    /**
     * 关闭容器
     */
    void close();
}