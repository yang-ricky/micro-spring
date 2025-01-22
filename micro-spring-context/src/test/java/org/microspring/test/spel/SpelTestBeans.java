package org.microspring.test.spel;

import org.microspring.beans.factory.annotation.Value;
import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;

/**
 * 包含所有SpEL测试用的Bean类
 */
public class SpelTestBeans {
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
} 