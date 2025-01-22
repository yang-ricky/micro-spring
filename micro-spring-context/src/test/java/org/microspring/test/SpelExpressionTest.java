package org.microspring.test;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.test.spel.SpelTestBeans.*;
import static org.junit.Assert.*;

public class SpelExpressionTest {
    
    @Test
    public void testBasicSpelExpression() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.spel");
        
        BeanA beanA = context.getBean(BeanA.class);
        
        assertEquals(110.0, beanA.getCalculatedPrice(), 0.001);  // 乘法
        assertEquals(15, beanA.getCalculatedQuantity());         // 加法
        assertEquals(80.0, beanA.getDiscountedPrice(), 0.001);  // 减法
        assertEquals(50.0, beanA.getHalfPrice(), 0.001);        // 除法
        assertEquals("testBean", beanA.getBeanName());          // 属性访问
    }

    @Test
    public void testErrorHandling() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.spel");
        
        ErrorBean errorBean = context.getBean(ErrorBean.class);
        
        // 验证错误情况下返回null
        assertNull("Invalid bean reference should return null", errorBean.getInvalidBeanRef());
        assertNull("Invalid property should return null", errorBean.getInvalidProperty());
        assertNull("Invalid expression should return null", errorBean.getInvalidExpression());
    }

    @Test
    public void testNullSafety() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.spel");
        
        NullValueBean nullBean = context.getBean(NullValueBean.class);
        assertNull("Null value should be handled", nullBean.getNullValue());
    }

    @Test
    public void testNestedPropertyAccess() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.spel");
        
        BeanC beanC = context.getBean(BeanC.class);
        
        assertEquals(200.0, beanC.getNestedPrice(), 0.001);
        assertEquals("child_nested", beanC.getNestedName());
    }
} 