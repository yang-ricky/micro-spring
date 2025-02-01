package org.microspring.context.annotation;

import org.junit.Test;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.env.StandardEnvironment;
import org.microspring.core.BeanDefinition;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;

public class ProfileIntegrationTest {

    @Test
    public void testProfileInContext() {
        // 测试默认环境
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        beanFactory.setEnvironment(new StandardEnvironment(new String[]{}));
        
        // 注册所有组件
        registerBean(beanFactory, "devDataSource", DevDataSource.class);
        registerBean(beanFactory, "prodDataSource", ProdDataSource.class);
        registerBean(beanFactory, "defaultDataSource", DefaultDataSource.class);
        registerBean(beanFactory, "testDataSource", TestDataSource.class);

        // 应用 Profile 处理器
        ProfileBeanFactoryPostProcessor processor = new ProfileBeanFactoryPostProcessor();
        processor.postProcessBeanFactory(beanFactory);

        // 测试默认环境
        final DefaultBeanFactory finalBeanFactory1 = beanFactory;
        assertNotNull(finalBeanFactory1.getBean("defaultDataSource"));
        assertThrows(Exception.class, () -> finalBeanFactory1.getBean("devDataSource"));
        assertThrows(Exception.class, () -> finalBeanFactory1.getBean("prodDataSource"));
        assertThrows(Exception.class, () -> finalBeanFactory1.getBean("testDataSource"));

        // 测试开发环境
        beanFactory = new DefaultBeanFactory();
        beanFactory.setEnvironment(new StandardEnvironment(new String[]{"dev"}));
        registerBean(beanFactory, "devDataSource", DevDataSource.class);
        registerBean(beanFactory, "prodDataSource", ProdDataSource.class);
        registerBean(beanFactory, "defaultDataSource", DefaultDataSource.class);
        registerBean(beanFactory, "testDataSource", TestDataSource.class);
        processor.postProcessBeanFactory(beanFactory);

        final DefaultBeanFactory finalBeanFactory2 = beanFactory;
        assertThrows(Exception.class, () -> finalBeanFactory2.getBean("defaultDataSource"));
        assertNotNull(finalBeanFactory2.getBean("devDataSource"));
        assertThrows(Exception.class, () -> finalBeanFactory2.getBean("prodDataSource"));
        assertNotNull(finalBeanFactory2.getBean("testDataSource"));

        // 测试生产环境
        beanFactory = new DefaultBeanFactory();
        beanFactory.setEnvironment(new StandardEnvironment(new String[]{"prod"}));
        registerBean(beanFactory, "devDataSource", DevDataSource.class);
        registerBean(beanFactory, "prodDataSource", ProdDataSource.class);
        registerBean(beanFactory, "defaultDataSource", DefaultDataSource.class);
        registerBean(beanFactory, "testDataSource", TestDataSource.class);
        processor.postProcessBeanFactory(beanFactory);

        final DefaultBeanFactory finalBeanFactory3 = beanFactory;
        assertThrows(Exception.class, () -> finalBeanFactory3.getBean("defaultDataSource"));
        assertThrows(Exception.class, () -> finalBeanFactory3.getBean("devDataSource"));
        assertNotNull(finalBeanFactory3.getBean("prodDataSource"));
        assertThrows(Exception.class, () -> finalBeanFactory3.getBean("testDataSource"));
    }

    private void registerBean(DefaultBeanFactory beanFactory, String beanName, Class<?> beanClass) {
        beanFactory.registerBeanDefinition(beanName, new BeanDefinition() {
            @Override
            public Class<?> getBeanClass() {
                return beanClass;
            }

            @Override
            public String getScope() {
                return "singleton";
            }

            @Override
            public boolean isSingleton() {
                return true;
            }

            @Override
            public String getInitMethodName() {
                return null;
            }

            @Override
            public void setInitMethodName(String initMethodName) {
            }

            @Override
            public String getDestroyMethodName() {
                return null;
            }

            @Override
            public void setDestroyMethodName(String destroyMethodName) {
            }

            @Override
            public List<ConstructorArg> getConstructorArgs() {
                return new ArrayList<>();
            }

            @Override
            public List<PropertyValue> getPropertyValues() {
                return new ArrayList<>();
            }

            @Override
            public void addConstructorArg(ConstructorArg arg) {
            }

            @Override
            public void addPropertyValue(PropertyValue propertyValue) {
            }

            @Override
            public boolean isLazyInit() {
                return false;
            }

            @Override
            public void setLazyInit(boolean lazyInit) {
            }

            @Override
            public boolean isPrimary() {
                return false;
            }

            @Override
            public void setPrimary(boolean primary) {
            }
        });
    }

    // 测试组件
    @Profile("dev")
    static class DevDataSource {
        public String getUrl() {
            return "jdbc:mysql://localhost:3306/dev_db";
        }
    }

    @Profile("prod")
    static class ProdDataSource {
        public String getUrl() {
            return "jdbc:mysql://prod-server:3306/prod_db";
        }
    }

    @Profile("default")
    static class DefaultDataSource {
        public String getUrl() {
            return "jdbc:h2:mem:default_db";
        }
    }

    @Profile({"dev", "test"})
    static class TestDataSource {
        public String getUrl() {
            return "jdbc:h2:mem:test_db";
        }
    }
} 