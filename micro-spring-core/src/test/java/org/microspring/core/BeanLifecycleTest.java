package org.microspring.core;

import org.junit.Test;
import org.microspring.core.aware.BeanNameAware;
import org.microspring.core.aware.BeanFactoryAware;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class BeanLifecycleTest {
    
    public static class DemoBean implements BeanNameAware {
        private String beanName;
        
        public void initMe() {
            System.out.println("Initializing " + beanName);
        }
        
        public void cleanup() {
            System.out.println("Cleaning up " + beanName);
        }
        
        @Override
        public void setBeanName(String name) {
            this.beanName = name;
        }
    }
    
    public static class FactoryAwareBean implements BeanFactoryAware {
        private BeanFactory beanFactory;
        
        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }
        
        public BeanFactory getBeanFactory() {
            return beanFactory;
        }
    }
    
    public static class OrderedBean implements BeanNameAware {
        private static List<String> initializationOrder = new ArrayList<>();
        private String beanName;
        
        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            initializationOrder.add("setBeanName:" + name);
        }
        
        public void initMe() {
            initializationOrder.add("init:" + beanName);
        }
        
        public void cleanup() {
            initializationOrder.add("cleanup:" + beanName);
        }
        
        public static List<String> getInitializationOrder() {
            return initializationOrder;
        }
        
        public static void clearInitializationOrder() {
            initializationOrder.clear();
        }
    }
    
    public static class MultiAwareBean implements BeanNameAware, BeanFactoryAware {
        private static List<String> callbackOrder = new ArrayList<>();
        
        @Override
        public void setBeanName(String name) {
            callbackOrder.add("setBeanName");
        }
        
        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            callbackOrder.add("setBeanFactory");
        }
        
        public void initMe() {
            callbackOrder.add("init");
        }
        
        public static List<String> getCallbackOrder() {
            return callbackOrder;
        }
        
        public static void clearCallbackOrder() {
            callbackOrder.clear();
        }
    }
    
    @Test
    public void testBeanNameAware() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        beanFactory.registerBeanDefinition("demoBean", bd);
        
        DemoBean bean = (DemoBean) beanFactory.getBean("demoBean");
        assertEquals("demoBean", bean.beanName); // 验证 setBeanName 被正确调用
    }
    
    @Test
    public void testBeanFactoryAware() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(FactoryAwareBean.class);
        beanFactory.registerBeanDefinition("factoryAwareBean", bd);
        
        FactoryAwareBean bean = (FactoryAwareBean) beanFactory.getBean("factoryAwareBean");
        assertSame(beanFactory, bean.getBeanFactory()); // 验证 setBeanFactory 被正确调用
    }
    
    @Test
    public void testInitMethodInvocation() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        bd.setInitMethodName("initMe");
        beanFactory.registerBeanDefinition("demoBean", bd);
        
        // 使用 ByteArrayOutputStream 捕获输出
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        beanFactory.getBean("demoBean");
        
        assertTrue(output.toString().contains("Initializing demoBean")); // 验证初始化方法被调用
    }
    
    @Test
    public void testDestroyMethodInvocation() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        bd.setDestroyMethodName("cleanup");
        beanFactory.registerBeanDefinition("demoBean", bd);
        
        beanFactory.getBean("demoBean"); // 创建bean
        
        // 使用 ByteArrayOutputStream 捕获输出
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        beanFactory.close();
        
        assertTrue(output.toString().contains("Cleaning up demoBean")); // 验证销毁方法被调用
    }
    
    @Test
    public void testCompleteLifecycle() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        bd.setInitMethodName("initMe");
        bd.setDestroyMethodName("cleanup");
        beanFactory.registerBeanDefinition("demoBean", bd);
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        DemoBean bean = (DemoBean) beanFactory.getBean("demoBean");
        assertEquals("demoBean", bean.beanName); // 验证 BeanNameAware
        assertTrue(output.toString().contains("Initializing demoBean")); // 验证初始化
        
        beanFactory.close();
        assertTrue(output.toString().contains("Cleaning up demoBean")); // 验证销毁
    }
    
    @Test
    public void testLazyInitBeanLifecycle() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        bd.setInitMethodName("initMe");
        bd.setDestroyMethodName("cleanup");
        bd.setLazyInit(true);
        beanFactory.registerBeanDefinition("lazyBean", bd);
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        // 验证容器启动时lazy bean未被初始化
        assertFalse(output.toString().contains("Initializing lazyBean"));
        
        // 第一次获取时才初始化
        DemoBean bean = (DemoBean) beanFactory.getBean("lazyBean");
        assertTrue(output.toString().contains("Initializing lazyBean"));
        
        beanFactory.close();
        assertTrue(output.toString().contains("Cleaning up lazyBean"));
    }
    
    @Test
    public void testAwareCallbackOrder() {
        MultiAwareBean.clearCallbackOrder();
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(MultiAwareBean.class);
        bd.setInitMethodName("initMe");
        beanFactory.registerBeanDefinition("multiAwareBean", bd);
        
        beanFactory.getBean("multiAwareBean");
        
        List<String> order = MultiAwareBean.getCallbackOrder();
        assertEquals("setBeanName", order.get(0));  // BeanNameAware应该最先调用
        assertEquals("setBeanFactory", order.get(1));  // 然后是BeanFactoryAware
        assertEquals("init", order.get(2));  // 最后是init方法
    }
    
    @Test
    public void testPrototypeBeanLifecycle() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        bd.setScope("prototype");
        bd.setInitMethodName("initMe");
        bd.setDestroyMethodName("cleanup");
        beanFactory.registerBeanDefinition("prototypeBean", bd);
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        // 每次获取都会创建新实例并初始化
        DemoBean bean1 = (DemoBean) beanFactory.getBean("prototypeBean");
        DemoBean bean2 = (DemoBean) beanFactory.getBean("prototypeBean");
        assertNotSame(bean1, bean2);
        
        String log = output.toString();
        assertEquals(2, log.split("Initializing prototypeBean").length - 1);
        
        // 原型Bean的销毁方法不会被容器调用
        beanFactory.close();
        assertFalse(log.contains("Cleaning up prototypeBean"));
    }
} 