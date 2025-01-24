package org.microspring.context;

import org.junit.Test;
import org.microspring.context.support.ClassPathXmlApplicationContext;
import org.microspring.test.xml.*;
import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;

public class ClassPathXmlApplicationContextTest {
    
    @Test
    public void testBasicBeanCreation() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        assertTrue(context.containsBean("testBean"));
        TestBean bean = context.getBean("testBean", TestBean.class);
        assertNotNull(bean);
        assertEquals("test", bean.getName());
        
        assertNotNull(context.getApplicationName());
        assertTrue(context.getStartupDate() > 0);
    }
    
    @Test
    public void testPropertyInjection() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        TestBean bean = context.getBean("propertyBean", TestBean.class);
        assertNotNull(bean);
        assertEquals("property-injected", bean.getName());
    }
    
    @Test
    public void testConstructorInjection() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        ConstructorBean bean = context.getBean("constructorBean", ConstructorBean.class);
        assertNotNull(bean);
        assertEquals("test", bean.getName());
    }
    
    @Test
    public void testDependencyInjection() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        DependentBean bean = context.getBean("dependentBean", DependentBean.class);
        assertNotNull(bean);
        assertNotNull(bean.getTestBean());
        assertEquals("test", bean.getTestBean().getName());
    }
    
    @Test
    public void testBeanScopes() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        // 测试单例作用域
        TestBean singleton1 = context.getBean("singletonBean", TestBean.class);
        TestBean singleton2 = context.getBean("singletonBean", TestBean.class);
        assertSame("Singleton beans should be the same instance", singleton1, singleton2);
        
        // 测试原型作用域
        TestBean prototype1 = context.getBean("prototypeBean", TestBean.class);
        TestBean prototype2 = context.getBean("prototypeBean", TestBean.class);
        assertNotSame("Prototype beans should be different instances", prototype1, prototype2);
    }
    
    @Test
    public void testBeanLifecycle() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        LifecycleBean bean = context.getBean("lifecycleBean", LifecycleBean.class);
        assertTrue("Bean should be initialized", bean.isInitialized());
        assertFalse("Bean should not be destroyed yet", bean.isDestroyed());
        
        context.close();
        assertTrue("Bean should be destroyed", bean.isDestroyed());
    }
    
    @Test
    public void testCollectionInjection() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        CollectionBean bean = context.getBean("collectionBean", CollectionBean.class);
        assertNotNull(bean);
        
        // 测试List注入
        List<String> list = bean.getList();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("value1", list.get(0));
        assertEquals("value2", list.get(1));
        
        // 测试Map注入
        Map<String, Object> map = bean.getMap();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }
    
    @Test
    public void testCircularDependency() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        CircularA beanA = context.getBean("circularA", CircularA.class);
        CircularB beanB = context.getBean("circularB", CircularB.class);
        
        assertNotNull(beanA);
        assertNotNull(beanB);
        assertSame(beanB, beanA.getCircularB());
        assertSame(beanA, beanB.getCircularA());
    }
    
    //@Test ❌
    public void testNestedBean() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        OuterBean outerBean = context.getBean("outerBean", OuterBean.class);
        assertNotNull(outerBean);
        assertNotNull(outerBean.getInnerBean());
        assertEquals("inner-value", outerBean.getInnerBean().getName());
    }
    
    //@Test ❌
    public void testFactoryBean() {
        ApplicationContext context = new ClassPathXmlApplicationContext("context-test.xml");
        
        TestBean bean = context.getBean("factoryBean", TestBean.class);
        assertNotNull(bean);
        assertEquals("factory-created", bean.getName());
    }
}