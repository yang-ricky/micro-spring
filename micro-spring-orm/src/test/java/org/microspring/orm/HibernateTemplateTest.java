package org.microspring.orm;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.DriverManagerDataSource;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.orm.config.OrmConfiguration;
import org.microspring.orm.entity.User;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class HibernateTemplateTest {

    private HibernateTemplate hibernateTemplate;
    private JdbcTransactionManager transactionManager;

    @Before
    public void setUp() {
        // 创建数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        // 创建事务管理器
        transactionManager = new JdbcTransactionManager(dataSource);

        // 创建ORM配置
        OrmConfiguration configuration = new OrmConfiguration(dataSource);
        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setHibernateProperties(props);
        configuration.setPackagesToScan(User.class.getName());
        configuration.afterPropertiesSet();

        SessionFactory sessionFactory = configuration.getSessionFactory();
        hibernateTemplate = new HibernateTemplate(sessionFactory);
        hibernateTemplate.setTransactionManager(transactionManager);
    }

    @Test
    public void testBasicCRUD() {
        // Create
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        hibernateTemplate.save(user);

        // Read
        User found = hibernateTemplate.get(User.class, 1L);
        assertNotNull(found);
        assertEquals("Test User", found.getName());

        // Update
        found.setName("Updated User");
        hibernateTemplate.update(found);
        User updated = hibernateTemplate.get(User.class, 1L);
        assertEquals("Updated User", updated.getName());

        // Delete
        hibernateTemplate.delete(updated);
        User deleted = hibernateTemplate.get(User.class, 1L);
        assertNull(deleted);
    }

    @Test
    public void testFind() {
        // Save some test data
        User user1 = new User();
        user1.setId(1L);
        user1.setName("User 1");
        hibernateTemplate.save(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setName("User 2");
        hibernateTemplate.save(user2);

        // Test HQL query
        List<User> users = hibernateTemplate.find("from User where name like ?1", "User%");
        assertEquals(2, users.size());
    }
} 