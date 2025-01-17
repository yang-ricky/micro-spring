package org.microspring.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.microspring.context.ApplicationContext;
import org.microspring.context.support.ClassPathXmlApplicationContext;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

public class JdbcTemplateTest {
    
    private ApplicationContext context;
    private JdbcTemplate jdbcTemplate;
    
    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext("datasource-test.xml");
        jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");
        initDatabase();
    }
    
    private void initDatabase() {
        try {
            jdbcTemplate.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(50) NOT NULL," +
                "age INT)"
            );
            
            jdbcTemplate.executeUpdate("DELETE FROM users");
            
            jdbcTemplate.executeUpdate(
                "INSERT INTO users (name, age) VALUES (?, ?), (?, ?)",
                "Tom", 20, "Jerry", 18
            );
        } catch (Exception e) {
            fail("Failed to initialize database: " + e.getMessage());
        }
    }
    
    @Test
    public void testInsert() throws Exception {
        int rows = jdbcTemplate.executeUpdate(
            "INSERT INTO users (name, age) VALUES (?, ?)",
            "Alice", 25
        );
        assertEquals(1, rows);
        
        User user = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE name = ?",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            },
            "Alice"
        );
        
        assertNotNull(user);
        assertEquals("Alice", user.getName());
        assertEquals(Integer.valueOf(25), user.getAge());
    }
    
    @Test
    public void testUpdate() throws Exception {
        // 更新操作测试
        int rows = jdbcTemplate.executeUpdate(
            "UPDATE users SET age = ? WHERE name = ?",
            30, "Tom"
        );
        assertEquals(1, rows);
        
        User user = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE name = ?",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            },
            "Tom"
        );
        
        assertEquals(Integer.valueOf(30), user.getAge());
    }
    
    @Test
    public void testDelete() throws Exception {
        // 删除操作测试
        int rows = jdbcTemplate.executeUpdate(
            "DELETE FROM users WHERE name = ?",
            "Jerry"
        );
        assertEquals(1, rows);
        
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            }
        );
        
        assertEquals(1, users.size());
        assertEquals("Tom", users.get(0).getName());
    }
    
    @Test
    public void testQueryForObjectWithNoResult() throws Exception {
        // 测试查询不存在的数据
        User user = jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE name = ?",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            },
            "NonExistent"
        );
        
        assertNull(user);
    }
    
    @Test(expected = SQLException.class)
    public void testQueryForObjectWithMultipleResults() throws Exception {
        // 测试查询返回多条结果时的异常
        jdbcTemplate.queryForObject(
            "SELECT * FROM users",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            }
        );
    }
    
    @Test(expected = SQLException.class)
    public void testInvalidSQL() throws Exception {
        // 测试无效SQL
        jdbcTemplate.executeUpdate("INVALID SQL");
    }
    
    @Test
    public void testQueryWithEmptyResult() throws Exception {
        // 测试空结果集
        jdbcTemplate.executeUpdate("DELETE FROM users");
        
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            }
        );
        
        assertTrue(users.isEmpty());
    }
    
    @Test
    public void testBatchUpdate() throws Exception {
        // 测试批量更新
        int[] rows = new int[3];
        rows[0] = jdbcTemplate.executeUpdate(
            "INSERT INTO users (name, age) VALUES (?, ?)",
            "User1", 21
        );
        rows[1] = jdbcTemplate.executeUpdate(
            "INSERT INTO users (name, age) VALUES (?, ?)",
            "User2", 22
        );
        rows[2] = jdbcTemplate.executeUpdate(
            "INSERT INTO users (name, age) VALUES (?, ?)",
            "User3", 23
        );
        
        for (int row : rows) {
            assertEquals(1, row);
        }
        
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users WHERE name LIKE 'User%' ORDER BY age",
            (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setAge(rs.getInt("age"));
                return u;
            }
        );
        
        assertEquals(3, users.size());
        assertEquals("User1", users.get(0).getName());
        assertEquals("User2", users.get(1).getName());
        assertEquals("User3", users.get(2).getName());
    }
} 