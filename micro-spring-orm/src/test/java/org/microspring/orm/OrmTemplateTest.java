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
        props.setProperty("hibernate.current_session_context_class", "thread");
        configuration.setHibernateProperties(props);
        configuration.setPackagesToScan(User.class.getName());
        configuration.afterPropertiesSet();
        
        SessionFactory sessionFactory = configuration.getSessionFactory();
        HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
        ormTemplate = new OrmTemplate(hibernateTemplate);
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

    @Test
    public void testTransactionRollback() {
        // 先保存一个用户
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);
        
        // 验证保存成功
        User saved = ormTemplate.get(User.class, 1L);
        assertNotNull(saved);
        assertEquals("Original Name", saved.getName());
        
        try {
            // 尝试用相同的ID保存另一个用户，这会触发异常
            User duplicateUser = new User();
            duplicateUser.setId(1L);  // 故意使用相同的ID
            duplicateUser.setName("New Name");
            ormTemplate.save(duplicateUser);
            
            fail("Should throw exception when saving duplicate ID");
        } catch (RuntimeException e) {
            // 预期会抛出异常
            // 验证第一个用户仍然存在，且数据未被修改
            User afterRollback = ormTemplate.get(User.class, 1L);
            assertNotNull(afterRollback);
            assertEquals("Original Name", afterRollback.getName());
        }
    }

    @Test
    public void testUpdateRollback() {
        // 先保存一个用户
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);
        
        try {
            ormTemplate.executeInTransaction((template, session) -> {
                User toUpdate = template.get(User.class, 1L);
                toUpdate.setName(null);
                session.update(toUpdate);
                return null;
            });
            fail("Should throw exception when updating with invalid data");
        } catch (RuntimeException e) {
            // 验证原始数据未被修改
            User afterRollback = ormTemplate.get(User.class, 1L);
            assertEquals("Original Name", afterRollback.getName());
        }
    }

    @Test
    public void testDeleteRollback() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        ormTemplate.save(user);
        
        try {
            ormTemplate.executeInTransaction((template, session) -> {
                user.setName("Updated Name");
                session.update(user);
                throw new RuntimeException("Simulated error during delete");
            });
            fail("Should throw exception");
        } catch (RuntimeException e) {
            // 验证更新操作被回滚
            User afterRollback = ormTemplate.get(User.class, 1L);
            assertEquals("Test User", afterRollback.getName());
        }
    }

    @Test
    public void testBatchOperationRollback() {
        // 批量保存多个用户
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setId((long) i);
            user.setName("User " + i);
            ormTemplate.save(user);
        }
        
        try {
            ormTemplate.executeInTransaction((template, session) -> {
                for (int i = 3; i <= 6; i++) {
                    User user = new User();
                    user.setId((long) i);
                    user.setName("Updated User " + i);
                    session.update(user);
                }
                return null;
            });
            fail("Should throw exception when updating non-existent entity in batch");
        } catch (RuntimeException e) {
            // 验证所有原始数据保持不变
            for (int i = 1; i <= 5; i++) {
                User afterRollback = ormTemplate.get(User.class, (long) i);
                assertNotNull(afterRollback);
                assertEquals("User " + i, afterRollback.getName());
            }
        }
    }

    @Test
    public void testQueryDuringTransaction() {
        // 保存初始数据
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        ormTemplate.save(user);
        
        try {
            ormTemplate.executeInTransaction((template, session) -> {
                User found = template.get(User.class, 1L);
                found.setName("Updated in Transaction");
                session.update(found);
                
                User invalid = new User();
                invalid.setId(1L);
                session.save(invalid);
                return null;
            });
            fail("Should throw exception");
        } catch (RuntimeException e) {
            // 验证所有操作都被回滚
            User afterRollback = ormTemplate.get(User.class, 1L);
            assertEquals("Test User", afterRollback.getName());
        }
    }
} 