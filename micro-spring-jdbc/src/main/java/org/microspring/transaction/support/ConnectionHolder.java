package org.microspring.transaction.support;

import java.sql.Connection;

public class ConnectionHolder {
    private final Connection connection;
    private boolean transactionActive;
    
    public ConnectionHolder(Connection connection) {
        this.connection = connection;
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public boolean isTransactionActive() {
        return transactionActive;
    }
    
    public void setTransactionActive(boolean transactionActive) {
        this.transactionActive = transactionActive;
    }
} 