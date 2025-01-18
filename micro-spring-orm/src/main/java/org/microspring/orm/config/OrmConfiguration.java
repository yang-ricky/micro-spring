package org.microspring.orm.config;

import org.hibernate.SessionFactory;
import org.microspring.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
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
            configuration.setProperties(hibernateProperties);
            
            // 设置数据源
            configuration.getProperties().put("hibernate.connection.datasource", dataSource);
            
            // 如果有包扫描路径，添加到配置中
            if (packagesToScan != null) {
                for (String packageName : packagesToScan) {
                    configuration.addPackage(packageName);
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