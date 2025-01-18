package org.microspring.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.DriverManagerDataSource;
import org.microspring.orm.config.OrmConfiguration;
import org.microspring.orm.entity.User;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class HibernateTemplateTest {

    private HibernateTemplate hibernateTemplate;
    private SessionFactory sessionFactory;

    @Before
    public void setUp() {
        // 创建数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        // 创建ORM配置
        OrmConfiguration configuration = new OrmConfiguration(dataSource);
        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        props.setProperty("hibernate.current_session_context_class", "thread");
        configuration.setHibernateProperties(props);
        configuration.setPackagesToScan(User.class.getName());
        configuration.afterPropertiesSet();

        this.sessionFactory = configuration.getSessionFactory();
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    @Test
    public void testBasicCRUD() {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            User user = new User();
            user.setId(1L);
            user.setName("Test User");
            
            hibernateTemplate.save(user);
            
            User found = hibernateTemplate.get(User.class, 1L);
            assertNotNull(found);
            assertEquals("Test User", found.getName());
            
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    public void testFind() {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            User user1 = new User();
            user1.setId(1L);
            user1.setName("Test User 1");
            hibernateTemplate.save(user1);

            User user2 = new User();
            user2.setId(2L);
            user2.setName("Test User 2");
            hibernateTemplate.save(user2);

            List<User> users = hibernateTemplate.find("from User where name like ?1", "%User%");
            assertEquals(2, users.size());
            
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    public void testTransactionRollback() {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            // 保存第一个用户
            User user1 = new User();
            user1.setId(1L);
            user1.setName("Test User 1");
            hibernateTemplate.save(user1);
            
            // 验证用户已保存
            User found = hibernateTemplate.get(User.class, 1L);
            assertNotNull(found);
            assertEquals("Test User 1", found.getName());
            
            // 尝试保存第二个用户，但抛出异常
            User user2 = new User();
            user2.setId(1L);  // 故意使用相同的ID来触发异常
            user2.setName("Test User 2");
            hibernateTemplate.save(user2);  // 这里会抛出异常
            
            tx.commit();
            fail("Should throw exception");
        } catch (Exception e) {
            tx.rollback();
            
            // 验证回滚后数据已被清除
            Session newSession = sessionFactory.getCurrentSession();
            newSession.beginTransaction();
            try {
                User user = hibernateTemplate.get(User.class, 1L);
                assertNull("Transaction should be rolled back, user should not exist", user);
                newSession.getTransaction().commit();
            } catch (Exception ex) {
                newSession.getTransaction().rollback();
                throw ex;
            }
        }
    }
} 