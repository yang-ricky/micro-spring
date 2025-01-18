package org.microspring.transaction.support;

import org.microspring.jdbc.DataSource;
import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.TransactionStatus;
import org.microspring.transaction.IllegalTransactionStateException;
import java.sql.SQLException;
import java.sql.Savepoint;

public abstract class AbstractPlatformTransactionManager {
    
    protected final DataSource dataSource;
    
    public AbstractPlatformTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 根据传播行为开始事务
     */
    public final TransactionStatus getTransaction(TransactionDefinition definition) throws SQLException {
        Object transaction = doGetTransaction();
        
        // 如果当前已存在事务
        if (isExistingTransaction(transaction)) {
            return handleExistingTransaction(definition, transaction);
        }
        
        // 当前不存在事务
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
            throw new IllegalTransactionStateException("No existing transaction found for transaction marked with propagation 'mandatory'");
        }
        
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
            definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
            definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            return startTransaction(definition, transaction);
        }
        
        return new DefaultTransactionStatus(transaction, false, false);
    }
    
    /**
     * 提交事务
     */
    public final void commit(TransactionStatus status) throws SQLException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction is already completed");
        }
        
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        if (defStatus.isRollbackOnly()) {
            rollback(status);
            return;
        }
        
        if (status.isNewTransaction()) {
            doCommit(defStatus);
        }
    }
    
    /**
     * 回滚事务
     */
    public final void rollback(TransactionStatus status) throws SQLException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction is already completed");
        }
        
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        if (status.isNewTransaction()) {
            doRollback(defStatus);
        } else if (defStatus.hasSavepoint()) {
            rollbackToSavepoint(status, defStatus.getSavepoint());
        }
    }
    
    // 以下方法由具体实现类实现
    protected abstract Object doGetTransaction() throws SQLException;
    
    protected abstract boolean isExistingTransaction(Object transaction);
    
    protected abstract void doBegin(Object transaction, TransactionDefinition definition) throws SQLException;
    
    protected abstract void doCommit(DefaultTransactionStatus status) throws SQLException;
    
    protected abstract void doRollback(DefaultTransactionStatus status) throws SQLException;
    
    protected abstract void doSetRollbackOnly(DefaultTransactionStatus status);
    
    protected abstract void doCleanupAfterCompletion(Object transaction);
    
    /**
     * 处理已存在的事务
     */
    private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Object transaction) 
            throws SQLException {
        
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
            // 挂起当前事务并创建新事务
            Object suspendedResources = suspend(transaction);
            DefaultTransactionStatus status = new DefaultTransactionStatus(null, true, true);
            status.setSuspendedResources(suspendedResources);
            try {
                Object newTransaction = doGetTransaction();
                doBegin(newTransaction, definition);
                status.setTransaction(newTransaction);
                TransactionSynchronizationManager.setCurrentTransactionStatus(status);
                return status;
            } catch (SQLException ex) {
                resume(transaction, suspendedResources);
                throw ex;
            }
        }
        
        if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
            DefaultTransactionStatus status = new DefaultTransactionStatus(transaction, false, true);
            status.setSavepoint(createSavepoint((TransactionStatus)status));
            return status;
        }
        
        // PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED
        return new DefaultTransactionStatus(transaction, false, true);
    }
    
    /**
     * 开启新事务
     */
    private TransactionStatus startTransaction(TransactionDefinition definition, Object transaction) 
            throws SQLException {
        
        doBegin(transaction, definition);
        return new DefaultTransactionStatus(transaction, true, true);
    }
    
    /**
     * 开启保存点事务
     */
    private TransactionStatus startSavepointTransaction(TransactionDefinition definition, Object transaction) 
            throws SQLException {
        
        DefaultTransactionStatus status = new DefaultTransactionStatus(transaction, false, true);
        status.setSavepoint(createSavepoint((TransactionStatus)status));
        return status;
    }
    
    /**
     * 创建保存点
     */
    public abstract Savepoint createSavepoint(TransactionStatus status) throws SQLException;
    
    /**
     * 回滚到保存点
     */
    public abstract void rollbackToSavepoint(TransactionStatus status, Savepoint savepoint) throws SQLException;
    
    /**
     * 释放保存点
     */
    public abstract void releaseSavepoint(TransactionStatus status, Savepoint savepoint) throws SQLException;
    
    protected abstract Object suspend(Object transaction) throws SQLException;
    protected abstract void resume(Object transaction, Object suspendedResources) throws SQLException;
} 