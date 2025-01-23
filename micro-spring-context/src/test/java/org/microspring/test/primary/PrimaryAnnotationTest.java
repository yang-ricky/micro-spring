package org.microspring.test.primary;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.context.annotation.Primary;
import org.microspring.stereotype.Component;
import org.microspring.core.exception.NoSuchBeanDefinitionException;
import static org.junit.Assert.*;

public class PrimaryAnnotationTest {

    interface Animal {
        String getName();
    }

    @Component
    @Primary
    static class Dog implements Animal {
        @Override
        public String getName() {
            return "dog";
        }
    }

    @Component
    static class Cat implements Animal {
        @Override
        public String getName() {
            return "cat";
        }
    }

    @Component
    static class Bird implements Animal {
        @Override
        public String getName() {
            return "bird";
        }
    }

    @Test
    public void testPrimaryAnnotation() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        // 测试通过类型获取bean时，应该返回@Primary标注的Dog
        Animal animal = context.getBean(Animal.class);
        assertNotNull("Should get a bean", animal);
        assertEquals("Should get the primary bean (Dog)", "dog", animal.getName());
        
        // 但仍然可以通过具体类型获取Cat
        Cat cat = context.getBean(Cat.class);
        assertNotNull("Should get cat bean", cat);
        assertEquals("Should get cat", "cat", cat.getName());
    }

    @Test
    public void testGetBeanByName() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        // 通过名称获取时，@Primary不应该影响结果
        Animal cat = context.getBean("cat", Animal.class);
        assertNotNull("Should get cat bean by name", cat);
        assertEquals("Should get cat when requesting by name", "cat", cat.getName());
        
        Animal dog = context.getBean("dog", Animal.class);
        assertNotNull("Should get dog bean by name", dog);
        assertEquals("Should get dog when requesting by name", "dog", dog.getName());
    }

    // 测试没有@Primary注解时的多个实现
    interface Fruit {
        String getName();
    }

    @Component
    static class Apple implements Fruit {
        @Override
        public String getName() {
            return "apple";
        }
    }

    @Component
    static class Orange implements Fruit {
        @Override
        public String getName() {
            return "orange";
        }
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testMultipleImplementationsWithoutPrimary() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        // 当有多个实现但没有@Primary时，应该抛出异常
        context.getBean(Fruit.class);
    }

    // 测试单个实现时不需要@Primary
    interface Vehicle {
        String getType();
    }

    @Component
    static class Car implements Vehicle {
        @Override
        public String getType() {
            return "car";
        }
    }

    @Test
    public void testSingleImplementation() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        // 只有一个实现时，不需要@Primary也能正确获取
        Vehicle vehicle = context.getBean(Vehicle.class);
        assertNotNull("Should get vehicle bean", vehicle);
        assertEquals("Should get car", "car", vehicle.getType());
    }
} 