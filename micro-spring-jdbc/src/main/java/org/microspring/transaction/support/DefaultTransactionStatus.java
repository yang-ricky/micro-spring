package org.microspring.transaction.support;

import org.microspring.transaction.TransactionStatus;
import java.sql.Savepoint;

public class DefaultTransactionStatus implements TransactionStatus {
    
    private Object transaction;
    private final boolean newTransaction;
    private boolean rollbackOnly;
    private boolean completed;
    private final boolean hasTransaction;
    private Savepoint savepoint;
    private Object suspendedResources;
    
    public DefaultTransactionStatus(Object transaction, boolean newTransaction, boolean hasTransaction) {
        this.transaction = transaction;
        this.newTransaction = newTransaction;
        this.hasTransaction = hasTransaction;
    }
    
    @Override
    public boolean isNewTransaction() {
        return newTransaction;
    }
    
    @Override
    public boolean hasSavepoint() {
        return savepoint != null;
    }
    
    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }
    
    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
    
    @Override
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted() {
        this.completed = true;
    }
    
    public Object getTransaction() {
        return transaction;
    }
    
    public void setSavepoint(Savepoint savepoint) {
        this.savepoint = savepoint;
    }
    
    public Savepoint getSavepoint() {
        return savepoint;
    }
    
    public void setSuspendedResources(Object suspendedResources) {
        this.suspendedResources = suspendedResources;
    }
    
    public Object getSuspendedResources() {
        return suspendedResources;
    }
    
    public void setTransaction(Object transaction) {
        this.transaction = transaction;
    }
} 