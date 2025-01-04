package org.microspring.core.condition;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.annotation.Conditional;
import org.microspring.stereotype.Component;
import org.microspring.core.io.ClassPathBeanDefinitionScanner;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;

public class ConditionalTest {
    
    private String originalOs;
    
    @Before
    public void setUp() {
        originalOs = System.getProperty("os.name");
    }
    
    @After
    public void tearDown() {
        if (originalOs != null) {
            System.setProperty("os.name", originalOs);
        } else {
            System.clearProperty("os.name");
        }
    }
    
    // 测试用的条件类
    public static class WindowsCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            String osName = context.getEnvironment("os.name");
            return osName != null && osName.toLowerCase().contains("windows");
        }
    }
    
    public static class LinuxCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            String osName = context.getEnvironment("os.name");
            return osName != null && osName.toLowerCase().contains("linux");
        }
    }
    
    // 测试用的Bean类
    @Component("windowsOnlyBean")
    @Conditional(WindowsCondition.class)
    public static class WindowsOnlyBean {
        public String getMessage() {
            return "This is Windows only bean";
        }
    }
    
    @Component("linuxOnlyBean")
    @Conditional(LinuxCondition.class)
    public static class LinuxOnlyBean {
        public String getMessage() {
            return "This is Linux only bean";
        }
    }
    
    @Test
    public void testWindowsCondition() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 10");
            
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            
            assertTrue("WindowsOnlyBean should be registered in Windows environment", 
                beanFactory.containsBean("windowsOnlyBean"));
            assertFalse("LinuxOnlyBean should not be registered in Windows environment", 
                beanFactory.containsBean("linuxOnlyBean"));
            
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }
    
    @Test
    public void testLinuxCondition() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            
            assertTrue("LinuxOnlyBean should be registered in Linux environment", 
                beanFactory.containsBean("linuxOnlyBean"));
            assertFalse("WindowsOnlyBean should not be registered in Linux environment", 
                beanFactory.containsBean("windowsOnlyBean"));
            
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }
    
    @Test
    public void testBeanInstantiation() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 10");
            
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            
            WindowsOnlyBean bean = (WindowsOnlyBean) beanFactory.getBean("windowsOnlyBean");
            assertNotNull("Bean should be created", bean);
            assertEquals("This is Windows only bean", bean.getMessage());
            
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }
    
    @Test
    public void testMultipleConditions() {
        @Component("multiConditionBean")
        @Conditional({WindowsCondition.class, LinuxCondition.class})
        class MultiConditionBean {}
        
        String originalOs = System.getProperty("os.name");
        try {
            // 设置为Windows环境
            System.setProperty("os.name", "Windows 10");
            
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            
            // 由于需要同时满足Windows和Linux条件，这个Bean不应该被创建
            assertFalse("MultiConditionBean should not be registered as it requires both Windows and Linux", 
                beanFactory.containsBean("multiConditionBean"));
            
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }
    
    @Test
    public void testMacCondition() {
        // 测试其他操作系统环境
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
            
            DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
            scanner.scan(this.getClass().getPackage().getName());
            
            // Mac环境下，两个Bean都不应该被创建
            assertFalse("WindowsOnlyBean should not be registered in Mac environment", 
                beanFactory.containsBean("windowsOnlyBean"));
            assertFalse("LinuxOnlyBean should not be registered in Mac environment", 
                beanFactory.containsBean("linuxOnlyBean"));
            
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }
    
    @Test
    public void testConditionContext() {
        // 测试ConditionContext的功能
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        DefaultConditionContext context = new DefaultConditionContext(beanFactory);
        
        String originalKey = System.getProperty("test.key");
        try {
            // 测试环境变量获取
            System.setProperty("test.key", "test.value");
            assertEquals("test.value", context.getEnvironment("test.key"));
            
            // 测试BeanFactory访问
            assertNotNull("BeanFactory should not be null", context.getBeanFactory());
            assertSame("Should return the same BeanFactory", beanFactory, context.getBeanFactory());
        } finally {
            if (originalKey != null) {
                System.setProperty("test.key", originalKey);
            } else {
                System.clearProperty("test.key");
            }
        }
    }
}