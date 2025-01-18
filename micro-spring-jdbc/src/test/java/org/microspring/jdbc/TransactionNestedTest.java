package org.microspring.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import org.microspring.transaction.TransactionStatus;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.ResultSet;
import static org.junit.Assert.*;

public class TransactionNestedTest {
    
    private JdbcTemplate jdbcTemplate;
    private JdbcTransactionManager transactionManager;
    
    @Before
    public void init() {
        // 初始化数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        
        jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(dataSource);
        transactionManager = new JdbcTransactionManager(dataSource);
        jdbcTemplate.setTransactionManager(transactionManager);
        
        // 创建测试表
        try {
            jdbcTemplate.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users3 (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "USERNAME VARCHAR(100)" +
                ")"
            );
            jdbcTemplate.executeUpdate("DELETE FROM users3");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testNestedTransactionWithInnerRollback() throws Exception {
        // 1. 外层事务成功，内层事务回滚到保存点
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Outer1");
        
        Savepoint savepoint = transactionManager.createSavepoint(outerStatus);
        try {
            jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Inner1");
            throw new RuntimeException("Inner transaction error");
        } catch (Exception e) {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
        }
        
        transactionManager.commit(outerStatus);
        
        // 验证：只有外层事务的数据存在
        assertEquals(1, countUsers("Outer1"));
        assertEquals(0, countUsers("Inner1"));
    }
    
    @Test
    public void testNestedTransactionWithOuterRollback() throws Exception {
        // 2. 内层事务成功，但外层事务回滚
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Outer2");
        
        Savepoint savepoint = transactionManager.createSavepoint(outerStatus);
        try {
            jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Inner2");
            // 内层事务成功
        } catch (Exception e) {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
        }
        
        // 外层事务回滚
        transactionManager.rollback(outerStatus);
        
        // 验证：所有数据都被回滚
        assertEquals(0, countUsers("Outer2"));
        assertEquals(0, countUsers("Inner2"));
    }
    
    @Test
    public void testMultiLevelNestedTransaction() throws Exception {
        // 3. 多层嵌套（嵌套多个保存点）
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Level1");
        
        Savepoint savepoint1 = transactionManager.createSavepoint(outerStatus);
        try {
            jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Level2");
            
            Savepoint savepoint2 = transactionManager.createSavepoint(outerStatus);
            try {
                jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Level3");
                throw new RuntimeException("Level 3 error");
            } catch (Exception e) {
                transactionManager.rollbackToSavepoint(outerStatus, savepoint2);
            }
            
            // Level2 的数据应该保留
        } catch (Exception e) {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint1);
        }
        
        transactionManager.commit(outerStatus);
        
        // 验证：Level1 和 Level2 的数据存在，Level3 被回滚
        assertEquals(1, countUsers("Level1"));
        assertEquals(1, countUsers("Level2"));
        assertEquals(0, countUsers("Level3"));
    }
    
    @Test
    public void testSavepointRelease() throws Exception {
        // 4. 保存点的释放
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Before");
        
        Savepoint savepoint = transactionManager.createSavepoint(outerStatus);
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "SavePoint");
        
        // 释放保存点
        transactionManager.releaseSavepoint(outerStatus, savepoint);
        
        try {
            // 尝试回滚到已释放的保存点应该失败
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
            fail("Should not be able to rollback to released savepoint");
        } catch (Exception e) {
            // 预期会抛出异常
        }
        
        transactionManager.commit(outerStatus);
        
        // 验证：所有数据都应该存在
        assertEquals(1, countUsers("Before"));
        assertEquals(1, countUsers("SavePoint"));
    }
    
    @Test
    public void testSavepointReuse() throws Exception {
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Outer");
        
        Savepoint savepoint = transactionManager.createSavepoint(outerStatus);
        
        // 第一次使用保存点
        try {
            jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Inner1");
            throw new RuntimeException("First inner error");
        } catch (Exception e) {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
        }
        
        // 再次使用同一个保存点
        try {
            jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Inner2");
            throw new RuntimeException("Second inner error");
        } catch (Exception e) {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
        }
        
        transactionManager.commit(outerStatus);
        
        assertEquals(1, countUsers("Outer"));
        assertEquals(0, countUsers("Inner1"));
        assertEquals(0, countUsers("Inner2"));
    }
    
    @Test
    public void testNestedTransactionIsolation() throws Exception {
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Outer");
        
        Savepoint savepoint = transactionManager.createSavepoint(outerStatus);
        try {
            // 内层事务看到外层事务的数据
            assertEquals(1, countUsers("Outer"));
            
            jdbcTemplate.executeUpdate("INSERT INTO users3 (USERNAME) VALUES (?)", "Inner");
            // 内层事务看到自己的数据
            assertEquals(1, countUsers("Inner"));
            
            throw new RuntimeException("Inner error");
        } catch (Exception e) {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
        }
        
        // 回滚后只能看到外层事务的数据
        assertEquals(1, countUsers("Outer"));
        assertEquals(0, countUsers("Inner"));
        
        transactionManager.commit(outerStatus);
    }
    
    @Test
    public void testNestedTransactionExceptionHandling() throws Exception {
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        
        // 测试回滚到已释放的保存点
        Savepoint savepoint = transactionManager.createSavepoint(outerStatus);
        transactionManager.releaseSavepoint(outerStatus, savepoint);
        try {
            transactionManager.rollbackToSavepoint(outerStatus, savepoint);
            fail("Should throw exception when rolling back to released savepoint");
        } catch (Exception expected) {
            // expected
        }
        
        // 测试在已提交的事务上创建保存点
        transactionManager.commit(outerStatus);
        try {
            transactionManager.createSavepoint(outerStatus);
            fail("Should throw exception when creating savepoint on committed transaction");
        } catch (Exception expected) {
            // expected
        }
    }
    
    private int countUsers(String username) throws SQLException {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users3 WHERE USERNAME = ?",
            new RowMapper<Integer>() {
                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            },
            username
        );
    }
} 