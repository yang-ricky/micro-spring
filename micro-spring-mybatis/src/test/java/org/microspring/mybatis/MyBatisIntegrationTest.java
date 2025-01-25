package org.microspring.mybatis;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.mybatis.test.model.User;
import org.microspring.mybatis.test.model.UserService;

import static org.junit.Assert.*;

public class MyBatisIntegrationTest {
    
    @Test
    public void testMyBatisIntegration() {
        AnnotationConfigApplicationContext context = 
        new AnnotationConfigApplicationContext("org.microspring.mybatis");
        
        UserService userService = context.getBean("userService", UserService.class);
        
        userService.init();  // 创建表
        
        // 创建用户
        User user = userService.createUser("test", "test123");
        assertNotNull("User ID should be generated", user.getId());
        
        // 测试查询
        User found = userService.findById(user.getId());
        assertNotNull("Should find user by ID", found);
        assertEquals("test", found.getUsername());
        assertEquals("test123", found.getPassword());
        
        // 测试更新
        found.setUsername("updated");
        found.setPassword("updated123");
        userService.updateUser(found);
        
        User updated = userService.findById(user.getId());
        assertEquals("updated", updated.getUsername());
        assertEquals("updated123", updated.getPassword());
        
        // 测试删除
        userService.deleteUser(user.getId());
        assertNull(userService.findById(user.getId()));
        
        userService.dropTable();
        context.close();
    }
}