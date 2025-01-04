package org.microspring.context;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.core.aware.BeanNameAware;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class BeanLifecycleContextTest {
    
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

    @Test
    public void testInitializationOrder() {
        OrderedBean.clearInitializationOrder();
        
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.microspring.test");
        DefaultBeanFactory beanFactory = (DefaultBeanFactory) context.getBeanFactory();
        
        // 注册两个OrderedBean
        DefaultBeanDefinition bd1 = new DefaultBeanDefinition(OrderedBean.class);
        bd1.setInitMethodName("initMe");
        beanFactory.registerBeanDefinition("orderedBean1", bd1);
        
        DefaultBeanDefinition bd2 = new DefaultBeanDefinition(OrderedBean.class);
        bd2.setInitMethodName("initMe");
        beanFactory.registerBeanDefinition("orderedBean2", bd2);
        
        // 刷新容器，触发初始化
        context.refresh();
        
        List<String> order = OrderedBean.getInitializationOrder();
        
        // 验证顺序：先设置beanName，再调用init方法
        assertTrue(order.indexOf("setBeanName:orderedBean1") < order.indexOf("init:orderedBean1"));
        assertTrue(order.indexOf("setBeanName:orderedBean2") < order.indexOf("init:orderedBean2"));
    }
    
    @Test
    public void testDestroyOrder() {
        OrderedBean.clearInitializationOrder();
        
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.microspring.test");
        DefaultBeanFactory beanFactory = (DefaultBeanFactory) context.getBeanFactory();
        
        // 注册两个带销毁方法的Bean
        DefaultBeanDefinition bd1 = new DefaultBeanDefinition(OrderedBean.class);
        bd1.setDestroyMethodName("cleanup");
        beanFactory.registerBeanDefinition("orderedBean1", bd1);
        
        DefaultBeanDefinition bd2 = new DefaultBeanDefinition(OrderedBean.class);
        bd2.setDestroyMethodName("cleanup");
        beanFactory.registerBeanDefinition("orderedBean2", bd2);
        
        // 获取Bean实例
        beanFactory.getBean("orderedBean1");
        beanFactory.getBean("orderedBean2");
        
        // 关闭容器
        context.close();
        
        List<String> order = OrderedBean.getInitializationOrder();
        
        // 验证销毁方法被调用
        assertTrue(order.contains("cleanup:orderedBean1"));
        assertTrue(order.contains("cleanup:orderedBean2"));
    }
    
    @Test
    public void testLifecycleInApplicationContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.microspring.test");
        DefaultBeanFactory beanFactory = (DefaultBeanFactory) context.getBeanFactory();
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        // 注册一个完整生命周期的Bean
        DefaultBeanDefinition bd = new DefaultBeanDefinition(OrderedBean.class);
        bd.setInitMethodName("initMe");
        bd.setDestroyMethodName("cleanup");
        beanFactory.registerBeanDefinition("lifecycleBean", bd);
        
        // 刷新容器，触发初始化
        context.refresh();
        
        OrderedBean bean = context.getBean("lifecycleBean", OrderedBean.class);
        assertNotNull(bean);
        
        List<String> order = OrderedBean.getInitializationOrder();
        assertTrue(order.contains("setBeanName:lifecycleBean")); // 验证BeanNameAware
        assertTrue(order.contains("init:lifecycleBean")); // 验证初始化方法
        
        // 关闭容器
        context.close();
        assertTrue(order.contains("cleanup:lifecycleBean")); // 验证销毁方法
    }
    
    @Test
    public void testContextRefreshOrder() {
        OrderedBean.clearInitializationOrder();
        
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.microspring.test");
        DefaultBeanFactory beanFactory = (DefaultBeanFactory) context.getBeanFactory();
        
        // 注册多个Bean测试初始化顺序
        DefaultBeanDefinition bd1 = new DefaultBeanDefinition(OrderedBean.class);
        bd1.setInitMethodName("initMe");
        beanFactory.registerBeanDefinition("bean1", bd1);
        
        DefaultBeanDefinition bd2 = new DefaultBeanDefinition(OrderedBean.class);
        bd2.setInitMethodName("initMe");
        beanFactory.registerBeanDefinition("bean2", bd2);
        
        // 刷新容器
        context.refresh();
        
        List<String> order = OrderedBean.getInitializationOrder();
        
        // 验证所有Bean都经过了完整的生命周期
        assertTrue(order.contains("setBeanName:bean1"));
        assertTrue(order.contains("init:bean1"));
        assertTrue(order.contains("setBeanName:bean2"));
        assertTrue(order.contains("init:bean2"));
        
        // 验证初始化顺序：每个Bean的Aware回调在其init方法之前
        assertTrue(order.indexOf("setBeanName:bean1") < order.indexOf("init:bean1"));
        assertTrue(order.indexOf("setBeanName:bean2") < order.indexOf("init:bean2"));
    }
} 