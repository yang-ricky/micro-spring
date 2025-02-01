package org.microspring.context.annotation;

import org.microspring.core.BeanFactoryPostProcessor;
import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.core.env.Environment;
import org.microspring.core.type.AnnotatedTypeMetadata;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * 处理 @Profile 注解的后处理器
 */
public class ProfileBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(DefaultBeanFactory beanFactory) {
        Set<String> beansToRemove = new HashSet<>();
        ProfileCondition condition = new ProfileCondition();
        ConditionContext context = new TestConditionContext(beanFactory);

        // 遍历所有 bean 定义
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            Class<?> beanClass = bd.getBeanClass();
            
            // 检查类上是否有 @Profile 注解
            if (beanClass.isAnnotationPresent(Profile.class)) {
                AnnotatedTypeMetadata metadata = new TestAnnotatedTypeMetadata(beanClass);
                // 如果条件不匹配，将 bean 添加到待移除列表
                if (!condition.matches(context, metadata)) {
                    beansToRemove.add(beanName);
                }
            }
        }

        // 移除不匹配的 bean 定义
        for (String beanName : beansToRemove) {
            beanFactory.removeBeanDefinition(beanName);
        }
    }

    // 内部类实现 ConditionContext 接口
    private static class TestConditionContext implements ConditionContext {
        private final DefaultBeanFactory beanFactory;

        public TestConditionContext(DefaultBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public DefaultBeanFactory getBeanFactory() {
            return beanFactory;
        }

        @Override
        public Environment getEnvironment() {
            return beanFactory.getEnvironment();
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }

    // 内部类实现 AnnotatedTypeMetadata 接口
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