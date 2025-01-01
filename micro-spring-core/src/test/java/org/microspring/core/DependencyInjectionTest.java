package org.microspring.core;

import org.junit.Test;
import org.microspring.core.aware.BeanNameAware;
import static org.junit.Assert.*;

public class DependencyInjectionTest {
    
    public static class Car implements BeanNameAware {
        private final Engine engine;
        private String brand;
        private String beanName;
        
        public Car(Engine engine) {
            this.engine = engine;
        }
        
        public void setBrand(String brand) {
            this.brand = brand;
        }
        
        @Override
        public void setBeanName(String name) {
            this.beanName = name;
        }
        
        public Engine getEngine() { return engine; }
        public String getBrand() { return brand; }
        public String getBeanName() { return beanName; }
    }
    
    public static class Engine {
        private String type;
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getType() { return type; }
    }
    
    @Test
    public void testDependencyInjection() {
        DefaultBeanFactory factory = new DefaultBeanFactory();
        factory.loadBeanDefinitions("di-test.xml");
        
        Car car = factory.getBean("car", Car.class);
        
        assertNotNull(car);
        assertNotNull(car.getEngine());
        assertEquals("V8", car.getEngine().getType());
        assertEquals("Tesla", car.getBrand());
        assertEquals("car", car.getBeanName());
    }
} 