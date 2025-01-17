package org.microspring.jdbc;

import org.microspring.core.BeanPostProcessor;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourcePostProcessor implements BeanPostProcessor {
    
    private static final int MAX_RETRY = 2;
    private static final long RETRY_DELAY = 100; // 100ms
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource) {
            DataSource ds = (DataSource) bean;
            testConnection(ds, beanName);
        }
        return bean;
    }
    
    private void testConnection(DataSource dataSource, String beanName) {
        SQLException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try (Connection conn = dataSource.getConnection()) {
                return; // 连接成功，直接返回
            } catch (SQLException e) {
                lastException = e;
                System.err.println("[WARN] Failed to connect on attempt " + attempt + " of " + MAX_RETRY + 
                                 ": " + e.getMessage());
                System.err.println("[WARN] Connection details: " + 
                                 ((DriverManagerDataSource)dataSource).getConnectionDetails());
                
                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败了，一定要抛出异常
        String errorMsg = "Failed to connect to database after " + MAX_RETRY + " attempts for bean '" + 
                         beanName + "': " + (lastException != null ? lastException.getMessage() : "Unknown error");
        throw new RuntimeException(errorMsg, lastException);
    }
}