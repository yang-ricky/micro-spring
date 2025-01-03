package org.microspring.core;

import org.junit.Test;
import org.microspring.core.aware.BeanNameAware;
import static org.junit.Assert.*;

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
    
    @Test
    public void testBeanLifecycle() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
            
        DefaultBeanDefinition bd = new DefaultBeanDefinition(DemoBean.class);
        bd.setInitMethodName("initMe");
        bd.setDestroyMethodName("cleanup");
        
        beanFactory.registerBeanDefinition("demoBean", bd);
        
        DemoBean bean = (DemoBean) beanFactory.getBean("demoBean");
        assertNotNull(bean);
        
        beanFactory.close();
    }
} 