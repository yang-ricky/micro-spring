package org.microspring.orm;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.DriverManagerDataSource;
import org.microspring.jdbc.JdbcTemplate;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.orm.config.OrmConfiguration;
import org.microspring.orm.entity.User;

import java.util.Properties;
import static org.junit.Assert.*;

public class OrmTemplateTest {

    private OrmTemplate ormTemplate;

    @Before
    public void setUp() {
        // 创建数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        // 配置Hibernate
        OrmConfiguration configuration = new OrmConfiguration(dataSource);
        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setHibernateProperties(props);
        configuration.setPackagesToScan(User.class.getName());
        configuration.afterPropertiesSet();
        
        SessionFactory sessionFactory = configuration.getSessionFactory();
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);
        
        HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
        ormTemplate = new OrmTemplate(hibernateTemplate, transactionManager);
    }

    @Test
    public void testSaveAndGet() {
        // 创建并保存用户
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        
        ormTemplate.save(user);
        
        // 查询并验证
        User found = ormTemplate.get(User.class, 1L);
        assertNotNull(found);
        assertEquals("Test User", found.getName());
    }

    @Test
    public void testUpdateAndDelete() {
        // 创建并保存用户
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);
        
        // 更新用户
        user.setName("Updated Name");
        ormTemplate.update(user);
        
        // 验证更新
        User updated = ormTemplate.get(User.class, 1L);
        assertEquals("Updated Name", updated.getName());
        
        // 删除用户
        ormTemplate.delete(updated);
        
        // 验证删除
        User deleted = ormTemplate.get(User.class, 1L);
        assertNull(deleted);
    }
} 