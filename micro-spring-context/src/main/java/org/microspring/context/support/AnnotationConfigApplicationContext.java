package org.microspring.context.support;

import org.microspring.core.DefaultBeanFactory;
import org.microspring.core.BeanDefinition;
import org.microspring.beans.factory.annotation.Scope;
import org.microspring.beans.factory.annotation.Lazy;
import org.microspring.core.beans.ConstructorArg;
import org.microspring.core.beans.PropertyValue;
import org.microspring.stereotype.Component;
import org.microspring.context.event.ApplicationListener;
import org.microspring.context.event.ContextRefreshedEvent;
import org.microspring.context.event.EventListenerMethodProcessor;
import org.microspring.context.event.SimpleApplicationEventPublisher;
import org.microspring.stereotype.Service;
import org.microspring.stereotype.Repository;
import org.microspring.context.scope.ScopeManager;
import org.microspring.context.scope.ObjectFactory;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class AnnotationConfigApplicationContext extends AbstractApplicationContext {
    private String basePackage;
    private final ScopeManager scopeManager = new ScopeManager();
    
    public AnnotationConfigApplicationContext() {
        super();
    }
    
    public AnnotationConfigApplicationContext(String basePackage) {
        super();
        this.basePackage = basePackage;
        refresh();
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public String getApplicationName() {
        return "AnnotationConfigApplicationContext";
    }

    @Override
    public void refresh() {
        if (basePackage != null) {
            // 1. 扫描组件
            System.out.println("Scanning package: " + basePackage);
            scanPackages(basePackage);
            
            // 2. 注册 BeanPostProcessor 和监听器
            super.refresh();
            
            // 3. 只初始化非延迟加载的单例bean
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                if (bd.isSingleton() && !bd.isLazyInit()) {
                    getBean(beanName);
                }
            }
        }
        
        // 4. 发布刷新完成事件
        publishEvent(new ContextRefreshedEvent(this));
    }

    protected void scanPackages(String... basePackages) {
        try {
            for (String basePackage : basePackages) {
                String path = basePackage.replace('.', '/');
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                
                // 获取类路径下的资源
                Enumeration<URL> resources = classLoader.getResources(path);

                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if (resource != null) {
                        File directory = new File(resource.getFile());
                        if (directory.exists()) {
                            scanDirectory(directory, basePackage);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error scanning packages: " + e.getMessage(), e);
        }
    }

    private void scanDirectory(File directory, String basePackage) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, basePackage + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    try {
                        String className = basePackage + "." + 
                            file.getName().substring(0, file.getName().length() - 6);
                        Class<?> clazz = Class.forName(className);
                        
                        // 检查是否有构造型注解
                        if (clazz.isAnnotationPresent(Component.class) ||
                            clazz.isAnnotationPresent(Service.class) ||
                            clazz.isAnnotationPresent(Repository.class)) {
                            registerBean(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        System.out.println("Warning: Could not load class: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Warning: Error scanning class: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void registerBean(Class<?> clazz) {
        String beanName = null;
        
        // 检查 @Service 注解
        Service serviceAnn = clazz.getAnnotation(Service.class);
        if (serviceAnn != null) {
            beanName = serviceAnn.value();
        }
        
        // 检查 @Repository 注解
        if (beanName == null || beanName.isEmpty()) {
            Repository repositoryAnn = clazz.getAnnotation(Repository.class);
            if (repositoryAnn != null) {
                beanName = repositoryAnn.value();
            }
        }
        
        // 检查 @Component 注解
        if (beanName == null || beanName.isEmpty()) {
            Component componentAnn = clazz.getAnnotation(Component.class);
            if (componentAnn != null) {
                beanName = componentAnn.value();
            }
        }
        
        // 如果没有指定bean名称，使用类名首字母小写作为bean名称
        if (beanName == null || beanName.isEmpty()) {
            beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0)) + 
                      clazz.getSimpleName().substring(1);
        }
        
        // 创建并注册BeanDefinition
        BeanDefinition bd = new BeanDefinition() {
            private boolean lazyInit = false;
            private String initMethodName;
            private String destroyMethodName;
            
            @Override
            public Class<?> getBeanClass() {
                return clazz;
            }
            
            @Override
            public String getScope() {
                Scope scope = clazz.getAnnotation(Scope.class);
                return scope != null ? scope.value() : "singleton";
            }
            
            @Override
            public boolean isSingleton() {
                return "singleton".equals(getScope());
            }
            
            @Override
            public String getInitMethodName() {
                return this.initMethodName;
            }
            
            @Override
            public void setInitMethodName(String initMethodName) {
                this.initMethodName = initMethodName;
            }
            
            @Override
            public String getDestroyMethodName() {
                return this.destroyMethodName;
            }
            
            @Override
            public void setDestroyMethodName(String destroyMethodName) {
                this.destroyMethodName = destroyMethodName;
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
                // 检查@Lazy注解
                Lazy lazy = clazz.getAnnotation(Lazy.class);
                return lazy != null;  // 如果有@Lazy注解，就返回true
            }
            
            @Override
            public void setLazyInit(boolean lazyInit) {
                this.lazyInit = lazyInit;
            }
        };
        beanFactory.registerBeanDefinition(beanName, bd);
    }

    protected void registerEventListenerProcessor() {
        // 直接创建并使用 EventListenerMethodProcessor
        EventListenerMethodProcessor processor = new EventListenerMethodProcessor(this);
        processor.processEventListenerMethods();
    }

    // 添加这个方法以支持测试用例
    public DefaultBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public Object getBean(String name) {
        BeanDefinition bd = beanFactory.getBeanDefinition(name);
        String scope = bd.getScope();
        
        // 对于 request 作用域
        if (Scope.REQUEST.equals(scope)) {
            HttpServletRequest request = scopeManager.getCurrentRequest();
            if (request != null) {
                return scopeManager.getBean(name, scope, (ObjectFactory) () -> super.getBean(name));
            }
        }
        
        // 对于 session 作用域
        if (Scope.SESSION.equals(scope)) {
            HttpSession session = scopeManager.getCurrentSession();
            if (session != null) {
                return scopeManager.getBean(name, scope, (ObjectFactory) () -> super.getBean(name));
            }
        }
        
        // 其他作用域使用父类的 getBean 方法
        return super.getBean(name);
    }

    public ScopeManager getScopeManager() {
        return this.scopeManager;
    }
} 