package org.microspring.transaction;

/**
 * 事务状态接口，用于追踪当前事务的状态
 */
public interface TransactionStatus {
    
    boolean isNewTransaction();
    
    boolean hasSavepoint();
    
    void setRollbackOnly();
    
    boolean isRollbackOnly();
    
    boolean isCompleted();
} 