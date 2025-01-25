package org.microspring.test.imports;

import org.junit.Test;
import static org.junit.Assert.*;

import org.microspring.context.annotation.Import;
import org.microspring.context.annotation.Configuration;
import org.microspring.context.annotation.ImportBeanDefinitionRegistrar;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.core.DefaultBeanFactory;

public class ImportBeanDefinitionRegistrarTest {

    // 测试用的bean类
    public static class DynamicBean {
        private String name;
        private int value;

        public DynamicBean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    // ImportBeanDefinitionRegistrar的实现类
    public static class DynamicBeanRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(Class<?> importingClass, DefaultBeanFactory beanFactory) {
            // 动态注册两个bean
            DynamicBean bean1 = new DynamicBean("bean1", 1);
            DynamicBean bean2 = new DynamicBean("bean2", 2);
            
            beanFactory.registerSingleton("dynamicBean1", bean1);
            beanFactory.registerSingleton("dynamicBean2", bean2);
        }
    }

    // 导入Registrar的配置类
    @Configuration
    @Import(DynamicBeanRegistrar.class)
    public static class RegistrarConfig {
    }

    @Test
    public void testDynamicBeanRegistration() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(RegistrarConfig.class);
        context.refresh();

        // 验证动态注册的bean
        DynamicBean bean1 = context.getBean("dynamicBean1", DynamicBean.class);
        assertNotNull("dynamicBean1 should not be null", bean1);
        assertEquals("bean1", bean1.getName());
        assertEquals(1, bean1.getValue());

        DynamicBean bean2 = context.getBean("dynamicBean2", DynamicBean.class);
        assertNotNull("dynamicBean2 should not be null", bean2);
        assertEquals("bean2", bean2.getName());
        assertEquals(2, bean2.getValue());
    }

    // 测试多个Registrar
    public static class AnotherDynamicBeanRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(Class<?> importingClass, DefaultBeanFactory beanFactory) {
            DynamicBean bean3 = new DynamicBean("bean3", 3);
            beanFactory.registerSingleton("dynamicBean3", bean3);
        }
    }

    @Configuration
    @Import({DynamicBeanRegistrar.class, AnotherDynamicBeanRegistrar.class})
    public static class MultiRegistrarConfig {
    }

    @Test
    public void testMultipleRegistrars() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(MultiRegistrarConfig.class);
        context.refresh();

        // 验证所有动态注册的bean
        DynamicBean bean1 = context.getBean("dynamicBean1", DynamicBean.class);
        DynamicBean bean2 = context.getBean("dynamicBean2", DynamicBean.class);
        DynamicBean bean3 = context.getBean("dynamicBean3", DynamicBean.class);

        assertNotNull("dynamicBean1 should not be null", bean1);
        assertNotNull("dynamicBean2 should not be null", bean2);
        assertNotNull("dynamicBean3 should not be null", bean3);

        assertEquals("bean3", bean3.getName());
        assertEquals(3, bean3.getValue());
    }
} 