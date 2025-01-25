package org.microspring.test.imports;

import org.junit.Test;
import static org.junit.Assert.*;

import org.microspring.context.annotation.Import;
import org.microspring.context.annotation.Configuration;
import org.microspring.context.annotation.ImportSelector;
import org.microspring.context.annotation.Bean;
import org.microspring.context.support.AnnotationConfigApplicationContext;

public class ImportSelectorTest {

    // 测试用的bean类
    public static class ServiceA {
        public String getName() {
            return "ServiceA";
        }
    }

    public static class ServiceB {
        public String getName() {
            return "ServiceB";
        }
    }

    // 配置类A
    @Configuration
    public static class ConfigA {
        @Bean
        public ServiceA serviceA() {
            return new ServiceA();
        }
    }

    // 配置类B
    @Configuration
    public static class ConfigB {
        @Bean
        public ServiceB serviceB() {
            return new ServiceB();
        }
    }

    // ImportSelector实现
    public static class MyImportSelector implements ImportSelector {
        @Override
        public String[] selectImports(Class<?> importingClass) {
            // 根据条件动态选择要导入的类
            return new String[] {
                ConfigA.class.getName(),
                ConfigB.class.getName()
            };
        }
    }

    // 使用ImportSelector的配置类
    @Configuration
    @Import(MyImportSelector.class)
    public static class MainConfig {
    }

    @Test
    public void testImportSelector() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(MainConfig.class);
        context.refresh();

        // 验证通过ImportSelector导入的配置类中的bean是否存在
        ServiceA serviceA = context.getBean(ServiceA.class);
        ServiceB serviceB = context.getBean(ServiceB.class);

        assertNotNull("ServiceA should not be null", serviceA);
        assertNotNull("ServiceB should not be null", serviceB);
        assertEquals("ServiceA", serviceA.getName());
        assertEquals("ServiceB", serviceB.getName());
    }

    // 条件导入的测试
    public static class ConditionalImportSelector implements ImportSelector {
        @Override
        public String[] selectImports(Class<?> importingClass) {
            // 根据某些条件决定导入哪些类
            if (importingClass.isAnnotationPresent(Configuration.class)) {
                return new String[] { ConfigA.class.getName() };
            }
            return new String[0];
        }
    }

    @Configuration
    @Import(ConditionalImportSelector.class)
    public static class ConditionalConfig {
    }

    @Test
    public void testConditionalImportSelector() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ConditionalConfig.class);
        context.refresh();

        // 验证只有ConfigA被导入
        ServiceA serviceA = context.getBean(ServiceA.class);
        assertNotNull("ServiceA should not be null", serviceA);

        // ServiceB不应该存在
        try {
            context.getBean(ServiceB.class);
            fail("ServiceB should not exist");
        } catch (Exception e) {
            // 预期的异常
        }
    }
} 