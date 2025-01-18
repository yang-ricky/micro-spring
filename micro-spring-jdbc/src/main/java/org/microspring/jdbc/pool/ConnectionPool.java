package org.microspring.jdbc.pool;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {
    Connection getConnection() throws SQLException;
    void releaseConnection(Connection connection);
    void shutdown();
    int getActiveConnections();
    int getIdleConnections();
} 