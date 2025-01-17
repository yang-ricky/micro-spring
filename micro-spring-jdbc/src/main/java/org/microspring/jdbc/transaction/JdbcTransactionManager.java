package org.microspring.jdbc.transaction;

import org.microspring.jdbc.DataSource;
import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.support.AbstractPlatformTransactionManager;
import org.microspring.transaction.support.DefaultTransactionStatus;
import org.microspring.transaction.support.TransactionSynchronizationManager;
import org.microspring.transaction.support.ConnectionHolder;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

public class JdbcTransactionManager extends AbstractPlatformTransactionManager {
    
    private final ThreadLocal<ConnectionHolder> connectionHolder = new ThreadLocal<>();
    
    public JdbcTransactionManager(DataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    protected Object doGetTransaction() throws SQLException {
        ConnectionHolder holder = connectionHolder.get();
        if (holder == null) {
            holder = new ConnectionHolder(dataSource.getConnection());
            connectionHolder.set(holder);
        }
        return holder;
    }
    
    @Override
    protected boolean isExistingTransaction(Object transaction) {
        ConnectionHolder holder = (ConnectionHolder) transaction;
        return holder.isTransactionActive();
    }
    
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws SQLException {
        ConnectionHolder holder = (ConnectionHolder) transaction;
        if (holder == null) {
            holder = new ConnectionHolder(dataSource.getConnection());
        }
        Connection con = holder.getConnection();
        
        if (definition.isReadOnly()) {
            con.setReadOnly(true);
        }
        
        if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
            con.setTransactionIsolation(definition.getIsolationLevel());
        }
        
        con.setAutoCommit(false);
        holder.setTransactionActive(true);
        connectionHolder.set(holder);
    }
    
    @Override
    protected void doCommit(DefaultTransactionStatus status) throws SQLException {
        ConnectionHolder holder = (ConnectionHolder) status.getTransaction();
        if (holder != null && holder.isTransactionActive()) {
            holder.getConnection().commit();
        }
    }
    
    @Override
    protected void doRollback(DefaultTransactionStatus status) throws SQLException {
        ConnectionHolder holder = (ConnectionHolder) status.getTransaction();
        if (holder != null && holder.isTransactionActive()) {
            holder.getConnection().rollback();
        }
    }
    
    @Override
    protected Savepoint createSavepoint(Object transaction) throws SQLException {
        ConnectionHolder holder = (ConnectionHolder) transaction;
        return holder.getConnection().setSavepoint();
    }
    
    @Override
    protected void rollbackToSavepoint(DefaultTransactionStatus status) throws SQLException {
        Savepoint savepoint = status.getSavepoint();
        ConnectionHolder holder = (ConnectionHolder) status.getTransaction();
        holder.getConnection().rollback(savepoint);
    }
    
    @Override
    protected void releaseSavepoint(DefaultTransactionStatus status) throws SQLException {
        Savepoint savepoint = status.getSavepoint();
        if (savepoint != null) {
            ConnectionHolder holder = (ConnectionHolder) status.getTransaction();
            holder.getConnection().releaseSavepoint(savepoint);
        }
    }
    
    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        status.setRollbackOnly();
    }
    
    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        ConnectionHolder holder = (ConnectionHolder) transaction;
        if (holder != null) {
            try {
                Connection con = holder.getConnection();
                con.setAutoCommit(true);
                con.setReadOnly(false);
                con.close();
            } catch (SQLException ex) {
                // log error
            } finally {
                connectionHolder.remove();
                DefaultTransactionStatus status = TransactionSynchronizationManager.getCurrentTransactionStatus();
                if (status != null && status.getSuspendedResources() != null) {
                    try {
                        resume(null, status.getSuspendedResources());
                    } catch (SQLException e) {
                        // log error
                    }
                }
                TransactionSynchronizationManager.clear();
            }
        }
    }
    
    @Override
    protected Object suspend(Object transaction) throws SQLException {
        ConnectionHolder holder = (ConnectionHolder) transaction;
        holder.getConnection().commit();
        connectionHolder.remove();
        return holder;
    }
    
    @Override
    protected void resume(Object transaction, Object suspendedResources) throws SQLException {
        ConnectionHolder holder = (ConnectionHolder) suspendedResources;
        holder.getConnection().setAutoCommit(false);
        connectionHolder.set(holder);
    }
    
    // 提供给JdbcTemplate使用的方法
    public Connection getCurrentConnection() {
        ConnectionHolder holder = connectionHolder.get();
        return holder != null ? holder.getConnection() : null;
    }
    
    public boolean hasCurrentConnection() {
        return connectionHolder.get() != null;
    }
    
    private DefaultTransactionStatus getCurrentTransactionStatus() {
        return TransactionSynchronizationManager.getCurrentTransactionStatus();
    }
} 