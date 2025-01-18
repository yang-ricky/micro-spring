package org.microspring.jdbc.pool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.DriverManagerDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class ConnectionPoolTest {
    
    private DriverManagerDataSource dataSource;
    private SimpleConnectionPool pool;
    
    @Before
    public void init() {
        dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        pool = new SimpleConnectionPool(dataSource, 5, 1000);
    }
    
    @After
    public void cleanup() {
        if (pool != null) {
            pool.shutdown();
        }
    }
    
    @Test
    public void testBasicPoolFunctionality() throws SQLException {
        // 测试获取连接
        Connection conn = pool.getConnection();
        assertNotNull("Should get connection from pool", conn);
        assertEquals("Should have one active connection", 1, pool.getActiveConnections());
        
        // 测试释放连接
        conn.close(); // 这里实际上是调用代理的close方法，会把连接返回池中
        assertEquals("Should have no active connections", 0, pool.getActiveConnections());
        assertEquals("Should have one idle connection", 1, pool.getIdleConnections());
    }
    
    @Test(timeout = 10000)
    public void testPoolUnderLoad() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            List<Future<Long>> futures = new ArrayList<>();
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    long threadStart = System.currentTimeMillis();
                    
                    try (Connection conn = pool.getConnection()) {
                        Thread.sleep(100);
                    } finally {
                        endLatch.countDown();
                    }
                    
                    return System.currentTimeMillis() - threadStart;
                }));
            }
            
            startLatch.countDown();
            endLatch.await(5, TimeUnit.SECONDS);
            
            for (Future<Long> future : futures) {
                long executionTime = future.get(1, TimeUnit.SECONDS);
                assertTrue("Operation took too long", executionTime < 5000);
            }
            
            assertEquals("All connections should be returned to pool", 
                0, pool.getActiveConnections());
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
    
    @Test
    public void testConnectionRetry() {
        // 使用一个不存在的主机来测试重试机制
        dataSource.setUrl("jdbc:h2:tcp://non-existent-host:9092/testdb");
        
        long start = System.currentTimeMillis();
        try {
            pool.getConnection();
            fail("Should throw exception after retries");
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - start;
            // 验证是否进行了3次重试（每次等待1秒）
            assertTrue("Should take at least 3 seconds for retries", duration >= 3000);
            assertTrue(e.getMessage().contains("Failed to create connection after 3 retries"));
        } finally {
            assertEquals("All connections should be closed", 0, pool.getActiveConnections());
        }
    }
    
    @Test
    public void testPoolExhaustion() throws SQLException {
        int maxPoolSize = 5;
        List<Connection> connections = new ArrayList<>();
        
        // 尝试获取超过池大小的连接
        for (int i = 0; i < maxPoolSize; i++) {
            connections.add(pool.getConnection());
        }
        
        // 设置较短的超时时间
        long start = System.currentTimeMillis();
        try {
            pool.getConnection();
            fail("Should throw exception when pool is exhausted");
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - start;
            assertTrue("Should fail fast when pool is exhausted", duration < 2000);
            assertTrue(e.getMessage().contains("Cannot get connection from pool (timeout)"));
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
    public void testPoolShutdown() throws SQLException {
        // 获取一些连接
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();
        
        // 关闭一些连接
        conn1.close();
        conn2.close();
        
        // 关闭连接池
        pool.shutdown();
        
        try {
            pool.getConnection();
            fail("Should not be able to get connection after shutdown");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("Connection pool is shutdown"));
        }
        
        assertEquals("Should have no active connections", 0, pool.getActiveConnections());
        assertEquals("Should have no idle connections", 0, pool.getIdleConnections());
    }
} 