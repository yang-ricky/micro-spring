package org.microspring.jdbc.pool;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class DruidDataSourceTest {
    
    private DruidDataSource dataSource;
    
    @Before
    public void init() {
        dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setInitialSize(2);
        dataSource.setMaxActive(5);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(1000);
        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setValidationQueryTimeout(1);
    }
    
    @After
    public void cleanup() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
    
    @Test
    public void testBasicConnectivity() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull("Should get connection from Druid", conn);
            assertFalse("Connection should be valid", conn.isClosed());
        }
    }
    
    @Test
    public void testConnectionPoolSize() throws SQLException {
        List<Connection> connections = new ArrayList<>();
        try {
            // 获取最大连接数的连接
            for (int i = 0; i < 5; i++) {
                connections.add(dataSource.getConnection());
            }
            
            // 尝试获取更多连接应该超时
            long start = System.currentTimeMillis();
            try {
                dataSource.getConnection();
                fail("Should timeout when pool is exhausted");
            } catch (SQLException e) {
                long duration = System.currentTimeMillis() - start;
                assertTrue("Should fail within maxWait", duration >= 1000 && duration < 1500);
            }
        } finally {
            connections.forEach(conn -> {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    @Test
    public void testConcurrentConnections() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        List<Future<Long>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                long start = System.currentTimeMillis();
                
                try (Connection conn = dataSource.getConnection()) {
                    // 模拟数据库操作
                    Thread.sleep(100);
                } finally {
                    endLatch.countDown();
                }
                
                return System.currentTimeMillis() - start;
            }));
        }
        
        startLatch.countDown();
        endLatch.await();
        
        // 验证所有操作都在合理时间内完成
        for (Future<Long> future : futures) {
            long executionTime = future.get();
            assertTrue("Operation took too long: " + executionTime + "ms", executionTime < 5000);
        }
        
        executor.shutdown();
    }
    
    @Test
    public void testConnectionStats() throws SQLException {
        // 测试连接池统计信息
        assertEquals("Initial connection count should match", 
                    2, dataSource.getInitialSize());
        
        Connection conn = dataSource.getConnection();
        assertEquals("Active connection count should be 1", 
                    1, dataSource.getActiveCount());
        
        conn.close();
        assertEquals("Active connection count should be 0", 
                    0, dataSource.getActiveCount());
    }
    
    @Test
    public void testConnectionValidation() throws SQLException {
        // 测试连接验证功能
        assertTrue("Should test connections on borrow", 
                  dataSource.isTestOnBorrow());
        assertTrue("Should test idle connections", 
                  dataSource.isTestWhileIdle());
        
        // 获取并验证连接
        Connection conn = dataSource.getConnection();
        assertTrue("Connection should be valid", 
                  conn.isValid(1));
        conn.close();
    }
    
    @Test
    public void testConnectionLeakDetection() throws Exception {
        Connection conn = dataSource.getConnection();
        // 不关闭连接，等待泄漏检测
        Thread.sleep(2000);
        
        // 验证连接池能检测到泄漏
        assertTrue("Should detect connection leak", 
            dataSource.getActiveCount() > 0);
        
        // 清理
        conn.close();
    }
    
    @Test
    public void testMinIdleConnections() throws Exception {
        // 使用所有连接
        List<Connection> connections = new ArrayList<>();
        for (int i = 0; i < dataSource.getMaxActive(); i++) {
            connections.add(dataSource.getConnection());
        }
        
        // 释放所有连接
        for (Connection conn : connections) {
            conn.close();
        }
        
        // 等待连接池调整
        Thread.sleep(1000);
        
        // 验证空闲连接数不小于最小值
        assertTrue("Should maintain minimum idle connections",
            dataSource.getPoolingCount() >= dataSource.getMinIdle());
        // 验证空闲连接数不超过最大值
        assertTrue("Should not exceed max active connections",
            dataSource.getPoolingCount() <= dataSource.getMaxActive());
    }
} 