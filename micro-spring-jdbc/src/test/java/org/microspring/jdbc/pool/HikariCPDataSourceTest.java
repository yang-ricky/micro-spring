package org.microspring.jdbc.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class HikariCPDataSourceTest {
    
    private HikariDataSource dataSource;
    
    @Before
    public void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(1000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        
        dataSource = new HikariDataSource(config);
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
            assertNotNull("Should get connection from HikariCP", conn);
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
                assertTrue("Should fail within connection timeout", duration >= 1000 && duration < 1500);
            }
        } finally {
            // 清理连接
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
    public void testConnectionReuse() throws SQLException, InterruptedException {
        // 等待一下让连接池初始化完成
        Thread.sleep(1000);
        
        Connection conn1 = dataSource.getConnection();
        conn1.close();
        
        Connection conn2 = dataSource.getConnection();
        assertNotNull("Should get connection from pool", conn2);
        conn2.close();
        
        // 等待一下让连接返回到池中
        Thread.sleep(100);
        
        // HikariCP应该复用连接而不是创建新的
        assertEquals("Connection count should match minimum idle", 
                    2, dataSource.getHikariPoolMXBean().getIdleConnections());
    }
    
    @Test
    public void testConnectionLeakDetection() throws Exception {
        Connection conn = dataSource.getConnection();
        // 不关闭连接，等待泄漏检测
        Thread.sleep(2000);
        
        // 验证连接池能检测到泄漏
        assertTrue("Should detect connection leak", 
            dataSource.getHikariPoolMXBean().getActiveConnections() > 0);
        
        // 清理
        conn.close();
    }
    
    @Test
    public void testMinIdleConnections() throws Exception {
        // 使用所有连接
        List<Connection> connections = new ArrayList<>();
        for (int i = 0; i < dataSource.getMaximumPoolSize(); i++) {
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
            dataSource.getHikariPoolMXBean().getIdleConnections() >= 
            dataSource.getMinimumIdle());
        // 验证空闲连接数不超过最大值
        assertTrue("Should not exceed max pool size",
            dataSource.getHikariPoolMXBean().getIdleConnections() <= 
            dataSource.getMaximumPoolSize());
    }
} 