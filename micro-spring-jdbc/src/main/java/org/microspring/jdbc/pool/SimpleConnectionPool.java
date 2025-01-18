package org.microspring.jdbc.pool;

import org.microspring.jdbc.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleConnectionPool implements ConnectionPool {
    private final DataSource dataSource;
    private final BlockingQueue<Connection> idleConnections;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final int maxPoolSize;
    private final int maxWaitMillis;
    private volatile boolean shutdown = false;
    
    public SimpleConnectionPool(DataSource dataSource, int maxPoolSize, int maxWaitMillis) {
        this.dataSource = dataSource;
        this.maxPoolSize = maxPoolSize;
        this.maxWaitMillis = maxWaitMillis;
        this.idleConnections = new ArrayBlockingQueue<>(maxPoolSize);
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (shutdown) {
            throw new SQLException("Connection pool is shutdown");
        }
        
        // 先尝试从空闲连接池获取
        Connection conn = idleConnections.poll();
        if (conn != null) {
            if (isConnectionValid(conn)) {
                activeConnections.incrementAndGet();
                return wrapConnection(conn);
            } else {
                // 连接已失效，创建新连接
                closeConnection(conn);
            }
        }
        
        // 如果没有空闲连接，且未达到最大连接数，创建新连接
        if (activeConnections.get() < maxPoolSize) {
            return createNewConnection();
        }
        
        // 等待其他连接释放
        try {
            conn = idleConnections.poll(maxWaitMillis, TimeUnit.MILLISECONDS);
            if (conn != null) {
                if (isConnectionValid(conn)) {
                    activeConnections.incrementAndGet();
                    return wrapConnection(conn);
                } else {
                    closeConnection(conn);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        throw new SQLException("Cannot get connection from pool (timeout)");
    }
    
    private Connection createNewConnection() throws SQLException {
        int retryCount = 3;
        SQLException lastException = null;
        
        while (retryCount > 0) {
            try {
                Connection conn = dataSource.getConnection();
                activeConnections.incrementAndGet();
                return wrapConnection(conn);
            } catch (SQLException e) {
                lastException = e;
                retryCount--;
                try {
                    Thread.sleep(1000); // 等待1秒后重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Connection creation interrupted", ie);
                }
            }
        }
        
        throw new SQLException("Failed to create connection after 3 retries", lastException);
    }
    
    @Override
    public void releaseConnection(Connection connection) {
        if (connection == null) return;
        
        try {
            if (isConnectionValid(connection)) {
                idleConnections.offer(connection);
            } else {
                closeConnection(connection);
            }
        } finally {
            activeConnections.decrementAndGet();
        }
    }
    
    private boolean isConnectionValid(Connection connection) {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // log error
            }
        }
    }
    
    private Connection wrapConnection(final Connection connection) {
        // 包装连接，使其在close时返回池中而不是真正关闭
        return new ConnectionProxy(connection, this);
    }
    
    @Override
    public void shutdown() {
        shutdown = true;
        idleConnections.forEach(this::closeConnection);
        idleConnections.clear();
    }
    
    @Override
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    @Override
    public int getIdleConnections() {
        return idleConnections.size();
    }
} 