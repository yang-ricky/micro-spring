package org.microspring.jdbc;

import org.microspring.jdbc.transaction.TransactionManager;
import java.sql.SQLException;

public class UserService {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionManager transactionManager;
    
    public UserService(JdbcTemplate jdbcTemplate, TransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
    }
    
    public void createUsersInTransaction(String... names) throws SQLException {
        try {
            transactionManager.begin();
            
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
            
            transactionManager.commit();
        } catch (Exception e) {
            transactionManager.rollback();
            throw e;
        }
    }
    
    public void createUsersWithSavepoint(String[] batch1, String[] batch2) throws SQLException {
        try {
            transactionManager.begin();  // 开始事务
            
            // 第一批插入
            for (String name : batch1) {
                jdbcTemplate.executeUpdate(
                    "INSERT INTO users (name, age) VALUES (?, ?)",
                    name, 20
                );
            }
            
            try {
                transactionManager.begin();  // 创建保存点
                
                // 第二批插入
                for (String name : batch2) {
                    if ("error".equals(name)) {
                        throw new RuntimeException("Simulated error");
                    }
                    jdbcTemplate.executeUpdate(
                        "INSERT INTO users (name, age) VALUES (?, ?)",
                        name, 20
                    );
                }
                
                transactionManager.commit();  // 释放保存点
            } catch (Exception e) {
                transactionManager.rollback();  // 回滚到保存点
                throw e;
            }
            
            transactionManager.commit();  // 提交整个事务
        } catch (Exception e) {
            transactionManager.rollback();  // 回滚整个事务
            throw e;
        }
    }
} 