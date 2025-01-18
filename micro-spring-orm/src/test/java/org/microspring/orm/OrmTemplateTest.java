package org.microspring.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.DriverManagerDataSource;
import org.microspring.jdbc.JdbcTemplate;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.orm.OrmTemplate.PropagationBehavior;
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

    @Test
    public void testTransactionIsolation() {
        // 准备初始数据
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);

        // 使用 READ_COMMITTED 隔离级别
        ormTemplate.executeInTransaction((template, session) -> {
            // 直接使用 session 而不是 template
            User found = session.get(User.class, 1L);
            found.setName("Updated in Transaction 1");
            session.update(found);

            // 在同一个会话中验证更新是否可见
            User inTransaction = session.get(User.class, 1L);
            assertEquals("Updated in Transaction 1", inTransaction.getName());

            // 在新会话中验证更新是否可见
            Session newSession = session.getSessionFactory().openSession();
            User fromNewSession = newSession.get(User.class, 1L);
            assertEquals("Original Name", fromNewSession.getName());
            newSession.close();
            return null;
        }, java.sql.Connection.TRANSACTION_READ_COMMITTED);

        // 使用 SERIALIZABLE 隔离级别
        ormTemplate.executeInTransaction((template, session) -> {
            User found = session.get(User.class, 1L);
            found.setName("Updated in Transaction 2");
            session.update(found);
            return null;
        }, java.sql.Connection.TRANSACTION_SERIALIZABLE);
    }

    @Test
    public void testNestedTransactions() {
        // 准备初始数据
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);

        try {
            // 外层事务
            ormTemplate.executeInTransaction((template1, session1) -> {
                User outer = template1.get(User.class, 1L);
                outer.setName("Updated in Outer");
                session1.update(outer);

                try {
                    // 内层事务
                    ormTemplate.executeInTransaction((template2, session2) -> {
                        User inner = template2.get(User.class, 1L);
                        inner.setName("Updated in Inner");
                        session2.update(inner);
                        throw new RuntimeException("Inner transaction rollback");
                    });
                    fail("Should throw exception from inner transaction");
                } catch (RuntimeException e) {
                    // 验证内层事务回滚后，外层事务的修改仍然存在
                    User afterInner = template1.get(User.class, 1L);
                    assertEquals("Updated in Outer", afterInner.getName());
                }

                return null;
            });
        } catch (RuntimeException e) {
            // 验证所有修改都被回滚
            User afterAll = ormTemplate.get(User.class, 1L);
            assertEquals("Original Name", afterAll.getName());
        }
    }

    @Test
    public void testTransactionPropagation_Required() {
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);

        // 外层事务
        ormTemplate.executeInTransaction((template1, session1) -> {
            User outer = session1.get(User.class, 1L);
            outer.setName("Updated in Outer");
            session1.update(outer);

            // 内层事务（REQUIRED）- 应该加入外层事务
            ormTemplate.executeInTransaction((template2, session2) -> {
                User inner = session2.get(User.class, 1L);
                assertEquals("Updated in Outer", inner.getName()); // 可以看到外层事务的修改
                inner.setName("Updated in Inner");
                session2.update(inner);
                return null;
            }, PropagationBehavior.REQUIRED);

            User afterInner = session1.get(User.class, 1L);
            assertEquals("Updated in Inner", afterInner.getName()); // 内层事务的修改在外层可见
            return null;
        });
    }

    @Test
    public void testTransactionPropagation_RequiresNew() {
        // 1. 保存初始数据
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);

        // 2. 外层事务
        ormTemplate.executeInTransaction((template1, session1) -> {
            User outer = session1.get(User.class, 1L);
            outer.setName("Updated in Outer");
            session1.update(outer);

            // 3. 内层事务（REQUIRES_NEW）
            try {
                ormTemplate.executeInTransaction((template2, session2) -> {
                    User inner = session2.get(User.class, 1L);
                    assertEquals("Original Name", inner.getName()); // 应该看到原始数据，因为外层事务还未提交
                    inner.setName("Updated in Inner");
                    session2.update(inner);
                    throw new RuntimeException("Inner transaction rollback");
                }, PropagationBehavior.REQUIRES_NEW);
            } catch (RuntimeException e) {
                // 4. 验证内层事务回滚后的状态
                User afterInner = session1.get(User.class, 1L);
                assertEquals("Updated in Outer", afterInner.getName());
            }
            return null;
        });

        // 5. 验证最终状态
        User afterAll = ormTemplate.get(User.class, 1L);
        assertEquals("Updated in Outer", afterAll.getName());
    }

    @Test
    public void testTransactionPropagation_Supports() {
        User user = new User();
        user.setId(1L);
        user.setName("Original Name");
        ormTemplate.save(user);

        // 在事务中执行
        ormTemplate.executeInTransaction((template1, session1) -> {
            // SUPPORTS - 应该加入当前事务
            ormTemplate.executeInTransaction((template2, session2) -> {
                User found = session2.get(User.class, 1L);
                found.setName("Updated with SUPPORTS");
                session2.update(found);
                return null;
            }, PropagationBehavior.SUPPORTS);

            // 验证修改在同一事务中可见
            User updated = session1.get(User.class, 1L);
            assertEquals("Updated with SUPPORTS", updated.getName());
            return null;
        });

        // 在非事务中执行 - 使用 session 直接操作
        ormTemplate.executeInTransaction((template, session) -> {
            User found = session.get(User.class, 1L);
            found.setName("Updated without Transaction");
            session.update(found);
            return null;
        }, PropagationBehavior.SUPPORTS);
    }
} 