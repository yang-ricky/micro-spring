package org.microspring.context.event;

import org.microspring.context.ApplicationContext;
import org.microspring.context.support.AbstractApplicationContext;
import org.microspring.stereotype.Component;
import org.microspring.core.BeanDefinition;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Map;

public class EventListenerMethodProcessor {
    
    private final AbstractApplicationContext applicationContext;
    
    public EventListenerMethodProcessor(AbstractApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void processEventListenerMethods() {
        // 1. 处理实现了ApplicationListener接口的bean
        String[] listenerNames = applicationContext.getBeanNamesForType(ApplicationListener.class);
        for (String listenerName : listenerNames) {
            ApplicationListener<?> listener = (ApplicationListener<?>) applicationContext.getBean(listenerName);
            applicationContext.addApplicationListener(listener);
        }
        
        // 2. 处理带有@EventListener注解的方法
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        if (beans == null || beans.isEmpty()) {
            return;
        }
        
        for (Object bean : beans.values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                EventListener eventListener = method.getAnnotation(EventListener.class);
                if (eventListener != null) {
                    validateMethod(method);
                    method.setAccessible(true);
                    
                    // 创建一个ApplicationListener适配器
                    ApplicationListener<?> listener = new ApplicationListener<ApplicationEvent>() {
                        @Override
                        public void onApplicationEvent(ApplicationEvent event) {
                            try {
                                if (method.getParameterTypes()[0].isInstance(event)) {
                                    method.invoke(bean, event);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to invoke event listener", e);
                            }
                        }
                    };
                    
                    // 注册监听器
                    applicationContext.addApplicationListener(listener);
                }
            }
        }
    }
    
    private void validateMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new IllegalStateException(
                "Event listener method must have exactly one parameter: " + method);
        }
        if (!ApplicationEvent.class.isAssignableFrom(paramTypes[0])) {
            throw new IllegalStateException(
                "Event listener method parameter must be assignable to ApplicationEvent: " + method);
        }
    }
} 