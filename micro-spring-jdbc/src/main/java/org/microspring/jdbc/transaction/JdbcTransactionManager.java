package org.microspring.jdbc.transaction;

import org.microspring.jdbc.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Stack;

public class JdbcTransactionManager implements TransactionManager {
    
    private final DataSource dataSource;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final ThreadLocal<Stack<Savepoint>> savepointStack = new ThreadLocal<>();
    private final ThreadLocal<Integer> savepointCounter = new ThreadLocal<>();
    
    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void begin() throws SQLException {
        int counter = getTransactionCounter();
        if (counter == 0) {
            // 最外层事务，创建新连接
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
            savepointStack.set(new Stack<>());
        } else {
            // 内嵌事务，创建保存点
            Connection conn = connectionHolder.get();
            Savepoint savepoint = conn.setSavepoint("SAVEPOINT_" + counter);
            savepointStack.get().push(savepoint);
        }
        savepointCounter.set(counter + 1);
    }
    
    @Override
    public void commit() throws SQLException {
        int counter = getTransactionCounter() - 1;
        if (counter == 0) {
            // 最外层事务，提交连接
            Connection conn = connectionHolder.get();
            try {
                conn.commit();
                conn.setAutoCommit(true);
            } finally {
                cleanup();
            }
        } else {
            // 内嵌事务，释放保存点
            savepointStack.get().pop();
        }
        savepointCounter.set(counter);
    }
    
    @Override
    public void rollback() throws SQLException {
        int counter = getTransactionCounter() - 1;
        Connection conn = connectionHolder.get();
        if (counter == 0) {
            // 最外层事务，回滚整个连接
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } finally {
                cleanup();
            }
        } else {
            // 内嵌事务，回滚到保存点
            Savepoint savepoint = savepointStack.get().pop();
            conn.rollback(savepoint);
        }
        savepointCounter.set(counter);
    }
    
    private int getTransactionCounter() {
        Integer counter = savepointCounter.get();
        return counter == null ? 0 : counter;
    }
    
    private void cleanup() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            conn.close();
        }
        connectionHolder.remove();
        savepointStack.remove();
        savepointCounter.remove();
    }
    
    public Connection getCurrentConnection() {
        return connectionHolder.get();
    }
    
    public boolean hasCurrentConnection() {
        return connectionHolder.get() != null;
    }
} 