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
import java.util.ArrayList;
import java.util.stream.Collectors;

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

    @Test
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

    @Test
    public void testFindByNameAndUsername() {
        // 创建测试数据
        User user1 = new User();
        user1.setId(nextId());
        user1.setName("Test User");
        user1.setUsername("testuser");
        User saved1 = userRepository.save(user1);
        System.out.println("Saved user1: " + saved1.getId() + ", " + saved1.getName() + ", " + saved1.getUsername());
        
        User user2 = new User();
        user2.setId(nextId());
        user2.setName("Test User");
        user2.setUsername("anotheruser");
        User saved2 = userRepository.save(user2);
        System.out.println("Saved user2: " + saved2.getId() + ", " + saved2.getName() + ", " + saved2.getUsername());
        
        // 测试AND查询
        List<User> users = userRepository.findByNameAndUsername("Test User", "testuser");
        System.out.println("Found users size: " + users.size());
        if (!users.isEmpty()) {
            System.out.println("Found user: " + users.get(0).getId() + ", " + users.get(0).getName() + ", " + users.get(0).getUsername());
        }
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
    }

    @Test
    public void testExtendedQueries() {
        // 创建测试数据
        User user1 = new User();
        user1.setId(nextId());
        user1.setName("Test User");
        user1.setUsername("testuser");
        user1.setAge(25);
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setId(nextId());
        user2.setName("Another User");
        user2.setUsername("anotheruser");
        user2.setAge(30);
        user2.setEmail("test@example.com");
        userRepository.save(user2);
        
        // 测试Like查询
        List<User> users = userRepository.findByNameLike("%Test%");
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("Test User", users.get(0).getName());
        
        // 测试Between查询
        users = userRepository.findByAgeBetween(20, 28);
        assertEquals(1, users.size());
        assertEquals(25, users.get(0).getAge().intValue());
        
        // 测试GreaterThan查询
        users = userRepository.findByAgeGreaterThan(28);
        assertEquals(1, users.size());
        assertEquals(30, users.get(0).getAge().intValue());
        
        // 测试组合查询
        users = userRepository.findByNameLikeAndAgeGreaterThan("%User%", 20);
        assertEquals(2, users.size());
        
        // 测试IsNull查询
        users = userRepository.findByEmailIsNull();
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
    }

    @Test
    public void testSorting() {
        // 创建测试数据
        createTestUsers();  // 创建5个用户
        
        // 测试升序排序
        Sort nameAscSort = Sort.by(Sort.Order.asc("name"));
        List<User> users = userRepository.findByAgeGreaterThan(20, nameAscSort);
        assertNotNull(users);
        assertTrue(users.size() > 1);
        assertTrue(users.get(0).getName().compareTo(users.get(1).getName()) <= 0);
        
        // 测试降序排序
        Sort ageDescSort = Sort.by(Sort.Order.desc("age"));
        users = userRepository.findByAgeGreaterThan(20, ageDescSort);
        assertNotNull(users);
        assertTrue(users.size() > 1);
        assertTrue(users.get(0).getAge() >= users.get(1).getAge());
        
        // 测试多字段排序
        Sort multiSort = Sort.by(
            Sort.Order.desc("age"),
            Sort.Order.asc("name")
        );
        users = userRepository.findByAgeGreaterThan(20, multiSort);
        assertNotNull(users);
        assertTrue(users.size() > 1);
        // 如果年龄相同，则名字应该是升序的
        for (int i = 0; i < users.size() - 1; i++) {
            if (users.get(i).getAge().equals(users.get(i + 1).getAge())) {
                assertTrue(users.get(i).getName().compareTo(users.get(i + 1).getName()) <= 0);
            }
        }
    }

    @Test
    public void testPagination() {
        // 创建测试数据
        createTestUsers();  // 创建5个用户
        
        // 测试分页 - 第一页
        Pageable pageable = Pageable.of(0, 2);
        List<User> users = userRepository.findByNameLike("%User%", pageable);
        assertNotNull(users);
        assertEquals(2, users.size());  // 第一页应该有2条记录
        
        // 测试分页 - 第二页
        pageable = Pageable.of(1, 2);
        users = userRepository.findByNameLike("%User%", pageable);
        assertNotNull(users);
        assertEquals(2, users.size());  // 第二页应该有2条记录
        
        // 测试分页 - 最后一页
        pageable = Pageable.of(2, 2);
        users = userRepository.findByNameLike("%User%", pageable);
        assertNotNull(users);
        assertEquals(1, users.size());  // 最后一页应该只有1条记录
        
        // 测试分页 - 超出范围的页
        pageable = Pageable.of(3, 2);
        users = userRepository.findByNameLike("%User%", pageable);
        assertNotNull(users);
        assertEquals(0, users.size());  // 超出范围的页应该返回空列表
        
        // 测试分页 - 不同的页大小
        pageable = Pageable.of(0, 3);
        users = userRepository.findByNameLike("%User%", pageable);
        assertNotNull(users);
        assertEquals(3, users.size());  // 应该返回3条记录
    }

    @Test
    public void testPaginationWithSorting() {
        // 创建测试数据
        createTestUsers();  // 创建5个用户
        
        // 测试分页和排序组合
        Pageable pageable = Pageable.of(0, 2, Sort.by(Sort.Order.desc("age")));
        List<User> users = userRepository.findByNameLike("%User%", pageable);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.get(0).getAge() >= users.get(1).getAge());  // 验证排序
        assertEquals(24, users.get(0).getAge().intValue());  // 第一页第一条应该是最大年龄
        
        // 验证所有页的数据正确性
        pageable = Pageable.of(1, 2, Sort.by(Sort.Order.desc("age")));
        users = userRepository.findByNameLike("%User%", pageable);
        assertEquals(2, users.size());
        assertEquals(22, users.get(0).getAge().intValue());  // 第二页第一条应该是第三大年龄
        
        pageable = Pageable.of(2, 2, Sort.by(Sort.Order.desc("age")));
        users = userRepository.findByNameLike("%User%", pageable);
        assertEquals(1, users.size());
        assertEquals(20, users.get(0).getAge().intValue());  // 最后一页应该是最小年龄
    }

    @Test
    public void testCustomQuery() {
        // 创建测试数据
        createTestUsers();
        
        // 测试@Query注解的查询
        List<User> users = userRepository.findUsersByCustomQuery("%User%", 22);
        assertNotNull(users);
        assertTrue(users.size() > 0);
        for (User user : users) {
            assertTrue(user.getName().contains("User"));
            assertTrue(user.getAge() > 22);
        }
        
        // 测试带排序的查询
        users = userRepository.findOldestUsers(20);
        assertNotNull(users);
        assertTrue(users.size() > 1);
        // 验证降序排序
        for (int i = 0; i < users.size() - 1; i++) {
            assertTrue(users.get(i).getAge() >= users.get(i + 1).getAge());
        }
        
        // 测试带分页的查询
        Pageable pageable = Pageable.of(0, 2);
        users = userRepository.findUsersByAgeRange(20, 25, pageable);
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public void testBatchOperations() {
        // 准备测试数据
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setId(nextId());
            user.setName("Batch User " + i);
            user.setAge(20 + i);
            users.add(user);
        }
        
        // 测试批量保存
        List<User> savedUsers = userRepository.saveAll(users);
        assertEquals(5, savedUsers.size());
        
        // 测试批量查询
        List<Long> ids = savedUsers.stream()
            .map(User::getId)
            .collect(Collectors.toList());
        List<User> foundUsers = userRepository.findAllById(ids);
        assertEquals(5, foundUsers.size());
        
        // 测试批量删除
        userRepository.deleteAll(savedUsers);
        List<User> remainingUsers = userRepository.findAllById(ids);
        assertTrue(remainingUsers.isEmpty());
    }

    @Test
    public void testBatchOperationsPerformance() {
        // 准备大量测试数据
        List<User> users = new ArrayList<>();
        int batchSize = 100;
        for (int i = 0; i < batchSize; i++) {
            User user = new User();
            user.setId(nextId());
            user.setName("Performance User " + i);
            user.setAge(20 + (i % 50));
            users.add(user);
        }
        
        // 测试批量保存性能
        long startTime = System.currentTimeMillis();
        List<User> savedUsers = userRepository.saveAll(users);
        long endTime = System.currentTimeMillis();
        System.out.println("Batch save " + batchSize + " users took: " + (endTime - startTime) + "ms");
        assertEquals(batchSize, savedUsers.size());
        
        // 测试批量查询性能
        List<Long> ids = savedUsers.stream()
            .map(User::getId)
            .collect(Collectors.toList());
        startTime = System.currentTimeMillis();
        List<User> foundUsers = userRepository.findAllById(ids);
        endTime = System.currentTimeMillis();
        System.out.println("Batch find " + batchSize + " users took: " + (endTime - startTime) + "ms");
        assertEquals(batchSize, foundUsers.size());
    }

    private void createTestUsers() {
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setId(nextId());
            user.setName("Test User " + i);
            user.setAge(20 + i);  // 20, 21, 22, 23, 24
            userRepository.save(user);
        }
    }
} 