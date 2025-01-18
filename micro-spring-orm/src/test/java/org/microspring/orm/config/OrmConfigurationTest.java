package org.microspring.orm.config;

import org.junit.Test;
import org.hibernate.SessionFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import org.microspring.orm.entity.User;

public class OrmConfigurationTest {

    @Test
    public void testBasicOrmConfiguration() {
        // 创建H2测试数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建ORM配置
        OrmConfiguration configuration = new OrmConfiguration(dataSource);
        
        // 设置Hibernate属性
        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        hibernateProperties.setProperty("hibernate.show_sql", "true");
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setHibernateProperties(hibernateProperties);
        
        // 设置实体扫描路径
        configuration.setPackagesToScan(User.class.getName());
        
        // 初始化
        configuration.afterPropertiesSet();
        
        // 验证SessionFactory创建成功
        SessionFactory sessionFactory = configuration.getSessionFactory();
        assertNotNull("SessionFactory should not be null", sessionFactory);
    }
} 