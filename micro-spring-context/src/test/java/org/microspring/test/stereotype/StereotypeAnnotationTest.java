package org.microspring.test.stereotype;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.core.BeanDefinition;
import static org.junit.Assert.*;

public class StereotypeAnnotationTest {
    
    @Test
    public void testRepositoryAnnotation() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            "org.microspring.test.stereotype");
        
        // 测试是否正确注册了 Repository bean
        assertTrue(context.containsBean("testRepo"));
        
        // 测试 bean 定义是否正确
        BeanDefinition bd = context.getBeanFactory().getBeanDefinition("testRepo");
        assertEquals(TestRepository.class, bd.getBeanClass());
        assertTrue(bd.isSingleton());
        
        // 测试是否可以正确获取 bean
        TestRepository repository = context.getBean("testRepo", TestRepository.class);
        assertNotNull(repository);
        assertEquals("test data", repository.getData());
    }

    @Test
    public void testServiceAnnotation() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            "org.microspring.test.stereotype");
        
        // 测试是否正确注册了 Service bean
        assertTrue(context.containsBean("testService"));
        
        // 测试 bean 定义是否正确
        BeanDefinition bd = context.getBeanFactory().getBeanDefinition("testService");
        assertEquals(TestService.class, bd.getBeanClass());
        assertTrue(bd.isSingleton());
        
        // 测试是否可以正确获取 bean
        TestService service = context.getBean("testService", TestService.class);
        assertNotNull(service);
    }

    @Test
    public void testDefaultBeanNaming() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            "org.microspring.test.stereotype");
        
        // 测试是否使用类名首字母小写作为 bean 名称
        assertTrue(context.containsBean("defaultNameRepository"));
        
        DefaultNameRepository repository = context.getBean(DefaultNameRepository.class);
        assertNotNull(repository);
        assertEquals("default data", repository.getData());
    }
} 