package org.microspring.orm.repository;

import org.junit.Before;
import org.junit.Test;
import org.microspring.orm.OrmTemplate;
import org.microspring.orm.HibernateTemplate;
import org.microspring.orm.config.OrmConfiguration;
import org.microspring.orm.entity.User;
import org.microspring.orm.repository.support.RepositoryBeanFactoryPostProcessor;
import org.microspring.jdbc.DriverManagerDataSource;

import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.*;

public class RepositoryTest {
    
    private UserRepository userRepository;
    private OrmTemplate ormTemplate;
    
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
    }
    
    @Test
    public void testBasicCrud() {
        // 创建用户
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        
        // 保存
        User saved = userRepository.save(user);
        assertNotNull(saved);
        assertEquals(1L, saved.getId().longValue());
        
        // 查询
        Optional<User> found = userRepository.findById(1L);
        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        
        // 更新
        user.setName("Updated User");
        User updated = userRepository.save(user);
        assertEquals("Updated User", updated.getName());
        
        // 删除
        userRepository.deleteById(1L);
        assertFalse(userRepository.existsById(1L));
    }
} 