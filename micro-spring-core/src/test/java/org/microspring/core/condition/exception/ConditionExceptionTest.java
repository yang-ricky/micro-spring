package org.microspring.core.condition.exception;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.annotation.Conditional;
import org.microspring.stereotype.Component;
import org.microspring.core.io.ClassPathBeanDefinitionScanner;
import org.microspring.core.condition.Condition;
import org.microspring.core.condition.ConditionContext;
import static org.junit.Assert.*;

public class ConditionExceptionTest {
    
    public static class InvalidCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            throw new RuntimeException("Condition evaluation failed");
        }
    }
    
    @Component("failureBean")
    @Conditional(InvalidCondition.class)
    public static class FailureBean {}
    
    @Test(expected = RuntimeException.class)
    public void testConditionEvaluationFailure() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
        scanner.scan(this.getClass().getPackage().getName());
    }
    
    @Test(expected = RuntimeException.class)
    public void testNullConditionContext() {
        InvalidCondition condition = new InvalidCondition();
        condition.matches(null);
    }
    
    private boolean checkExceptionMessage(Throwable e) {
        Throwable current = e;
        int maxDepth = 10;  // 设置最大深度
        int depth = 0;
        
        while (current != null && depth < maxDepth) {
            if (current.getMessage() != null && 
                (current.getMessage().contains("Failed to evaluate condition") ||
                 current.getMessage().contains("Condition evaluation failed"))) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
    
    @Test
    public void testExceptionMessage() {
        try {
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue("Expected exception message not found", checkExceptionMessage(e));
        }
    }
    
    @Test
    public void testMultipleFailures() {
        @Component("multiFailureBean")
        @Conditional({InvalidCondition.class, InvalidCondition.class})
        class MultiFailureBean {}
        
        try {
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            // 检查异常链
            Throwable current = e;
            boolean foundExpectedMessage = false;
            while (current != null) {
                if (current.getMessage() != null && 
                    (current.getMessage().contains("Failed to evaluate condition") ||
                     current.getMessage().contains("Condition evaluation failed"))) {
                    foundExpectedMessage = true;
                    break;
                }
                current = current.getCause();
            }
            assertTrue("Expected exception message not found", foundExpectedMessage);
        }
    }
} 