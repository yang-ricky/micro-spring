package org.microspring.context;

import org.microspring.core.BeanFactory;
import org.microspring.context.event.ApplicationEventPublisher;
import java.lang.annotation.Annotation;
import java.util.Map;

public interface ApplicationContext extends BeanFactory, ApplicationEventPublisher {
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
    
    Object getBean(String name);
    <T> T getBean(String name, Class<T> requiredType);
    <T> T getBean(Class<T> requiredType);
    boolean containsBean(String name);
    
    // 新增方法：获取带有指定注解的所有bean
    Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType);
}