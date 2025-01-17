package org.microspring.transaction;

/**
 * 事务定义接口，包含事务的基本属性
 */
public interface TransactionDefinition {
    
    // 事务传播行为
    int PROPAGATION_REQUIRED = 0;
    int PROPAGATION_SUPPORTS = 1;
    int PROPAGATION_MANDATORY = 2;
    int PROPAGATION_REQUIRES_NEW = 3;
    int PROPAGATION_NOT_SUPPORTED = 4;
    int PROPAGATION_NEVER = 5;
    int PROPAGATION_NESTED = 6;
    
    // 事务隔离级别
    int ISOLATION_DEFAULT = -1;
    int ISOLATION_READ_UNCOMMITTED = 1;
    int ISOLATION_READ_COMMITTED = 2;
    int ISOLATION_REPEATABLE_READ = 4;
    int ISOLATION_SERIALIZABLE = 8;
    
    // 获取传播行为
    int getPropagationBehavior();
    
    // 获取隔离级别
    int getIsolationLevel();
    
    // 获取超时时间
    int getTimeout();
    
    // 是否只读事务
    boolean isReadOnly();
} 