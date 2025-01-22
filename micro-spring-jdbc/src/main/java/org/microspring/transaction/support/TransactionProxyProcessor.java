package org.microspring.transaction.support;

import org.microspring.core.BeanPostProcessor;
import org.microspring.transaction.TransactionStatus;
import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.annotation.Transactional;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import java.util.Arrays;

public class TransactionProxyProcessor implements BeanPostProcessor {
    
    private final AbstractPlatformTransactionManager transactionManager;
    
    public TransactionProxyProcessor(AbstractPlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        
        // 检查类或方法是否有@Transactional注解
        if (!hasTransactionalAnnotation(beanClass)) {
            return bean;
        }
        
        // 创建动态代理
        return Proxy.newProxyInstance(
            beanClass.getClassLoader(),
            beanClass.getInterfaces().length > 0 ? beanClass.getInterfaces() : new Class<?>[] { beanClass },
            new TransactionInvocationHandler(bean, transactionManager)
        );
    }
    
    private boolean hasTransactionalAnnotation(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Transactional.class)) {
            return true;
        }
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) {
                return true;
            }
        }
        return false;
    }
    
    private static class TransactionInvocationHandler implements InvocationHandler {
        private final Object target;
        private final AbstractPlatformTransactionManager transactionManager;
        
        public TransactionInvocationHandler(Object target, AbstractPlatformTransactionManager transactionManager) {
            this.target = target;
            this.transactionManager = transactionManager;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            
            Transactional transactional = method.getAnnotation(Transactional.class);
            if (transactional == null) {
                transactional = target.getClass().getAnnotation(Transactional.class);
            }
            
            if (transactional == null) {
                return method.invoke(target, args);
            }
            
            // 创建事务定义
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            definition.setPropagationBehavior(transactional.propagation());
            definition.setIsolationLevel(transactional.isolation());
            definition.setReadOnly(transactional.readOnly());
            
            TransactionStatus status = transactionManager.getTransaction(definition);
            
            try {
                Object result = method.invoke(target, args);
                transactionManager.commit(status);
                return result;
            } catch (Throwable ex) {
                // 获取原始异常
                Throwable actualException = ex;
                if (ex instanceof java.lang.reflect.InvocationTargetException) {
                    actualException = ((java.lang.reflect.InvocationTargetException) ex).getTargetException();
                }
                
                if (shouldRollback(transactional, actualException)) {
                    transactionManager.rollback(status);
                } else {
                    transactionManager.commit(status);
                }
                throw actualException;
            }
        }
        
        private boolean shouldRollback(Transactional transactional, Throwable ex) {
            if (transactional.rollbackFor().length == 0) {
                return ex instanceof RuntimeException || ex instanceof Error;
            }
            
            for (Class<? extends Throwable> rollbackType : transactional.rollbackFor()) {
                if (rollbackType.isInstance(ex)) {
                    return true;
                }
            }
            return false;
        }
    }
} 