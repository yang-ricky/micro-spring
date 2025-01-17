package org.microspring.transaction;

public class IllegalTransactionStateException extends RuntimeException {
    
    public IllegalTransactionStateException(String message) {
        super(message);
    }
    
    public IllegalTransactionStateException(String message, Throwable cause) {
        super(message, cause);
    }
} 