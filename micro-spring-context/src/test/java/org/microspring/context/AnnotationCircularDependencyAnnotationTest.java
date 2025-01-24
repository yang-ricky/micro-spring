package org.microspring.context;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.test.circular.*;
import static org.junit.Assert.*;

public class AnnotationCircularDependencyAnnotationTest {
    
    @Test
    public void testFieldBasedCircularDependency() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.circular");
        
        CircularFieldA a = context.getBean(CircularFieldA.class);
        CircularFieldB b = context.getBean(CircularFieldB.class);
        CircularFieldC c = context.getBean(CircularFieldC.class);
        
        assertNotNull("CircularFieldA should not be null", a);
        assertNotNull("CircularFieldB should not be null", b);
        assertNotNull("CircularFieldC should not be null", c);
        
        // 验证依赖注入是否成功
        assertNotNull("B reference in A should not be null", a.getB());
        assertNotNull("C reference in B should not be null", b.getC());
        assertNotNull("A reference in C should not be null", c.getA());
        
        // 验证是否是相同的实例（单例）
        assertSame("Should get the same instance of CircularFieldB", b, a.getB());
        assertSame("Should get the same instance of CircularFieldC", c, b.getC());
        assertSame("Should get the same instance of CircularFieldA", a, c.getA());
        
        // 验证三方循环引用是否正确
        assertSame("Circular reference A->B->C->A should lead back to the same A", 
                  a, a.getB().getC().getA());
        assertSame("Circular reference B->C->A->B should lead back to the same B", 
                  b, b.getC().getA().getB());
        assertSame("Circular reference C->A->B->C should lead back to the same C", 
                  c, c.getA().getB().getC());
    }

    @Test
    public void testMethodBasedCircularDependency() {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.test.circular");
        
        CircularMethodA a = context.getBean(CircularMethodA.class);
        CircularMethodB b = context.getBean(CircularMethodB.class);
        CircularMethodC c = context.getBean(CircularMethodC.class);
        
        assertNotNull("CircularMethodA should not be null", a);
        assertNotNull("CircularMethodB should not be null", b);
        assertNotNull("CircularMethodC should not be null", c);
        
        // 验证依赖注入是否成功
        assertNotNull("B reference in A should not be null", a.getB());
        assertNotNull("C reference in B should not be null", b.getC());
        assertNotNull("A reference in C should not be null", c.getA());
        
        // 验证是否是相同的实例（单例）
        assertSame("Should get the same instance of CircularMethodB", b, a.getB());
        assertSame("Should get the same instance of CircularMethodC", c, b.getC());
        assertSame("Should get the same instance of CircularMethodA", a, c.getA());
        
        // 验证三方循环引用是否正确
        assertSame("Circular reference A->B->C->A should lead back to the same A", 
                  a, a.getB().getC().getA());
        assertSame("Circular reference B->C->A->B should lead back to the same B", 
                  b, b.getC().getA().getB());
        assertSame("Circular reference C->A->B->C should lead back to the same C", 
                  c, c.getA().getB().getC());
    }
} 