package org.microspring.context.event;

public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent> {
    /**
     * 判断是否支持给定的事件类型
     */
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

    /**
     * 判断是否支持给定的事件源类型
     */
    boolean supportsSourceType(Class<?> sourceType);

    /**
     * 获取监听器的优先级，数字越小优先级越高
     */
    int getOrder();
} 