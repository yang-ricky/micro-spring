package org.microspring.context.event;

import java.util.Collection;

/**
 * 事件广播器接口
 */
public interface ApplicationEventMulticaster {
    /**
     * 添加监听器
     */
    void addApplicationListener(ApplicationListener<?> listener);

    /**
     * 移除监听器
     */
    void removeApplicationListener(ApplicationListener<?> listener);

    /**
     * 移除所有监听器
     */
    void removeAllListeners();

    /**
     * 广播事件给所有适合的监听器
     */
    void multicastEvent(ApplicationEvent event);
} 