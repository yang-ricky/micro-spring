package org.microspring.context.annotation;

import org.junit.Test;
import org.microspring.core.env.StandardEnvironment;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.env.Environment;
import org.microspring.core.type.AnnotatedTypeMetadata;
import org.microspring.core.BeanDefinition;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import static org.junit.Assert.*;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class ProfileTest {

    // 测试组件：数据源配置
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

    // 测试组件：多个 profile
    @Profile({"dev", "test"})
    static class TestDataSource {
        public String getUrl() {
            return "jdbc:h2:mem:test_db";
        }
    }

    @Test
    public void testProfileAnnotation() {
        // 测试默认 profile
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        StandardEnvironment environment = new StandardEnvironment(new String[]{});
        
        // 注册所有组件
        registerBeanDefinition(beanFactory, "devDataSource", DevDataSource.class);
        registerBeanDefinition(beanFactory, "prodDataSource", ProdDataSource.class);
        registerBeanDefinition(beanFactory, "defaultDataSource", DefaultDataSource.class);
        registerBeanDefinition(beanFactory, "testDataSource", TestDataSource.class);

        // 创建 ProfileCondition
        ProfileCondition condition = new ProfileCondition();
        TestConditionContext context = new TestConditionContext(environment, beanFactory);

        // 测试默认 profile
        assertTrue(condition.matches(context, new TestAnnotatedTypeMetadata(DefaultDataSource.class)));
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(DevDataSource.class)));
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(ProdDataSource.class)));
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(TestDataSource.class)));

        // 测试开发环境
        environment = new StandardEnvironment(new String[]{"dev"});
        context = new TestConditionContext(environment, beanFactory);
        assertTrue(condition.matches(context, new TestAnnotatedTypeMetadata(DevDataSource.class)));
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(ProdDataSource.class)));
        assertTrue(condition.matches(context, new TestAnnotatedTypeMetadata(TestDataSource.class)));

        // 测试生产环境
        environment = new StandardEnvironment(new String[]{"prod"});
        context = new TestConditionContext(environment, beanFactory);
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(DevDataSource.class)));
        assertTrue(condition.matches(context, new TestAnnotatedTypeMetadata(ProdDataSource.class)));
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(TestDataSource.class)));

        // 测试测试环境
        environment = new StandardEnvironment(new String[]{"test"});
        context = new TestConditionContext(environment, beanFactory);
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(DevDataSource.class)));
        assertFalse(condition.matches(context, new TestAnnotatedTypeMetadata(ProdDataSource.class)));
        assertTrue(condition.matches(context, new TestAnnotatedTypeMetadata(TestDataSource.class)));
    }

    private void registerBeanDefinition(DefaultBeanFactory beanFactory, String beanName, Class<?> beanClass) {
        BeanDefinition bd = new BeanDefinition() {
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
        };
        beanFactory.registerBeanDefinition(beanName, bd);
    }

    // 测试用的内部类
    private static class TestConditionContext implements ConditionContext {
        private final Environment environment;
        private final DefaultBeanFactory beanFactory;

        public TestConditionContext(Environment environment, DefaultBeanFactory beanFactory) {
            this.environment = environment;
            this.beanFactory = beanFactory;
        }

        @Override
        public DefaultBeanFactory getBeanFactory() {
            return beanFactory;
        }

        @Override
        public Environment getEnvironment() {
            return environment;
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }

    private static class TestAnnotatedTypeMetadata implements AnnotatedTypeMetadata {
        private final Class<?> targetClass;

        public TestAnnotatedTypeMetadata(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public boolean isAnnotated(String annotationName) {
            return targetClass.isAnnotationPresent(Profile.class);
        }

        @Override
        public Map<String, Object> getAnnotationAttributes(String annotationName) {
            Profile profile = targetClass.getAnnotation(Profile.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("value", profile.value());
            return attributes;
        }
    }
} 