package org.microspring.transaction.support;

import org.microspring.transaction.TransactionDefinition;

public class DefaultTransactionDefinition implements TransactionDefinition {
    
    private int propagationBehavior = PROPAGATION_REQUIRED;
    private int isolationLevel = ISOLATION_DEFAULT;
    private int timeout = -1;
    private boolean readOnly = false;
    
    public DefaultTransactionDefinition() {
    }
    
    public DefaultTransactionDefinition(int propagationBehavior) {
        this.propagationBehavior = propagationBehavior;
    }
    
    @Override
    public int getPropagationBehavior() {
        return propagationBehavior;
    }
    
    @Override
    public int getIsolationLevel() {
        return isolationLevel;
    }
    
    @Override
    public int getTimeout() {
        return timeout;
    }
    
    @Override
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setPropagationBehavior(int propagationBehavior) {
        this.propagationBehavior = propagationBehavior;
    }
    
    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
} 