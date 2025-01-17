package org.microspring.jdbc;

import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.TransactionStatus;
import org.microspring.transaction.support.AbstractPlatformTransactionManager;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import java.sql.SQLException;

public class UserService {
    private final JdbcTemplate jdbcTemplate;
    private final AbstractPlatformTransactionManager transactionManager;
    
    public UserService(JdbcTemplate jdbcTemplate, AbstractPlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
    }
    
    public void createUsersInTransaction(String... names) throws SQLException {
        TransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            for (String name : names) {
                // 插入用户，如果名字为"error"则模拟失败
                if ("error".equals(name)) {
                    throw new RuntimeException("Simulated error");
                }
                
                jdbcTemplate.executeUpdate(
                    "INSERT INTO users (name, age) VALUES (?, ?)",
                    name, 20
                );
            }
            
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
    
    public void createUsersWithSavepoint(String[] batch1, String[] batch2) throws SQLException {
        TransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            // 第一批插入
            for (String name : batch1) {
                jdbcTemplate.executeUpdate(
                    "INSERT INTO users (name, age) VALUES (?, ?)",
                    name, 20
                );
            }
            
            // 第二批使用NESTED传播行为
            TransactionDefinition nestedDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);
            TransactionStatus nestedStatus = transactionManager.getTransaction(nestedDef);
            
            try {
                for (String name : batch2) {
                    if ("error".equals(name)) {
                        throw new RuntimeException("Simulated error");
                    }
                    jdbcTemplate.executeUpdate(
                        "INSERT INTO users (name, age) VALUES (?, ?)",
                        name, 20
                    );
                }
                
                transactionManager.commit(nestedStatus);
            } catch (Exception e) {
                transactionManager.rollback(nestedStatus);
                throw e;
            }
            
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
    
    public void createUsersWithPropagation(int propagation, String[] names) throws SQLException {
        TransactionDefinition def = new DefaultTransactionDefinition(propagation);
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            for (String name : names) {
                if ("error".equals(name)) {
                    throw new RuntimeException("Simulated error");
                }
                jdbcTemplate.executeUpdate(
                    "INSERT INTO users (name, age) VALUES (?, ?)",
                    name, 20
                );
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
    
    public void createUsersWithNestedService(String[] outerNames, String[] innerNames, int innerPropagation) 
            throws SQLException {
        // 外层事务
        TransactionDefinition outerDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus outerStatus = transactionManager.getTransaction(outerDef);
        
        try {
            // 插入外层数据
            for (String name : outerNames) {
                jdbcTemplate.executeUpdate(
                    "INSERT INTO users (name, age) VALUES (?, ?)",
                    name, 20
                );
            }
            
            // 内层事务
            TransactionDefinition innerDef = new DefaultTransactionDefinition(innerPropagation);
            TransactionStatus innerStatus = transactionManager.getTransaction(innerDef);
            
            try {
                for (String name : innerNames) {
                    if ("error".equals(name)) {
                        throw new RuntimeException("Simulated error");
                    }
                    jdbcTemplate.executeUpdate(
                        "INSERT INTO users (name, age) VALUES (?, ?)",
                        name, 20
                    );
                }
                transactionManager.commit(innerStatus);
            } catch (RuntimeException e) {
                transactionManager.rollback(innerStatus);
                throw e;  // 总是抛出异常
            }
            
            transactionManager.commit(outerStatus);
        } catch (Exception e) {
            transactionManager.rollback(outerStatus);
            throw e;
        }
    }
} 