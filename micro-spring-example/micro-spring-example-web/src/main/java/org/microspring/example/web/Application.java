package org.microspring.example.web;


import org.microspring.core.DefaultBeanFactory;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.Context;
import org.microspring.core.DefaultBeanDefinition;
import org.microspring.web.context.support.AnnotationConfigWebApplicationContext;
import org.microspring.web.servlet.DispatcherServlet;

import org.microspring.example.web.controller.UserController;

public class Application {
    public static void main(String[] args) throws LifecycleException {
        // 创建并配置 Tomcat
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        
        // 配置临时目录
        String baseDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        tomcat.setBaseDir(baseDir);
        
        // 创建 Context
        String contextPath = "";
        Context context = tomcat.addContext(contextPath, baseDir);
        
        // 创建 BeanFactory 并注册组件
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        
        // 注册 Controller
        beanFactory.registerBeanDefinition("userController", 
            new DefaultBeanDefinition(UserController.class));
        
        // 创建并配置 Spring 上下文
        AnnotationConfigWebApplicationContext applicationContext = 
            new AnnotationConfigWebApplicationContext(beanFactory);
        applicationContext.refresh();
        
        // 创建并注册 DispatcherServlet
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        Tomcat.addServlet(context, "dispatcherServlet", dispatcherServlet);
        context.addServletMappingDecoded("/*", "dispatcherServlet");
        
        // 启动 Tomcat
        tomcat.start();
        System.out.println("Server started on port 8080");
        tomcat.getServer().await();
    }
} 