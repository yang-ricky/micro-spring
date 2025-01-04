package org.microspring.test;

import org.junit.Test;
import org.microspring.beans.factory.annotation.Value;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class SpelExpressionTest {

    @Component
    public static class BeanB {
        private double price = 100.0;
        private String name = "testBean";
        private int quantity = 5;
        
        public double getPrice() { return price; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
    }

    @Component 
    public static class BeanA {
        @Value("#{beanB.price * 1.1}")
        private double calculatedPrice;
        
        @Value("#{beanB.quantity + 10}")
        private int calculatedQuantity;
        
        @Value("#{beanB.price - 20.0}")
        private double discountedPrice;
        
        @Value("#{beanB.price / 2}")
        private double halfPrice;
        
        @Value("#{beanB.name}")
        private String beanName;

        public double getCalculatedPrice() { return calculatedPrice; }
        public int getCalculatedQuantity() { return calculatedQuantity; }
        public double getDiscountedPrice() { return discountedPrice; }
        public double getHalfPrice() { return halfPrice; }
        public String getBeanName() { return beanName; }
    }

    @Component
    public static class ErrorBean {
        @Value("#{nonExistentBean.price * 1.1}")
        private Double invalidBeanRef;
        
        @Value("#{beanB.nonExistentProperty + 100}")
        private Double invalidProperty;
        
        @Value("#{beanB.price * * 1.1}")
        private Double invalidExpression;
        
        public Double getInvalidBeanRef() { return invalidBeanRef; }
        public Double getInvalidProperty() { return invalidProperty; }
        public Double getInvalidExpression() { return invalidExpression; }
    }

    @Component
    public static class NullValueBean {
        @Value("#{null}")
        private String nullValue;
        
        public String getNullValue() { return nullValue; }
    }

    @Component
    public static class NestedChild {
        private double price = 100.0;
        private String name = "child";
        
        public double getPrice() { return price; }
        public String getName() { return name; }
    }

    @Component
    public static class NestedParent {
        private NestedChild child;
        
        @Autowired
        public void setChild(NestedChild child) {
            this.child = child;
        }
        
        public NestedChild getChild() { return child; }
    }

    @Component
    public static class BeanC {
        @Value("#{nestedParent.child.price * 2}")
        private Double nestedPrice;
        
        @Value("#{nestedParent.child.name + '_nested'}")
        private String nestedName;
        
        public Double getNestedPrice() { return nestedPrice; }
        public String getNestedName() { return nestedName; }
    }

    @Test
    public void testBasicSpelExpression() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test");
        
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
            new AnnotationConfigApplicationContext("org.microspring.test");
        
        ErrorBean errorBean = context.getBean(ErrorBean.class);
        
        // 验证错误情况下返回null
        assertNull("Invalid bean reference should return null", errorBean.getInvalidBeanRef());
        assertNull("Invalid property should return null", errorBean.getInvalidProperty());
        assertNull("Invalid expression should return null", errorBean.getInvalidExpression());
    }

    @Test
    public void testNullSafety() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test");
        
        NullValueBean nullBean = context.getBean(NullValueBean.class);
        assertNull("Null value should be handled", nullBean.getNullValue());
    }

    @Test
    public void testNestedPropertyAccess() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test");
        
        BeanC beanC = context.getBean(BeanC.class);
        
        assertEquals(200.0, beanC.getNestedPrice(), 0.001);
        assertEquals("child_nested", beanC.getNestedName());
    }
} 