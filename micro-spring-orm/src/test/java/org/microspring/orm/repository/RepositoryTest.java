package org.microspring.orm.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.microspring.orm.OrmTemplate;
import org.microspring.orm.HibernateTemplate;
import org.microspring.orm.config.OrmConfiguration;
import org.microspring.orm.entity.User;
import org.microspring.orm.repository.support.RepositoryBeanFactoryPostProcessor;
import org.microspring.jdbc.DriverManagerDataSource;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.*;

public class RepositoryTest {
    
    private UserRepository userRepository;
    private OrmTemplate ormTemplate;
    private static long idCounter;
    
    @Before
    public void setUp() {
        // 设置数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        
        // 配置ORM
        OrmConfiguration configuration = new OrmConfiguration(dataSource);
        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setHibernateProperties(props);
        configuration.setPackagesToScan(User.class.getName());
        configuration.afterPropertiesSet();
        
        // 创建OrmTemplate
        HibernateTemplate hibernateTemplate = new HibernateTemplate(configuration.getSessionFactory());
        this.ormTemplate = new OrmTemplate(hibernateTemplate);
        
        // 扫描并创建Repository
        RepositoryBeanFactoryPostProcessor processor = 
            new RepositoryBeanFactoryPostProcessor("org.microspring.orm.repository", ormTemplate);
        processor.scanAndCreateRepositories();
        
        userRepository = (UserRepository) processor.getRepositories().stream()
            .filter(repo -> repo instanceof UserRepository)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("UserRepository not found"));
        
        // 先清理数据，再重置计数器
        userRepository.deleteAll();
        idCounter = 1;
    }
    
    @After
    public void tearDown() {
        // 清理所有数据
        userRepository.deleteAll();
    }
    
    private synchronized long nextId() {
        return idCounter++;
    }
    
    @Test
    public void testBasicCrud() {
        // 创建用户
        long userId = nextId();  // 先获取ID
        User user = new User();
        user.setId(userId);
        user.setName("Test User");
        
        // 保存
        User saved = userRepository.save(user);
        assertNotNull(saved);
        assertEquals(userId, saved.getId().longValue());  // 使用保存的ID
        
        // 查询
        Optional<User> found = userRepository.findById(userId);  // 使用保存的ID
        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        
        // 更新
        user.setName("Updated User");
        User updated = userRepository.save(user);
        assertEquals("Updated User", updated.getName());
        
        // 删除
        userRepository.deleteById(userId);  // 使用保存的ID
        assertFalse(userRepository.existsById(userId));  // 使用保存的ID
    }

    @Test
    public void testFindByName() {
        // 创建测试数据
        long id1 = nextId();
        User user1 = new User();
        user1.setId(id1);
        user1.setName("Test User");
        userRepository.save(user1);
        
        long id2 = nextId();
        User user2 = new User();
        user2.setId(id2);
        user2.setName("Another User");
        userRepository.save(user2);
        
        // 测试findByName
        List<User> users = userRepository.findByName("Test User");
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("Test User", users.get(0).getName());
    }

    //@Test
    public void testFindByUsername() {
        // 创建测试数据
        User user1 = new User();
        user1.setId(nextId());  // 使用动态ID
        user1.setName("Test User");
        user1.setUsername("testuser");
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setId(nextId());  // 使用动态ID
        user2.setName("Another User");
        user2.setUsername("anotheruser");
        userRepository.save(user2);
        
        // 测试findByUsername
        List<User> users = userRepository.findByUsername("testuser");
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
    }

    //@Test
    public void testFindByNameAndUsername() {
        // 创建测试数据
        User user1 = new User();
        user1.setId(nextId());  // 使用动态ID
        user1.setName("Test User");
        user1.setUsername("testuser");
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setId(nextId());  // 使用动态ID
        user2.setName("Test User");
        user2.setUsername("anotheruser");
        userRepository.save(user2);
        
        // 测试AND查询
        List<User> users = userRepository.findByNameAndUsername("Test User", "testuser");
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        
        // 测试OR查询
        users = userRepository.findByNameOrUsername("Wrong Name", "testuser");
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        
        // 测试复杂查询
        users = userRepository.findByUsernameAndNameOrId("wronguser", "Test User", nextId());
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(nextId(), users.get(0).getId().longValue());
    }
} 