package org.microspring.test.primary;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.core.exception.NoSuchBeanDefinitionException;
import static org.junit.Assert.*;

public class PrimaryAnnotationTest {

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

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testMultipleImplementationsWithoutPrimary() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        // 当有多个实现但没有@Primary时，应该抛出异常
        context.getBean(Fruit.class);
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

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testMultiplePrimaryAnnotationsShouldThrowException() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        // 这个测试用例验证正确的行为：当有多个@Primary bean时应该抛出异常
        try {
            context.getBean(Database.class);
            fail("Should throw NoSuchBeanDefinitionException when multiple primary beans are found");
        } catch (NoSuchBeanDefinitionException e) {
            assertTrue(e.getMessage().contains("Multiple primary beans found"));
            throw e;
        }
    }

    @Test
    public void testPrimaryWithInheritance() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        Shape shape = context.getBean(Shape.class);
        assertNotNull("Should get a shape bean", shape);
        assertEquals("Should get the primary circle bean", "circle", shape.getShape());

        ColoredCircle coloredCircle = context.getBean(ColoredCircle.class);
        assertNotNull("Should get colored circle bean", coloredCircle);
        assertEquals("Should get colored circle", "colored-circle", coloredCircle.getShape());
    }

    @Test
    public void testPrimaryWithQualifier() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.primary");
        
        PrinterUser printerUser = context.getBean(PrinterUser.class);
        assertNotNull("Should get printer user bean", printerUser);
        assertEquals("Default printer should be laser (Primary)", "laser", printerUser.getDefaultPrinterType());
        assertEquals("Qualified printer should be inkjet", "inkjet", printerUser.getInkjetPrinterType());
    }
} 