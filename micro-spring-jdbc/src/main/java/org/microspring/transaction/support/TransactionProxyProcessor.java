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
        
        // 添加调试日志
        System.out.println("Processing bean: " + beanName + ", class: " + beanClass.getName());
        System.out.println("Has @Transactional: " + beanClass.isAnnotationPresent(Transactional.class));
        
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
            System.out.println("Invoking method: " + method.getName());
            
            Transactional transactional = method.getAnnotation(Transactional.class);
            if (transactional == null) {
                transactional = target.getClass().getAnnotation(Transactional.class);
            }
            
            if (transactional == null) {
                System.out.println("No transaction needed for: " + method.getName());
                return method.invoke(target, args);
            }
            
            // 创建事务定义
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            definition.setPropagationBehavior(transactional.propagation());
            definition.setIsolationLevel(transactional.isolation());
            definition.setReadOnly(transactional.readOnly());
            
            System.out.println("Starting transaction for: " + method.getName());
            TransactionStatus status = transactionManager.getTransaction(definition);
            
            try {
                Object result = method.invoke(target, args);
                System.out.println("Committing transaction for: " + method.getName());
                transactionManager.commit(status);
                return result;
            } catch (Throwable ex) {
                System.out.println("Exception in transaction: " + ex.getMessage());
                // 获取原始异常
                Throwable actualException = ex;
                if (ex instanceof java.lang.reflect.InvocationTargetException) {
                    actualException = ((java.lang.reflect.InvocationTargetException) ex).getTargetException();
                }
                
                if (shouldRollback(transactional, actualException)) {
                    System.out.println("Rolling back transaction for: " + method.getName());
                    transactionManager.rollback(status);
                } else {
                    System.out.println("Committing transaction despite exception for: " + method.getName());
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