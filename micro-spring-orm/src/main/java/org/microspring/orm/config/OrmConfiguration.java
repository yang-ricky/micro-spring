package org.microspring.orm.config;

import org.hibernate.SessionFactory;
import org.microspring.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.microspring.jdbc.DataSource;
import java.util.Properties;

public class OrmConfiguration implements InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(OrmConfiguration.class);
    
    private DataSource dataSource;
    private Properties hibernateProperties;
    private String[] packagesToScan;
    private SessionFactory sessionFactory;

    public OrmConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
        this.hibernateProperties = new Properties();
    }

    public void setHibernateProperties(Properties hibernateProperties) {
        this.hibernateProperties.putAll(hibernateProperties);
    }

    public void setPackagesToScan(String... packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void afterPropertiesSet() {
        if (dataSource == null) {
            throw new IllegalStateException("No DataSource found for ORM initialization");
        }

        try {
            // 基础Hibernate配置
            org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
            
            // 合并所有属性
            Properties props = new Properties();
            props.putAll(hibernateProperties);
            // 设置连接属性
            props.put("hibernate.connection.driver_class", "org.h2.Driver");
            props.put("hibernate.connection.url", dataSource.getUrl());
            props.put("hibernate.connection.username", dataSource.getUsername());
            props.put("hibernate.connection.password", dataSource.getPassword());
            // 配置当前session上下文
            props.put("hibernate.current_session_context_class", "thread");
            
            // 设置所有属性
            configuration.setProperties(props);
            
            // 添加所有实体类
            if (packagesToScan != null) {
                for (String packageToScan : packagesToScan) {
                    // 直接添加实体类
                    configuration.addAnnotatedClass(Class.forName(packageToScan));
                }
            }

            sessionFactory = configuration.buildSessionFactory();
            logger.info("micro-spring-orm: SessionFactory created successfully");
            
        } catch (Exception e) {
            logger.error("Failed to create SessionFactory", e);
            throw new RuntimeException("Failed to create SessionFactory", e);
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
} 