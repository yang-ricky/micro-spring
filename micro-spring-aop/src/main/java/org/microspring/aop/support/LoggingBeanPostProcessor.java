package org.microspring.aop.support;

import org.microspring.core.BeanPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.aop.annotation.Loggable;
import org.microspring.aop.advice.LogAdvice;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class LoggingBeanPostProcessor implements BeanPostProcessor {
    private final DefaultBeanFactory beanFactory;
    
    public LoggingBeanPostProcessor(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().isAnnotationPresent(Loggable.class)) {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            if (interfaces.length == 0) {
                System.out.println("[INFO] Bean '" + beanName + 
                    "' using CGLIB proxy as it doesn't implement any interface");
                return createCglibProxy(bean);
            }
            System.out.println("[INFO] Bean '" + beanName + 
                "' using JDK dynamic proxy as it implements interfaces");
            return createProxy(bean, interfaces);
        }
        return bean;
    }

    private Object createProxy(final Object target, Class<?>[] interfaces) {
        LogAdvice logAdvice = beanFactory.getBean("logAdvice", LogAdvice.class);
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            interfaces,
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    logAdvice.before(method, args);
                    try {
                        Object result = method.invoke(target, args);
                        logAdvice.afterReturning(method, result);
                        return result;
                    } catch (Exception e) {
                        Throwable targetException = e.getCause() != null ? e.getCause() : e;
                        logAdvice.afterThrowing(method, (Exception)targetException);
                        throw targetException;
                    }
                }
            }
        );
    }

    private Object createCglibProxy(final Object target) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, 
                                  MethodProxy proxy) throws Throwable {
                System.out.println("[LogAdvice(CGLIB)] Before method: " + method.getName());
                try {
                    Object result = proxy.invokeSuper(obj, args);
                    System.out.println("[LogAdvice(CGLIB)] After method: " + method.getName() + 
                        " took " + System.nanoTime() + " ns");
                    return result;
                } catch (Exception e) {
                    Throwable targetException = e.getCause() != null ? e.getCause() : e;
                    System.out.println("[LogAdvice(CGLIB)] Exception in method: " + 
                        method.getName() + ": " + targetException.getMessage());
                    throw targetException;
                }
            }
        });
        return enhancer.create();
    }
} 