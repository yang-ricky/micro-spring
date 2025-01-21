package org.microspring.mybatis.transaction;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class MicroSpringTransactionFactory implements TransactionFactory {
    
    @Override
    public Transaction newTransaction(Connection conn) {
        return new MicroSpringTransaction(conn);
    }
    
    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        try {
            Connection conn = dataSource.getConnection();
            if (level != null) {
                conn.setTransactionIsolation(level.getLevel());
            }
            conn.setAutoCommit(autoCommit);
            return new MicroSpringTransaction(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create transaction", e);
        }
    }
    
    @Override
    public void setProperties(Properties props) {
        // 不需要额外属性
    }
    
    private static class MicroSpringTransaction implements Transaction {
        private Connection connection;
        
        MicroSpringTransaction(Connection connection) {
            this.connection = connection;
        }
        
        @Override
        public Connection getConnection() {
            return connection;
        }
        
        @Override
        public void commit() throws SQLException {
            if (connection != null && !connection.getAutoCommit()) {
                connection.commit();
            }
        }
        
        @Override
        public void rollback() throws SQLException {
            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
            }
        }
        
        @Override
        public void close() throws SQLException {
            if (connection != null) {
                connection.close();
            }
        }
        
        @Override
        public Integer getTimeout() {
            return null;
        }
    }
}