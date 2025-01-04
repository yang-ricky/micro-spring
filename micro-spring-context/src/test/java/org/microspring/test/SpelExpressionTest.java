package org.microspring.test;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.stereotype.Component;

import static org.junit.Assert.assertEquals;

public class SpelExpressionTest {

    @Component
    public static class BeanB {
        private double price = 100.0;
        public double getPrice() { return price; }
    }

    @Component 
    public static class BeanA {
        @Value("#{beanB.price * 1.1}")
        private double calculatedPrice;
        public double getCalculatedPrice() { return calculatedPrice; }
    }

    @Test
    public void testSpelExpression() {
        // 初始化容器,指定要扫描的包
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test");
        
        // 验证SpEL表达式计算结果
        BeanA beanA = context.getBean(BeanA.class);
        assertEquals(110.0, beanA.getCalculatedPrice(), 0.001);
    }
} 