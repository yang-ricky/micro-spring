package org.microspring.mybatis;

import org.junit.Test;
import org.microspring.context.support.ClassPathXmlApplicationContext;
import org.microspring.mybatis.annotation.Mapper;

import static org.junit.Assert.*;

public class MyBatisIntegrationTest {
    
    @Mapper
    public static interface UserMapper {
        void createTable();
        void dropTable();
        void insert(User user);
        User findById(Integer id);
    }
    
    public static class User {
        private Integer id;
        private String name;
        
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    public static class UserService {
        private final UserMapper userMapper;
        
        public UserService(UserMapper userMapper) {
            this.userMapper = userMapper;
        }
        
        public void createTable() {
            userMapper.createTable();
        }
        
        public void dropTable() {
            userMapper.dropTable();
        }
        
        public void insert(User user) {
            userMapper.insert(user);
        }
        
        public User findById(Integer id) {
            return userMapper.findById(id);
        }
    }
    
    //@Test
    public void testMyBatisIntegration() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("mybatis-test.xml");
        UserService userService = context.getBean("userService", UserService.class);
        
        userService.createTable();
        
        User user = new User();
        user.setId(1);
        user.setName("test");
        userService.insert(user);
        
        User found = userService.findById(1);
        assertNotNull(found);
        assertEquals(Integer.valueOf(1), found.getId());
        assertEquals("test", found.getName());
        
        userService.dropTable();
    }
    
    //@Test
    public void testTransactionRollback() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("mybatis-test.xml");
        UserService userService = context.getBean("userService", UserService.class);
        
        userService.createTable();
        
        User user = new User();
        user.setId(1);
        user.setName("test");
        userService.insert(user);
        
        try {
            // 插入重复ID，应该触发回滚
            User duplicate = new User();
            duplicate.setId(1);
            duplicate.setName("duplicate");
            userService.insert(duplicate);
            fail("Should throw exception for duplicate key");
        } catch (Exception e) {
            // 预期的异常
        }
        
        User found = userService.findById(1);
        assertNotNull(found);
        assertEquals("test", found.getName()); // 原始数据应该保持不变
        
        userService.dropTable();
    }
}