package org.microspring.test.imports;

import org.junit.Test;
import static org.junit.Assert.*;
import org.microspring.context.annotation.Import;
import org.microspring.context.annotation.Configuration;
import org.microspring.context.annotation.Bean;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.stereotype.Component;

public class ImportAnnotationTest {

    @Configuration
    public static class TestConfig {
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    public static class TestService {
        public String serve() {
            return "served";
        }
    }

    // 测试导入普通配置类
    @Configuration
    public static class ImportConfig1 {
        @Bean
        public ImportedBean1 importedBean1() {
            return new ImportedBean1("test1");
        }
    }

    public static class ImportedBean1 {
        private String name;
        public ImportedBean1(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // 测试导入的配置类
    @Configuration
    public static class ImportConfig2 {
        @Bean
        public ImportedBean2 importedBean2() {
            return new ImportedBean2("test2");
        }
    }

    @Configuration
    @Import({ImportConfig1.class, ImportConfig2.class})
    public static class MainImportConfig {
        @Bean
        public String mainBean() {
            return "main";
        }
    }

    public static class ImportedBean2 {
        private String name;
        public ImportedBean2(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    public void testImportAnnotation() {
        // 创建容器时不指定包扫描路径
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        // 直接注册主配置类
        context.register(MainImportConfig.class);
        
        // 刷新容器
        context.refresh();
        
        // 验证主配置类中的 bean
        String mainBean = context.getBean("mainBean", String.class);
        assertEquals("main", mainBean);
        
        // 验证通过 @Import 导入的配置类中的 bean
        ImportedBean1 importedBean1 = context.getBean("importedBean1", ImportedBean1.class);
        assertEquals("test1", importedBean1.getName());
        
        ImportedBean2 importedBean2 = context.getBean("importedBean2", ImportedBean2.class);
        assertEquals("test2", importedBean2.getName());
    }
} 