package org.microspring.jdbc;

import org.junit.Test;
import org.microspring.context.support.AnnotationConfigApplicationContext;
import org.microspring.stereotype.Component;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.transaction.support.TransactionProxyProcessor;
import org.microspring.transaction.support.AbstractPlatformTransactionManager;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import java.sql.SQLException;
import static org.junit.Assert.*;

public class TransactionalServiceTest {
    
    @Component
    public static class TestDataSource extends DriverManagerDataSource {
        public TestDataSource() {
            setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
            setUsername("sa");
            setPassword("");
        }
    }
    
    @Component
    public static class TestJdbcTemplate extends JdbcTemplate {
        @Autowired
        public TestJdbcTemplate(DataSource dataSource) {
            setDataSource(dataSource);
        }
    }
    
    @Component
    public static class TestTransactionManager extends JdbcTransactionManager {
        @Autowired
        public TestTransactionManager(DataSource dataSource, JdbcTemplate jdbcTemplate) {
            super(dataSource);
            jdbcTemplate.setTransactionManager(this);
        }
    }
    
    @Component
    public static class TestTransactionProxyProcessor extends TransactionProxyProcessor {
        @Autowired
        public TestTransactionProxyProcessor(AbstractPlatformTransactionManager transactionManager) {
            super(transactionManager);
        }
    }
    
    @Component
    public static class TestTableInitializer {
        private final JdbcTemplate jdbcTemplate;
        
        @Autowired
        public TestTableInitializer(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            try {
                init();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize database", e);
            }
        }
        
        private void init() throws SQLException {
            jdbcTemplate.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users1 (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "USERNAME VARCHAR(100)" +
                ")"
            );
        }
    }
    
    @Test
    public void testTransactionalAnnotation() throws SQLException {
        AnnotationConfigApplicationContext context = 
            new AnnotationConfigApplicationContext("org.microspring.jdbc");
            
        ITransactionalUserService userService = context.getBean("transactionalUserService", ITransactionalUserService.class);
        try {
            userService.createUsers("Alice", "error");
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        
        // 验证事务回滚
        assertEquals(0, userService.countUsers());
    }
} 