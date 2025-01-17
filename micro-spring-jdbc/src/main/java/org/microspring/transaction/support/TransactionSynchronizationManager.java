package org.microspring.transaction.support;

public abstract class TransactionSynchronizationManager {
    
    private static final ThreadLocal<DefaultTransactionStatus> currentTransactionStatus = new ThreadLocal<>();
    
    public static void setCurrentTransactionStatus(DefaultTransactionStatus status) {
        currentTransactionStatus.set(status);
    }
    
    public static DefaultTransactionStatus getCurrentTransactionStatus() {
        return currentTransactionStatus.get();
    }
    
    public static void clear() {
        currentTransactionStatus.remove();
    }
} 