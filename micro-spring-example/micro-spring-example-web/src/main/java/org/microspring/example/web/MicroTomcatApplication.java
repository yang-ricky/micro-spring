package org.microspring.example.web;


import org.microspring.core.DefaultBeanFactory;

import java.io.File;

import org.microspring.core.DefaultBeanDefinition;
import org.microspring.web.context.support.AnnotationConfigWebApplicationContext;
import org.microspring.web.servlet.DispatcherServlet;

import com.microtomcat.MicroTomcat;
import com.microtomcat.container.Context;

import org.microspring.example.web.controller.UserController;


public class MicroTomcatApplication {
    public static void main(String[] args) {
        try {
            // 创建并配置 MicroTomcat
            MicroTomcat tomcat = new MicroTomcat(8080);
            
            // 配置临时目录
            String baseDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
            
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
            tomcat.addServlet(context, "dispatcherServlet", dispatcherServlet);
            tomcat.addServletMappingDecoded(context, "/*", "dispatcherServlet");
            
            context.addFilter("authFilter", new AuthenticationFilter());
            context.addFilterMapping("/*", "authFilter");

            // 启动服务器
            tomcat.start();
            System.out.println("Server started on port 8080");
            
            // 等待服务器停止
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}