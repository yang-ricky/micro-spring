package org.microspring.aop.support;

import org.microspring.core.BeanPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.aop.Aspect;
import org.microspring.aop.advice.LogAdvice;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class AspectBeanPostProcessor implements BeanPostProcessor {
    private final DefaultBeanFactory beanFactory;
    private final List<Object> aspects = new ArrayList<>();
    
    public AspectBeanPostProcessor(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean.getClass().isAnnotationPresent(Aspect.class)) {
            aspects.add(bean);
            // 按order排序
            aspects.sort((a1, a2) -> {
                Aspect aspect1 = a1.getClass().getAnnotation(Aspect.class);
                Aspect aspect2 = a2.getClass().getAnnotation(Aspect.class);
                return Integer.compare(aspect1.order(), aspect2.order());
            });
        }
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!aspects.isEmpty() && !bean.getClass().isAnnotationPresent(Aspect.class)) {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                return createProxy(bean, interfaces);
            }
        }
        return bean;
    }
    
    private Object createProxy(final Object target, Class<?>[] interfaces) {
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            interfaces,
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    
                    // 按order顺序执行before方法
                    for (Object aspect : aspects) {
                        try {
                            Method beforeMethod = aspect.getClass().getDeclaredMethod("before", Method.class, Object[].class);
                            beforeMethod.setAccessible(true);
                            beforeMethod.invoke(aspect, method, args);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    try {
                        Object result = method.invoke(target, args);
                        
                        // 按order相反顺序执行after方法
                        List<Object> reversedAspects = new ArrayList<>(aspects);
                        Collections.reverse(reversedAspects);
                        
                        for (Object aspect : reversedAspects) {
                            try {
                                Method afterMethod = aspect.getClass().getDeclaredMethod("afterReturning", 
                                    Method.class, Object.class);
                                afterMethod.setAccessible(true);
                                afterMethod.invoke(aspect, method, result);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        return result;
                    } catch (Exception e) {
                        Throwable targetException = e.getCause() != null ? e.getCause() : e;
                        
                        // 按order相反顺序执行afterThrowing方法
                        List<Object> reversedAspects = new ArrayList<>(aspects);
                        Collections.reverse(reversedAspects);
                        
                        for (Object aspect : reversedAspects) {
                            try {
                                Method throwingMethod = aspect.getClass().getDeclaredMethod("afterThrowing", 
                                    Method.class, Exception.class);
                                throwingMethod.setAccessible(true);
                                throwingMethod.invoke(aspect, method, targetException);
                            } catch (Exception ex) {
                                System.err.println("[ERROR] Failed to invoke afterThrowing method on aspect: " + 
                                    aspect.getClass().getSimpleName() + ", error: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                        throw targetException;
                    }
                }
            }
        );
    }
} 