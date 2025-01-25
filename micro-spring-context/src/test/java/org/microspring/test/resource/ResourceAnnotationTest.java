package org.microspring.test.resource;

import org.junit.Test;
import org.junit.Before;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import static org.junit.Assert.*;

public class ResourceAnnotationTest {
    
    private AnnotationConfigApplicationContext context;
    
    @Before
    public void setUp() {
        context = new AnnotationConfigApplicationContext("org.microspring.test.resource");
    }
    
    @Test
    public void testResourceAnnotation() {
        // 尝试获取所有相关的bean
        ResourceTestBeans testBean = context.getBean(ResourceTestBeans.class);
        assertNotNull("ResourceTestBeans should not be null", testBean);
        
        TestService directTestService = context.getBean(TestService.class);
        
        // 测试按名称注入
        TestService namedService = testBean.getNamedService();
        assertNotNull(namedService);
        assertEquals("testService", namedService.getName());
        
        // 测试按字段名注入
        TestService testService = testBean.getTestService();
        assertNotNull(testService);
        assertEquals("testService", testService.getName());
        
        // 测试按类型注入
        AnotherService anotherService = testBean.getAnotherService();
        assertNotNull(anotherService);
        assertEquals("anotherService", anotherService.getName());
        
        // 测试集合类型注入
        assertNotNull(testBean.getServiceList());
        assertTrue(testBean.getServiceList().size() >= 2); // 至少包含TestService和CustomNamedService
        
        assertNotNull(testBean.getServiceMap());
        assertTrue(testBean.getServiceMap().size() >= 2);
        assertTrue(testBean.getServiceMap().containsKey("testService"));
        assertTrue(testBean.getServiceMap().containsKey("customNamedService"));
        
        // 测试setter方法注入
        TestService setterService = testBean.getSetterService();
        assertNotNull(setterService);
        assertEquals("testService", setterService.getName());
    }
    
    @Test
    public void testResourceNamePriority() {
        ResourceTestBeans testBean = context.getBean(ResourceTestBeans.class);
        
        // 验证@Resource的name属性优先于字段名
        TestService namedService = testBean.getNamedService();
        TestService testService = testBean.getTestService();
        
        // 应该是同一个实例（因为都是按名称"testService"注入的）
        assertSame(namedService, testService);
    }
    
    @Test
    public void testResourceTypeInjection() {
        ResourceTestBeans testBean = context.getBean(ResourceTestBeans.class);
        
        // 验证当按名称找不到时，会降级为按类型查找
        AnotherService anotherService = testBean.getAnotherService();
        assertNotNull(anotherService);
        
        // 验证是否是单例
        AnotherService anotherInstance = context.getBean(AnotherService.class);
        assertSame(anotherService, anotherInstance);
    }
} 