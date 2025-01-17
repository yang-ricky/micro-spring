package org.microspring.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.microspring.context.ApplicationContext;
import org.microspring.context.support.ClassPathXmlApplicationContext;
import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.IllegalTransactionStateException;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import java.util.List;
import static org.junit.Assert.*;

public class TransactionTest {
    
    private ApplicationContext context;
    private UserService userService;
    private JdbcTemplate jdbcTemplate;
    
    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext("datasource-test.xml");
        userService = (UserService) context.getBean("userService");
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
        } catch (Exception e) {
            fail("Failed to initialize database: " + e.getMessage());
        }
    }
    
    @Test
    public void testSuccessfulTransaction() throws Exception {
        userService.createUsersInTransaction("Alice", "Bob", "Charlie");
        
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users ORDER BY name",
            (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                return user;
            }
        );
        
        assertEquals(3, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
        assertEquals("Charlie", users.get(2).getName());
    }
    
    @Test
    public void testTransactionRollbackOnLastRecord() throws Exception {
        try {
            // 第三条记录会失败
            userService.createUsersInTransaction("Alice", "Bob", "error");
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // 验证前两条记录也被回滚
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                return user;
            }
        );
        
        assertTrue("All records should be rolled back", users.isEmpty());
    }
    
    @Test
    public void testTransactionRollbackOnMiddleRecord() throws Exception {
        try {
            // 第二条记录会失败
            userService.createUsersInTransaction("Alice", "error", "Charlie");
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // 验证第一条记录也被回滚
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                return user;
            }
        );
        
        assertTrue("First record should be rolled back", users.isEmpty());
    }
    
    @Test
    public void testTransactionWithSavepoint() throws Exception {
        try {
            userService.createUsersWithSavepoint(
                new String[]{"Alice", "Bob"},
                new String[]{"Charlie", "error"}
            );
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // 验证回滚到保存点后的状态
        List<User> users = jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                return user;
            }
        );
        
        assertTrue("All operations should be rolled back", users.isEmpty());
    }
    
    @Test
    public void testPropagationRequired() throws Exception {
        // REQUIRED: 使用现有事务，如果没有则创建新事务
        userService.createUsersWithPropagation(
            TransactionDefinition.PROPAGATION_REQUIRED,
            new String[]{"Alice", "Bob"}
        );
        
        List<User> users = queryUsers();
        assertEquals(2, users.size());
    }
    
    @Test
    public void testPropagationRequiresNew() throws Exception {
        // REQUIRES_NEW: 总是创建新事务，挂起现有事务
        try {
            userService.createUsersWithNestedService(
                new String[]{"Alice", "Bob"},      // 外层事务
                new String[]{"Charlie", "error"},  // 内层事务（会失败）
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
            );
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // 验证外层事务的数据保存了（因为内层是独立的新事务）
        List<User> users = queryUsers();
        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("Bob", users.get(1).getName());
    }
    
    @Test
    public void testPropagationNested() throws Exception {
        // NESTED: 如果有事务则创建嵌套事务（保存点），否则同REQUIRED
        try {
            userService.createUsersWithNestedService(
                new String[]{"Alice", "Bob"},      // 外层事务
                new String[]{"Charlie", "error"},  // 内层事务（会失败）
                TransactionDefinition.PROPAGATION_NESTED
            );
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // 验证外层事务的数据也回滚了
        List<User> users = queryUsers();
        assertTrue(users.isEmpty());
    }
    
    @Test(expected = IllegalTransactionStateException.class)
    public void testPropagationMandatory() throws Exception {
        // MANDATORY: 必须在事务中运行，否则抛异常
        userService.createUsersWithPropagation(
            TransactionDefinition.PROPAGATION_MANDATORY,
            new String[]{"Alice", "Bob"}
        );
    }
    
    @Test
    public void testPropagationSupports() throws Exception {
        // SUPPORTS: 有事务就用事务，没有就不用
        userService.createUsersWithPropagation(
            TransactionDefinition.PROPAGATION_SUPPORTS,
            new String[]{"Alice", "Bob"}
        );
        
        List<User> users = queryUsers();
        assertEquals(2, users.size());
    }
    
    private List<User> queryUsers() throws Exception {
        return jdbcTemplate.query(
            "SELECT * FROM users ORDER BY name",
            (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                return user;
            }
        );
    }
} 