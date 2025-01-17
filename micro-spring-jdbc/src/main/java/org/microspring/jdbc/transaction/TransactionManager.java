package org.microspring.jdbc.transaction;

import java.sql.SQLException;

public interface TransactionManager {
    void begin() throws SQLException;
    void commit() throws SQLException;
    void rollback() throws SQLException;
} 