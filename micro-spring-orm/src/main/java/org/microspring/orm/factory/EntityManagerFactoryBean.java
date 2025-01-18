package org.microspring.orm.factory;

import org.microspring.beans.factory.FactoryBean;
import org.microspring.beans.factory.InitializingBean;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.util.Properties;

public class EntityManagerFactoryBean implements FactoryBean<EntityManagerFactory>, InitializingBean {
    
    private EntityManagerFactory emf;
    private DataSource dataSource;
    private Properties jpaProperties;
    private String[] packagesToScan;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setJpaProperties(Properties properties) {
        this.jpaProperties = properties;
    }

    public void setPackagesToScan(String... packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void afterPropertiesSet() {
        Properties props = new Properties();
        
        // 基础配置
        if (jpaProperties != null) {
            props.putAll(jpaProperties);
        }
        
        // 设置Hibernate作为JPA实现
        props.put("javax.persistence.provider", "org.hibernate.jpa.HibernatePersistenceProvider");
        
        // 设置数据源
        if (dataSource != null) {
            props.put("javax.persistence.dataSource", dataSource);
        }
        
        // 设置实体扫描
        if (packagesToScan != null) {
            props.put("hibernate.packagesToScan", String.join(",", packagesToScan));
        }

        this.emf = Persistence.createEntityManagerFactory("micro-spring-jpa", props);
    }

    @Override
    public EntityManagerFactory getObject() {
        return this.emf;
    }

    @Override
    public Class<?> getObjectType() {
        return EntityManagerFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
} 